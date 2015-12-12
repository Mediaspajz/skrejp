(ns skrejp.retrieval.component
  (:use [skrejp.retrieval.ann])
  (:require [skrejp.core :as core])
  (:require [clojure.core.typed :as t])
  (:require [clojure.core.async :as async :refer [<! >! <!! chan go-loop]])
  (:require [skrejp.logger.ann :as logger])
  (:require [com.stuartsierra.component :as component])
  (:require [org.httpkit.client :as http])
  (:require [clojure.core.async :as async])
  (:import (java.io ByteArrayInputStream)))

(t/ann ^:no-check feedparser-clj.core/parse-feed [(t/U t/Str ByteArrayInputStream) -> t/HMap])

(defn fetch-page [{:keys [http-req-opts]}]
  (fn [[doc {:keys [url-fn process-fn] :as all-keys}] c]
    (http/get (url-fn doc) http-req-opts
              (fn [resp]
                (let [values (process-fn doc resp)]
                  (if (nil? values)
                    (async/close! c)
                    (async/onto-chan c (map (fn [value] [value all-keys]) values))))))))

(defn get-host-c [{:keys [http-req-opts key-chans key thread-cnts-fn result-c]}]
  (if (contains? key-chans key)
    (key-chans key)
    (let [new-host-c (chan 512)]
      (async/pipeline-async (thread-cnts-fn key) result-c
                            (fetch-page {:http-req-opts http-req-opts})
                            new-host-c)
      (go-loop [res (<! result-c)]
        (when-not (nil? res)
          (let [[doc {:keys [out-c]}] res]
            (>! out-c doc)
            (recur (<! result-c)))))
      new-host-c)))

(defprotocol IRetrievalPipelineBuilder
  (pipeline-retrieval
    [this out-c inp-c {:keys [key-fn process-fn url-fn key-chans]}]))

(defrecord RetrievalPlumbing
  [http-req-opts thread-cnts-fn result-c key-chans]

  IRetrievalPipelineBuilder

  (pipeline-retrieval
    [_this out-c inp-c {:keys [key-fn process-fn url-fn]}]
    (go-loop [doc (<! inp-c)]
      (when-not (nil? doc)
        (let
          [key (key-fn doc)
           host-c (get-host-c {:http-req-opts  http-req-opts
                               :key-chans      @key-chans
                               :key            key
                               :thread-cnts-fn thread-cnts-fn
                               :result-c       result-c})]
          (>! host-c [doc {:out-c out-c :url-fn url-fn :process-fn process-fn}])
          (swap! key-chans assoc key host-c)
          (recur (<! inp-c)))))))

(defn build-retrieval-plumbing [params]
  (map->RetrievalPlumbing
    (assoc params :result-c (chan 512) :key-chans (atom {}))))

(defn build-retrieval-pipeline
  ([plumbing out-c inp-c params]
   (pipeline-retrieval plumbing out-c inp-c
                       (select-keys params [:key-fn :process-fn :url-fn]))
    plumbing)
  ([out-c inp-c params]
   (let [plumbing (build-retrieval-plumbing
                    (select-keys params [:http-req-opts :thread-cnts-fn]))]
     (build-retrieval-pipeline plumbing out-c inp-c params)
     plumbing)))

(t/ann-record RetrievalComponent [inp-doc-c :- core/TDocChan
                                  out-doc-c :- core/TDocChan])

(defrecord RetrievalComponent
  [retrieval-plumbing key-fn process-fn url-fn inp-doc-c out-doc-c]
  component/Lifecycle

  (start [this]
    (t/tc-ignore
      (logger/info (:logger this) "PageContentRetrieval: Starting")

      (build-retrieval-pipeline
        retrieval-plumbing out-doc-c inp-doc-c
        {:key-fn key-fn :process-fn process-fn :url-fn url-fn}))
    this)

  (stop [this]
    (t/tc-ignore
      (logger/info (:logger this) "PageContentRetrieval: Stopping"))
    this))

(t/defn build-component
  "Build a PageRetrieval component."
  [retrieval-plumbing
   opts
   chans :- (t/HMap :mandatory {:inp-doc-c core/TDocChan :out-doc-c core/TDocChan})] :- RetrievalComponent
  (map->RetrievalComponent {:retrieval-plumbing retrieval-plumbing
                            :key-fn             (:key-fn opts)
                            :process-fn         (:process-fn opts)
                            :url-fn             (:url-fn opts)
                            :inp-doc-c          (:inp-doc-c chans)
                            :out-doc-c          (:out-doc-c chans)}))
