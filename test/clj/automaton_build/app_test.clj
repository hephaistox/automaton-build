(ns automaton-build.app-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.adapters.build-config :as build-config]
   [automaton-core.adapters.deps-edn :as deps-edn]
   [automaton-core.adapters.files :as files]
   [automaton-build.app.find-an-app-for-test :as bafaaft]
   [automaton-build.app :as sut]))

(def app-stub
  "Application build config as it is loaded on disk"
  {:monorepo   {:app-dir "app_stub"}
   :publication   {:repo-address "git@github.com:hephaistox/app-stub.git"
                   :as-lib 'app-stub
                   :branch "main"}
   :run-env {:test-env {:host-repo-address "test1"
                        :remote-name "prod-test-env"
                        :app-id "app id"}
             :prod-env {:host-repo-address "test1-prod"
                        :remote-name "prod-test-env"
                        :app-id "app id2"}}
   :templating {:app-title "app stub "}

   :app-name "app-stub"
   :cust-app? true})

(def built-app-stub
  "Application after the loading"
  (assoc app-stub
         :deps-edn {:paths ["src/clj" "src/cljs"]
                    :aliases {:foo {:extra-paths ["src/cljc"]}}}
         :app-dir (files/create-temp-dir)))

(deftest validate-test
  (testing "Valid file"
    (is (sut/validate app-stub))))

(deftest load-deps-edn-test
  (testing "Non existing file should return nil only"
    (is (nil? (deps-edn/load-deps-edn "non-existing-directory")))))

(deftest get-src-dirs-test
  (testing "All 3 directories are found"
    (is (= 3
           (count (sut/get-deps-src-dirs built-app-stub))))))

(deftest get-existing-src-dirs
  (testing "All directories are removed as directories does not exist"
    (is (empty?
         (sut/get-existing-src-dirs built-app-stub)))))

(deftest search-in-app-code-test
  (testing "Non existing app is empty"
    (is (empty?
         (sut/search-in-codefiles built-app-stub))))
  (let [files  (sut/search-in-codefiles bafaaft/one-app)]
    (testing "Non existing app is empty"
      (is (< 10 (count files))))
    (testing "shadow-cljs cache directory is excluded"
      (is (filter #(re-find #"\.shadow-cljs" %) files)))))

(deftest build-app-test
  (testing "Test app generation"
    (let [app-dir (:app-dir built-app-stub)
          build-config-stub-filename (build-config/spit-build-config
                                      app-dir
                                      app-stub
                                      ";;Generated for `app_test`")
          deps-stub {:foofoo :barbar}]
      (deps-edn/spit-deps-edn  app-dir
                               deps-stub
                               ";;Generated for `app_test`")
      (is (= (assoc built-app-stub
                    :deps-edn deps-stub)
             (sut/build-app build-config-stub-filename)))))
  (testing "App generation should be able to adapt to the directory"
    (let [app-dir (:app-dir built-app-stub)
          build-config-stub-filename (build-config/spit-build-config app-dir
                                                                     app-stub
                                                                     ";;Generated for `app_test`")
          deps-stub {:foofoo :barbar}]
      (deps-edn/update-deps-edn app-dir
                                (constantly deps-stub))

      (is (= (assoc built-app-stub
                    :deps-edn deps-stub)
             (sut/build-app build-config-stub-filename))))))

(deftest is-cust-app-but-template?-test
  (testing "Template app and non cust app are refused"
    (is (not (sut/is-cust-app-but-template? {:template-app? true})))
    (is (not (sut/is-cust-app-but-template? {}))))
  (testing "Cust app are accepted"
    (is (sut/is-cust-app-but-template? {:cust-app? true}))))

(deftest code-files-repo-test
  (testing "No codefile found as the app does not exist"
    (is (nil? (sut/code-files-repo built-app-stub)))))

(comment
  (->
   (build-config/search-for-build-config)
   first
   sut/build-app)
;
  )
(deftest is-cust-app-or-everything?-test
  (testing "Accept cust app and everything"
    (is (sut/is-cust-app-or-everything? {:cust-app? true}))
    (is (sut/is-cust-app-or-everything? {:everything? true})))
  (testing "Refuse non cust app and everything"
    (is (not (sut/is-cust-app-or-everything? {})))))
