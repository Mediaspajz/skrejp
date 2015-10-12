(ns skrejp.storage-test
  (:require [skrejp.storage.component :as storage]
            [skrejp.storage.ann :as storage-ann])
  (:require [expectations :refer :all])
  (:require [clojurewerkz.elastisch.rest.index :as esi])
  (:require [environ.core :refer [env]]))

(let
  [driver (storage/build-elastic-driver
            {:storage   {:es {:url         (env :es-host)
                              :index-name  (env :es-indexname)
                              :entity-name (env :es-entityname)}}
             :doc-id-fn #(% :url)})
   doc-id  "http://example.com/foobar.html"
   _doc    (storage-ann/store driver {:id doc-id :title "Foo" :body "Bar" :http-payload "page body"})
   ret-doc (do (esi/flush (:es-conn driver))
               (storage-ann/get-doc driver doc-id))]
  (expect "Foo" (ret-doc :title))
  (expect "Bar" (ret-doc :body))
  (expect false (contains? ret-doc :id))
  (expect false (contains? ret-doc :http-payload)))
; TODO: drop the testing index
