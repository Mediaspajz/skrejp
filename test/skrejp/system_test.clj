(ns skrejp.system-test
  (:require [skrejp.logger :as logger])
  (:require [clojure.core.async :as async :refer [go go-loop chan <! <!! >!]])
  (:require [com.stuartsierra.component :as component])
  (:require [expectations :refer :all])
  (:require [skrejp.system :as sys])
  (:use     org.httpkit.fake))

(def http-req-opts {:timeout    10 ; ms
                    :user-agent "User-Agent-string"
                    :headers    {"X-Header" "Value"} } )

(def config-opts {:http-req-opts http-req-opts
                  :scraper-defs  {"example.com" {:title   [:h3#title]
                                                 :content [:div.content]
                                                 :title_length (fn [doc] (count (doc :title)))}
                                  "usa.example.com" "example.com"}
                  :feeds ["http://example.com/rss.xml"]})

(def out-c (chan 2))

(def test-system
  (sys/build-scraper-system
    config-opts {:logger  (reify logger/ILogger (info [_ _])),
                 :storage {:doc-c out-c}}))

(with-fake-http
  [ "http://example.com/rss.xml"
    "<?xml version=\"1.0\" encoding=\"utf-8\" ?>
     <rss version=\"2.0\" xml:base=\"http://example.com/rss.xml\">
       <channel>
         <item>
         <title>Foo</title>
         <link>http://example.com/foo.html</link>
         </item>
         <item>
         <title>Bar</title>
         <link>http://usa.example.com/bar.html</link>
         </item>
       </channel>
     </rss>"

     "http://example.com/foo.html"
     "<body>
        <h3 id='title'>Foo Title</h3>
        <div class='content'>Foo Content</div>
      </body>"

     "http://usa.example.com/bar.html"
     "<body>
        <h3 id='title'>Bar Title</h3>
        <div class='content'>Bar Content</div>
      </body>" ]
  (do
    (alter-var-root (var test-system) component/start)
    (let
      [[res1 res2] (<!! (async/into [] out-c))]
      (expect "Foo Title" (:title res1))
      (expect "Bar Title" (:title res2))
      (expect 9 (:title_length res2))
      (expect "http://example.com/foo.html"     (:url res1))
      (expect "http://usa.example.com/bar.html" (:url res2))
    (alter-var-root (var test-system) component/stop))))