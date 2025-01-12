(ns automaton-build.code.clj-test
  (:require
   [automaton-build.code.clj :as sut]
   [automaton-build.os.file  :as build-file]
   [clojure.test             :refer [deftest is]]))

(def tmp-dir (build-file/create-temp-dir))

tmp-dir
"/var/folders/vr/yw9lwdp973d1dmcvx4jqx7y40000gn/T/e78378fb-737f-4d50-acf4-269283a5e6d27613299554445681926/"


(deftest copy-project-files-test
  (is
   (= [{:path "src/"
        :relative-path "src"
        :src-dir ""
        :status :success
        :directory? true
        :options {:replace-existing true
                  :copy-attributes true}
        :exist? true}
       {:path "env/"
        :relative-path "env"
        :src-dir ""
        :status :success
        :directory? true
        :options {:replace-existing true
                  :copy-attributes true}
        :exist? true}
       {:path "pom.xml/"
        :relative-path "pom.xml"
        :src-dir ""
        :file? true
        :status :success
        :options {:replace-existing true
                  :copy-attributes true}
        :exist? true}]
      (->> (sut/copy-project-files "" ["src" "env" "pom.xml"] tmp-dir)
           (mapv #(dissoc % :dst-dir :target-dir-path :apath))))))

(deftest compile-jar-test
  (is
   ;
   (sut/compile-jar "" ["src" "env"] "tmp/test.jar" println println true)))
