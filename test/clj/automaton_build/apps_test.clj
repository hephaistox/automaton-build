(ns automaton-build.apps-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.adapters.build-config :as build-config]
   [automaton-build.apps :as sut]
   [automaton-build.app.find-an-app-for-test :as bafaaft]))

(def apps-stub
  [{:cust-app? true
    :app-name "app-stub"
    :publication {:as-lib 'app-stub
                  :branch "master"
                  :repo-address "git@github.com:hephaistox/app-stub.git"}
    :monorepo {:app-dir "app_stub"}
    :run-env {:test-env {:host-repo-address "test1"
                         :remote-name "test-env"
                         :app-id "app id"}
              :prod-env {:host-repo-address "git+ssh://git@push-n2-par-clevercloud-customers.services.clever-cloud.com/app_c3f92380-27fd-4145-b5df-7b383e1bfd0c.git"
                         :remote-name "prod-env"
                         :app-id "app id2"}}}
   {:cust-app? false
    :app-name "everything"
    :monorepo {:app-dir "everything"}
    :everything? true}
   {:app-name "non-cust-app"
    :monorepo {:app-dir "non-cust-app"}}
   {:monorepo {:app-dir "base_app"}
    :app-name "base-app"}
   {:build? true
    :monorepo {:app-dir "build"}
    :app-name "build"}])

(comment
  (sut/build-apps (build-config/search-for-build-config))
;
  )
(deftest validate-test
  (testing "Checking test data are valid"
    (sut/validate apps-stub)))

(deftest search-for-one-key-test
  (testing "Find the selected project"
    (is (sut/search-for-one-key :everything?
                                [{:deps-edn {:foo :bar}
                                  :everything? true}])))

  (testing "One selected project is required"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Exactly one everything project is required"
                          (sut/search-for-one-key :everything?
                                                  [{}])))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Exactly one everything project is required"
                          (sut/search-for-one-key :everything?
                                                  [{:deps-edn {:foo :bar}
                                                    :everything? true}
                                                   {:deps-edn {:foo :bar}
                                                    :everything? true}]))))

  (testing "One simple search is working"
    (is (= "everything"
           (:app-name (sut/search-for-one-key :everything?
                                              apps-stub))))))

(deftest template-app-test
  (testing "Find the template app"
    (is (= {:deps-edn {:foo :bar}, :template-app? true}
           (sut/template-app [{:deps-edn {:foo :bar}
                               :template-app? true}])))))

(deftest everything-test
  (testing "Find the everything app"
    (is (= {:deps-edn {:foo :bar}, :everything? true}
           (sut/everything [{:deps-edn {:foo :bar}
                             :everything? true}])))))

(deftest build-test
  (testing "Find the build app"
    (is (= {:deps-edn {:foo :bar}, :build? true}
           (sut/build [{:deps-edn {:foo :bar}
                        :build? true}])))))

(deftest search-app-by-name-test
  (testing "Find images"
    (is (= "app-stub"
           (:app-name (sut/search-app-by-name apps-stub
                                              "app-stub")))))
  (testing "Find non existing images"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Exactly one app called (.*) exists"
                          (sut/search-app-by-name apps-stub
                                                  "foo")))))

