(ns automaton-build.code.artifacts
  "A proxy to library for building artifacts in Clojure projects."
  (:require
   [clojure.tools.build.api :as clj-build-api]
   [clojure.tools.deps.util.io]))

(defn create-basis
  "Create a basis from a set of deps sources and aliases."
  ([] (clj-build-api/create-basis))
  ([params] (clj-build-api/create-basis params)))

(defn write-pom
  "Create pom file to META-INF/maven/<groupId>/<artifactId>/pom.xml"
  [params]
  (clj-build-api/write-pom params))

(defn compile-clj
  "Compile Clojure source to classes in :class-dir. Clojure source files are found in :basis :paths by default, or override with :src-dirs."
  [params]
  (clj-build-api/compile-clj params))

(defn set-project-root!
  "Sets project root variable that's defaulted to \".\" to `root`"
  [root]
  (clj-build-api/set-project-root! root))

(defn jar "Create jar file containing contents of class-dir." [params] (clj-build-api/jar params))

(defn uber
  "Create uberjar file. An uberjar is a self-contained jar file containing
  both the project contents AND the contents of all dependencies. Which makes it runnable with java -jar command"
  [params]
  (clj-build-api/uber params))
