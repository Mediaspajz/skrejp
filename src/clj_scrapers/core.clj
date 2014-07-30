(ns clj-scrapers.core
  (:require [clojurewerkz.urly.core :refer [url-like host-of]])
  (:require [clojure.string :refer [join trim]])
  (:require [org.httpkit.client :as http])
  (:require [net.cgrand.enlive-html :as html])
  )

(def scrapers-ns *ns*)

(def http-options { :timeout    1000
                    :user-agent "Mozilla/5.0 (Windows NT 5.2; rv:2.0.1) Gecko/20100101 Firefox/4.0.1" } )

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
      first
      html/text)
  )

(defmulti scrape classify-url-source)

(defmacro defscraper [source mappings]
  `(defmethod scrape ~source [url#]
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

(defscraper ::www.bumm.sk
  { :title   [:div#content :div#article_detail_title]
    :summary [:div#content :div#article_detail_lead]
    :content [:div#content :div#article_detail_text] }
  )

(defscraper ::felvidek.ma
  { :title   [:article :header.article-header :h1.article-title :a]
    :content [:section.article-content] }
  )

(defscraper ::ujszo.com
  { :title   [:div.node.node-article :h1]
    :loc     [:div.node.node-article :div.field-name-field-lead :span.place]
    :summary [:div.node.node-article :div.field-name-field-lead :p]
    :content [:div.node.node-article :div.field-name-body] }
  )

(derive ::vasarnap.ujszo.com ::ujszo.com)

(defscraper ::www.parameter.sk
  { :title   [:div#page_container :div#content :h1]
    :summary [:div#content :div.field-name-field-lead :p]
    :content [:div#content :div.node-content] }
  )

(defscraper ::www.hirek.sk
  { :title    [:span.tcikkcim]
    :summary  [:span#tcikkintro]
    :content  [:div#tcikktext] }
  )
