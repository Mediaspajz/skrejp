(ns skrejp.retrieval
  (:require [clojure.core.async :as async :refer [<! >! <!! chan go-loop]])
  (:require [skrejp.logger :as logger])
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
    (logger/info (:logger this) "Starting PageContentRetrieval")
    (let
      [inp-doc-c  (chan 512)
       out-doc-c  (chan 512)
       comp-setup (assoc this :inp-doc-c inp-doc-c :out-doc-c out-doc-c)]
      (go-loop [doc (<! inp-doc-c) host-chans {}]
        (when-not (nil? doc)
          (let
            [host     (urly/host-of (urly/url-like (doc :url)))
             next-doc (<! inp-doc-c)]
            (if (nil? next-doc)
              (do
                (<!! (async/timeout 10000))
                (for [host-c (vals host-chans)] (async/close! host-c))
                (async/close! out-doc-c))
              (let [host-c (get-host-c comp-setup host-chans host)]
                (>! host-c next-doc)
                (recur next-doc (assoc host-chans host host-c)))))))
      comp-setup))

  (stop [this]
    (logger/info (:logger this) "Stopping PageContentRetrieval")
    this)

  IRetrieval

  (fetch-page [this]
    (fn [doc c]
      (http/get (doc :url) (:http-opts this)
                (fn [{:keys [error] :as resp}]
                  (if-not error
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
  (map->RetrievalComponent (select-keys config-options [:http-req-opts])) )