(deftest cust-apps-test
  (testing "Customer app list"
    (is (= #{"app-stub"}
           (into #{} (map :app-name (sut/cust-apps apps-stub)))))))

(deftest app-names-test
  (testing "Application names are retrieved"
    (is (= #{"app-stub"
             "everything"
             "non-cust-app"
             "base-app"
             "build"}
           (sut/app-names apps-stub)))))

(deftest cust-app-names-test
  (testing "Customer app names"
    (is (= #{"app-stub"}
           (sut/cust-app-names apps-stub)))))

(deftest app-but-everything-names-test
  (testing "Application names are retrieved but everything is removed"
    (is (= #{"app-stub"
             "non-cust-app"
             "base-app"
             "build"}
           (sut/app-but-everything-names apps-stub)))))

(deftest get-libs-test
  (testing "List applications with their lib alias"
    (is (= ['app-stub]
           (sut/get-libs apps-stub)))))

(deftest is-app?-test
  (testing "Testing existing app"
    (is (sut/is-app? apps-stub "base-app")))
  (testing "Testing non existing app"
    (is (not (sut/is-app? apps-stub "smurf")))))

(deftest is-app-but-everything?-test
  (testing "An application which is not everything is found"
    (is (sut/is-app-but-everything? apps-stub "app-stub")))
  (testing "Everything is not rejected"
    (is (not (sut/is-app-but-everything? apps-stub "everything")))))

#_{:clj-kondo/ignore [:unresolved-namespace]}
(def apps-w-deps-stub
  [{:cust-app? true
    :app-name "app-stub"
    :app-dir "ldir/app_stub"
    :monorepo {:app-dir "app_stub"}
    :publication {:as-lib 'hephaistox/app-stub}
    :deps-edn {:deps {'babashka/process {}
                      'hephaistox/automaton {}
                      'hephaistox/build {}}}
    :dir "app_stub"}
   {:cust-app? false
    :app-name "everything"
    :app-dir "ldir/everything"
    :monorepo {:app-dir "everything"}
    :publication {:as-lib 'hephaistox/everything}
    :deps-edn {:deps {}}
    :dir "everthing"
    :everything? true}
   {:monorepo {:app-dir "base_app"}
    :app-dir "ldir/base_app"
    :publication {:as-lib 'hephaistox/automaton}
    :deps-edn {:deps {'hephaistox/build {}}}
    :app-name "base-app"}
   {:build? true
    :monorepo {:app-dir "build"}
    :app-dir "ldir/build_app"
    :publication {:as-lib 'hephaistox/build}
    :deps-edn {:deps {}}
    :app-name "build"}])

(deftest app-dependency-graph-test
  (testing "Test the graph creation"
    (is (= {'hephaistox/app-stub {:edges {'babashka/process {}
                                          'hephaistox/automaton {}
                                          'hephaistox/build {}},
                                  :app-name "app-stub",
                                  :app-dir "ldir/app_stub"},
            'hephaistox/automaton {:edges {'hephaistox/build {}}
                              :app-name "base-app"
                              :app-dir "ldir/base_app"}
            'hephaistox/build {:edges {}
                               :app-dir "ldir/build_app"
                               :app-name "build"}
            'hephaistox/everything {:edges {},
                                    :app-name "everything",
                                    :app-dir "ldir/everything"}}
           (sut/app-dependency-graph apps-w-deps-stub)))))

(deftest remove-not-required-apps-test
  (testing "Remove not required apps"
    (is (= {'hephaistox/automaton {:edges {'hephaistox/build {}}
                              :app-name "base-app"
                              :app-dir "ldir/base_app"}
            'hephaistox/build {:edges {}
                               :app-dir "ldir/build_app"
                               :app-name "build"}}
           (-> (sut/app-dependency-graph apps-w-deps-stub)
               sut/remove-not-required-apps)))))

(deftest code-files-repo-test
  (testing "No files should be found for that stub"
    (is (nil?
         (sut/code-files-repo apps-stub)))))

(deftest get-existing-src-dirs-test
  (testing "Src dirs exists and are a vector of strings"
    (let [res (sut/get-existing-src-dirs bafaaft/apps)]
      (is (< 5 (count res)))
      (is (every? string? res))
      (is (vector res)))))

(deftest first-app-matching-test
  (testing "All applications are scanned if all return false"
    (let [cnt (atom 0)]
      (sut/first-app-matching bafaaft/apps
                              (fn [_app]
                                (swap! cnt inc)
                                false))
      (is (= (count bafaaft/apps)
             @cnt))))
  (testing "Only the first is scanned is all return true"
    (let [cnt (atom 0)]
      (sut/first-app-matching bafaaft/apps
                              (fn [_app]
                                (swap! cnt inc)
                                true))
      (is (= 1
             @cnt)))))

(deftest search-in-apps-code
  (let [files (sut/code-filenames-in-apps bafaaft/apps)]
    (testing "At least 500 files has been found"
      (is (< 500 (count files)))
      (is (sequential? files)))))
