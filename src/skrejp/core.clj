(ns skrejp.core
  (:require [com.stuartsierra.component :as component]) )

(defrecord Storage []
  component/Lifecycle

  (start [component]
    (println ";; Starting the Storage")
    component
    )

  (stop [component]
    (println ";; Stopping the Storage")
    component
    )
  )

(defn new-storage
  "Create a new storage."
  []
  (map->Storage {})
  )

(defrecord PageRetrieval [storage error-handling]
  component/Lifecycle

  (start [this]
    (println ";; Starting PageContentRetrieval")
    this)

  (stop [this]
    (println ";; Stopping PageContentRetrieval")
    this))

(defn page-retrieval-component
  "Create a PageRetrieval component."
  [config-options]
  (map->PageRetrieval {:options config-options})
  )

(defrecord ErrorHandling []
  component/Lifecycle

  (start [this]
    (println ";; Starting ErrorHandling")
    this)

  (stop [this]
    (println ";; Stopping ErrorHanding")
    this))

(defn error-handling-component
  "Create an ErrorHandling component."
  []
  (map->ErrorHandling {})
  )

(defrecord CrawlPlanner []
  component/Lifecycle

  (start [this]
    (println ";; Starting CrawlPlanner")
    this)

  (stop [this]
    (println ";; Stopping CrawlPlanner")
    this))

(defn crawl-planner-component
  "Create a CrawlPlanner component."
  []
  (map->CrawlPlanner {})
  )

(defrecord ScraperVerification []
  component/Lifecycle

  (start [this]
    (println ";; Starting ScraperVerification")
    this)

  (stop [this]
    (println ";; Stopping ScraperVerification")
    this)
  )

(defn scraper-verification-component
  "Create a ScraperVerification component."
  []
  (map->ScraperVerification {})
  )

(defn simple-system
  "Create a simple example system."
  [config-options]
  (component/system-map
    :storage (new-storage)
    :error-handling (error-handling-component)
    :page-retrieval (component/using
                      (page-retrieval-component config-options)
                      [:storage :error-handling]
                      )
    :crawl-planner (component/using
                     (crawl-planner-component)
                     [:page-retrieval :error-handling]
                     )
    :scraper-verification (component/using
                            (scraper-verification-component)
                            [:storage :page-retrieval :error-handling]
                            )
    )
  )
