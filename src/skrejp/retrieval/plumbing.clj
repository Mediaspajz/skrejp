(ns skrejp.retrieval.plumbing
  (:require [org.httpkit.client :as http])
  (:require [clojure.core.async :as async])
  (:require [clojure.core.async :as async :refer [<! >! <!! chan go-loop]]))

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
