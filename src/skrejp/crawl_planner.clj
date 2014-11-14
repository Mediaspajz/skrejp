(ns skrejp.crawl-planner
  (:require [com.stuartsierra.component :as component])
  (:require [clojure.set :refer [rename-keys]])
  )

(defprotocol ICrawlPlanner
  (map-feed-to-docs [this])
  )

(defrecord CrawlPlannerComponent [page-retrieval scraper error-handling]
  component/Lifecycle

  (start [this]
    (println ";; Starting CrawlPlanner")
    this)

  (stop [this]
    (println ";; Stopping CrawlPlanner")
    this)

  ICrawlPlanner

  (map-feed-to-docs [this]
    (comp (mapcat :entries)
          (map (fn [entry]
                 (assoc (select-keys entry [:title])
                        :url (or (entry :link) (entry :uri))))
               )))
  )

(defn build-component
  "Build a CrawlPlanner component."
  []
  (map->CrawlPlannerComponent {})
  )