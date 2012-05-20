(defproject sheaf "0.0.1-SNAPSHOT"
  :description "Static blogging engine"
  :url "https://github.com/darrenmutz/sheaf"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.apache.lucene/lucene-core "3.0.2"]
                 [org.clojure/tools.cli "0.2.2-SNAPSHOT"]
                 [lancet "1.0.0"]
                 [enlive "1.0.0-SNAPSHOT"]
                 [org.clojure/data.json "0.1.1"]
                 [joda-time "2.0"]]
  :main sheaf.core)
