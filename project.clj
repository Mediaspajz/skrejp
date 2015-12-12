(defproject skrejp "0.2.0-SNAPSHOT"
  :description   "Scraper library based on core.async and components"
  :url           "https://github.com/infiniteiteration/skrejp"
  :license       {:name "Eclipse Public License"
                  :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins      [[lein-expectations "0.0.8"]
                 [lein-typed        "0.3.5"]
                 [lein-marginalia   "0.8.0"]]
  :profiles      {:uberjar {:aot :all}
                  :dev {:dependencies [[environ       "1.0.1"]
                                       [expectations  "2.1.4"]
                                       [http-kit.fake "0.2.2"]
                                       [debugger "0.1.7"]]}}
  :core.typed    {:check [skrejp.core skrejp.retrieval.component skrejp.logger.component skrejp.storage.component
                          skrejp.scraper-verification.component skrejp.crawl-planner.component
                          skrejp.error-handling.component skrejp.scraper.component skrejp.system]}
  :dependencies [[org.clojure/clojure    "1.7.0"]
                 [org.clojure/core.typed "0.3.19"]
                 [org.clojure/core.async "0.2.374"]
                 [com.stuartsierra/component "0.3.1"]
                 [org.clojure/tools.reader "0.10.0"]

                 ;; crawling, parsing, scraping
                 [org.clojars.scsibug/feedparser-clj "0.4.0"]
                 [http-kit          "2.1.19"]
                 [clojurewerkz/urly "1.0.0"]
                 [enlive            "1.1.6"]
                 [clj-time          "0.11.0"]

                 ;; storage
                 [clojurewerkz/elastisch "2.2.0-beta5"]])
