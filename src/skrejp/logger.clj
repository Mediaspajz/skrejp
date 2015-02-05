(ns skrejp.logger
  (:require [com.stuartsierra.component :as component]))

(defprotocol ILogger
  "## ILogger
  Defines methods for logging the events of the scraping system. Every other component is supposed to use it.
  But this component is not dependent on other part of the system. It acts as a bridge between logger libraries and
  the system."

  (info [this msg])
  (debug [this msg]))

(defrecord LoggerComponent []
  ILogger

  (info [this msg]
    (println ";;" msg))

  (debug [this msg]
    (println "..." msg))

  component/Lifecycle

  (start [this]
    (info this "Starting Logger")
    this)

  (stop [this]
    (info this "Stopping Logger")
    this))

(defn build-component
  "Build a Logger component."
  [config-options]
  (map->LoggerComponent (select-keys config-options [])))
