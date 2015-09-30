(ns skrejp.storage.component
  (:use [skrejp.storage.ann])
  (:require [skrejp.logger.ann :as logger]
            [skrejp.core :as core])
  (:require [clojure.core.async :refer [go go-loop chan <! >!]])
  (:require [clojure.core.typed :as t])
  (:require [com.stuartsierra.component :as component])
  (:require [clojurewerkz.elastisch.rest :as es]
            [clojurewerkz.elastisch.rest.document :as esd])
  (:require [clojurewerkz.support.json]))

(t/ann-record
  Storage [doc-c :- core/TDocChan
           es-conn :- t/Any
           conf :- TStorageConf])

(defrecord Storage [doc-c es-conn conf]
  component/Lifecycle

  (start [this]
    (t/tc-ignore
      (logger/info (:logger this) "Storage: Starting")
      (go-loop
        [doc (<! (:doc-c this))]
        (if (nil? doc)
          (logger/info (:logger this) "Storage: Input channel closed")
          (do
            (store this doc)
            (recur (<! (:doc-c this)))))))
    this)

  (stop [this]
    (t/tc-ignore
      (logger/info (:logger this) "Storage: Stopping"))
    this)

  IStorage

  (store [this doc]
    (t/tc-ignore
      (logger/info (:logger this) (dissoc doc :url :http-payload :content))
      (esd/create (:es-conn this)
                  (get-in this [:conf :es :index-name])
                  (get-in this [:conf :es :entity-name])
                  (dissoc doc :id :http-payload)
                  :id (doc :id))))

  (get-doc [this doc-id]
    (t/tc-ignore
      (let [response (esd/get (:es-conn this)
                              (get-in this [:conf :es :index-name])
                              (get-in this [:conf :es :entity-name])
                              doc-id)]
        (when-not (nil? response) (response :_source)))))

  (contains-doc? [this doc]
    (t/tc-ignore
      (let [doc-id (doc :id)]
        (and
          (not (nil? doc-id))
          (not (nil? (get-doc this (doc :id)))))))))

(t/ann ^:no-check clojurewerkz.elastisch.rest/connect [t/Any -> t/HMap])

(t/defn build-component
  "Build a new storage."
  [conf-opts :- (t/HMap :mandatory {:storage TStorageConf})] :- Storage
  (map->Storage {:conf (:storage conf-opts)
                 :doc-c (core/doc-chan)
                 :es-conn (es/connect (get-in conf-opts [:storage :es :url]))}))
