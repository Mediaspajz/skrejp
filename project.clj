(defproject clj-scrapers "0.1.0-SNAPSHOT"
  :description   "FIXME: write description"
  :url           "http://example.com/FIXME"
  :license       {:name "Eclipse Public License"
                  :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins      [[lein-expectations "0.0.7"]]
  :core.typed    {:check [clj-scrapers.scraper]}
  :dependencies [[org.clojure/clojure     "1.6.0"]
                 [prismatic/schema        "0.2.6"]
                 [http-kit                "2.1.16"]
                 [clojurewerkz/urly       "1.0.0"]
                 [enlive                  "1.1.5"]
                 [com.gfredericks/vcr-clj "0.3.3"]
                 [expectations            "2.0.6"]
                 [clojurewerkz/elastisch  "2.0.0"]
                 [org.clojars.scsibug/feedparser-clj "0.4.0"]])
