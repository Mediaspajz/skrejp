(ns clj-scrapers.core
  (:require [org.httpkit.client :as http])
  (:require [net.cgrand.enlive-html :as enlive])
  )


;(defn load-page
  ;"Loads the page"
  ;(http/get "http://www.bumm.sk/index.php?show=97202")
  ;)

 (defn classify-url-source [url]
   (condp re-find url
    #"^http://ujszo\.com/"           :ujszo.com
    #"^http://(www\.){0,1}bumm\.sk/" :bumm.sk
     nil
    )
   )
