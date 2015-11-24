(ns skrejp.retrieval.component
  (:use [skrejp.retrieval.ann])
  (:require [skrejp.core :as core])
  (:require [clojure.core.typed :as t])
  (:require [clojure.core.async :as async :refer [<! >! <!! chan go-loop]])
  (:require [skrejp.logger.ann :as logger])
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
          (let
            [host (urly/host-of (urly/url-like (doc :url)))
             host-c (get-host-c this host-chans host)]
            (>! host-c doc)
            (recur (<! inp-doc-c) (assoc host-chans host host-c))))))
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
             (xf result (-> resp :body parse-feed-str)))))))))

(t/defn build-component
  "Build a PageRetrieval component."
  [conf-opts :- (t/HMap :mandatory {:http-req-opts core/THttpReqOpts})
   chans :- (t/HMap :mandatory {:inp-doc-c core/TDocChan :out-doc-c core/TDocChan})] :- RetrievalComponent
  (map->RetrievalComponent {:http-req-opts (:http-req-opts conf-opts)
                            :inp-doc-c     (:inp-doc-c chans)
                            :out-doc-c     (:out-doc-c chans)}))
