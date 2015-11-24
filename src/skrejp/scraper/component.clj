(ns skrejp.scraper.component
  (:use [skrejp.scraper.ann])
  (:require [clojure.core.typed :as t])
  (:require [skrejp.core :as core])
  (:require [skrejp.logger.ann :as logger])
  (:require [com.stuartsierra.component :as component])
  (:require [clojure.string :as str])
  (:require [clojurewerkz.urly.core :as urly])
  (:require [clojure.core.async :as async :refer [go go-loop chan <! >!]])
  (:require [net.cgrand.enlive-html :as html])
  (:import (clojure.lang PersistentVector Fn)))

(t/tc-ignore
  (defn extract-sel
    "Extract the selection, return tag first found tag."
    [doc sel]
    (-> (java.io.StringReader. (:http-payload doc))
        html/html-resource
        (html/select sel)
        first)))

(t/tc-ignore
  (defn extract-tag
    "Extract the text content of the first tag specified by the selector."
    [doc sel]
    (-> (extract-sel doc sel) html/text str/trim)))

(t/tc-ignore
  (defn extract-attr
    "Extract the content of the tag's attribute specified by the selector."
    [doc sel attr]
    (get-in (extract-sel doc sel) [:attrs attr])))

(t/tc-ignore
  (extend-protocol IScraper
    PersistentVector
    (scrape-attr [vec doc] (extract-tag doc vec))
    Fn
    (scrape-attr [func doc] (func doc))))

(t/tc-ignore
  (defn present? [val]
    (not (cond
           (string? val) (empty? val)
           :else (nil? val)))))

;; ScraperComponent implements a component LifeCycle.
;; It depends on the page-retrieval, storage and error-handling components.
;; scraper-defs contains the scraper definitions.
;; url-c is a channel for the urls to process.
;; The ScraperComponent processes the urls coming through the url-c and puts it to the
;; doc-c of the storage component.
(t/ann-record ScraperComponent
              [scraper-defs :- TScraperDefs
               inp-doc-c :- core/TDocChan])

(defrecord ScraperComponent
  [scraper-defs inp-doc-c]
  component/Lifecycle

  (start [this]
    (t/tc-ignore
      (logger/info (:logger this) "Scraper: Starting")
      (let
        [inp-c (:inp-doc-c this)
         out-c (:out-doc-c this)]
        (async/pipeline 20 out-c (scrape this) inp-c)))
    this)

  (stop [this]
    (t/tc-ignore
      (logger/info (:logger this) "Scraper: Stopping"))
    this)

  IScraperComp

  (get-scraper-def [this url]
    (t/tc-ignore
      (let
        [url-host (urly/host-of (urly/url-like url))
         scraper-def-entry ((:scraper-defs this) url-host)]
        (cond
          (map? scraper-def-entry) (merge scraper-def-entry (-> this :scraper-defs :shared))
          (string? scraper-def-entry) (get-scraper-def this scraper-def-entry)
          :else
          (throw (Exception. (format "Missing scraper definition for host %s" url-host)))))))

  (scrape [this]
    (t/tc-ignore
      (fn [xf]
        (fn ([] (xf)) ([result] (xf result))
          ([result doc]
           (let
             [scraper-def (get-scraper-def this (doc :url))
              scraped-doc (reduce
                            (fn [doc-accu [attr sel]]
                              (let [val (scrape-attr sel doc-accu)]
                                (if (present? val) (assoc doc-accu attr val) doc-accu)))
                            doc scraper-def)]
             (xf result (merge doc scraped-doc)))))))))

(t/defn build-component
  "Build a Scraper component."
  [conf-opts :- (t/HMap :mandatory {:scraper-defs TScraperDefs})
   chans :- (t/HMap :mandatory {:inp-doc-c core/TDocChan :out-doc-c core/TDocChan})] :- ScraperComponent
  (map->ScraperComponent {:scraper-defs (:scraper-defs conf-opts)
                          :inp-doc-c (:inp-doc-c chans)
                          :out-doc-c (:out-doc-c chans)}))
