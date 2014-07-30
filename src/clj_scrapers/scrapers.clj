(ns clj-scrapers.scrapers
  (:require [clojurewerkz.urly.core :refer [url-like host-of]])
  (:require [clojure.string :refer [join trim]])
  (:require [org.httpkit.client :as http])
  (:require [net.cgrand.enlive-html :as html])
  )

(def this-ns *ns*)

(def http-options { :timeout    1000
                    :user-agent "Mozilla/5.0 (Windows NT 5.2; rv:2.0.1) Gecko/20100101 Firefox/4.0.1" } )

(defn classify-url-source [url]
  (keyword (str this-ns) (host-of (url-like url)))
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
      first
      html/text)
  )

(defmulti scrape classify-url-source)

(defmacro defscraper [source mappings]
  `(defmethod scrape (keyword "clj-scrapers.scrapers" (name ~source)) [url#]
    (fetch-page url#
      (fn [body#]
        (reduce
         (fn [scraped-content# [attr# selector#]]
           (assoc scraped-content# attr# (trim (extract-tag body# selector#)))
           )
         { :url url# } ~mappings
         )
        )
      )
    )
  )
