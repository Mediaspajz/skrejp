(ns skrejp.scraper-verification
  (:require [skrejp.logger :as logger])
  (:require [com.stuartsierra.component :as component]))

(defrecord ScraperVerificationComponent [storage page-retrieval error-handling]
  component/Lifecycle

  (start [this]
    (logger/info (:logger this) "ScraperVerification: Starting")
    this)

  (stop [this]
    (logger/info (:logger this) "ScraperVerification: Stopping")
    this))

(defn build-component
  "Build a ScraperVerification component."
  [conf-opts]
  (map->ScraperVerificationComponent (select-keys conf-opts [])))
