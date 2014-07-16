(ns clj-scrapers.core-test
  (:require [clj-scrapers.core :refer :all])
  (:require [expectations :refer :all])
  )

(expect :bumm.sk   (classify-url-source "http://bumm.sk/index.php?show=97202"))
(expect :ujszo.com (classify-url-source "http://ujszo.com/napilap/kulfold/2014/06/23/szorult-helyzetben-a-tusk-kormany"))
