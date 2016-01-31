(ns skrejp.test-utils
  (:require [expectations :as e]))

(defn- collect-test-ns []
  ['skrejp.crawl-planner-test 'skrejp.scraper-test 'skrejp.storage-test 'skrejp.system-test])

(defn run-tests
  "Reload and execute test namespaces."
  []
  (let [test-ns-list (collect-test-ns)]
    (apply require (conj test-ns-list :reload))
    (e/run-tests test-ns-list)))
