(ns skrejp.storage
  (:require [skrejp.logger :as logger])
  (:require [clojure.core.async :refer [go go-loop chan <! >!]])
  (:require [com.stuartsierra.component :as component])
  (:require [clojurewerkz.elastisch.rest :as es]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd])
  (:require [clojurewerkz.support.json]))

(defprotocol IStorage
  "## IStorage
  Defines methods for storing documents scraped by the system. Storage component is independent from other parts of
  the system. The _scraper component_ uses it for storing the scraped documents."
  (store   [this doc])
  (get-doc [this doc-id])
  (contains-doc? [this doc-id]))

(defrecord Storage [logger doc-c]
  component/Lifecycle

  (start [this]
    (logger/info (:logger this) "Storage: Starting")
    (let [doc-c    (chan 512)
          es-conn  (es/connect (get-in this [:es :url]))
          setup    (assoc this
                          :doc-c    doc-c
                          :es-conn  es-conn)]
      (go-loop
        [doc (<! doc-c)]
        (if (nil? doc)
          (logger/info (:logger this) "Storage: Input channel closed")
          (do
            (store setup doc)
            (recur (<! doc-c)))))
      setup))

  (stop [this]
    (logger/info (:logger this) "Storage: Stopping")
    this)

  IStorage

  (store [this doc]
    (logger/info (:logger this) (dissoc doc :url :http-payload :content))
    (esd/create (:es-conn this)
                (get-in this [:es :index-name])
                (get-in this [:es :entity-name])
                (dissoc doc :id :http-payload)
                :id (doc :id)))

  (get-doc [this doc-id]
    (let [response (esd/get (:es-conn this)
                            (get-in this [:es :index-name])
                            (get-in this [:es :entity-name])
                            doc-id)]
      (when-not (nil? response) (response :_source))))

  (contains-doc? [this doc]
    (let [doc-id (doc :id)]
      (and
        (not (nil? doc-id))
        (not (nil? (get-doc this (doc :id))))))))

(defn build-component
  "Build a new storage."
  [conf-opts]
  (map->Storage (conf-opts :storage)))
