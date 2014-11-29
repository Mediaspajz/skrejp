(ns skrejp.retrieval-test
  (:require [skrejp.retrieval :as retrieval])
  (:require [expectations :refer :all])
  (:require [org.httpkit.client :as http])
  (:use     org.httpkit.fake)
  (:require [clojure.core.async :refer [chan go <! >! <!!] :as async]))

(def http-req-opts {:timeout    10 ; ms
                    :user-agent "User-Agent-string"
                    :headers    {"X-Header" "Value"}})

(with-fake-http
  [ "http://example.com/p1" "foo"
    "http://example.com/p2" "bar" ]
  (let
    [ret-cmpnt (retrieval/build-component {:http-req-opts http-req-opts})
     in-c  (async/chan 2)
     out-c (async/chan 2)]
    (async/pipeline-async 2 out-c (retrieval/fetch-page ret-cmpnt) in-c)
    (go (>! in-c {:url "http://example.com/p1"}) (>! in-c {:url "http://example.com/p2"}))
    (<!! (async/timeout (http-req-opts :timeout)))
    (expect "foo" (:http-payload (<!! out-c)))
    (expect "bar" (:http-payload (<!! out-c)))
    (async/close! out-c) ) )

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
         <link>http://example.com/bar.html</link>
         </item>
       </channel>
     </rss>" ]
  (let
    [ret-cmpnt (retrieval/build-component {:http-req-opts http-req-opts})
     c (async/chan 1 (retrieval/fetch-feed ret-cmpnt))]
    (go (>! c "http://example.com/rss.xml"))
    (let
      [feed (<!! c) entries (:entries feed)]
      (expect 2 (-> feed :entries count))
      (expect "Foo" (-> entries first  :title) )
      (expect "http://example.com/bar.html" (-> entries second :link)))))
