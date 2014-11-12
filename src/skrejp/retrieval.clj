(ns skrejp.retrieval
  (:require [com.stuartsierra.component :as component])
  (:require [org.httpkit.client :as http])
  )

(defprotocol IRetrieval
  "## IRetrieval
  Defines methods for fetching pages.
  *fetch-page* is a transducer for fetching a page from a url.
  It expects the URL of the resource and it is pushing the fetch page to the channel it is applied on.
  If the error-fn is passed, it calls the error-fn function in case of an error."
  (fetch-page [this] [this error-fn])
  )

(defrecord RetrievalComponent [http-opts]
  component/Lifecycle
  IRetrieval

  (start [this]
    (println ";; Starting PageContentRetrieval")
    this)

  (stop [this]
    (println ";; Stopping PageContentRetrieval")
    this)

  (fetch-page [this]
    (fn [xf]
      (fn ([] (xf))
        ([result] (xf result))
        ([result url]
         (http/get url (:http-opts this)
                   (fn [{:keys [error] :as resp}]
                     (if-not error
                       (xf result resp)
                       )
                     )
                   )
         result)
        )
      )
    )
  )

(defn build-component
  "Build a PageRetrieval component."
  [config-options]
  (map->RetrievalComponent (select-keys config-options [:http-req-opts]))
  )
