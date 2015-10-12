(ns skrejp.storage.ann
  (:require [clojure.core.typed :as t]
            [skrejp.core :as core]))

(t/defalias TStorageConf (t/HMap :complete? false))

(t/defalias TEngineOpts
  (t/HMap :mandatory {:storage-check-inp-c core/TDocChan
                      :storage-check-out-c core/TDocChan
                      :store-doc-c core/TDocChan}))

(t/defalias TElasticDriverOpts
  (t/HMap :mandatory {:storage TStorageConf
                      :doc-id-fn core/TDocIdFn}))

(t/defprotocol IStorageDriver
               "## IStorage
               Defines methods for storing documents scraped by the system. Storage component is independent from other parts of
               the system. The _scraper component_ uses it for storing the scraped documents."
               (store   [this :- IStorageDriver doc :- core/TDoc])
               (get-doc [this :- IStorageDriver doc-id :- t/Str])
               (contains-doc? [this :- IStorageDriver doc-id :- t/Str]))
