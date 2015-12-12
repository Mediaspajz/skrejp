(ns skrejp.crawl-planner.ann
 (:require [clojure.core.typed :as t]
            [clojure.core.typed.async :as ta]))

(t/defalias TFeedUrl t/Str)
(t/defalias TFeedUrlVec (t/Vec TFeedUrl))

(t/defalias TPlannerCmd (t/U ':plan-feeds))
(t/defalias TPlannerCmdVec (t/Vec TPlannerCmd))
(t/defalias TPlannerCmdChan (ta/Chan TPlannerCmd))
