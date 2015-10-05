(ns skrejp.system
  (:use [skrejp.ann])
  (:require [clojure.core.typed :as t])
  (:require [com.stuartsierra.component :as component])
  (:require [skrejp.error-handling.component :as error-handling])
  (:require [skrejp.scraper-verification.component :as scraper-verification]
            [skrejp.logger.component :as logger]
            [skrejp.storage.component :as storage]
            [skrejp.scraper.component :as scraper]
            [skrejp.retrieval.component :as retrieval]
            [skrejp.crawl-planner.component :as crawl-planner]
            [skrejp.core :as core]))

(t/ann ^:no-check com.stuartsierra.component/system-map [t/Any * -> TSystemMap])
(t/ann ^:no-check com.stuartsierra.component/using [t/Any t/Any -> t/Any])

(t/ann build-scraper-system [TSystemConf -> TSystemMap])
(t/ann build-scraper-system [TSystemConf t/Map -> TSystemMap])

(defn build-scraper-system
  "Build a scraper system."
  ([conf-opts] (build-scraper-system conf-opts {}))
  ([conf-opts comps]
   (let [scraper-inp-c (core/doc-chan)
         store-doc-c (get conf-opts :store-doc-c (core/doc-chan))]
     (component/system-map
       :logger (or (:logger comps) (logger/build-component conf-opts))
       :storage (or (:storage comps)
                    (component/using
                      (storage/build-component
                        (assoc conf-opts
                          :store-doc-c store-doc-c))
                      [:logger]))
       :error-handling (component/using
                         (error-handling/build-component conf-opts)
                         [:logger])
       :page-retrieval (component/using
                         (retrieval/build-component
                           (assoc conf-opts
                             :inp-doc-c (core/doc-chan)
                             :out-doc-c (core/doc-chan)))
                         [:logger :storage])
       :crawl-planner (component/using
                        (crawl-planner/build-component
                          (assoc conf-opts :cmd-c (core/cmd-chan)
                                           :out-doc-c scraper-inp-c))
                        [:logger :page-retrieval :error-handling :scraper])
       :scraper (component/using
                  (scraper/build-component
                    (assoc conf-opts :inp-doc-c scraper-inp-c
                                     :out-doc-c store-doc-c))
                  [:logger :page-retrieval :storage :error-handling])
       :scraper-verification
       (component/using
         (scraper-verification/build-component conf-opts)
         [:logger :storage :page-retrieval :error-handling])))))
