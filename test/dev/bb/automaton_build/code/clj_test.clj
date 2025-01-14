(ns automaton-build.code.clj-test
  (:require
   [automaton-build.code.clj :as sut]
   [automaton-build.os.file  :as build-file]
   [clojure.test             :refer [deftest is]]))

(def test-printers
  {:normalln (fn [& args] (println (apply str "n:" args)))
   :errorln (fn [& args] (println (apply str "e:" args)))
   :heading (fn [& args] (println (apply str "**:" args)))})

(deftest copy-project-files-test
  (is
   (= [{:path "src/"
        :relative-path "src"
        :src-dirpath ""
        :status :success
        :type :directory
        :options {:replace-existing true
                  :copy-attributes true}
        :exist? true}
       {:path "env/"
        :relative-path "env"
        :src-dirpath ""
        :status :success
        :type :directory
        :options {:replace-existing true
                  :copy-attributes true}
        :exist? true}
       {:path "pom.xml/"
        :relative-path "pom.xml"
        :src-dirpath ""
        :type :file
        :status :success
        :options {:replace-existing true
                  :copy-attributes true}
        :exist? true}]
      (->> (sut/copy-project-files "" ["src" "env" "pom.xml"] (build-file/create-temp-dir))
           (mapv
            #(select-keys % [:path :relative-path :src-dirpath :status :type :options :exist?]))))))

(deftest compile-jar-test
  (with-out-str (is
                 (= :success
                    (-> (sut/compile-jar "" ["src" "pom.xml"] "tmp/test.jar" test-printers true nil)
                        ;
                        :status)))))
