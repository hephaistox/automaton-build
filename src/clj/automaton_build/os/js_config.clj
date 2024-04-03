(ns automaton-build.os.js-config
  "Everything about config.js files manipulation"
  (:require
   [automaton-build.log      :as build-log]
   [automaton-build.os.files :as build-files]
   [clojure.string           :as str]))

(defn join-config-items
  "Joins config items (like prestes requires, content paths etc.). Any items in a way that is acceptable by js config files"
  [config-items]
  (str/join "," config-items))

(defn js-require
  "Turns `package-name` into js require"
  [package-name]
  (str "require('" package-name "')"))

(defn load-js-config
  [filepath]
  (when (build-files/is-existing-file? filepath)
    (build-files/read-file filepath)))

(defn write-js-config
  [filepath content header]
  (try (build-files/spit-file filepath content header)
       (catch Exception e
         (build-log/error-exception (ex-info "Writing js config file has failed"
                                             {:path filepath
                                              :content content}
                                             e))
         nil)))
