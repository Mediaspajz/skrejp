(ns clj-scrapers.archives
  (:require [clj-scrapers.core])
  (:require [clj-scrapers.scrapers  :refer [scrape fetch-page extract-sel extract-href
                                            classify-url-source source-keyword]])
  (:require [clojure.core.async     :refer [<!! >! <! chan go onto-chan] :as async])
  (:require [clojurewerkz.urly.core :as urly])
  (:require [org.httpkit.client     :as http])
  (:require [net.cgrand.enlive-html :as html])
  )

(defn scrape-index-page [url article-urls-c]
  (let
      [ page-result-c
        (fetch-page url
                    (fn [body]
                      (map #(get-in % [:attrs :href])
                           (extract-sel body [:div#content :span.field-content :a]))
                      )) ]
    (go (onto-chan article-urls-c
                   (map (partial urly/resolve url)
                        (<! page-result-c)))
        (async/close! article-urls-c)
        )))

(defn walk-index-pages [initial-url index-page-urls-c]
  (go
    (>! index-page-urls-c initial-url)
    (loop [current-url initial-url]
      (let [next-url-c
            (fetch-page current-url
                        #(extract-href % [:div#content :ul.pager :li.pager-next :a]))
            next-url (urly/resolve initial-url (<! next-url-c)) ]
        (>! index-page-c next-url)
        (recur next-url)
        ))
    (async/close! index-page-urls-c)
    )
  )

(defn sink [f c]
  (go (loop []
        (when-some [v (<! c)]
                   (f v)
                   (recur)))))

(defn -main [& args]
  (let [index-page-c (scrape-next-indexes "http://ujszo.com/online")]
    (dotimes [n 10]
      (let [ index-page-url (<!! index-page-c)
             article-urls-c (scrape-index index-page-url) ]
        (sink (fn [article-url]
                (let [ article-page-c (scrape article-url)
                       article        (<!! article-page-c) ]
                  (printf "%s\nTITLE: %s\nSUMMARY: %s\n\n" (:url article) (:title article) (:summary article))
                  )
                )
              article-urls-c)
        )
      )
    )
  )
