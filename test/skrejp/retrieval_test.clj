(ns skrejp.retrieval-test
  (:use     org.httpkit.fake)
  (:require [skrejp.retrieval.ann :as retrieval-ann]
            [skrejp.retrieval.component :as retrieval])
  (:require [expectations :refer :all])
  (:require [clojure.core.async :refer [chan go <! >! <!!] :as async]))

