(ns skrejp.scraper
  (:require [skrejp.logger :as logger])
  (:require [com.stuartsierra.component :as component])
  (:require [skrejp.retrieval :as ret])
  (:require [clojurewerkz.urly.core :as urly])
  (:require [clojure.core.async :as async :refer [go go-loop chan <! >!]])
  (:require [net.cgrand.enlive-html :as html]))

(defn classify-url-source [url] (keyword (urly/host-of (urly/url-like url))))

(defn extract-sel [body sel]
  (-> (java.io.StringReader. body) html/html-resource (html/select sel)))

(defn extract-tag [body sel]
  (-> (extract-sel body sel) first html/text) )

(defprotocol IScraper
  "## IScraper
  Defines methods for scraping structure content from web pages.
  *scrape* is a transducer taking a http responses and returning map with extacted values.
  *get-scraper-def* returns a scraper definition for a url."

  (scrape [this])
  (get-scraper-def [this url]))

;; ScraperComponent implements a component LifeCycle.
;; It depends on the page-retrieval, storage and error-handling components.
;; scraper-defs contains the scraper definitions.
;; url-c is a channel for the urls to process.
;; The ScraperComponent processes the urls coming through the url-c and puts it to the
;; doc-c of the storage component.
(defrecord ScraperComponent
  [scraper-defs page-retrieval storage error-handling doc-c]
  component/Lifecycle

  (start [this]
    (logger/info (:logger this) "Starting Scraper")
    (let
      [doc-c    (chan 512)
       fetch-c  (chan 512)
       scrape-c (chan 512)
       component-setup (assoc this :doc-c doc-c)]
      (async/pipeline-async 20 fetch-c (-> this :page-retrieval ret/fetch-page) doc-c)
      (async/pipeline 20 scrape-c (scrape this) fetch-c)
      (go (<! (async/timeout 1000))
          (println (:logger this) (<! scrape-c))
          (println (:logger this) (<! scrape-c)))
      component-setup))

  (stop [this]
    (logger/info (:logger this) "Stopping Scraper")
    this)

  IScraper

  (get-scraper-def [this url]
    (let
      [scraper-def-entry ((:scraper-defs this) (classify-url-source url))]
      (if (coll? scraper-def-entry)
        scraper-def-entry
        ((:scraper-defs this) scraper-def-entry))))

  (scrape [this]
    (fn [xf]
      (fn ([] (xf)) ([result] (xf result))
        ([result doc]
         (let
           [scraper-def (get-scraper-def this (doc :url))
            scraped-doc (into {}
                              (map
                                (fn [[attr sel]]
                                  [attr (extract-tag (:http-payload doc) sel)])
                                scraper-def))]
           (xf result (merge doc scraped-doc))))))))

(defn build-component
  "Build a Scraper component."
  [config-options]
  (map->ScraperComponent (select-keys config-options [:scraper-defs])))
