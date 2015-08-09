(ns skrejp.scraper-verification
  (:require [clojure.core.typed :as t])
  (:require [skrejp.logger :as logger])
  (:require [com.stuartsierra.component :as component]))

(t/defprotocol IScraperVerifier)

(t/ann-record ScraperVerificationComponent [])

(defrecord ScraperVerificationComponent []
  component/Lifecycle

  (start [this]
    (t/tc-ignore
      (logger/info (:logger this) "ScraperVerification: Starting"))
    this)

  (stop [this]
    (t/tc-ignore
      (logger/info (:logger this) "ScraperVerification: Stopping"))
    this)

  IScraperVerifier)

(t/defn build-component
  "Build a ScraperVerification component."
  [_conf-opts :- (t/HMap :complete? false)] :- IScraperVerifier
  (map->ScraperVerificationComponent {}))
