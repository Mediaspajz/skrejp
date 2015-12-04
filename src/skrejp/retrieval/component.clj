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

(def ^:private inner-retrieval-chan (chan 512))

(t/tc-ignore
  (defn get-host-c [retr-cmpnt {:keys [key-chans key out-c thread-cnts-fn url-fn process-fn]}]
    (if (contains? key-chans key)
      (key-chans key)
      (let [new-host-c (chan 512)]
        (async/pipeline-async (thread-cnts-fn key) inner-retrieval-chan (fetch-page retr-cmpnt {:out-c out-c :url-fn url-fn :process-fn process-fn}) new-host-c)
        (go-loop [res (<! inner-retrieval-chan)]
          (when-not (nil? res)
            (let [[out-c doc] res]
              (>! out-c doc)
              (recur (<! inner-retrieval-chan)))))
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

      (build-retrieval-chan this {:key-fn         (fn [doc] (urly/host-of (urly/url-like (doc :url))))
                                  :thread-cnts-fn (fn [_key] 5)
                                  :process-fn     (fn [doc resp]
                                                    (when-not (:error resp)
                                                      (assoc doc :http-payload (resp :body))))
                                  :inp-c          inp-doc-c
                                  :out-c          out-doc-c
                                  :url-fn         :url}))
    this)

  (stop [this]
    (t/tc-ignore
      (logger/info (:logger this) "PageContentRetrieval: Stopping"))
    this)

  IRetrieval

  (build-retrieval-chan
    [this {:keys [key-fn thread-cnts-fn process-fn inp-c out-c url-fn]}]
    (go-loop [doc (<! inp-c) key-chans {}]
      (when-not (nil? doc)
        (let
          [key (key-fn doc)
           host-c (get-host-c this {:key-chans key-chans
                                    :key key
                                    :out-c out-c
                                    :thread-cnts-fn thread-cnts-fn
                                    :url-fn url-fn
                                    :process-fn process-fn})]
          (>! host-c doc)
          (recur (<! inp-c) (assoc key-chans key host-c))))))

  (fetch-page [this {:keys [out-c url-fn process-fn]}]
    (fn [doc c]
      (t/tc-ignore
        (http/get (url-fn doc) (:http-req-opts this)
                  (fn [resp]
                    (let [value (process-fn doc resp)]
                      (when-not (nil? value) (async/put! c [out-c value])))
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
