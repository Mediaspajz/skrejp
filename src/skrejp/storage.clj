(ns skrejp.storage
  (:require [skrejp.logger :as logger])
  (:require [clojure.core.async :refer [go go-loop chan <! >!]])
  (:require [com.stuartsierra.component :as component])
  (:require [clojurewerkz.elastisch.rest :as es]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]))

(defprotocol IStorage
  "## IStorage
  Defines methods for storing documents scraped by the system. Storage component is independent from other parts of
  the system. The _scraper component_ uses it for storing the scraped documents."
  (store   [this doc])
  (get-doc [this doc-id]))

(defrecord Storage [logger doc-c]
  component/Lifecycle

  (start [this]
    (logger/info (:logger this) "Starting Storage")
    (let [doc-c    (chan 512)
          es-conn  (es/connect (get-in this [:es :url]))
          setup    (assoc this
                          :doc-c    doc-c
                          :es-conn  es-conn)]
      (go-loop
        [doc (<! doc-c)]
        (if (nil? doc)
          (logger/info (:logger this) "Storage input channel closed")
          (do
            (store setup doc)
            (recur (<! doc-c)))))
      setup)
    )

  (stop [this]
    (logger/info (:logger this) "Stopping Storage")
    this)

  IStorage

  (store [this doc]
    (logger/info (:logger this) (dissoc doc :http-payload))
    (esd/create (:es-conn this)
                (get-in this [:es :index-name])
                (get-in this [:es :entity-name])
                (dissoc doc :http-payload :published_at)))

  (get-doc [this doc-id]
    (let [response (esd/get (:es-conn this)
                            (get-in this [:es :index-name])
                            (get-in this [:es :entity-name])
                            doc-id)]
      (response :_source))))

(defn build-component
  "Build a new storage."
  [conf-opts]
  (map->Storage (conf-opts :storage)))
