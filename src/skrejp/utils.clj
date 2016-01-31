(ns skrejp.utils)

(defn build-improve-fn [{:keys [freezed-attrs]}]
  (let [freezed-set (set freezed-attrs)]
    (fn [doc & attr-val-pairs]
      (let [doc-update (apply hash-map attr-val-pairs)
            check-attrs (clojure.set/intersection (set (keys doc)) (set (keys doc-update)) freezed-set)]
        (if (not= (select-keys doc check-attrs)
                  (select-keys doc-update check-attrs))
          (throw
            (ex-info "Can not update freezed attributes"
                     {:cause :scraping-error
                      :url (:url doc)
                      :doc (select-keys doc check-attrs)
                      :doc-update (select-keys doc-update check-attrs)}))
          (merge doc doc-update))))))
