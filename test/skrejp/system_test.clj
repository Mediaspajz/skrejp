(ns skrejp.system-test
  (:require [com.stuartsierra.component :as component])
  (:require [expectations :refer :all])
  (:require [skrejp.system :as sys])
  (:use     org.httpkit.fake))

(def http-req-opts {:timeout    10 ; ms
                    :user-agent "User-Agent-string"
                    :headers    {"X-Header" "Value"} } )

(def config-opts {:http-req-opts {:timeout    10 ; ms
                                  :user-agent "User-Agent-string"
                                  :headers    {"X-Header" "Value"} }
                  :scraper-defs {:example.com {:title   [:h3#title]
                                               :content [:div.content] }
                                 :usa.example.com :example.com}
                  :feeds ["http://example.com/rss.xml"]} )

(def test-system (sys/build-scraper-system config-opts))

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
        <h3 id='title'>Foo Title</h1>
        <div class='content'>Foo Content</div>
      </body>"

     "http://usa.example.com/bar.html"
     "<body>
        <h3 id='title'>Bar Title</h1>
        <div class='content'>Bar Content</div>
      </body>" ]
  (let
    []
    (alter-var-root (var test-system) component/start)
    (alter-var-root (var test-system) component/stop)
    )
  )