(ns skrejp.scraper
  (:require [com.stuartsierra.component :as component])
  )

(defrecord ScraperComponent [storage error-handling]
  component/Lifecycle

  (start [this]
    (println ";; Starting Scraper")
    this)

  (stop [this]
    (println ";; Stopping Scraper")
    this))

(defn build-component
  "Build a Scraper component."
  [config-options]
  (map->ScraperComponent {:options config-options})
  )
