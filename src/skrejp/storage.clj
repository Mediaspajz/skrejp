(ns skrejp.storage
  (:require [com.stuartsierra.component :as component])
  )

(defrecord Storage []
  component/Lifecycle

  (start [this]
    (println ";; Starting the Storage")
    this
    )

  (stop [this]
    (println ";; Stopping the Storage")
    this
    )
  )

(defn build-component
  "Build a new storage."
  []
  (map->Storage {})
  )
