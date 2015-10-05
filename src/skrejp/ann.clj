(ns skrejp.ann
  (:require [clojure.core.typed :as t]
            [skrejp.storage.ann :as storage]
            [skrejp.scraper.ann :as scraper]
            [skrejp.crawl-planner.ann :as crawl-planner]
            [skrejp.core :as core]))

(t/defalias TSystemConf
  (t/HMap :mandatory {:http-req-opts core/THttpReqOpts
                      :scraper-defs  scraper/TScraperDefs
                      :feeds         crawl-planner/TFeedUrlVec
                      :planner-cmds  crawl-planner/TPlannerCmdVec
                      :storage       storage/TStorageConf
                      :doc-id-fn     core/TDocIdFn}
          :optional {:store-doc-c core/TDocChan}))

(t/defalias TSystemMap t/Any)
