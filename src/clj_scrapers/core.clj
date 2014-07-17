(ns clj-scrapers.core
  ;(:require [org.httpkit.client :as http])
  ;(:require [net.cgrand.enlive-html :as enlive]
  (:require [clojurewerkz.urly.core :refer [url-like host-of]])
  )

(def scrapers-ns *ns*)


(defn classify-url-source [url]
  (keyword (str scrapers-ns) (host-of (url-like url)))
  )
