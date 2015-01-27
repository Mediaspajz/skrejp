(ns skrejp.crawl-planner
  (:require [skrejp.logger :as logger])
  (:require [skrejp.retrieval :as ret])
  (:require [clojure.core.async :as async :refer [go go-loop chan put! <! >!]])
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
    (let
      [cmd-c (chan 16) comp-setup (assoc this :cmd-c cmd-c)]
      (logger/info (:logger this) "Starting CrawlPlanner")
      (async/onto-chan cmd-c (or (:planner-cmds comp-setup) []) false)
      (go-loop [cmd (<! cmd-c)]
        (case cmd
          :plan-feeds (plan-feeds comp-setup))
        (recur (<! cmd-c)))
      comp-setup))

  (stop [this]
    (logger/info (:logger this) "Stopping CrawlPlanner")
    this)

  ICrawlPlanner

  (plan-feeds [this]
    (let
      [docs (into []
                  (comp (ret/fetch-feed (:page-retrieval this)) mapcat-feed-to-docs)
                  (:feeds this))]
      (async/onto-chan (-> this :scraper :doc-c) docs))))

(defn build-component
  "Build a CrawlPlanner component."
  [config-opts]
  (map->CrawlPlannerComponent (select-keys config-opts [:feeds :planner-cmds])))