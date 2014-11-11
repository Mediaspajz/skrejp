(ns skrejp.scraper-verification
  (:require [com.stuartsierra.component :as component])
  )

(defrecord ScraperVerificationComponent [storage page-retrieval error-handling]
  component/Lifecycle

  (start [this]
    (println ";; Starting ScraperVerification")
    this)

  (stop [this]
    (println ";; Stopping ScraperVerification")
    this)
  )

(defn build-component
  "Build a ScraperVerification component."
  []
  (map->ScraperVerificationComponent {})
  )
