(ns skrejp.error-handling
  (:require [skrejp.logger :as logger])
  (:require [com.stuartsierra.component :as component]))

(defrecord ErrorHandlingComponent []
  component/Lifecycle

  (start [this]
    (logger/info (:logger this) "ErrorHandling: Starting")
    this)

  (stop [this]
    (logger/info (:logger this) "ErrorHandling: Stopping")
    this))

(defn build-component
  "build an ErrorHandling component."
  [conf-opts]
  (map->ErrorHandlingComponent (select-keys conf-opts [])))