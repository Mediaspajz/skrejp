(ns skrejp.error-handling
  (:require [com.stuartsierra.component :as component])
  )

(defrecord ErrorHandlingComponent []
  component/Lifecycle

  (start [this]
    (println ";; Starting ErrorHandling")
    this)

  (stop [this]
    (println ";; Stopping ErrorHanding")
    this))

(defn build-component
  "build an ErrorHandling component."
  []
  (map->ErrorHandlingComponent {})
  )