(ns skrejp.error-handling.component
  (:use [skrejp.error-handling.ann])
  (:require [clojure.core.typed :as t])
  (:require [skrejp.logger.ann :as logger])
  (:require [com.stuartsierra.component :as component]))

(t/ann-record ErrorHandlingComponent [])

(defrecord ErrorHandlingComponent []
  component/Lifecycle

  (start [this]
    (t/tc-ignore
      (logger/info (:logger this) "ErrorHandling: Starting"))
    this)

  (stop [this]
    (t/tc-ignore
      (logger/info (:logger this) "ErrorHandling: Stopping"))
    this))

(t/defn build-component
  "build an ErrorHandling component."
  [_conf-opts :- (t/HMap :complete? false)] :- ErrorHandlingComponent
  (map->ErrorHandlingComponent {}))