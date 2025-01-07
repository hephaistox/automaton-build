(ns automaton-build.os.edn-utils
  "Read an edn file."
  (:require
   [automaton-build.code.formatter :as build-formatter]
   [automaton-build.os.cmds        :as build-commands]
   [automaton-build.os.file        :as build-file]
   [clojure.edn                    :as edn]))

(defn str->edn [raw-content] (edn/read-string raw-content))

(defn read-edn
  "Read file which name is `edn-filename`.

  Returns:

  * `filename`
  * `raw-content` if file can be read.
  * `invalid?` to `true` whatever why.
  * `exception` if something wrong happened.
  * `edn` if the translation."
  [edn-filename]
  (let [res (build-file/read-file edn-filename)
        {:keys [raw-content invalid?]} res]
    (if invalid?
      res
      (try (assoc res :edn (str->edn raw-content))
           (catch Exception e (assoc res :exception e :invalid? true))))))

(defn formatter-setup
  []
  (let [home-setup-file-desc (build-formatter/read-home-setup)
        {:keys [raw-content invalid?]
         :as resp}
        home-setup-file-desc]
    (if invalid?
      resp
      (if (build-formatter/is-zprint-using-project-setup? raw-content)
        {:message "zprint use properly project setup."
         :status :ok}
        {:exception
         "zprint local configuration is missing. Please add `:search-config? true` in your `~/.zprintc`"}))))

(defn format-file
  "Format the `edn` file
  Format file is not blocking if the formatter is not setup or if the file does not exist

  Returns nil if successfully updated
  Params:
  * `filename` to format"
  [filename]
  (let [setup (formatter-setup)]
    (cond
      (not (= :ok (:status setup))) setup
      (not (build-file/is-existing-file? filename)) {:exception
                                                     "Can't format file `%s` as it's not found"}
      :else (let [res (build-commands/blocking-cmd (build-formatter/format-file-cmd filename) "")]
              (if (= 0 (:exit res)) nil {:exception (select-keys res [:out :err])})))))

(defn write
  "Spit the `content` in the edn file called `edn-filename`.
  Params:
  * `edn-filename` Filename
  * `content` What is spitted
  Return nil if successful else map with :exception"
  [edn-filename content]
  (if (nil? edn-filename)
    {:exception "Impossible to save the file due to empty filename"}
    (if-let [file-write-ex (build-file/write-file edn-filename content)]
      file-write-ex
      (try (format-file edn-filename) (catch Exception e {:exception e})))))
