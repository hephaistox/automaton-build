#_{:heph-ignore {:forbidden-words ["tap>"]}}
(ns automaton-build.code.forbidden-words-test
  (:require
   [automaton-build.code.forbidden-words :as sut]
   [clojure.test                         :refer [deftest is]]))

(deftest coll-to-alternate-in-regexp-test
  (is (= "^.*(automaton[-_]foobar|automaton[-_]foobar).*$"
         (str (sut/coll-to-alternate-in-regexp [#"automaton[-_]foobar" #"automaton[-_]foobar"])))
      "One word is transformed")
  (is (= "^.*(TO-DO|foo.*bar).*$" (str (sut/coll-to-alternate-in-regexp ["TO-DO" #"foo.*bar"])))
      "Strings and regexp are accepted."))

(deftest forbidden-words-matches-test
  (is (empty? (sut/forbidden-words-matches #"(TO-DO|foo.*bar)" ""))
      "Returns `nil` if no match happens.")
  (is (sut/forbidden-words-matches (sut/coll-to-alternate-in-regexp ["TO-DO" #"foo.*bar"])
                                   "not in the returned value.\nhey, this is foo.XX.bar\n a"))
  (is
   (nil?
    (sut/forbidden-words-matches
     (sut/coll-to-alternate-in-regexp ["TO-DO" #"foo.*bar"])
     "#_{:heph-ignore {:forbidden-words [\"tap>\"]}}\nnot in the returned value.\nhey, this is foo.XX.bar\n a"))
   "When the forbidden-words tag is added, no match is found")
  (is
   (nil?
    (sut/forbidden-words-matches
     (sut/coll-to-alternate-in-regexp ["TO-DO" #"foo.*bar"])
     "#_{:heph-ignore {:other :one, :forbidden-words [\"tap>\"]}}\nnot in the returned value.\nhey, this is foo.XX.bar\n a"))
   "When the forbidden-words tag is not the first one, no match is found")
  (is
   (=
    ["hey, this is foo.XX.bar"]
    (sut/forbidden-words-matches
     (sut/coll-to-alternate-in-regexp ["TO-DO" #"foo.*bar"])
     "#_{:heph-ignore {:other :one,} :forbidden-words [\"tap>\"]}}\nnot in the returned value.\nhey, this is foo.XX.bar\n a"))
   "If `:forbiden-words` is out of the `hep-ignore` 's map, the match are found")
  (is (= ["hey, this is foo.XX.bar" "TO-DO"]
         (sut/forbidden-words-matches
          (sut/coll-to-alternate-in-regexp ["TO-DO" #"foo.*bar"])
          "\nnot in the returned value.\nhey, this is foo.XX.bar\n a\nTO-DO"))
      "If many match occur, they're all returned."))
