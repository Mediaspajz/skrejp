(ns skrejp.system
  (:require [clojure.core.typed :as t])
  (:require [com.stuartsierra.component  :as component])
  (:require [skrejp.core                 :as core])
  (:require [skrejp.retrieval            :as retrieval])
  (:require [skrejp.scraper              :as scraper])
  (:require [skrejp.storage              :as storage])
  (:require [skrejp.error-handling       :as error-handling])
  (:require [skrejp.logger               :as logger])
  (:require [skrejp.crawl-planner.component :as crawl-planner])
  (:require [skrejp.crawl-planner.ann :as crawl-planner-ann])
  (:require [skrejp.scraper-verification :as scraper-verification]))

(t/defalias TSystemConf
  (t/HMap :mandatory {:http-req-opts core/THttpReqOpts
                      :scraper-defs scraper/TScraperDefs
                      :feeds crawl-planner-ann/TFeedUrlVec
                      :planner-cmds crawl-planner-ann/TPlannerCmdVec
                      :storage storage/TStorageConf}))

(t/defalias TSystemMap t/Any)

(t/ann ^:no-check com.stuartsierra.component/system-map [t/Any * -> TSystemMap])
(t/ann ^:no-check com.stuartsierra.component/using [t/Any t/Any -> t/Any])

(t/ann build-scraper-system [TSystemConf -> TSystemMap])
(t/ann build-scraper-system [TSystemConf t/Map -> TSystemMap])

(defn build-scraper-system
  "Build a scraper system."
  ([conf-opts] (build-scraper-system conf-opts {}))
  ([conf-opts comps]
    (component/system-map
      :logger         (or (:logger comps) (logger/build-component conf-opts))
      :storage        (or (:storage comps) (component/using
                                             (storage/build-component conf-opts)
                                             [:logger]))
      :error-handling (component/using
                        (error-handling/build-component conf-opts)
                        [:logger])
      :page-retrieval (component/using
                        (retrieval/build-component conf-opts)
                        [:logger :storage])
      :crawl-planner  (component/using
                        (crawl-planner/build-component conf-opts)
                        [:logger :page-retrieval :error-handling :scraper])
      :scraper        (component/using
                        (scraper/build-component conf-opts)
                        [:logger :page-retrieval :storage :error-handling])
      :scraper-verification
                      (component/using
                        (scraper-verification/build-component conf-opts)
                        [:logger :storage :page-retrieval :error-handling]))))