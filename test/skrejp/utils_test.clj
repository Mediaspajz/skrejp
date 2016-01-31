(ns skrejp.utils-test
  (:require [skrejp.utils :as utils])
  (:require [expectations :refer :all]))

(def improve (utils/build-improve-fn {:freezed-attrs [:title]}))

; Fills empty map
(expect {:title "Foo" :body "BAR"} (improve {} :title "Foo" :body "BAR"))

; Can add in missing not freezed attribute
(expect {:title "Foo" :body "BAR"} (improve {:title "Foo"} :title "Foo" :body "BAR"))

; Ignores freezed attributes missing from the update
(expect {:title "Foo" :body "BAR"} (improve {:title "Foo"} :body "BAR"))

; Can change not freezed attribute
(expect {:title "Foo" :body "BAR"} (improve {:body "bar"} :title "Foo" :body "BAR"))

; Raises error when freezed attribute is changed
(expect clojure.lang.ExceptionInfo (improve {:title "foo"} :title "Foo" :body "BAR"))
