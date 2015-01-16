(ns skrejp.crawl-planner
  (:require [skrejp.logger :as logger])
  (:require [skrejp.retrieval :as ret])
  (:require [clojure.core.async :as async :refer [go chan put! >!]])
  (:require [com.stuartsierra.component :as component]))

(def mapcat-feed-to-docs
  (comp (mapcat :entries)
        (map (fn [entry]
               (assoc (select-keys entry [:title])
                 :url (or (entry :link) (entry :uri)))))))

(defprotocol ICrawlPlanner
  (plan-feeds [this]))

(defrecord CrawlPlannerComponent [page-retrieval scraper error-handling feeds]
  component/Lifecycle

  (start [this]
    (logger/info (:logger this) "Starting CrawlPlanner")
    (plan-feeds this)
    this)

  (stop [this]
    (logger/info (:logger this) "Stopping CrawlPlanner")
    this)

  ICrawlPlanner

  (plan-feeds [this]
    (let
      [docs (into []
                  (comp (-> (:page-retrieval this) ret/fetch-feed) mapcat-feed-to-docs)
                  (:feeds this))]
      (async/onto-chan (-> this :scraper :doc-c) docs))))

(defn build-component
  "Build a CrawlPlanner component."
  [config-opts]
  (map->CrawlPlannerComponent (select-keys config-opts [:feeds])))