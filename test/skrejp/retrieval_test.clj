(ns skrejp.retrieval-test
  (:require [skrejp.retrieval :as retrieval])
  (:require [expectations :refer :all])
  (:require [org.httpkit.client :as http])
  (:use     org.httpkit.fake)
  (:require [clojure.core.async :refer [go <! >! <!!] :as async])
  )

(def http-req-opts {:timeout    10 ; ms
                    :user-agent "User-Agent-string"
                    :headers    {"X-Header" "Value"} } )

(with-fake-http
  [ "http://example.com/p1" "foo"
    "http://example.com/p2" "bar" ]
  (let
    [ret-cmpnt (retrieval/build-component {:http-req-opts http-req-opts})
     c (async/chan 2 (retrieval/fetch-page ret-cmpnt)) ]
    (go (>! c "http://example.com/p1") (>! c "http://example.com/p2") )
    (<!! (async/timeout (http-req-opts :timeout)))
    (expect "foo" (:body (<!! c)))
    (expect "bar" (:body (<!! c)))
    (async/close! c)
    )
  )
