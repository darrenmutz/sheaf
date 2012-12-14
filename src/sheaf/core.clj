;; Copyright (c) 2012, Darren Mutz
;; All rights reserved.
;;
;; Redistribution and use in source and binary forms, with or without
;; modification, are permitted provided that the following conditions
;; are met:
;;
;;   * Redistributions of source code must retain the above copyright
;;     notice, this list of conditions and the following disclaimer.
;;   * Redistributions in binary form must reproduce the above
;;     copyright notice, this list of conditions and the following
;;     disclaimer in the documentation and/or other materials provided
;;     with the distribution.
;;   * Neither the name of Darren Mutz nor the names of its
;;     contributors may be used to endorse or promote products derived
;;     from this software without specific prior written permission.
;;
;; THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
;; "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
;; LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
;; FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
;; COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
;; INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
;; (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
;; SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
;; HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
;; STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
;; ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
;; OF THE POSSIBILITY OF SUCH DAMAGE.

(ns ^{:author "Darren Mutz"
      :see-also
      [["https://github.com/darrenmutz/sheaf" "Source code"]]}
  sheaf.core
  (:import com.petebevin.markdown.MarkdownProcessor)
  (:import org.joda.time.DateTime)
  (:import org.joda.time.DateTimeZone)
  (:import org.joda.time.format.DateTimeFormatterBuilder)
  (:import org.joda.time.format.ISODateTimeFormat)
  (:gen-class)
  (:use clojure.java.io)
  (:use clojure.tools.cli)
  (:use [clojure.data.json :only (json-str write-json read-json)])
  (:use [net.cgrand.enlive-html :only
         (at deftemplate defsnippet content emit* set-attr do->
          first-child html-resource select nth-of-type any-node
          text-node html-content)])
  (:use hiccup.core)
  (:use hiccup.page)
  (:use hiccup.util))

(load-file (str (System/getProperty (str "user.home")) "/.sheaf"))

(def ^:dynamic *template-url*
  (str "file:///" (*config* :sheaf-root) "/" (*config* :template-file)))

(def ^:dynamic *archive-root*
  (str (*config* :sheaf-root) "/" (*config* :archive-dir)))

(defn create-metadata [archive article]
  (sort #(compare (%2 :publish-time) (%1 :publish-time)) (cons article archive)))

(defn delete-metadata [archive slug]
  (remove #(= (% :slug) slug) archive))

(defn get-archive-filename [month year]
  (str *archive-root* "/" month "-" year ".json"))

(defn read-archive [filename]
  (try
    (seq (read-json (slurp filename)))
    (catch java.io.FileNotFoundException e [])))
  
(defn article-exists? [archive slug]
  (contains? (apply sorted-set (map :slug archive)) slug))

(defn get-article-metadata [archive slug]
  (first (filter #(= (% :slug) slug) archive)))

(defn fetch-content [url]
  (html-resource (java.net.URL. url)))

(defn long-form-date [datetime]
  (-> (DateTimeFormatterBuilder.)
      (.appendMonthOfYearText)
      (.appendLiteral " ")
      (.appendDayOfMonth 1)
      (.appendLiteral ", ")
      (.appendYear 4 4)
      (.toFormatter)
      (.print datetime)))

(def ^:dynamic *link-sel* [[:.archive-list (nth-of-type 1)] :> first-child])

(defn hexify [s]
  (apply str (map #(format "%02x" %) (.getBytes s "UTF-8"))))

(defn unhexify [s]
  (let [bytes (into-array Byte/TYPE
                          (map (fn [[x y]]
                                 (unchecked-byte (Integer/parseInt (str x y) 16)))
                               (partition 2 s)))]
    (String. bytes "UTF-8")))

(defsnippet link-model (fetch-content *template-url*) *link-sel*
  [{:keys [month year]}]
  [:a] (do->
        (content (str month " " year))
        (set-attr :href (str "/" month "-" year))))

(defn elipsis-glyphs
  "Convert triple periods to precomposed elipsis glyphs."
  [s]
  (clojure.string/replace #"\.\.\." "&#8230;"))

(defn long-dashes
  "Convert all double dashes to en dashes and triple dashes to em
  dashes. Leave single dashes alone."
  [s]
  (-> s
      (clojure.string/replace #"---" "&#8212;")
      (clojure.string/replace #"--" "&#8211;")))

(defn curly-single-quote
  "Naively convert all single quotes to right single quotation marks,
  as though all single quotation marks appear in an English
  contraction or possessive. Ignores matching and all other context."
  [s]
  (clojure.string/replace s #"'" "&#8217;"))

(defn smart-quote
  "Naive smart quoter. Turns typewriter double quotes into curly
   matched double quotes on a best effort basis. Assumes all double
   quotes should be transformed and that they appear perfectly
   balanced in the input. No attempt is made to reason about the
   interation with existing curly quotes."
  [s]
  (let [tokens (clojure.string/split s #"\"")]
    (apply str (map #(%1 %2) (cycle [identity #(str "&#8220;" % "&#8221;")]) tokens))))

(deftemplate article-template (fetch-content *template-url*)
  [article title datetime permalink link]
  (*config* :articles-selector)   (apply content article)
  (*config* :page-title-selector) (content title)
  (*config* :title-selector)      (content title)
  (*config* :title-selector)      (set-attr :href (if link link permalink))
  (*config* :title-selector)      (set-attr :id (if link "link" "permalink"))
  (*config* :time-selector)       (content (long-form-date datetime))
  (*config* :time-selector)       (set-attr "datetime" (.toString datetime))
  (*config* :permalink-selector)  (set-attr "href" permalink)
  (*config* :archives-selector)   nil)

(deftemplate index-template (fetch-content *template-url*)
  [page-title articles archive-month-years]
  (*config* :page-title-selector)     (content page-title)
  (*config* :index-articles-selector) (apply content articles)
  (*config* :archive-list-selector)   (content (map link-model
                                                    archive-month-years)))

(defn try-write [filename content]
  (do
    (.mkdirs (java.io.File. (.getParent (java.io.File. filename))))
    (try
      (do
        (spit filename content)
        (println "Wrote" filename))
      (catch java.io.FileNotFoundException e
        (do
          (println "Couldn't write to" filename)
          (. System (exit 1)))))))

(defn delete-by-filename [filename]
  (if (.delete (java.io.File. filename))
    (do
      (println "Deleted" filename)
      true)
    (println "Failed to delete" filename)))

(defn get-year-month-day-millis [datetime]
  (hash-map :year       (.getYear datetime)
            :month      (.getAsShortText (.monthOfYear datetime))
            :month-full (.getAsText (.monthOfYear datetime))
            :month-num  (.getMonthOfYear datetime)
            :day        (.getDayOfMonth datetime)
            :millis     (.getMillis datetime)))

(defn get-relative-path [month year slug]
  (str month "-" year "/" slug ".html"))

(defn get-publish-time [archive slug]
  (loop [remainder archive]
    (if (empty? remainder)
      nil
      (if (= ((first remainder) :slug) slug)
        ((first remainder) :publish-time)
        (recur (rest remainder))))))

(defn write-article [relative-path article-content title publish-time link archive slug]
  (try-write (str (*config* :doc-root) "/" relative-path)
             (apply str (article-template
                         article-content
                         title
                         publish-time
                         (str "/" relative-path)
                         link))))
  
(defn unlink-article [relative-path]
  (let [target-filename (str (*config* :doc-root) "/" relative-path)]
    (if (.delete (java.io.File. target-filename))
      (do
        (println "Deleted" target-filename)
        true)
      (println "Failed to delete" target-filename))))

(defn publish-article [now slug title article-content link]
  (let [ymdm (get-year-month-day-millis now)
        year (ymdm :year)
        month (ymdm :month)
        millis (ymdm :millis)
        archive-filename (get-archive-filename month year)
        archive (read-archive archive-filename)]
    (if (article-exists? archive slug)
      (println "Can't publish an article that already exists.")
      (let [relative-path (get-relative-path month year slug)
            encoded-title (hexify title)]
        (try-write archive-filename
                   (json-str (create-metadata archive
                                              {:slug slug
                                               :publish-time millis
                                               :relative-path relative-path
                                               :title encoded-title})))
        (write-article relative-path article-content title now link archive slug)
        true))))

(defn revise-article [month year slug title article-content link now]
  (let [archive-filename (get-archive-filename month year)
        archive (read-archive archive-filename)
        revision-time (:millis (get-year-month-day-millis now))]
    (if (article-exists? archive slug)
      (let [orig-metadata (get-article-metadata archive slug)
            relative-path (get-relative-path month year slug)
            encoded-title (hexify title)
            updated-archive (create-metadata (delete-metadata archive slug)
                                             (assoc orig-metadata
                                               :revision-time revision-time
                                               :title encoded-title))]
        (try-write archive-filename (json-str updated-archive))
        (write-article relative-path article-content title (get-publish-time archive slug)
                       link archive slug)
        true)
      (println "Can't revise an article that doesn't exist."))))

(defn delete-article [month year slug]
  (let [archive-filename (get-archive-filename month year)
        archive (read-archive archive-filename)]
    (if (article-exists? archive slug)
      (if-let [metadata (get-article-metadata archive slug)]
        (do
          (if (unlink-article (metadata :relative-path))
            (let [lighter-archive (delete-metadata archive slug)]
              (if (empty? lighter-archive)
                (delete-by-filename archive-filename)
                (try-write archive-filename
                           (json-str (delete-metadata archive slug))))
              true))))
      (println "Can't delete an article that doesn't exist."))))

(defn datetime-from-month-year [month-year-string]
  (if-let [[match month year] (re-find #"([A-Z][a-z]+)-([0-9]+)"
                                       month-year-string)]
    (let [builder (DateTimeFormatterBuilder.)
          custom-builder (.builder
                          (.appendYear
                           (.appendLiteral
                            (.appendMonthOfYearShortText builder)) "-") 4 4)
          formatter (.toFormatter custom-builder)]
      (.parseDateTime formatter month-year-string)))
  nil)

(defn dir-list [dir]
  (apply vector (.list (java.io.File. dir))))

(defn datetime-from-month-year [month year]
  (-> (DateTimeFormatterBuilder.)
      (.appendMonthOfYearShortText)
      (.appendLiteral "-")
      (.appendYear 4 4)
      (.toFormatter)
      (.parseDateTime (str month "-" year))))

(defn annotated-archive-from-file [filename]
  (let [archive-regex #"([A-Z][a-z]+)-([0-9]+).json"]
    (if-let [[filename month year] (re-find archive-regex filename)]
      (hash-map :filename (str *archive-root* "/" filename)
                :datetime (datetime-from-month-year month year)
                :month month
                :year year))))

(defn archives-to-seq
  ([archives]
     (archives-to-seq nil archives))
  ([articles archives]
     (lazy-seq
      (if (first articles)
        (cons (first articles) (archives-to-seq (rest articles) archives))
        (if (first archives)
          (let [articles (read-archive (:filename (first archives)))]
            (cons (first articles) (archives-to-seq (rest articles)
                                                    (rest archives))))
          nil)))))

(defn get-desc-sorted-archives []
  (sort #(compare (%2 :datetime) (%1 :datetime))
        (map annotated-archive-from-file (dir-list *archive-root*))))

(defn generate-index [articles target-dir page-title archive-month-years]
  (if (empty? articles)
    (println "Not generating an empty index.")
    (let [article-urls (map #(str "file:///" (*config* :doc-root) "/"
                                  (% :relative-path)) articles)
          article-contents (map fetch-content article-urls)
          article-nodes (map #(select % (*config* :article-selector))
                             article-contents)]
      (if (and (not (empty? article-nodes)) (not (empty? archive-month-years)))
        (try-write (str target-dir "/index.html")
                   (apply str (index-template page-title article-nodes archive-month-years)))
        (println "Not generating empty index.")))))

(defn escaped-article-content-from-url [url]
  (escape-html
   (apply str
          (emit* (-> (html-resource (java.net.URL. url))
                     (select (*config* :input-article-selector)))))))

(defn epoch-to-utc-timestamp [epoch-time]
  (if epoch-time
    (-> (ISODateTimeFormat/dateTime)
        (.print (DateTime. epoch-time (DateTimeZone/UTC))))
    nil))

(defn atom-entry [title link publish-time revision-time content]
  (let [publish-ymdm (get-year-month-day-millis (DateTime. publish-time))
        datestamp (apply str
                         (interpose \-
                                    ((juxt #(format "%04d" (:year %))
                                           #(format "%02d" (:month-num %))
                                           #(format "%02d" (:day %)))
                                     publish-ymdm)))
        [publish-utc-timestamp revision-utc-timestamp]
        (map epoch-to-utc-timestamp [publish-time revision-time])]
    [:entry
     [:title title]
     [:link {:rel "alternate" :type "text/html" :href link}]
     [:id (str "tag:" (*config* :base-url) "," datestamp ":/" publish-time)]
     [:published publish-utc-timestamp]
     [:updated (if revision-utc-timestamp revision-utc-timestamp publish-utc-timestamp)]
     [:author
      [:name (*config* :author-name)]
      [:uri (*config* :author-uri)]]
     [:content {:type "html"} content]]))

(defn generate-atom [articles target-dir now]
  (if (empty? articles)
    (println "Not generating an empty index.")
    (let [local-article-urls (map #(str "file:///" (*config* :doc-root)
                                        (% :relative-path)) articles)
          article-contents (map escaped-article-content-from-url local-article-urls)
          absolute-article-urls (map #(str "http://" (*config* :base-url) "/"
                                           (% :relative-path)) articles)
          titles (map #(unhexify (:title %)) articles)
          publish-times (map :publish-time articles)
          revision-times (map :revision-time articles)
          atom-entries (map atom-entry
                            titles
                            absolute-article-urls
                            publish-times
                            revision-times
                            article-contents)
          latest-update (apply max
                               (remove #(nil? %)
                                       (reduce conj
                                               publish-times
                                               revision-times)))]
      (try-write
       (str target-dir (*config* :atom-filename))
       (apply
        str
        (html (xml-declaration "utf-8")
              [:feed {:xmlns "http://www.w3.org/2005/Atom"}
               [:title (*config* :blog-title)]
               [:subtitle (*config* :blog-subtitle)]
               [:link {:rel "alternate"
                       :type "text/html"
                       :href (str "http://" (*config* :base-url) "/")}]
               [:link {:rel "self"
                       :type "application/atom+xml"
                       :href (str "http://" (*config* :base-url) "/"
                                  (*config* :atom-filename))}]
               [:id (*config* :feed-id)]
               [:updated (epoch-to-utc-timestamp latest-update)]
               [:rights
                (format
                 "Copyright © %d, %s"
                 (:year (get-year-month-day-millis (DateTime. latest-update)))
                 (*config* :author-name))]
               atom-entries]))))))

(defn generate-indices [for-datetime max-root-articles]
  (let [ymdm (get-year-month-day-millis for-datetime)
        year (str (ymdm :year))
        month (ymdm :month)
        month-full (ymdm :month-full)
        sorted-archives (get-desc-sorted-archives)
        target-month-archive (filter #(and (= year (:year %)) (= month (:month %)))
                                     sorted-archives)
        target-month-articles (archives-to-seq target-month-archive)
        archive-month-years (map #(select-keys % [:month :year])
                                 sorted-archives)
        target-month-articles-asc (sort #(compare (%1 :publish-time)
                                                  (%2 :publish-time))
                                        target-month-articles)
        all-articles (archives-to-seq sorted-archives)]
    (generate-index target-month-articles-asc
                    (str (*config* :doc-root) "/" month "-" year)
                    (str month-full " " year " - " (*config* :blog-title))
                    target-month-archive)
    (generate-index (take max-root-articles all-articles)
                    (*config* :doc-root)
                    (*config* :blog-title)
                    archive-month-years)
    (generate-atom (take max-root-articles all-articles) (*config* :doc-root)
                   for-datetime)))

(defn get-article-content [input-filename]
  (let [uri (str "file://" input-filename)]
    ;; If the input looks like it contains markdown, convert it to html
    (if (re-seq #"^*.md$" uri)
      (let [markdown (.markdown (MarkdownProcessor.) (slurp (input-stream (java.net.URL. uri))))]
        (-> markdown
            smart-quote
            curly-single-quote
            long-dashes
            (.getBytes "UTF-8")
            (java.io.ByteArrayInputStream.)
            (java.io.InputStreamReader.)
            html-resource
            ))
      (select (fetch-content uri) (*config* :input-article-selector)))))

(defn usage-and-exit [usage]
  (do
    (println usage)
    (. System (exit 1))))

(defn -main [& args]
  (let [[options args usage]
        (cli args
             ["-p" "--publish" "Publish an article" :flag true]
             ["-r" "--revise"  "Revise an article" :flag true]
             ["-d" "--delete"  "Delete an article" :flag true]
             ["-m" "--month"   "Month an article to revise was published in"]
             ["-y" "--year"    "Year an article to revise was published in"]
             ["-s" "--slug"    "Article slug, ex: my-article-title"]
             ["-t" "--title"   "The article's title"]
             ["-l" "--link"    "Title links externally link, ex: \"http://www.noaa.gov\""]
             ["-a" "--article" "File containing an article written in markdown or HTML, 
                                       ex: path/to/article.html or path/to/another-article.md.
                                       Markdown articles are styled typographically. For example,
                                       quotes and dashes in markdown input are, respectively,
                                       converted to curly and long versions in an opinionated
                                       way. HTML articles are considered raw and not similarly
                                       styled."]
             ["-w" "--watch"   "Optionally watch an input while revising." :flag true]
             ["-h" "--help"    "Display usage."])
        now (DateTime.)
        publish (options :publish)
        revise (options :revise)
        delete (options :delete)
        month (options :month)
        year (options :year)
        slug (options :slug)
        title (options :title)
        link (options :link)
        article (options :article)
        watch (options :watch)
        help (options :help)]
      (if (not (or publish revise delete help))
          (usage-and-exit usage))
      (if delete
        (if (and slug month year)
          (if (delete-article month year slug)
            (generate-indices (datetime-from-month-year month year)
                              (*config* :max-home-page-articles)))
          (do
            (println "Delete requires options slug, month and year.")
            (usage-and-exit usage)))
        (if revise
          (if (and slug title article)
            (loop [previous-modified 0]
              (let [current-modified (.lastModified (java.io.File. article))]
                (if (not (= previous-modified current-modified))
                  (do
                    (if (revise-article month year slug title (get-article-content article) link now)
                      (generate-indices (datetime-from-month-year month year)
                                        (*config* :max-home-page-articles)))
                    (if watch (recur current-modified)))
                  (do
                    (Thread/sleep 200)
                    (recur previous-modified)))))
            (do
              (println (str "Revise requires slug, title, article, month and year. "
                            "If you're revising a link post, include the link option, too. "))
              (usage-and-exit usage)))
          (if publish
            (if (and slug title article)
              (if (publish-article now slug title (get-article-content article) link)
                (generate-indices now (*config* :max-home-page-articles)))
              (do
                (println "Publish requires slug, title and article.")
                (usage-and-exit usage))))))))
