(ns skrejp.scraper
  (:require [com.stuartsierra.component :as component])
  (:require [clojurewerkz.urly.core :as urly])
  (:require [clojure.core.async     :refer [go chan put! >!]])
  (:require [net.cgrand.enlive-html :as html]) )

(defn classify-url-source [url] (keyword (urly/host-of (urly/url-like url))) )

(defn extract-sel [body sel]
  (-> (java.io.StringReader. body) html/html-resource (html/select sel)) )

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
  [scraper-defs
   page-retrieval storage error-handling
   url-c]
  component/Lifecycle
  IScraper

  (start [this]
    (println ";; Starting Scraper")
    (assoc this :url-c (chan 512)) )

  (stop [this]
    (println ";; Stopping Scraper")
    this)

  (get-scraper-def [this url]
    (let
      [scraper-def-entry ((:scraper-defs this) (classify-url-source url))]
      (if (coll? scraper-def-entry)
        scraper-def-entry
        ((:scraper-defs this) scraper-def-entry) ) ) )

  (scrape [this]
    (fn [xf]
      (fn ([] (xf)) ([result] (xf result))
        ([result http-resp]
         (let
           [scraper-def (get-scraper-def this (http-resp :url))
            scraped-doc (into {}
                              (map
                                (fn [[attr sel]]
                                  [attr (extract-tag (:body http-resp) sel)])
                                scraper-def)
                              ) ]
           (xf result (assoc scraped-doc :url (http-resp :url)))
          ))))))

(defn build-component
  "Build a Scraper component."
  [config-options]
  (map->ScraperComponent (select-keys config-options [:scraper-defs ])) )
