(ns skrejp.system
  (:require [com.stuartsierra.component  :as component])
  (:require [skrejp.retrieval            :as retrieval])
  (:require [skrejp.scraper              :as scraper])
  (:require [skrejp.storage              :as storage])
  (:require [skrejp.error-handling       :as error-handling])
  (:require [skrejp.logger               :as logger])
  (:require [skrejp.crawl-planner        :as crawl-planner])
  (:require [skrejp.scraper-verification :as scraper-verification]))

(defn build-scraper-system
  "Build a scraper system."
  ([conf-opts] (build-scraper-system conf-opts {}))
  ([conf-opts comps]
    (component/system-map
      :logger         (or (comps :logger) (logger/build-component conf-opts))
      :storage        (or (comps :storage) (component/using
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