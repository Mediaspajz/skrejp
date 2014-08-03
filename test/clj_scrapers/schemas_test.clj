(ns clj-scrapers.schemas-test
  (:require [clj-scrapers.schemas :refer :all])
  (:require [expectations :refer :all])
  )

(expect nil
        (article-errors {:url "http://www.bumm.sk/123.html" :title "123" :content "123123"}))

(expect {:url 'missing-required-key}
        (article-errors {:title "123" :content "123123"}))
