(ns skrejp.logger.component
  (:use [skrejp.logger.ann])
  (:require [com.stuartsierra.component :as component])
  (:require [clojure.core.typed :as t]))

(t/ann-record LoggerComponent [])

(defrecord LoggerComponent []
  ILogger

  (info [_this msg]
    (println ";;" msg)
    nil)

  (debug [_this msg]
    (println "..." msg)
    nil)

  component/Lifecycle

  (start [this]
    (info this "Starting Logger")
    this)

  (stop [this]
    (info this "Stopping Logger")
    this))

(t/defn build-component
  "Build a Logger component."
  [_config-options :- (t/HMap :complete? false)] :- LoggerComponent
  (map->LoggerComponent {}))
