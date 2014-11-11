(ns skrejp.scraper
  (:require [com.stuartsierra.component :as component])
  (:require [clojurewerkz.urly.core :as urly])
  (:require [clojure.core.async     :refer [go chan put! >!]])
  )

;; ScraperComponent implements a component LifeCycle.
;; It depends on the page-retrieval, storage and error-handling components.
;; scraper-defs contains the scraper definitions.
;; url-c is a channel for the urls to process.
;; The ScraperComponent processes the urls coming through the url-c and puts it to the
;; doc-c of the storage component.
(defrecord ScraperComponent
  [scraper-defs
   page-retrieval storage error-handling
   url-c]
  component/Lifecycle

  (start [this]
    (println ";; Starting Scraper")
    (assoc this :url-c (chan 512))
    )

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
