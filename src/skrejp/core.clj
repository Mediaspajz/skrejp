(ns skrejp.core
  (:require [com.stuartsierra.component :as component])
  (:require [skrejp.retrieval            :as retrieval])
  (:require [skrejp.storage              :as storage])
  (:require [skrejp.error-handling       :as error-handling])
  (:require [skrejp.crawl-planner        :as crawl-planner])
  (:require [skrejp.scraper-verification :as scraper-verification])
  )

(defn simple-system
  "Create a simple example system."
  [config-options]
  (component/system-map
    :storage        (storage/build-component)
    :error-handling (error-handling/build-component)
    :page-retrieval (component/using
                      (retrieval/build-component config-options)
                      [:storage :error-handling]
                      )
    :crawl-planner  (component/using
                      (crawl-planner/build-component)
                      [:page-retrieval :error-handling]
                      )
    :scraper-verification
                    (component/using
                      (scraper-verification/build-component)
                      [:storage :page-retrieval :error-handling]
                      )
    )
  )
