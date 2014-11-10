(ns skrejp.scraper
  (:require [com.stuartsierra.component :as component])
  (:require [clojurewerkz.urly.core :as urly])
  )

(defrecord ScraperComponent [scraper-defs storage error-handling]
  component/Lifecycle

  (start [this]
    (println ";; Starting Scraper")
    this)

  (stop [this]
    (println ";; Stopping Scraper")
    this))

(defn build-component
  "Build a Scraper component."
  [scraper-defs]
  (map->ScraperComponent {:scraper-defs scraper-defs})
  )

(def this-ns *ns*)

(defn source-keyword [source] (keyword (str this-ns) source))

(defn classify-url-source [url]
  (source-keyword (urly/host-of (urly/url-like url)))
  )
