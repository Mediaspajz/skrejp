(ns skrejp.app
  (:require [skrejp.scraper :as scraper])
  (:require [clojurewerkz.urly.core :as urly])
  (:require [com.stuartsierra.component :as component])
  (:require [clojure.core.async :as async :refer [<!! >!!]])
  (:require [clojure.string :refer [lower-case]])
  (:require [clj-time.core :as t])
  (:require [skrejp.system :as system])
  (:require [skrejp.crawl-planner :as planner])
  (:require [clojure.tools.cli :refer [cli]])
  (:import  [org.apache.commons.daemon Daemon DaemonContext])
  (:gen-class :implements [org.apache.commons.daemon.Daemon]))

(defn parse-int [s] (Integer/parseInt s))

(def month-signs {"jan" 1 "feb" 2 "már" 3 "ápr"  4 "máj"  5 "jún"  6
                  "júl" 7 "aug" 8 "sze" 9 "okt" 10 "nov" 11 "dec" 12})

(defn parse-time-str [str]
  (try
    (let
      [[_ xy xmo xd _ xh xmi]
       (re-find #"(\d{4})\. (\w{3})\w+ (\d{1,2})\.([^\d]+(\d{1,2}):(\d{1,2}))?" str)
       [y d h mi] (map parse-int (take-while (complement nil?) [xy xd xh xmi]))
       mo (-> xmo lower-case month-signs)
       time-params (take-while (complement nil?) [y mo d h mi])]
      (when (>= (count time-params) 3)
        (apply t/date-time time-params)))
    (catch Exception _ nil)))

(def conf-opts
  {:feeds
     ["http://ujszo.com/rss.xml"
      "http://vasarnap.ujszo.com/rss.xml"
      "http://www.bumm.sk/rss/rss.xml"
      "http://www.felvidek.ma/?format=feed&type=rss"
      "http://www.parameter.sk/rss.xml"
      "http://www.hirek.sk/rss/hirek.xml"]
   :scraper-defs
     {:shared            {:source  #(-> % :url urly/url-like urly/host-of)}
      "www.bumm.sk"      {:title   [:h2#page_title]
                          :summary [:div.page_lead]
                          :content [:div.page_body]
                          :published_at
                          #(-> % (scraper/extract-tag [:div.page_public_date]) parse-time-str)}
      "felvidek.ma"      {:title   [:article :header.article-header :h1.article-title :a]
                          :content [:section.article-content]
                          :published_at
                          #(-> % (scraper/extract-tag [:dd.create]) parse-time-str)}
      "ujszo.com"        {:title   [:div.node.node-article :h1]
                          :loc     [:div.node.node-article :div.field-name-field-lead :span.place]
                          :summary [:div.node.node-article :div.field-name-field-lead :p]
                          :content [:div.node.node-article :div.field-name-body]
                          :published_at
                          #(-> % (scraper/extract-tag [:div.article-header]) parse-time-str)}
      "www.parameter.sk" {:title   [:div#content :h1]
                          :summary [:div#content :div.field-name-field-lead]
                          :content [:div#content :div.field-name-body]
                          :published_at
                          #(-> % (scraper/extract-tag [:div.article-header]) parse-time-str)}
      "www.hirek.sk"     {:title   [:span.tcikkcim]
                          :summary [:span#tcikkintro]
                          :content [:div#tcikktext]
                          :published_at
                          #(-> % (scraper/extract-tag [:span.tcikkinfo]) parse-time-str)}
      "vasarnap.ujszo.com" "ujszo.com"}
   :http-req-opts
     {:timeout    200 ; ms
      :user-agent "User-Agent-string"
      :headers    {"X-Header" "Value"}}
   :storage {:es {:url         "http://localhost:9200"
                  :index-name  "mediaspajz_development_articles" ; "mediaspajz_test"
                  :entity-name "article"}}})

(defn start-scraper-system
  "Starts the scraper system."
  [scraper-system]
  (component/start scraper-system))

(defn stop-scraper-system
  "Stops the passed in system"
  [scraper-system]
  (component/stop scraper-system))

(def daemon-state (atom {}))

(defn init-daemon [args]
  (swap! daemon-state assoc :running true))

;;TODO: move that to crawl-planner component, initialize by planner-schedules
(defn start-daemon []
  (let
    [sys (start-scraper-system
           (system/build-scraper-system
             (assoc conf-opts :planner-cmds [:plan-feeds])))]
    (while (:running @daemon-state)
      (>!! (-> sys :crawl-planner :cmd-c) :plan-feeds)
      (<!! (async/timeout (* 4 60 1000))))
    (stop-scraper-system sys)))

(defn stop-daemon []
  (swap! daemon-state assoc :running false))

;; Daemon implementation

(defn -init [this ^DaemonContext context]
  (init-daemon (.getArguments context)))

(defn -start [this]
  (future (start-daemon)))

(defn -stop [this]
  (stop-daemon))

(defn -destroy [this])

(defn identity-after-timeout
  [comp secs]
  (do (<!! (async/timeout (* secs 1000))) comp))

(defn -main [& args]
  (let [[opts args banner]
        (cli args
             ["-h" "--help"   "Print this help"                :default false :flag true]
             ["-e" "--exec"   "Runs retrieval for <n> seconds" :default false :parse-fn #(Integer. %)]
             ["-d" "--daemon" "Execute in background"          :default false :flag true])]
    (when (:help opts)
      (println banner))
    (when (:exec opts)
      (start-scraper-system
        (system/build-scraper-system
          (assoc conf-opts :planner-cmds [:plan-feeds]))))
    (when (:daemon opts)
      (init-daemon args)
      (start-daemon))))