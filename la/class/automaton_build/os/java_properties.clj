(ns automaton-build.os.java-properties
  "Adapter to java properties"
  (:require
   [clojure.string :as str]))

(defn get-java-properties
  "Get the java properties"
  []
  (into {} (System/getProperties)))

(defn get-java-property
  "Get a java property named `property-name`, if not found return `default-value`"
  ([property-name default-value]
   (or (System/getProperty property-name) default-value))
  ([property-name] (System/getProperty property-name)))

(defn split-property-value
  "Returns a collection of values from `property-value` splitted by ','."
  [property-value]
  (str/split property-value #","))
