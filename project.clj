(defproject sheaf "0.1.0"
  :description "Static blogging engine"
  :url "https://github.com/darrenmutz/sheaf"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.apache.lucene/lucene-core "3.0.2"]
                 [org.clojure/tools.cli "0.2.1"]
                 [lancet "1.0.0"]
                 [enlive "1.0.0"]
                 [org.clojure/data.json "0.1.1"]
                 [joda-time "2.0"]
                 [hiccup "1.0.1"]]
  :dev-dependencies [[lein-marginalia "0.7.1"]]
  :main sheaf.core)
