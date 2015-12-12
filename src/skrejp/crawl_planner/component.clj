(ns skrejp.crawl-planner.component
  (:use [skrejp.crawl-planner.ann])
  (:require [skrejp.logger.ann :as logger])
  (:require [clojure.core.async :as async :refer [go go-loop chan put! <! >!]])
  (:require [clojure.core.typed :as t])
  (:require [com.stuartsierra.component :as component]
            [skrejp.core :as core]))

(t/tc-ignore
  (def mapcat-feed-to-docs
    (comp (mapcat :entries)
          (map (fn [entry]
                 (assoc (select-keys entry [:title])
                   :url (or (entry :link) (entry :uri))
                   :published_at (org.joda.time.DateTime. (entry :published-date))))))))

(t/ann-record CrawlPlannerComponent
              [feeds :- TFeedUrlVec
               planner-cmds :- TPlannerCmdVec
               cmd-c :- TPlannerCmdChan
               out-doc-c :- core/TDocChan])

(defrecord CrawlPlannerComponent [feeds planner-cmds cmd-c out-doc-c]
  component/Lifecycle

  (start [this]
    (t/tc-ignore
      (logger/info (:logger this) "CrawlPlanner: Starting")
      (async/onto-chan (:cmd-c this) (or planner-cmds []) false)
      (go-loop [cmd (<! (:cmd-c this))]
        (logger/info (:logger this) (format "CrawlPlanner: Received: %s" cmd))
        (case cmd
          :plan-feeds (async/onto-chan out-doc-c feeds false))
        (recur (<! cmd-c))))
    this)

  (stop [this]
    (t/tc-ignore
      (logger/info (:logger this) "CrawlPlanner: Stopping"))
      (async/close! out-doc-c)
    this))

(t/defn build-component
  "Build a CrawlPlanner component."
  [conf-opts :- (t/HMap :mandatory
                        {:feeds TFeedUrlVec
                         :planner-cmds TPlannerCmdVec} :complete? false)
   chans :- (t/HMap :mandatory
                        {:cmd-c TPlannerCmdChan
                         :out-doc-c core/TDocChan} :complete? false)] :- CrawlPlannerComponent
  (map->CrawlPlannerComponent {:feeds        (:feeds conf-opts)
                               :planner-cmds (:planner-cmds conf-opts)
                               :cmd-c        (:cmd-c chans)
                               :out-doc-c    (:out-doc-c chans)}))

