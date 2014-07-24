(ns clj-scrapers.core-test
  (:require [clj-scrapers.core :refer :all])
  (:require [expectations :refer :all])
  )

(expect :clj-scrapers.core/bumm.sk
        (classify-url-source "http://bumm.sk/index.php?show=97202"))
(expect :clj-scrapers.core/ujszo.com
        (classify-url-source "http://ujszo.com/napilap/kulfold/2014/06/23/szorult-helyzetben-a-tusk-kormany"))

(def bumm-page (scrape "http://www.bumm.sk/97202/zsenialis-magyar-kajak-kenusok-8-arany-5-ezust.html"))

(expect "http://www.bumm.sk/97202/zsenialis-magyar-kajak-kenusok-8-arany-5-ezust.html" (:url @bumm-page))
(expect "Zseniális magyar kajak-kenusok: 8 arany, 5 ezüst!" (:title   @bumm-page))
(expect #"^A magyar versenyzők nagyszerű teljesítményt"     (:summary @bumm-page))
