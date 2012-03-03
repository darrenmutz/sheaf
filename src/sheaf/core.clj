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
  (:use [net.cgrand.enlive-html :only (deftemplate content set-attr
                                       html-resource select)]))

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

(deftemplate article-template (fetch-content *template-url*) [article title datetime permalink]
  (*config* :articles-selector)  (apply content article)
  (*config* :title-selector)     (content title)
  (*config* :time-selector)      (content (long-form-date datetime))
  (*config* :time-selector)      (set-attr "datetime" (.toString datetime))
  (*config* :permalink-selector) (set-attr "href" permalink))

(deftemplate index-template (fetch-content *template-url*) [articles]
  (*config* :articles-selector) (apply content articles))

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

(defn publish-article [now slug title article-url]
  (let [year (.getYear now)
        month (.getAsShortText (.monthOfYear now))
        archive (read-archive (get-archive-filename month year))]
    (if (article-exists? archive slug)
      (println "Can't publish an article that already exists.")
      (let [relative-path (str month "-" year "/" slug ".html")]
        (try-write (get-archive-filename month year)
                   (json-str (insert-article archive
                                             {:slug slug
                                              :title title
                                              :publish-time (.getMillis now)
                                              :relative-path relative-path})))
        (try-write (str (*config* :doc-root) "/" relative-path)
                   (apply str (article-template
                               (select (fetch-content article-url)
                                       [:p])
                               title
                               now
                               (str (*config* :uri-root) relative-path))))
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

(defn generate-index [now max-articles]
  (let [sorted-archives (get-sorted-archives)
        articles (take max-articles (archives-to-seq nil sorted-archives))
        article-urls (map #(str "file:///" (*config* :doc-root) "/" (% :relative-path)) articles)
        article-contents (map fetch-content article-urls)
        article-nodes (map #(select % [:article-selector]) article-contents)]
    (try-write (str (*config* :doc-root) "/index.html")
               (apply str (index-template article-nodes)))))

(defn -main [& args]
  (let [[options args usage]
        (cli args
             ["-p" "--publish" "Publish an article" :flag true]
             ["-r" "--revise"  "Revise an article" :flag true]
             ["-d" "--delete"  "Delete an article" :flag true]
             ["-s" "--slug"    "Article slug, ex: my-article-title"]
             ["-t" "--title"   "Article title, ex: \"My article title\""]
             ["-h" "--html"    "File containing html article, ex: path/to/article.html"])
        now (DateTime.)]
    (if (not (reduce #(or %1 %2) (map options [:publish :revise :delete])))
      (do
        (println usage)
        (. System (exit 1))))
    (if (options :revise)
      (not-implemented "--revise"))
    (if (options :delete)
      (not-implemented "--delete"))
    (if (and (options :title) (options :html))
      (if (publish-article now (options :slug) (options :title) (str "file:///" (options :html)))
        (generate-index now (*config* :max-home-page-articles))))))

