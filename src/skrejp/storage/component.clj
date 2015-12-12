(ns skrejp.storage.component
  (:use [skrejp.storage.ann])
  (:require [skrejp.logger.ann :as logger]
            [skrejp.core :as core])
  (:require [clojure.core.async :as async :refer [go go-loop chan <! >!]])
  (:require [clojure.core.typed :as t])
  (:require [com.stuartsierra.component :as component])
  (:require [clojurewerkz.elastisch.rest :as es]
            [clojurewerkz.elastisch.rest.document :as esd])
  (:require [clojurewerkz.support.json]))

(t/ann-record
  StorageEngine [driver :- IStorageDriver
                 check-inp-c :- core/TDocChan
                 check-out-c :- core/TDocChan
                 store-doc-c :- core/TDocChan])

(defrecord StorageEngine [driver check-inp-c check-out-c store-doc-c]
  component/Lifecycle

  (start [this]
    (t/tc-ignore
      (logger/info (:logger this) "Storage: Starting")
      (go-loop
        [doc (<! (:store-doc-c this))]
        (if (nil? doc)
          (logger/info (:logger this) "Storage: Input channel closed")
          (do
            (logger/info (:logger this) (dissoc doc :url :http-payload :content))
            (store (:driver this) doc)
            (recur (<! (:store-doc-c this))))))
      (async/pipeline 1 (:check-out-c this) (remove  #(contains-doc? (-> this :driver) %)) (:check-inp-c this)))
    this)

  (stop [this]
    (t/tc-ignore
      (logger/info (:logger this) "Storage: Stopping"))
    this))

(t/ann-record
  ElasticDriver [es-conn :- t/Any
                 conf :- TStorageConf
                 doc-id-fn :- core/TDocIdFn])

(defrecord ElasticDriver [es-conn conf doc-id-fn]

  IStorageDriver

  (store [this doc]
    (t/tc-ignore
      (esd/create es-conn
                  (get-in this [:conf :es :index-name])
                  (get-in this [:conf :es :entity-name])
                  (dissoc doc :id :http-payload)
                  :id (doc-id-fn doc))))

  (get-doc [this doc-id]
    (t/tc-ignore
      (let [response (esd/get es-conn
                              (get-in this [:conf :es :index-name])
                              (get-in this [:conf :es :entity-name])
                              doc-id)]
        (when-not (nil? response) (response :_source)))))

  (contains-doc? [this doc]
    (t/tc-ignore
      (let [doc-id (doc-id-fn doc)]
        (and
          (not (nil? doc-id))
          (not (nil? (get-doc this doc-id))))))))

(t/ann ^:no-check clojurewerkz.elastisch.rest/connect [t/Any -> t/HMap])

(t/defn build-engine
  "Build a new storage engine."
  [driver :- IStorageDriver conf-opts :- TChanMap] :- StorageEngine
  (map->StorageEngine {:driver driver
                       :check-inp-c (:check-inp-c conf-opts)
                       :check-out-c (:check-out-c conf-opts)
                       :store-doc-c (:store-doc-c conf-opts)}))

(t/defn build-elastic-driver
  [conf-opts :- TElasticDriverOpts] :- ElasticDriver
  (map->ElasticDriver {:conf (:storage conf-opts)
                       :es-conn (es/connect (get-in conf-opts [:storage :es :url]))
                       :doc-id-fn (:doc-id-fn conf-opts)}))

(t/defn build-elastic-component
  "Build a new elastic storage."
  [conf-opts :- TElasticDriverOpts
   chans :- TChanMap] :- StorageEngine
  (build-engine (build-elastic-driver conf-opts) chans))
