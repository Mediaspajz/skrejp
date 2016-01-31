(ns skrejp.scraper-test
  (:require [skrejp.scraper.component :as scraper]
            [skrejp.scraper.ann :as scraper-ann])
  (:require [clojurewerkz.urly.core :as urly])
  (:require [expectations :refer :all]
            [skrejp.core :as core]
            [skrejp.logger.ann :as logger]))

(let
  [inp-c (core/doc-chan)
   out-c (core/doc-chan)
   scraper-cmpnt (assoc
                   (scraper/build-component
                     {:scraper-defs
                               {"example.com"     {:title   [:h1#title]
                                                   :content [:div#content]
                                                   :path    (fn [doc]
                                                              (-> doc :url urly/url-like urly/path-of))}
                                "www.example.com" "example.com"}
                      :improve (fn [doc attr val]
                                 (if (and (= attr :title) (not= (attr doc) val))
                                   (throw (ex-info "Can not change already defined title" {:cause :scraping-error}))
                                   (assoc doc attr val)))}
                     {:inp-doc-c inp-c :out-doc-c out-c})
                   :logger (reify logger/ILogger (info [_ _]) (debug [_ _])))
   invalid-page-body "<html><body><h1 id='title'>Changed Title</h1><div id='content'>Changed Content</div></body></html>"
   invalid-page-resp {:title "Foo Title"
                      :url "http://www.example.com/invalid.html"
                      :http-payload invalid-page-body}
   valid-page-body "<html><body><h1 id='title'>Foo Title</h1><div id='content'>Bar Content</div></body></html>"
   valid-page-resp {:title "Foo Title"
                    :url "http://www.example.com/index.html"
                    :http-payload valid-page-body}
   articles (into [] (scraper-ann/scrape scraper-cmpnt) [valid-page-resp invalid-page-resp])
   article (first articles)
   ]
  (expect 1 (count articles))
  (expect "http://www.example.com/index.html" (article :url))
  (expect "Foo Title"   (article :title))
  (expect "Bar Content" (article :content))
  (expect "/index.html" (article :path))
  )

(def page-body
  "<body><div id='section'><time datetime='2015-05-27T15:11:40+00:00'>2015-05-27</time></div></body>")

(expect "2015-05-27" (scraper/extract-tag {:http-payload page-body} [:div#section :time]))

(expect "2015-05-27T15:11:40+00:00"
        (scraper/extract-attr {:http-payload page-body} [:div#section :time] :datetime))

