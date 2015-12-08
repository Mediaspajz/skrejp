(ns skrejp.retrieval.ann
  (:require [clojure.core.typed :as t]))

;TODO: type annotations very weak here
(t/defprotocol IRetrieval
  "## IRetrieval
  Defines methods for fetching pages.
  *fetch-page* is a transducer for fetching a page from a url.
  It expects the URL of the resource and it is pushing the fetch page to the channel it is applied on.
  If the error-fn is passed, it calls the error-fn function in case of an error."
  (fetch-feed [this :- IRetrieval] :- (t/IFn [t/Any -> t/Any])))

