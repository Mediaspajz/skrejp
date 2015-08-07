(ns skrejp.core
  (:require [clojure.core.typed :as t]
            [clojure.core.typed.async :as ta]))

(t/defalias TDoc t/Map)

(t/defalias TDocChan (ta/Chan TDoc))

(t/defalias THttpReqOpts (t/HMap :complete? false))

