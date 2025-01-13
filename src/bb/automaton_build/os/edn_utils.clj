(ns automaton-build.os.edn-utils
  "Read an edn file."
  (:require
   [automaton-build.code.formatter :as build-formatter]
   [automaton-build.os.cmd         :as build-cmd]
   [automaton-build.os.file        :as build-file]
   [automaton-build.os.filename    :as build-filename]
   [clojure.edn                    :as edn]))

(defn str->edn "Turns `raw-content` string into an edn" [raw-content] (edn/read-string raw-content))

(defn read-edn
  "Read `edn-filename` and returns a map with:
  * `:filepath`
  * `:afilepath`
  * `:raw-content` if file can be read.
  * `:invalid?` is boolean
  * `:exception` if something wrong happened.
  * `:edn` if the translation."
  ([{:keys [errorln uri-str]
     :as _printers}
    edn-filename]
   (let [res (build-file/read-file edn-filename)
         {:keys [raw-content invalid?]} res]
     (if (or (nil? raw-content) invalid?)
       res
       (try (assoc res :edn (str->edn raw-content))
            (catch Exception e
              (when (and (fn? errorln) (fn? uri-str))
                (errorln (str "File"
                              (-> edn-filename
                                  uri-str
                                  build-filename/absolutize)
                              "not found")))
              (assoc res :exception e :status :edn-failed))))))
  ([edn-filename] (read-edn nil edn-filename)))

(defn format-file
  "Format the `edn` file
  Format file is not blocking if the formatter is not setup or if the file does not exist

  Returns nil if successfully updated
  Params:
  * `filename` to format"
  [filename]
  (let [setup (build-formatter/formatter-setup)]
    (cond
      (not (= :ok (:status setup))) setup
      (not (build-file/is-existing-file? filename)) {:exception
                                                     "Can't format file `%s` as it's not found"}
      :else (build-cmd/as-string (build-formatter/format-file-cmd filename) ""))))

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
