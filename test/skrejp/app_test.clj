(ns skrejp.app-test
  (:require [skrejp.app :as app])
  (:require [clj-time.core :as t])
  (:require [expectations :refer :all]))

;; # skrejp.app/parse-time-str
;; It parses `bumm.sk`'s time label.
(expect (t/date-time 2014 12 6 14 29) (app/parse-time-str "2014. december 6. szombat - 14:29"))

;; It parases `ujszo.com`'s time label.
(expect (t/date-time 2014 12 6 18 24) (app/parse-time-str "2014. december 6., szombat 18:24"))

;; It parses `www.felvidek.ma`'s time label.
(expect (t/date-time 2014 12 6) (app/parse-time-str "2014. DECEMBER 06."))

;; It parses `www.hirek.sk`'s time label.
(expect (t/date-time 2014 12 6 15 27)
        (app/parse-time-str "2014. december 06., szombat  15:27 | Hírek.sk  | Forrás: TASR"))

;; It parses `www.parameter.sk`'s time label.
(expect (t/date-time 2014 12 6 18 44) (app/parse-time-str "2014. december 6. - 18:44"))

;; Returns `nil` when the string is not parsable
(expect nil (app/parse-time-str "this can not be parsed"))

;; At least year, month and day has to be provided otherwise it returns `nil`.
(expect nil (app/parse-time-str "2014. DECEMBER"))
