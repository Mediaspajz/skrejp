(ns skrejp.retrieval.pages
  (:require [skrejp.retrieval.component :as retrieval]
            [clojurewerkz.urly.core :as urly]))

(defn build-page-retrieval-component [retrieval-plumbing improve chans]
  (retrieval/build-component
    retrieval-plumbing
    {:key-fn     (fn [doc] (urly/host-of (urly/url-like (doc :url))))

     :process-fn (fn [doc resp]
                   (when-not (:error resp)
                     (list (improve doc :http-payload (resp :body)))))

     :url-fn     :url}
    chans))

