(ns clj-scrapers.scrapers
  (:require [clojure.core.async     :refer [go chan put! >!]])
  (:require [clojure.string         :refer [join trim]])
  (:require [clojurewerkz.urly.core :as urly])
  (:require [org.httpkit.client     :as http])
  (:require [net.cgrand.enlive-html :as html])
  )

(def this-ns *ns*)

(defn source-keyword [source] (keyword (str this-ns) source))

(def http-options { :timeout    1000
                    :user-agent "Mozilla/5.0 (Windows NT 5.2; rv:2.0.1) Gecko/20100101 Firefox/4.0.1" } )

(defn classify-url-source [url]
  (source-keyword (urly/host-of (urly/url-like url)))
  )

(defn fetch-page [url scrape-fn]
  (let [ page-chan (chan 1) ]
    (http/get url http-options
      (fn [{:keys [status headers body error]}]
        (put! page-chan (scrape-fn body))
        )
      )
    page-chan
    )
  )

(defn extract-sel [body sel]
  (-> (java.io.StringReader. body) html/html-resource (html/select sel))
  )

(defn extract-href [body sel]
  (-> (extract-sel body sel) first (get-in [:attrs :href]))
  )

(defn extract-tag [body sel]
  (-> (extract-sel body sel) first html/text)
  )

(defn collect-attrs
  "Scrape the passed attributes from a string based on mappings."
  [mappings body]
  (reduce
    (fn [scraped-content [attr selector]]
      (assoc scraped-content attr (trim (extract-tag body selector)))
      )
    {} mappings
    )
  )

(defmulti scrape classify-url-source)

(defmacro defscraper
  "Define a scraper for a source."
  [source mappings]
  `(defmethod scrape ~(source-keyword (name source)) [url#]
    (fetch-page url#
      (fn [body#] (-> (collect-attrs ~mappings body#) (assoc :url url#)))
      )
    )
  )

(defn derive-scraper
  "Derive scraper of a source from another source."
  [sub-source super-source]
  (derive (source-keyword sub-source) (source-keyword super-source))
  )
