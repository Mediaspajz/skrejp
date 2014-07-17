(ns clj-scrapers.core
  (:require [clojurewerkz.urly.core :refer [url-like host-of]])
  (:require [org.httpkit.client :as http])
  (:require [net.cgrand.enlive-html :as html])
  )

(def scrapers-ns *ns*)


(defn classify-url-source [url]
  (keyword (str scrapers-ns) (host-of (url-like url)))
  )

(defrecord Article [title summary body full-page url author published-at])

(defmulti scrape classify-url-source)

(defmethod scrape ::www.bumm.sk [url]
  (let
    [ page @(http/get url)
      body (:body page) ]
    (-> (java.io.StringReader. body)
        html/html-resource
        (html/select [:div#content :div#article_detail_title])
        first :content first )
    )
  )

;(defn load-page
  ;"Loads the page"
  ;(http/get "http://www.bumm.sk/index.php?show=97202")
  ;)
