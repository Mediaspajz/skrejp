(ns clj-scrapers.core
  (:require [clj-scrapers.scrapers :refer [defscraper scrape]])
  )

(defscraper "www.bumm.sk"
  { :title   [:div#content :div#article_detail_title]
    :summary [:div#content :div#article_detail_lead]
    :content [:div#content :div#article_detail_text] }
  )

(defscraper "felvidek.ma"
  { :title   [:article :header.article-header :h1.article-title :a]
    :content [:section.article-content] }
  )

(defscraper "ujszo.com"
  { :title   [:div.node.node-article :h1]
    :loc     [:div.node.node-article :div.field-name-field-lead :span.place]
    :summary [:div.node.node-article :div.field-name-field-lead :p]
    :content [:div.node.node-article :div.field-name-body] }
  )

(derive :clj-scrapers.scrapers/vasarnap.ujszo.com :clj-scrapers.scrapers/ujszo.com)

(defscraper "www.parameter.sk"
  { :title   [:div#page_container :div#content :h1]
    :summary [:div#content :div.field-name-field-lead :p]
    :content [:div#content :div.node-content] }
  )

(defscraper "www.hirek.sk"
  { :title    [:span.tcikkcim]
    :summary  [:span#tcikkintro]
    :content  [:div#tcikktext] }
  )
