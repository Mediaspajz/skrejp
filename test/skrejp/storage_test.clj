(ns skrejp.storage-test
  (:require [skrejp.storage :as storage])
  (:require [skrejp.logger :as logger])
  (:require [expectations :refer :all])
  (:require [clojurewerkz.elastisch.rest.index :as esi])
  (:require [com.stuartsierra.component :as component]))
  ; (:require [skrejp.logger :as logger])

(let
  [cmpnt (component/start (assoc
                            (storage/build-component
                              {:storage {:es {:host "0.0.0.0"
                                              :port  9200
                                              :index-name  "mediaspajz_test"
                                              :entity-name "article"}}})
                            :logger (reify logger/ILogger (info [_ _]) (debug [_ _]))))
   doc     (storage/store cmpnt {:title "Foo" :body "Bar"})
   doc-id  (:_id doc)
   ret-doc (do (esi/flush (:es-conn cmpnt))
               (storage/get-doc cmpnt doc-id))]
  (expect "Foo" (ret-doc :title))
  (expect "Bar" (ret-doc :body)))
; TODO: drop the testing index
