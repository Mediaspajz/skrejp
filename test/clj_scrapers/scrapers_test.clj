(ns clj-scrapers.scrapers-test
  (:require [clj-scrapers.scrapers :refer :all])
  (:require [expectations :refer :all])
  )

(expect :clj-scrapers.scrapers/bumm.sk
        (classify-url-source "http://bumm.sk/index.php?show=97202"))
(expect :clj-scrapers.scrapers/ujszo.com
        (classify-url-source "http://ujszo.com/napilap/kulfold/2014/06/23/szorult-helyzetben-a-tusk-kormany"))
