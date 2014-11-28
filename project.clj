(defproject clj-scrapers "0.1.0-SNAPSHOT"
  :description   "FIXME: write description"
  :url           "http://example.com/FIXME"
  :license       {:name "Eclipse Public License"
                  :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins      [[lein-expectations "0.0.7"]
                 [lein-marginalia   "0.8.0"]]
  :core.typed    {:check [clj-scrapers.scraper]}
  :dependencies [[org.clojure/clojure     "1.7.0-alpha4"]
                 [org.clojure/core.async  "0.1.319.0-6b1aca-alpha"]
                 [prismatic/schema        "0.3.3"]
                 [com.stuartsierra/component "0.2.2"]

                 ;; crawling, parsing, scraping
                 [org.clojars.scsibug/feedparser-clj "0.4.0"]
                 [http-kit          "2.1.19"]
                 [clojurewerkz/urly "1.0.0"]
                 [enlive            "1.1.5"]

                 ;; logging, metrics
                 [com.taoensso/timbre "3.3.1"]
                 ;[analytics-clj       "0.2.2"] ;; segment.io not used - no user actions
                 [clj-librato         "0.0.5"]

                 ;; testing
                 [expectations      "2.0.13"]
                 [http-kit.fake     "0.2.2"]

                 ;; storage
                 [clojurewerkz/elastisch  "2.0.0"]]
  :main          clj-scrapers.archives)
