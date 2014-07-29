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
(expect #"A szombati döntők során három-három arany-"       (:content @bumm-page))

(def felvidekma-page (scrape "http://felvidek.ma/felvidek/hitelet/47584-boldog-a-szemetek-mert-lat"))
(expect "http://felvidek.ma/felvidek/hitelet/47584-boldog-a-szemetek-mert-lat" (:url @felvidekma-page))
(expect "Boldog a szemetek, mert lát…"                      (:title   @felvidekma-page))
(expect #"Történelmi tény az is, hogy 1751. június 16-án"   (:content @felvidekma-page))

(def ujszo-page (scrape "http://ujszo.com/online/kozelet/2014/07/28/kiska-penzunk-nincs-de-segitunk-terchovanak"))
(expect "http://ujszo.com/online/kozelet/2014/07/28/kiska-penzunk-nincs-de-segitunk-terchovanak" (:url @ujszo-page))
(expect "Kiska: pénzünk nincs, de segítünk Terchovának"     (:title   @ujszo-page))
(expect #"^Andrej Kiska államfő is ellátogatott szombaton"  (:summary @ujszo-page))
(expect #"A kormány a katasztrófa miatt ma megszakítja"     (:content @ujszo-page))
(expect "Terchová" (:loc @ujszo-page))

(def parameter-page (scrape "http://www.parameter.sk/rovat/belfold/2014/07/28/smer-mar-odaig-mereszkedett-hogy-egyenesen-tekintelyelvunek-neveztek-kiskat"))
(expect "http://www.parameter.sk/rovat/belfold/2014/07/28/smer-mar-odaig-mereszkedett-hogy-egyenesen-tekintelyelvunek-neveztek-kiskat" (:url @parameter-page))
(expect "A Smer már odáig merészkedett, hogy egyenesen tekintélyelvűnek nevezték Kiskát" (:title   @parameter-page))
(expect #"^Bár a Robert Fico vezette Smer-SD tudhatta, hogy nem lesz könnyű dolga"       (:summary @parameter-page))
(expect #"Az Aktualne.sk azonban megkérdezte Tomáš Koziak politológust, aki nem lát"     (:content @parameter-page))
