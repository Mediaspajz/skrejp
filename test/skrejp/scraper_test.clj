(ns skrejp.scraper-test
  (:require [skrejp.scraper :refer :all])
  (:require [expectations :refer :all])
  )

(expect :skrejp.scraper/bumm.sk
        (classify-url-source "http://bumm.sk/index.php?show=97202"))
(expect :skrejp.scraper/ujszo.com
        (classify-url-source "http://ujszo.com/napilap/kulfold/2014/06/23/szorult-helyzetben-a-tusk-kormany"))
