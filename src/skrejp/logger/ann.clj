(ns skrejp.logger.ann
  (:require [clojure.core.typed :as t]))

(t/defprotocol ILogger
               "## ILogger
               Defines methods for logging the events of the scraping system. Every other component is supposed to use it.
               But this component is not dependent on other part of the system. It acts as a bridge between logger libraries and
               the system."

               (info [this :- ILogger msg :- t/Any] :- nil)
               (debug [this :- ILogger msg :- t/Any] :- nil))

