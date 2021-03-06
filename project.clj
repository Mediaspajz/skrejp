(defproject skrejp "0.2.1-SNAPSHOT"
  :description   "Scraper library based on core.async and components"
  :url           "https://github.com/infiniteiteration/skrejp"
  :license       {:name "Eclipse Public License"
                  :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins      [[lein-expectations "0.0.8"]
                 [lein-marginalia   "0.8.0"]]
  :profiles      {:uberjar {:aot :all}
                  :dev {:dependencies [[environ       "1.0.3"]
                                       [expectations  "2.1.8"]
                                       [http-kit.fake "0.2.2"]
                                       [debugger "0.2.0"]]}}
  :core.typed    {:check [skrejp.core skrejp.retrieval.component skrejp.logger.component skrejp.storage.component
                          skrejp.scraper-verification.component skrejp.crawl-planner.component
                          skrejp.error-handling.component skrejp.scraper.component skrejp.system]}
  :dependencies [[org.clojure/clojure    "1.8.0"]
                 [org.clojure/core.typed "0.3.23"]
                 [org.clojure/core.async "0.2.382"]
                 [com.stuartsierra/component "0.3.1"]
                 [org.clojure/tools.reader "0.10.0"]

                 ;; crawling, parsing, scraping
                 [adamwynne/feedparser-clj "0.5.2"]
                 [http-kit          "2.1.19"]
                 [clojurewerkz/urly "1.0.0"]
                 [enlive            "1.1.6"]
                 [clj-time          "0.12.0"]

                 ;; storage
                 [clojurewerkz/elastisch "2.2.1"]])
