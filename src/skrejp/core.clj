(ns skrejp.core
  (:require [clojure.core.typed :as t]
            [clojure.core.typed.async :as ta])
  (:require [clojure.core.async :as async])
  (:require [skrejp.crawl-planner.ann :as planner]))

(t/defalias THttpReqOpts (t/HMap :complete? false))

(t/defalias TDoc t/Map)

(t/defalias TDocChan (ta/Chan TDoc))

(t/defalias TDocIdFn (t/Fn [TDoc -> t/Any]))

(t/defn doc-chan
  "Build document channel."
  [] :- TDocChan
  (async/chan 512))

(t/defn cmd-chan
  [] :- planner/TPlannerCmdChan
  (async/chan 16))
