(ns clj-scrapers.scrapers
  (:require [clojure.core.async :refer [go chan >!]])
  (:require [clojurewerkz.urly.core :refer [url-like host-of]])
  (:require [clojure.string :refer [join trim]])
  (:require [org.httpkit.client :as http])
  (:require [net.cgrand.enlive-html :as html])
  )

(def this-ns *ns*)

(defn- source-keyword [source] (keyword (str this-ns) source))

(def http-options { :timeout    1000
                    :user-agent "Mozilla/5.0 (Windows NT 5.2; rv:2.0.1) Gecko/20100101 Firefox/4.0.1" } )

(defn classify-url-source [url] (source-keyword (host-of (url-like url)))
  )

(defn fetch-page [url scrape-fn]
  (let [ page-chan (chan) ]
    (http/get url http-options
      (fn [{:keys [status headers body error]}]
        (go (>! page-chan (scrape-fn body)))
        )
      )
    page-chan
    )
  )

(defn extract-tag [body selector]
  (-> (java.io.StringReader. body)
      html/html-resource
      (html/select selector)
      first
      html/text)
  )

(defn collect-attrs
  "Scrape the passed attributes from a string based on mappings."
  [mappings body]
  (reduce
    (fn [scraped-content [attr selector]]
      (assoc scraped-content attr (trim (extract-tag body selector)))
      )
    {} mappings)
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
