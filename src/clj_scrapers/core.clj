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

(defrecord Article [title summary body full-page url author published-at])

(defn create-article [attrs]
  (merge (Article. nil nil nil nil nil nil nil) attrs)
  )

(defn fetch-page [url scrape]
  (http/get url http-options scrape)
  )

(defmulti scrape classify-url-source)

(defmethod scrape ::www.bumm.sk [url]
  (let [ page (promise) ]
    (fetch-page url
      (fn [{:keys [status headers body error]}]
        (deliver page
                 (create-article
                  { :title (-> (java.io.StringReader. body)
                               html/html-resource
                               (html/select [:div#content :div#article_detail_title])
                               first :content first )
                    :url url }
                  )
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
