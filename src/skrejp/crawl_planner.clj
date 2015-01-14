(ns skrejp.crawl-planner
  (:require [skrejp.logger :as logger])
  (:require [skrejp.retrieval :as ret])
  (:require [clojure.core.async :as async :refer [go chan put! >!]])
  (:require [com.stuartsierra.component :as component])
  (:require [clojure.set :refer [rename-keys]]) )

(def mapcat-feed-to-docs
  (comp (mapcat :entries)
        (map (fn [entry]
               (assoc (select-keys entry [:title])
                 :url (or (entry :link) (entry :uri)))))))

(defrecord CrawlPlannerComponent [page-retrieval scraper error-handling feeds]
  component/Lifecycle

  (start [this]
    (logger/info (:logger this) "Starting CrawlPlanner")
    (let
      [docs (into []
                  (comp (-> (:page-retrieval this) ret/fetch-feed) mapcat-feed-to-docs)
                  (:feeds this))]
      (async/onto-chan (-> this :scraper :doc-c) docs))
    this)

  (stop [this]
    (logger/info (:logger this) "Stopping CrawlPlanner")
    this))

(defn build-component
  "Build a CrawlPlanner component."
  [config-opts]
  (map->CrawlPlannerComponent (select-keys config-opts [:feeds])))