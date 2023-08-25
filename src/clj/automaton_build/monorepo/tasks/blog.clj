(ns automaton-build.monorepo.tasks.blog
  (:require
   [clojure.string :as cs]
   [automaton-build.adapters.edn-utils :as baeu]
   [automaton-build.blog :as bb]
   [automaton-build.adapters.files :as files]
   [automaton-build.env-setup :as env-setup]))

(defn documents-folder
  "Directory with customer materials"
  [path]
  (files/create-file-path (get-in env-setup/env-setup [:customer-materials :dir])
                          path))

(defn pdf-metadata
  [document-name description keywords]
  (merge {:title    document-name
          :author   "Hephaistox"
          :creator "Hephaistox"
          :subject  description}
         (when keywords
           {:keywords (str keywords)})))

(defn blog-md->pdf
  "Creates pdf file from md document, adding metadata and branding."
  [_ {:keys [filename
             language]}]
  (let [path (documents-folder filename)

        edn-map-path (str path ".edn")

        language (keyword language)

        information-map (get (baeu/read-edn edn-map-path)
                             language)

        file-name (:file-name information-map)

        edn-pdf-metadata (:metadata information-map)

        document-name (:title edn-pdf-metadata)

        path-parts (cs/split filename #"/")

        file-directories (cs/join "/" (drop-last path-parts))

        md-path (documents-folder (str file-directories "/" file-name))

        tmp-pdf-dir (-> (get-in env-setup/env-setup [:customer-materials :tmp-dir])
                        (files/create-dir-path file-directories))

        pdf-path (str tmp-pdf-dir
                      "/"
                      file-name)

        resources-dir (documents-folder "resources/")

        description (or
                     (:description edn-pdf-metadata)
                     (str "Hephaistox document named: " file-name))

        keywords (str (cs/join ","
                               ["hephaistox" "supply" "chain" "it"])
                      (when (:keywords edn-pdf-metadata)
                        (str ","
                             (:keywords edn-pdf-metadata))))

        pdf-metadata (pdf-metadata document-name
                                   description
                                   keywords)]
    (bb/blog-md->pdf
     {:md-path md-path
      :pdf-path pdf-path
      :resources-dir resources-dir
      :document-name document-name
      :pdf-metadata pdf-metadata})))
