(ns skrejp.storage
  (:require [skrejp.logger :as logger])
  (:require [clojure.core.async :as async :refer [go go-loop chan <! >!]])
  (:require [com.stuartsierra.component :as component]) )

(defprotocol IStorage
  "## IStorage
  Defines methods for storing documents scraped by the system. Storage component is independent from other parts of
  the system. The _scraper component_ uses it for storing the scraped documents."
  (store [this doc]))

(defrecord Storage [logger doc-c]
  component/Lifecycle

  (start [this]
    (logger/info (:logger this) "Starting Storage")
    (let [doc-c (chan 512)
          component-setup (assoc this :doc-c doc-c)]
      (go-loop
        [doc (<! doc-c)]
        (if (nil? doc)
          (logger/info  (:logger this) "Storage input channel closed")
          (do
            (store this doc)
            (recur (<! doc-c)))))
      component-setup))

  (stop [this]
    (logger/info (:logger this) "Stopping Storage")
    this)

  IStorage

  (store [this doc]
    (logger/debug (:logger this) doc)))

(defn build-component
  "Build a new storage."
  [conf-opts]
  (map->Storage (select-keys conf-opts [])))
