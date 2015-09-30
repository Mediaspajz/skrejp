(ns skrejp.storage.ann
  (:require [clojure.core.typed :as t]
            [skrejp.core :as core]))

(t/defalias TStorageConf (t/HMap :complete? false))

(t/defprotocol IStorage
               "## IStorage
               Defines methods for storing documents scraped by the system. Storage component is independent from other parts of
               the system. The _scraper component_ uses it for storing the scraped documents."
               (store   [this :- IStorage doc :- core/TDoc])
               (get-doc [this :- IStorage doc-id :- t/Str])
               (contains-doc? [this :- IStorage doc-id :- t/Str]))
