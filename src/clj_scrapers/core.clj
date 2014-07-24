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
  (http/get url http-options scrape)
  )

(defn extract-tag-content [body selector]
  (-> (java.io.StringReader. body)
      html/html-resource
      (html/select selector)
      first :content first)
  )

(defmulti scrape classify-url-source)

(defmethod scrape ::www.bumm.sk [url]
  (let [ page (promise) ]
    (fetch-page url
      (fn [{:keys [status headers body error]}]
        (deliver page
          { :title (extract-tag-content body [:div#content :div#article_detail_title])
            :url url }
          )
        )
      )
    page
    )
  )

;(defn load-page
  ;"Loads the page"
  ;(http/get "http://www.bumm.sk/index.php?show=97202")
  ;)
