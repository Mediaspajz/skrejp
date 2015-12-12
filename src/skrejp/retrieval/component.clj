(ns skrejp.retrieval.component
  (:use [skrejp.retrieval.ann])
  (:require [skrejp.core :as core])
  (:require [skrejp.retrieval.plumbing :as plumbing])
  (:require [clojure.core.typed :as t])
  (:require [skrejp.logger.ann :as logger])
  (:require [com.stuartsierra.component :as component]))

(defn build-retrieval-pipeline
  ([plumbing out-c inp-c params]
   (plumbing/pipeline-retrieval plumbing out-c inp-c
                       (select-keys params [:key-fn :process-fn :url-fn]))
    plumbing)
  ([out-c inp-c params]
   (let [plumbing (plumbing/build-retrieval-plumbing
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
