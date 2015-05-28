(ns skrejp.scraper
  (:require [skrejp.logger :as logger])
  (:require [com.stuartsierra.component :as component])
  (:require [clojure.string :as str])
  (:require [clojurewerkz.urly.core :as urly])
  (:require [clojure.core.async :as async :refer [go go-loop chan <! >!]])
  (:require [net.cgrand.enlive-html :as html]))

(defn extract-sel
  "Extract the selection, return tag first found tag."
  [doc sel]
  (-> (java.io.StringReader. (:http-payload doc))
      html/html-resource
      (html/select sel)
      first))

(defn extract-tag
  "Extract the text content of the first tag specified by the selector."
  [doc sel]
  (-> (extract-sel doc sel) html/text str/trim))

(defn extract-attr
  "Extract the content of the tag's attribute specified by the selector."
  [doc sel attr]
  (get-in (extract-sel doc sel) [:attrs attr]))

(defn compute-sel [doc sel]
  (cond
    (vector? sel) (extract-tag doc sel)
    (fn?     sel) (sel doc)
    :else (class sel)))

(defn present? [val]
  (not (cond
         (string? val) (empty? val)
         :else         (nil?   val))))

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
    (logger/info (:logger this) "Scraper: Starting")
    (let
      [doc-c (chan 512)
       out-c (-> this :storage :doc-c)
       retr-inp-c (-> this :page-retrieval :inp-doc-c)
       retr-out-c (-> this :page-retrieval :out-doc-c)
       component-setup (assoc this :doc-c doc-c)]
      (async/pipe doc-c retr-inp-c)
      (async/pipeline 20 out-c (scrape this) retr-out-c)
      component-setup))

  (stop [this]
    (logger/info (:logger this) "Scraper: Stopping")
    this)

  IScraper

  (get-scraper-def [this url]
    (let
      [url-host (urly/host-of (urly/url-like url))
       scraper-def-entry ((:scraper-defs this) url-host)]
      (cond
        (map?    scraper-def-entry) (merge scraper-def-entry (-> this :scraper-defs :shared))
        (string? scraper-def-entry) (get-scraper-def this scraper-def-entry)
        :else
        (throw (Exception. (format "Missing scraper definition for host %s" url-host))))))

  (scrape [this]
    (fn [xf]
      (fn ([] (xf)) ([result] (xf result))
        ([result doc]
         (let
           [scraper-def (get-scraper-def this (doc :url))
            scraped-doc (reduce
                          (fn [doc-accu [attr sel]]
                            (let [val (compute-sel doc-accu sel)]
                              (if (present? val) (assoc doc-accu attr val) doc-accu)))
                          doc scraper-def)]
           (xf result (merge doc scraped-doc))))))))

(defn build-component
  "Build a Scraper component."
  [config-options]
  (map->ScraperComponent (select-keys config-options [:scraper-defs])))
