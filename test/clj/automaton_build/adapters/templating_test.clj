(ns automaton-build.adapters.templating-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]

   [automaton-build.adapters.files :as files]
   [automaton-build.adapters.templating :as sut]))

(deftest replace-contract
  (testing "What contract is with str/replace"
    (is (= "This foo-app is stored in foo_app"
           (str/replace "This template-app is stored in template_app"
                        #"template-app|template_app"
                        {"template-app" "foo-app"
                         "template_app" "foo_app"})))))

(deftest get-keys-test
  (testing "Simple example"
    (is (= ["a" "c" "e"]
           (sut/get-keys {:a "b"
                          :c :d
                          :e 1}
                         "" ""))))
  (testing "Example with delimiters"
    (is (= ["fooabar" "foocbar" "fooebar"]
           (sut/get-keys {:a "b"
                          :c :d
                          :e 1}
                         "foo" "bar")))))

(deftest kw-to-replacement-test
  (testing "kw is letters only"
    (is (= "foo"
           (sut/kw-to-replacement :foo))))
  (testing "kw is letters only and numbers"
    (is (= "foo2"
           (sut/kw-to-replacement :foo2))))
  (testing "kw is letters only and minus"
    (is (= "foo-bar"
           (sut/kw-to-replacement :foo-bar))))
  (testing "kw is letters only and minus"
    (is (= "22"
           (str/replace "foo-bar"
                        (sut/kw-to-replacement :foo-bar)
                        "22")))))

(deftest replacement-pattern-test
  (testing "Simple map generates key and values"
    (is (= ["a|c|e"
            {"a" "b"
             "c" ":d"
             "e" "1"}]
           (sut/replacement-pattern {:a "b"
                                     :c :d
                                     :e 1}
                                    ""
                                    ""))))
  (testing "Simple map with prefix and suffix"
    (is (= ["fooabar|foocbar|fooebar"
            {"fooabar" "b"
             "foocbar" ":d"
             "fooebar" "1"}]
           (sut/replacement-pattern {:a "b"
                                     :c :d
                                     :e 1}
                                    "foo" "bar")))))

#_(deftest render-content-test
    (testing "Failing is throwing an exception"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Impossible to render the template"
                            (sut/render-content "foo-foo-bar"
                                                1 "" ""))))
    (testing "Simple string replacement"
      (is (= "The 1 is now replaced with something else"
             (sut/render-content "The foo is now replaced with something else"
                                 {:foo "1"} "" "")))
      (is (= "The 1 is now replaced with something else"
             (sut/render-content "The foo is now replaced with something else"
                                 {:foo 1} "" ""))))
    (testing "Simple string replacement with delimiters"
      (is (= "The 1 is now replaced with something else"
             (sut/render-content "The yofooho is now replaced with something else"
                                 {:foo "1"} "yo" "ho")))
      (is (= "The 1 is now replaced with something else"
             (sut/render-content "The foo is now replaced with something else"
                                 {:foo 1} "" ""))))
    (testing "Escape characters are not replaced"
      (is (= "This &<>\" is ok"
             (sut/render-content "This foo is ok"
                                 {:foo "&<>\""} "" ""))))
    (testing "Realistic example"
      (is (= "This foo-bar is stored in foo_bar"
             (sut/render-content "This template-app is stored in template_app"
                                 {:template-app "foo-bar"
                                  :template_app "foo_bar"} "" "")))
      (is (= "This foo-bar is stored in template_app"
             (sut/render-content "This template-app is stored in template_app"
                                 {:template-app "foo-bar"} "" "")))
      (is (= "This foo-bar is stored in foo-bar"
             (sut/render-content "This templateapp is stored in templateapp"
                                 {:templateapp "foo-bar"} "" "")))))

(deftest escape-minus
  (testing "with no minus"
    (is
     (= "foo"
        (sut/escape-minus "foo"))))
  (testing "with minus"
    (is
     (= "fo\\-o"
        (sut/escape-minus "fo-o"))))
  (testing "so usable in regexp"
    (is (= "this app is ok"
           (str/replace "this fo-o is ok"
                        (re-pattern (sut/escape-minus "fo-o"))
                        "app")))))

(deftest search-tokens-test
  (testing "Tokens are found in the codefiles"
    (is (= {:elll "", :l ""}
           (sut/search-tokens {"foo" "hello"
                               "bar" "Again, say heellllo"}
                              "he"
                              "lo")))))

(comment
  (sut/change-marker (slurp "template_app/README.md")
                     #"Don't modify"
                     "Template-app manages this namespace")
  (sut/rename-dirs (files/search-files "."
                                       "**{clj,cljs,cljc,edn,md,json,xml,txt,css}")
                   "realtime"
                   "duplex")
  ;
  )
