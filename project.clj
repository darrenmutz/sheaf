(defproject sheaf "0.1.0"
  :description "Static blogging engine"
  :url "https://github.com/darrenmutz/sheaf"
  :dependencies [[enlive "1.0.0"]
                 [hiccup "1.0.1"]
                 [joda-time "2.0"]
                 [lancet "1.0.0"]
                 [org.apache.lucene/lucene-core "3.0.2"]
                 [org.clojars.nakkaya/markdownj "1.0.2b4"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/data.json "0.1.1"]
                 [org.clojure/tools.cli "0.2.1"]]
  :dev-dependencies [[lein-marginalia "0.7.1"]]
  :main sheaf.core)
