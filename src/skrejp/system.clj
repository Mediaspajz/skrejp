(ns skrejp.system
  (:use [skrejp.ann])
  (:require [clojure.core.typed :as t])
  (:require [com.stuartsierra.component :as component])
  (:require [skrejp.error-handling.component :as error-handling])
  (:require [skrejp.scraper-verification.component :as scraper-verification]
            [skrejp.logger.component :as logger]
            [skrejp.retrieval.plumbing :as retrieval]
            [skrejp.storage.component :as storage]
            [skrejp.scraper.component :as scraper]
            [skrejp.retrieval.feeds :as feeds]
            [skrejp.retrieval.pages :as pages]
            [skrejp.crawl-planner.component :as crawl-planner]
            [skrejp.core :as core]))

(t/ann ^:no-check com.stuartsierra.component/system-map [t/Any * -> TSystemMap])
(t/ann ^:no-check com.stuartsierra.component/using [t/Any t/Any -> t/Any])

(t/ann build-scraper-system [TSystemConf -> TSystemMap])
(t/ann build-scraper-system [TSystemConf t/Map -> TSystemMap])

(defn build-chan-map
  ([] (build-chan-map {}))
  ([opts] (build-chan-map opts (fn [_key] (core/doc-chan))))
  ([opts build-chan] (memoize (fn [key] (get opts key (build-chan key))))))

(defn build-scraper-system
  "Build a scraper system."
  ([conf-opts] (build-scraper-system conf-opts {}))
  ([conf-opts comps]
   (let [retrieval-plumbing
         (retrieval/build-retrieval-plumbing
           (assoc (select-keys conf-opts [:http-req-opts])
             :thread-cnts-fn (constantly 5)))

         chan-map
         (build-chan-map
           {[:storage :page-retrieval] (get conf-opts :retrieval-inp-c (core/doc-chan))
            [:feed-retrieval :storage] (get conf-opts :storage-check-c (core/doc-chan))
            [:scraper :storage]        (get conf-opts :store-doc-c (core/doc-chan))})]

     (component/system-map
       :logger (or (:logger comps) (logger/build-component conf-opts))

       :error-handling (component/using
                         (error-handling/build-component conf-opts)
                         [:logger])
       :crawl-planner (component/using
                        (crawl-planner/build-component
                          conf-opts
                          {:cmd-c     (core/cmd-chan)
                           :out-doc-c (chan-map [:crawl-planner :feed-retrieval])})
                        [:logger :page-retrieval :error-handling :scraper])
       :feed-retrieval (component/using
                         (let [chans {:inp-doc-c (chan-map [:crawl-planner :feed-retrieval])
                                      :out-doc-c (chan-map [:feed-retrieval :storage])}]
                           (feeds/build-feed-retrieval-component retrieval-plumbing chans))
                         [:logger])
       :page-retrieval (component/using
                         (let [chans {:inp-doc-c (chan-map [:storage :page-retrieval])
                                      :out-doc-c (chan-map [:page-retrieval :scraper])}]
                           (pages/build-page-retrieval-component retrieval-plumbing chans))
                         [:logger])
       :scraper (component/using
                  (scraper/build-component
                    conf-opts
                    {:inp-doc-c (chan-map [:page-retrieval :scraper])
                     :out-doc-c (chan-map [:scraper :storage])})
                  [:logger :page-retrieval :error-handling])
       :storage (or (:storage comps)
                    (component/using
                      (storage/build-elastic-component
                        conf-opts
                        {:check-inp-c (chan-map [:feed-retrieval :storage])
                         :check-out-c (chan-map [:storage :page-retrieval])
                         :store-doc-c (chan-map [:scraper :storage])})
                      [:logger]))
       :scraper-verification (component/using
                               (scraper-verification/build-component conf-opts)
                               [:logger :storage :page-retrieval :error-handling])))))
