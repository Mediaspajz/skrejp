(ns skrejp.scraper.ann
  (:require [clojure.core.typed :as t]
            [skrejp.core :as core]))

(t/defprotocol IScraper
               (scrape-attr [this :- IScraper doc :- core/TDoc]
                            "Scrape the document, the scrape rules are in the vector."))

(t/defprotocol IScraperComp
               "## IScraperComp
               Defines methods for scraping structure content from web pages.
               *scrape* is a transducer taking a http responses and returning map with extacted values.
               *get-scraper-def* returns a scraper definition for a url."

               (scrape [this :- IScraperComp] :- t/Any)
               (get-scraper-def [this :- IScraperComp url :- t/Str] :- t/Any))

(t/defalias TScraperDefs t/Any)

