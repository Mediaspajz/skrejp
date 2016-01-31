(ns skrejp.retrieval.feeds
  (:require [clojure.core.typed :as t]
            [feedparser-clj.core :as feeds]
            [skrejp.retrieval.component :as retrieval]
            [clojurewerkz.urly.core :as urly])
  (:import (java.io ByteArrayInputStream)
           (org.joda.time DateTime)))

(t/ann ^:no-check feedparser-clj.core/parse-feed [(t/U t/Str ByteArrayInputStream) -> t/HMap])

(t/defn ^:private parse-feed-str
        "Parses the feed passed in as a string."
        [^String feed-s :- t/Str] :- t/HMap
        (let
          [input-stream (ByteArrayInputStream. (.getBytes feed-s "UTF-8"))]
          (feeds/parse-feed input-stream)))

(defn build-feed-retrieval-component [retrieval-plumbing improve chans]
  (retrieval/build-component
    retrieval-plumbing
    {:key-fn     (fn [feed-url]
                   (-> feed-url urly/url-like urly/host-of))

     :process-fn (fn [_feed-url resp]
                   (when-not (:error resp)
                     (map (fn [entry]
                            (improve (select-keys entry [:title])
                              :url (or (:link entry) (:url entry))
                              :published_at (DateTime. (:published-date entry))))
                          (-> resp :body parse-feed-str :entries))))

     :url-fn     identity}
    chans))
