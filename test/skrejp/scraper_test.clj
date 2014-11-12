(ns skrejp.scraper-test
  (:require [skrejp.scraper :as scraper])
  (:require [expectations :refer :all])
  )

(expect :bumm.sk
        (scraper/classify-url-source "http://bumm.sk/index.php?show=97202"))
(expect :ujszo.com
        (scraper/classify-url-source "http://ujszo.com/napilap/kulfold/2014/06/23/szorult-helyzetben-a-tusk-kormany"))

(let
  [scraper-cmpnt (scraper/build-component
                   { :scraper-defs
                     { :example.com { :title [:h1#title] :content [:div#content] } } }
                   )
   page-body "<html><body><h1 id='title'>Foo Title</h1><div id='content'>Bar Content</div></body></html>"
   page-resp { :url "http://example.com/index.html" :body page-body }
   article (first (into [] (scraper/scrape scraper-cmpnt) [page-resp])) ]
  (expect "http://example.com/index.html" (article :url))
  (expect "Foo Title"   (article :title))
  (expect "Bar Content" (article :content))
  )

