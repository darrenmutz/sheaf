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

(ns sheaf.core
  (:import org.joda.time.DateTime)
  (:import org.joda.time.format.DateTimeFormatterBuilder)
  (:gen-class)
  (:use clojure.java.io)
  (:use clojure.tools.cli)
  (:use [clojure.data.json :only (json-str write-json read-json)])
  (:use [net.cgrand.enlive-html :only (deftemplate defsnippet content set-attr
                                       do-> first-child html-resource select
                                       nth-of-type any-node)]))

(load-file (str (System/getProperty (str "user.home")) "/.sheaf"))

(def ^:dynamic *template-url*
  (str "file:///" (*config* :sheaf-root) "/" (*config* :template-file)))

(def ^:dynamic *archive-root*
  (str (*config* :sheaf-root) "/" (*config* :archive-dir)))

(defn insert-article [archive article]
  (sort #(compare (%2 :publish-time) (%1 :publish-time)) (cons article archive)))

(defn get-archive-filename [month year]
  (str *archive-root* "/" month "-" year ".json"))

(defn read-archive [filename]
  (try
    (seq (read-json (slurp filename)))
    (catch java.io.FileNotFoundException e [])))
  
(defn article-exists? [archive slug]
  (contains? (apply sorted-set (map :slug archive)) slug))

(defn fetch-content [url]
  (html-resource (java.net.URL. url)))

(defn long-form-date [datetime]
  (.print (.toFormatter (.appendYear
                         (.appendLiteral
                          (.appendDayOfMonth
                           (.appendLiteral
                            (.appendMonthOfYearText (DateTimeFormatterBuilder.)) " ") 1) ", ") 4 4))
          datetime))

(def *link-sel* [[:.archive-list (nth-of-type 1)] :> first-child])

(defsnippet link-model (fetch-content *template-url*) *link-sel*
  [{:keys [month year]}]
  [:a] (do->
        (content (str month " " year))
        (set-attr :href (str "/" month "-" year))))

(deftemplate article-template (fetch-content *template-url*) [article title datetime permalink link]
  (*config* :articles-selector)  (apply content article)
  (*config* :title-selector)     (content title)
  (*config* :title-selector)     (set-attr :href (if link link permalink))
  (*config* :title-selector)     (set-attr :id (if link "link" "permalink"))
  (*config* :time-selector)      (content (long-form-date datetime))
  (*config* :time-selector)      (set-attr "datetime" (.toString datetime))
  (*config* :permalink-selector) (set-attr "href" permalink)
  (*config* :archives-selector)  nil)

(deftemplate index-template (fetch-content *template-url*) [articles archive-month-years]
  (*config* :index-articles-selector) (apply content articles)
  (*config* :archive-list-selector)   (content (map link-model archive-month-years)))

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

(defn get-year-month-millis [now]
  (hash-map :year   (.getYear now)
            :month  (.getAsShortText (.monthOfYear now))
            :millis (.getMillis now)))

(defn get-relative-path [month year slug]
  (str month "-" year "/" slug ".html"))

(defn update-title [archive slug title]
  (map #(if (= (% :slug) slug) (assoc % :title title) %) archive))

(defn get-publish-time [archive slug]
  (loop [remainder archive]
    (if (empty? remainder)
      nil
      (if (= ((first remainder) :slug) slug)
        ((first remainder) :publish-time)
        (recur (rest remainder))))))

(defn write-article [relative-path article-url title publish-time link archive slug]
  (try-write (str (*config* :doc-root) "/" relative-path)
             (apply str (article-template
                         (select (fetch-content article-url)
                                 (*config* :input-article-selector))
                         title
                         publish-time
                         (str "/" relative-path)
                         link))))
  
(defn revise-article [month year slug title article-url link]
  (let [archive (read-archive (get-archive-filename month year))]
    (if (article-exists? archive slug)
      (let [relative-path (get-relative-path month year slug)]
        (try-write (get-archive-filename month year)
                   (json-str (update-title archive slug title)))
        (write-article relative-path article-url title (get-publish-time archive slug)
                       link archive slug)
        true)
      (println "Can't revise an article that doesn't exist."))))

(defn publish-article [now slug title article-url link]
  (let [ymm (get-year-month-millis now)
        year (ymm :year)
        month (ymm :month)
        millis (ymm :millis)
        archive (read-archive (get-archive-filename month year))]
    (if (article-exists? archive slug)
      (println "Can't publish an article that already exists.")
      (let [relative-path (get-relative-path month year slug)]
        (try-write (get-archive-filename month year)
                   (json-str (insert-article archive
                                             {:slug slug
                                              :title title
                                              :publish-time millis
                                              :relative-path relative-path})))
        (write-article relative-path article-url title now link archive slug)
        true))))

(defn not-implemented [option]
  (println "Option '" option "' is not yet implemented")
  (. System (exit 1)))

(defn datetime-from-month-year [month-year-string]
  (if-let [[match month year] (re-find #"([A-Z][a-z]+)-([0-9]+)" month-year-string)]
    (let [builder (DateTimeFormatterBuilder.)
          custom-builder (.builder (.appendYear (.appendLiteral (.appendMonthOfYearShortText builder)) "-")
                                   4 4)
          formatter (.toFormatter custom-builder)]
      (.parseDateTime formatter month-year-string)))
  nil)

(defn dir-list [dir]
  (apply vector (.list (java.io.File. dir))))

(defn datetime-from-month-year [month year]
  (let [builder (DateTimeFormatterBuilder.)
        custom-builder (.appendYear (.appendLiteral (.appendMonthOfYearShortText builder) "-") 4 4)
        formatter (.toFormatter custom-builder)]
    (.parseDateTime formatter (str month "-" year))))

(defn annotated-archive-from-file [filename]
  (let [archive-regex #"([A-Z][a-z]+)-([0-9]+).json"]
    (if-let [[filename month year] (re-find archive-regex filename)]
      (hash-map :filename (str *archive-root* "/" filename)
                :datetime (datetime-from-month-year month year)
                :month month
                :year year))))

(defn archives-to-seq [articles archives]
  (lazy-seq
   (if (first articles)
     (cons (first articles) (archives-to-seq (rest articles) archives))
     (if (first archives)
       (let [articles (read-archive (:filename (first archives)))]
         (cons (first articles) (archives-to-seq (rest articles) (rest archives))))
       nil))))

(defn get-sorted-archives []
  (sort #(compare (%1 :datetime) (%2 :datetime))
        (map annotated-archive-from-file (dir-list *archive-root*))))

(defn generate-index [articles target-dir archive-month-years]
  (let [article-urls (map #(str "file:///" (*config* :doc-root) "/" (% :relative-path)) articles)
        article-contents (map fetch-content article-urls)
        article-nodes (map #(select % (*config* :article-selector)) article-contents)]
    (try-write (str target-dir "/index.html")
               (apply str (index-template article-nodes archive-month-years)))))

(defn generate-indices [now max-root-articles]
  (let [ymm (get-year-month-millis now)
        year (ymm :year)
        month (ymm :month)
        sorted-archives (get-sorted-archives)
        archive-month-years (map #(select-keys % [:month :year]) sorted-archives)
        this-months-articles (archives-to-seq nil (vector (first sorted-archives)))
        all-articles (archives-to-seq nil sorted-archives)]
    (generate-index this-months-articles (str (*config* :doc-root) "/" month "-" year)
                    archive-month-years)
    (generate-index (take max-root-articles all-articles) (*config* :doc-root)
                    archive-month-years)))

(defn usage-and-exit [usage]
  (do
    (println usage)
    (. System (exit 1))))

(defn -main [& args]
  (let [[options args usage]
        (cli args
             ["-p" "--publish" "Publish an article" :flag true]
             ["-r" "--revise"  "Revise an article" :flag true]
             ["-m" "--month"   "Month an article to revise was published in"]
             ["-y" "--year"    "Year an article to revise was published in"]
             ["-d" "--delete"  "Delete an article" :flag true]
             ["-s" "--slug"    "Article slug, ex: my-article-title"]
             ["-t" "--title"   "Article title, ex: \"My article title\""]
             ["-l" "--link"    "Title is an offsite link, ex: \"http://www.noaa.gov\""]
             ["-h" "--html"    "File containing html article, ex: path/to/article.html"]
             ["-c" "--config"  "Load config from this filename instead of ~/.sheaf"])
        now (DateTime.)]
    (if (not (reduce #(or %1 %2) (map options [:publish :revise :delete])))
      (usage-and-exit usage))
    (if (options :delete)
      (not-implemented "--delete"))
    (if (reduce #(and (options %1) (options %2)) [:revise :slug :title :html :month :year])
      (let [slug (options :slug)
            title (options :title)
            html (options :html)
            month (options :month)
            year (options :year)]
        (if (revise-article month year slug title (str "file:///" html) (options :link))
          (generate-indices now (*config* :max-home-page-articles))))
      (usage-and-exit usage))
    (if (reduce #(and (options %1) (options %2)) [:publish :slug :title :html])
      (let [slug (options :slug)
            title (options :title)
            html (options :html)]
        (if (publish-article now slug title (str "file:///" html) (options :link))
          (generate-indices now (*config* :max-home-page-articles))))
      (usage-and-exit usage))))
