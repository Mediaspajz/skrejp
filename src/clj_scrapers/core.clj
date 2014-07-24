(ns clj-scrapers.core
  (:require [clojurewerkz.urly.core :refer [url-like host-of]])
  (:require [org.httpkit.client :as http])
  (:require [net.cgrand.enlive-html :as html])
  )

(def scrapers-ns *ns*)

(def http-options { :timeout    1000
                    :user-agent "User-Agent-string" } )

(defn classify-url-source [url]
  (keyword (str scrapers-ns) (host-of (url-like url)))
  )

(defn fetch-page [url scrape]
  (let [ page (promise) ]
    (http/get url http-options
      (fn [{:keys [status headers body error]}] (deliver page (scrape body))))
    page
    )
  )

(defn extract-tag [body selector]
  (-> (java.io.StringReader. body)
      html/html-resource
      (html/select selector)
      first :content first)
  )

(defmulti scrape classify-url-source)

(defmethod scrape ::www.bumm.sk [url]
  (fetch-page url
    (fn [body]
      { :title (extract-tag body [:div#content :div#article_detail_title])
        :url url }
      )
    )
  )

;(defn load-page
  ;"Loads the page"
  ;(http/get "http://www.bumm.sk/index.php?show=97202")
  ;)
