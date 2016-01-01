(set-env!
  :dependencies '[[org.clojure/clojure    "1.7.0"]
                  [org.clojure/core.typed "0.3.19"]
                  [org.clojure/core.async "0.2.374"]
                  [com.stuartsierra/component "0.3.1"]
                  [org.clojure/tools.reader "0.10.0"]

                  ;; crawling, parsing, scraping
                  [org.clojars.scsibug/feedparser-clj "0.4.0"]
                  [http-kit          "2.1.19"]
                  [clojurewerkz/urly "1.0.0"]
                  [enlive            "1.1.6"]
                  [clj-time          "0.11.0"]

                  ;; storage
                  [clojurewerkz/elastisch "2.2.0-rc1"]

                  [boot-deps "0.1.6"]

                  ;; test env
                  [environ "1.0.1" :scope "test"]
                  [http-kit.fake "0.2.2" :scope "test"]
                  [debugger "0.1.7" :scope "test"]

                  [seancorfield/boot-expectations "1.0.3" :scope "test"]]

  :source-paths #(conj % "src" "test"))

(require '[seancorfield.boot-expectations :refer :all])
