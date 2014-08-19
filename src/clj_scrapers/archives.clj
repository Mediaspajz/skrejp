(ns clj-scrapers.archives
  (:require [clj-scrapers.scrapers  :refer [fetch-page extract-sel extract-href]])
  (:require [clojure.core.async     :refer [<!! >! <! chan go go-loop] :as async])
  (:require [clojurewerkz.urly.core :as urly])
  (:require [org.httpkit.client     :as http])
  (:require [net.cgrand.enlive-html :as html])
  )

(defn scrape-next-index-pages [initial-url]
  (let [ index-page-c (chan 100) ]
    (go
      (>! index-page-c initial-url)
      (loop [current-url initial-url]
        (let [next-url-c
              (fetch-page current-url
                (fn [body]
                  (extract-href body [:div.content :ul.pager :li.pager-next :a])
                  )
                )
              next-url (urly/resolve initial-url (<! next-url-c)) ]
          (>! index-page-c next-url)
          (recur next-url)
          )
        )
      )
      index-page-c
    )
  )

(defn scrape-index-page [url]
  (fetch-page url
    (fn [body]
      (map
        #(get-in % [:attrs :href])
        (extract-sel body [:div.content :span.field-content :a])
        )
      )
    )
  )

(defn -main [& args]
  (let
    [c (scrape-next-index-pages "http://ujszo.com/cimkek/online-archivum")
     v1 (<!! c)
     v2 (<!! c)
     v3 (<!! c)]
    (println v1 v2 v3)
    )
  )
