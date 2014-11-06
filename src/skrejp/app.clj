(ns skrejp.app
  (:require [com.stuartsierra.component :as component])
  (:require [skrejp.system :as system])
  )

(def config-options {})

(def scraper-system (system/build-scraper-system config-options))

(defn start-scraper-system
  "starts the scraper system."
  []
  (alter-var-root (var scraper-system) component/start)
  )

(defn stop-scraper-system
  "Stops the passed in system"
  []
  (alter-var-root (var scraper-system) component/stop)
  )
