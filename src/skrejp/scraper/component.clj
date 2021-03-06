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
  (:import (clojure.lang PersistentVector Fn ExceptionInfo)))

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
               inp-doc-c :- core/TDocChan
               out-doc-c :- core/TDocChan])

(defrecord ScraperComponent [scraper-defs inp-doc-c out-doc-c improve]
  component/Lifecycle

  (start [this]
    (t/tc-ignore
      (logger/info (:logger this) "Scraper: Starting")
      (let
        [inp-c inp-doc-c out-c out-doc-c]
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
         scraper-def-entry (scraper-defs url-host)]
        (cond
          (map? scraper-def-entry) (merge scraper-def-entry (:shared scraper-defs))
          (string? scraper-def-entry) (get-scraper-def this scraper-def-entry)
          :else
          (throw (Exception. (format "Missing scraper definition for host %s" url-host)))))))

  (scrape [this]
    (fn [xf]
      (fn
        ([] (xf))
        ([result] (xf result))
        ([result doc]
         (try
           (let
             [scraper-def (get-scraper-def this (doc :url))
              scraped-doc (reduce
                            (fn [doc-accu [attr sel]]
                              (let [val (scrape-attr sel doc-accu)]
                                (if (present? val)
                                  (improve doc-accu attr val)
                                  doc-accu)))
                            doc scraper-def)]
             (xf result scraped-doc))
           (catch clojure.lang.ExceptionInfo e
             (if (= :scraping-error (:cause (ex-data e)))
               (do
                 (logger/info (:logger this) (ex-data e))
                 (xf result))
               (throw e)))))))))

(t/defn build-component
  "Build a Scraper component."
  [conf-opts :- (t/HMap :mandatory {:scraper-defs TScraperDefs})
   chans :- (t/HMap :mandatory {:inp-doc-c core/TDocChan :out-doc-c core/TDocChan})] :- ScraperComponent
  (map->ScraperComponent (merge (select-keys conf-opts [:scraper-defs :improve]) (select-keys chans [:inp-doc-c :out-doc-c]))))
