(ns clj-scrapers.core
  ;(:require [org.httpkit.client :as http])
  ;(:require [net.cgrand.enlive-html :as enlive]
  (:require [clojurewerkz.urly.core :refer [url-like host-of]])
  )


;(defn load-page
  ;"Loads the page"
  ;(http/get "http://www.bumm.sk/index.php?show=97202")
  ;)

(defn classify-url-source [url]
  (keyword (host-of (url-like url)))
  )
