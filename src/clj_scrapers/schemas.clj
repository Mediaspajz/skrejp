(ns clj-scrapers.schemas
  (:require [schema.core :as s])
  )

(def ^:private ArticleSchema
  "Article schema."
  {(s/required-key :url)     s/Str
   (s/required-key :title)   s/Str
   (s/optional-key :summary) s/Str
   (s/required-key :content) s/Str
   (s/optional-key :loc)     s/Str
   }
  )

(defn article-errors [article]
  (s/check ArticleSchema article)
  )
