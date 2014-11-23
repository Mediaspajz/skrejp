(ns skrejp.storage
  (:require [skrejp.logger :as logger])
  (:require [com.stuartsierra.component :as component]) )

(defrecord Storage []
  component/Lifecycle

  (start [this]
    (logger/info (:logger this) "Starting Storage")
    this)

  (stop [this]
    (logger/info (:logger this) "Stopping Storage")
    this))

(defn build-component
  "Build a new storage."
  [conf-opts]
  (map->Storage (select-keys conf-opts [])))
