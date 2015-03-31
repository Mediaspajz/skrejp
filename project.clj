(defproject clj-scrapers "0.1.0-SNAPSHOT"
  :description   "Scraper library based on core.async"
  :url           "http://example.com/FIXME"
  :license       {:name "Eclipse Public License"
                  :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins      [[lein-expectations "0.0.7"]
                 [lein-marginalia   "0.8.0"]]
  :profiles      {:uberjar {:aot :all}}
  :core.typed    {:check [clj-scrapers.scraper]}
  :dependencies [[org.clojure/clojure     "1.7.0-alpha5"]
                 [org.clojure/core.async  "0.1.319.0-6b1aca-alpha"]
                 [com.stuartsierra/component "0.2.3"]

                 ;; crawling, parsing, scraping
                 [org.clojars.scsibug/feedparser-clj "0.4.0"]
                 [http-kit          "2.1.19"]
                 [clojurewerkz/urly "1.0.0"]
                 [enlive            "1.1.5"]
                 [clj-time          "0.9.0"]

                 ;; testing
                 [expectations      "2.1.0"]
                 [http-kit.fake     "0.2.2"]

                 ;; storage
                 [clj-http "1.1.0"]
                 [clojurewerkz/elastisch "2.1.0"]])
