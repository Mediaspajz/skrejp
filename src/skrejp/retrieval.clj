(ns skrejp.retrieval
  (:require [skrejp.core :as core])
  (:require [clojure.core.typed :as t])
  (:require [clojure.core.async :as async :refer [<! >! <!! chan go-loop]])
  (:require [skrejp.logger :as logger]
            [skrejp.storage :as storage])
  (:require [com.stuartsierra.component :as component])
  (:require [org.httpkit.client :as http])
  (:require [clojure.core.async :as async]
            [clojurewerkz.urly.core :as urly]
            [feedparser-clj.core :as feeds])
  (:import (java.io ByteArrayInputStream)))

(t/ann ^:no-check feedparser-clj.core/parse-feed [(t/U t/Str ByteArrayInputStream) -> t/HMap])

(t/defn parse-feed-str
  "Parses the feed passed in as a string."
  [^String feed-s :- t/Str] :- t/HMap
  (let
    [input-stream (ByteArrayInputStream. (.getBytes feed-s "UTF-8"))]
    (feeds/parse-feed input-stream)))

;TODO: type annotations very weak here
(t/defprotocol IRetrieval
  "## IRetrieval
  Defines methods for fetching pages.
  *fetch-page* is a transducer for fetching a page from a url.
  It expects the URL of the resource and it is pushing the fetch page to the channel it is applied on.
  If the error-fn is passed, it calls the error-fn function in case of an error."
  (fetch-page [this :- IRetrieval] :- (t/IFn [t/Any t/Any -> t/Any]))
  (fetch-feed [this :- IRetrieval] :- (t/IFn [t/Any -> t/Any])))

(t/tc-ignore
  (defn get-host-c [setup host-chans host]
    (if (contains? host-chans host)
      (host-chans host)
      (let [new-host-c (chan 512)]
        (async/pipeline-async 5 (:out-doc-c setup) (fetch-page setup) new-host-c)
        new-host-c))))

(t/ann-record RetrievalComponent [http-req-opts :- core/THttpReqOpts
                                  inp-doc-c :- core/TDocChan
                                  out-doc-c :- core/TDocChan])

(defrecord RetrievalComponent [http-req-opts inp-doc-c out-doc-c]
  component/Lifecycle

  (start
    [this]
    (t/tc-ignore
      (logger/info (:logger this) "PageContentRetrieval: Starting")
      (go-loop [doc (<! inp-doc-c) host-chans {}]
        (when-not (nil? doc)
          (logger/info (:logger this) (format "PageContentRetrieval: Received %s" (doc :url)))
          (let [doc-w-id (assoc doc :id (doc :url))]
            (if (storage/contains-doc? (:storage this) doc-w-id)
              (recur (<! inp-doc-c) host-chans)
              (let
                [host (urly/host-of (urly/url-like (doc :url)))
                 host-c (get-host-c this host-chans host)]
                (>! host-c doc-w-id)
                (recur (<! inp-doc-c) (assoc host-chans host host-c))))))))
    this)

  (stop [this]
    (t/tc-ignore
      (logger/info (:logger this) "PageContentRetrieval: Stopping"))
    this)

  IRetrieval

  (fetch-page [this]
    (fn [doc c]
      (t/tc-ignore
        (http/get (doc :url) (:http-req-opts this)
                  (fn [{:keys [error] :as resp}]
                    (when-not error
                      (async/put! c (assoc doc :http-payload (resp :body))))
                    (async/close! c))))))

  (fetch-feed [this]
    (fn [xf]
      (t/tc-ignore
        (fn ([] (xf)) ([result] (xf result))
          ([result url]
           (let
             [resp @(http/get url (:http-req-opts this))]
             (xf result (-> resp :body parse-feed-str))))))))
  )

(t/defn build-component
  "Build a PageRetrieval component."
  [config-options :- (t/HMap :mandatory {:http-req-opts core/THttpReqOpts})] :- RetrievalComponent
  (map->RetrievalComponent {:http-req-opts (:http-req-opts config-options)
                            :inp-doc-c (core/doc-chan)
                            :out-doc-c (core/doc-chan)}))
