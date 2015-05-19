(ns skrejp.retrieval
  (:require [clojure.core.async :as async :refer [<! >! <!! chan go-loop]])
  (:require [skrejp.logger :as logger]
            [skrejp.storage :as storage])
  (:require [com.stuartsierra.component :as component])
  (:require [org.httpkit.client :as http])
  (:require [feedparser-clj.core :as feeds]
            [clojure.core.async :as async]
            [clojurewerkz.urly.core :as urly])
  (:import  [java.io ByteArrayInputStream]))

(defn parse-feed-str [feed-s]
  (let
    [input-stream (ByteArrayInputStream. (.getBytes feed-s "UTF-8"))]
    (feeds/parse-feed input-stream)))

(defprotocol IRetrieval
  "## IRetrieval
  Defines methods for fetching pages.
  *fetch-page* is a transducer for fetching a page from a url.
  It expects the URL of the resource and it is pushing the fetch page to the channel it is applied on.
  If the error-fn is passed, it calls the error-fn function in case of an error."
  (fetch-page [this] [this error-fn])
  (fetch-feed [this]))

(defn get-host-c [setup host-chans host]
  (if (contains? host-chans host)
    (host-chans host)
    (let [new-host-c (chan 512)]
      (async/pipeline-async 5 (:out-doc-c setup) (fetch-page setup) new-host-c)
      new-host-c)))

(defrecord RetrievalComponent [http-opts inp-doc-c out-doc-c]
  component/Lifecycle

  (start [this]
    (logger/info (:logger this) "PageContentRetrieval: Starting")
    (let
      [inp-doc-c  (chan 512)
       out-doc-c  (chan 512)
       comp-setup (assoc this :inp-doc-c inp-doc-c :out-doc-c out-doc-c)]
      (go-loop [doc (<! inp-doc-c) host-chans {}]
        (logger/info (:logger this) (format "PageContentRetrieval: Received %s" (doc :url)))
        (when-not (nil? doc)
          (let [doc-w-id (assoc doc :id (doc :url))]
            (if (storage/contains-doc? (:storage comp-setup) doc-w-id)
              (recur (<! inp-doc-c) host-chans)
              (let
                [host (urly/host-of (urly/url-like (doc :url)))
                 host-c (get-host-c comp-setup host-chans host)]
                (>! host-c doc-w-id)
                (recur (<! inp-doc-c) (assoc host-chans host host-c)))))))
      comp-setup))

  (stop [this]
    (logger/info (:logger this) "PageContentRetrieval: Stopping")
    this)

  IRetrieval

  (fetch-page [this]
    (fn [doc c]
      (http/get (doc :url) (:http-opts this)
                (fn [{:keys [error] :as resp}]
                  (when-not error
                    (async/put! c (assoc doc :http-payload (resp :body))))
                  (async/close! c)))))

  (fetch-feed [this]
    (fn [xf]
      (fn ([] (xf)) ([result] (xf result))
        ([result url]
         (let
           [resp @(http/get url (:http-opts this))]
           (xf result (-> resp :body parse-feed-str))))))))

(defn build-component
  "Build a PageRetrieval component."
  [config-options]
  (map->RetrievalComponent (select-keys config-options [:http-req-opts])))