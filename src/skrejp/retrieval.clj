(ns skrejp.retrieval
  (:require [com.stuartsierra.component :as component])
  )

(defprotocol IRetrieval
  "## IRetrieval
  Defines methods for fetching pages.
  *fetch-page* is used for fetching a page from a url.
  It expects the URL of the resource and a channel to which it is pushing the fetch page.
  If the error-fn is passed, it calls the error-fn function in case of an error."
  (fetch-page
    [this url page-c] [this url page-c error-fn])
  )

(defrecord RetrievalComponent []
  component/Lifecycle
  IRetrieval

  (start [this]
    (println ";; Starting PageContentRetrieval")
    this)

  (stop [this]
    (println ";; Stopping PageContentRetrieval")
    this)

  (fetch-page [this url page-c]
    nil)
  )

(defn build-component
  "Build a PageRetrieval component."
  [config-options]
  (map->RetrievalComponent {:options config-options})
  )
