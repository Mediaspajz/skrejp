(ns skrejp.system
  (:require [com.stuartsierra.component  :as component])
  (:require [skrejp.retrieval            :as retrieval])
  (:require [skrejp.scraper              :as scraper])
  (:require [skrejp.storage              :as storage])
  (:require [skrejp.error-handling       :as error-handling])
  (:require [skrejp.crawl-planner        :as crawl-planner])
  (:require [skrejp.scraper-verification :as scraper-verification]) )

(defn build-scraper-system
  "Build a scraper system."
  [config-options]
  (let
    [{:keys [scraper-defs]} config-options]
    (component/system-map
      :storage        (storage/build-component)
      :error-handling (error-handling/build-component)
      :page-retrieval (retrieval/build-component config-options)
      :crawl-planner  (component/using
                        (crawl-planner/build-component)
                        [:page-retrieval :error-handling :scraper]
                        )
      :scraper        (component/using
                        (scraper/build-component scraper-defs)
                        [:page-retrieval :storage :error-handling]
                        )
      :scraper-verification
                      (component/using
                        (scraper-verification/build-component)
                        [:storage :page-retrieval :error-handling]
                        ) ) ) )
