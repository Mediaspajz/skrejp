(ns clj-scrapers.core
  (:require [org.httpkit.client :as http])
  (:require [net.cgrand.enlive-html :as enlive])
  )


;(defn load-page
  ;"Loads the page"
  ;(http/get "http://www.bumm.sk/index.php?show=97202")
  ;)

 (defn classify-url-source [url]
   (cond
    (re-find #"^http://ujszo\.com/" url)   :ujszo.com
    (re-find #"^http://(www\.){0,1}bumm\.sk/" url) :bumm.sk
    )
   )
