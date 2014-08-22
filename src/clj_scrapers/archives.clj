(ns clj-scrapers.archives
  (:require [clj-scrapers.scrapers  :refer [fetch-page extract-sel extract-href
                                            classify-url-source source-keyword]])
  (:require [clojure.core.async     :refer [<!! >! <! chan go go-loop] :as async])
  (:require [clojurewerkz.urly.core :as urly])
  (:require [org.httpkit.client     :as http])
  (:require [net.cgrand.enlive-html :as html])
  )

(defn- gen-scrape-index
  [index-href-sel]
  (defn scrape-index-page [url]
    (fetch-page url
                (fn [body] (map #(get-in % [:attrs :href])
                                (extract-sel body index-href-sel))))))

(defn- gen-scrape-next-indexes
  [href-sel]
  (fn [initial-url]
    (let [ index-page-c (chan 100) ]
      (go
        (>! index-page-c initial-url)
        (loop [current-url initial-url]
          (let [next-url-c
                (fetch-page current-url
                            (fn [body] (extract-href body href-sel)))
                next-url (urly/resolve initial-url (<! next-url-c)) ]
            (>! index-page-c next-url)
            (recur next-url)
            )))
      index-page-c
      )))

(defmulti scrape-index        classify-url-source)
(defmulti scrape-next-indexes classify-url-source)

(defmacro defindexscraper
  "Define an index scraper for a source."
  [source index-link-sel next-index-link-sel]
  `(let [ source-kw#           ~(source-keyword (name source))
          scrape-index#        (gen-scrape-index        ~index-link-sel)
          scrape-next-indexes# (gen-scrape-next-indexes ~next-index-link-sel) ]
       (defmethod scrape-index        source-kw# [url#] (apply scrape-index# [url#]))
       (defmethod scrape-next-indexes source-kw# [url#] (apply scrape-next-indexes# [url#]))
     ))

(defindexscraper "ujszo.com"
                 [:div.content :span.field-content :a]
                 [:div.content :ul.pager :li.pager-next :a])

(defn -main [& args]
  (let
      [c (scrape-next-indexes "http://ujszo.com/cimkek/online-archivum")
       v1 (<!! c)
       v2 (<!! c)
       v3 (<!! c)]
    (println v1 v2 v3)
    ))
