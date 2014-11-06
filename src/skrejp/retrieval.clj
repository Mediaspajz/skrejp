(ns skrejp.retrieval
  (:require [com.stuartsierra.component :as component])
  )

(defrecord RetrievalComponent []
  component/Lifecycle

  (start [this]
    (println ";; Starting PageContentRetrieval")
    this)

  (stop [this]
    (println ";; Stopping PageContentRetrieval")
    this))

(defn build-component
  "Build a PageRetrieval component."
  [config-options]
  (map->RetrievalComponent {:options config-options})
  )
