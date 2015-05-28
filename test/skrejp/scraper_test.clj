(ns skrejp.scraper-test
  (:require [skrejp.scraper :as scraper])
  (:require [clojurewerkz.urly.core :as urly])
  (:require [expectations :refer :all]))

(let
  [scraper-cmpnt (scraper/build-component
                   {:scraper-defs
                    {"example.com" {:title   [:h1#title]
                                    :content [:div#content]
                                    :path    (fn [doc]
                                               (-> doc :url
                                                   urly/url-like urly/path-of))}
                     "www.example.com" "example.com"}})
   page-body "<html><body><h1 id='title'>Foo Title</h1><div id='content'>Bar Content</div></body></html>"
   page-resp { :url "http://www.example.com/index.html" :http-payload page-body }
   article (first (into [] (scraper/scrape scraper-cmpnt) [page-resp])) ]
  (expect "http://www.example.com/index.html" (article :url))
  (expect "Foo Title"   (article :title))
  (expect "Bar Content" (article :content))
  (expect "/index.html" (article :path)))

(def page-body
  "<body><div id='section'><time datetime='2015-05-27T15:11:40+00:00'>2015-05-27</time></div></body>")

(expect "2015-05-27" (scraper/extract-tag {:http-payload page-body} [:div#section :time]))

(expect "2015-05-27T15:11:40+00:00"
        (scraper/extract-attr {:http-payload page-body} [:div#section :time] :datetime))
