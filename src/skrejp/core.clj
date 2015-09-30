(ns skrejp.core
  (:require [clojure.core.typed :as t]
            [clojure.core.typed.async :as ta])
  (:require [clojure.core.async :as async]))

(t/defalias THttpReqOpts (t/HMap :complete? false))

(t/defalias TDoc t/Map)

(t/defalias TDocChan (ta/Chan TDoc))

(t/defn doc-chan
  "Build document channel."
  [] :- TDocChan
  (async/chan 512))

