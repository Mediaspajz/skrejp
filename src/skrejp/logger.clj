(ns skrejp.logger
  (:require [com.stuartsierra.component :as component])
  (:require [clojure.core.typed :as t]))

(t/defprotocol ILogger
  "## ILogger
  Defines methods for logging the events of the scraping system. Every other component is supposed to use it.
  But this component is not dependent on other part of the system. It acts as a bridge between logger libraries and
  the system."

  (info [this :- ILogger msg :- t/Any] :- nil)
  (debug [this :- ILogger msg :- t/Any] :- nil))

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
