(ns skrejp.app
  (:require [com.stuartsierra.component :as component])
  (:require [clojure.core.async :as async :refer [<!!]])
  (:require [clj-time.core :as t])
  (:require [skrejp.system :as system]) )

(defn parse-int [s]
  (Integer/parseInt s))

(defn find-date-in-url [doc]
  (let
    [[_ year month day] (re-find #"/(\d+)/(\d+)/(\d+)" (doc :url))]
    (apply t/date-time (map parse-int [year month day]))))

(def config-options
  {:feeds
     ["http://ujszo.com/rss.xml"
      "http://vasarnap.ujszo.com/rss.xml"
      "http://www.bumm.sk/rss/rss.xml"
      "http://www.felvidek.ma/?format=feed&type=rss"
      "http://www.parameter.sk/rss.xml"
      "http://www.hirek.sk/rss/hirek.xml"]
   :scraper-defs
     {"www.bumm.sk"      {:title   [:h2#page_title]
                          :summary [:div.page_lead]
                          :content [:div.page_body]
                          :published_at find-date-in-url}
      "felvidek.ma"      {:title   [:article :header.article-header :h1.article-title :a]
                          :content [:section.article-content]}
      "ujszo.com"        {:title   [:div.node.node-article :h1]
                          :loc     [:div.node.node-article :div.field-name-field-lead :span.place]
                          :summary [:div.node.node-article :div.field-name-field-lead :p]
                          :content [:div.node.node-article :div.field-name-body]
                          :published_at find-date-in-url}
      "www.parameter.sk" {:title   [:div#content :h1]
                          :summary [:div#content :div#field-name-field-lead]
                          :content [:div#content :div#field-name-body]
                          :published_at find-date-in-url}
      "www.hirek.sk"     {:title   [:span.tcikkcim]
                          :summary [:span#tcikkintro]
                          :content [:div#tcikktext]
                          :published_at (fn [doc]
                                          (let
                                            [[_ y mo d h mi s]
                                             (re-find #"/(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})" (doc :url))]
                                            (apply t/date-time
                                                   (map parse-int [y mo d h mi s]))))}
      "vasarnap.ujszo.com" "ujszo.com"}
   :http-req-opts
     {:timeout    200 ; ms
      :user-agent "User-Agent-string"
      :headers    {"X-Header" "Value"}}})

(def scraper-system (system/build-scraper-system config-options))

(defn start-scraper-system
  "starts the scraper system."
  []
  (alter-var-root (var scraper-system) component/start))

(defn stop-scraper-system
  "Stops the passed in system"
  []
  (alter-var-root (var scraper-system) component/stop))

(defn -main []
  (start-scraper-system)
  (<!! (async/timeout 10000)))
