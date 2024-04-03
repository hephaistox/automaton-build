(ns automaton-build.doc.blog
  "Blog page creation"
  (:require
   [automaton-build.doc.html     :as build-html]
   [automaton-build.log          :as build-log]
   [automaton-build.os.edn-utils :as build-edn-utils]
   [automaton-build.os.files     :as build-files]
   [automaton-build.os.pdf       :as build-pdf]
   [automaton-build.web.uri      :as build-uri]
   [clojure.string               :as str]))

(defn pdf-metadata
  [document-name description keywords]
  (merge {:title document-name
          :author "Hephaistox"
          :creator "Hephaistox"
          :subject description}
         (when keywords {:keywords keywords})))

(defn create-html-pdf
  "Creates pdf file from md document with pdf document meta data and hephaistox footer/header on every page.
   * `md-path` - place where markdown file can be found
   * `title` - string displayed document name in the footer
   * `pdf-metadata` - map with metadata to include into the pdf file
   * `html-path` - place where html will be stored
   * `pdf-path` - place where pdf will be stored
   * `resources-dir` - directory where you can find resources that the md file is referencing"
  [{:keys [md-path title pdf-metadata html-path pdf-path resources-dir]}]
  (if md-path
    (let [header-id "margin-header"
          html-str (build-html/md->html-str {:md-path md-path
                                             :html-path html-path
                                             :header-id header-id})]
      (when html-path
        (build-log/trace-format "Generate html `%s` from: `%s` "
                                html-path
                                md-path)
        (build-files/spit-file html-path html-str))
      (when pdf-path
        (build-log/trace-format "Generate pdf `%s` from `%s`" pdf-path md-path)
        (build-pdf/html-str->pdf
         {:html-str html-str
          :output-path pdf-path
          :resources-dir resources-dir
          :pdf-metadata pdf-metadata
          :margin-box {:margin-box
                       {:top-left {:element header-id}
                        :bottom-left {:text (str "Hephaistox - " title)}
                        :bottom-right {:paging [:page " of " :pages]}}}
          :styles (-> (build-files/create-dir-path resources-dir "blog.css")
                      build-files/absolutize
                      build-uri/from-file-path)})))
    (build-log/warn-format "Md path `%s` is not found" md-path)))

(defn configuration-data-by-language-to-html-pdf
  "Creates pdf file from md document, adding metadata and branding."
  [{:keys [metadata filename]
    :as blog-data}]
  (let [{:keys [title description keywords]
         :or {description (str "Hephaistox document named: " filename)}}
        metadata
        keywords (str/join ","
                           (concat ["hephaistox" "supply" "chain" "it"]
                                   (when keywords (str "," keywords))))]
    (create-html-pdf (assoc (select-keys blog-data
                                         [:md-path :pdf-path :html-path :title])
                            :pdf-metadata
                            (pdf-metadata title description keywords)))))

(defn configuration-data
  [blog-config-file output-html-dir output-pdf-dir]
  (->> (build-edn-utils/read-edn blog-config-file)
       (map
        (fn [[lang
              {:keys [filename]
               :as configuration-data}]]
          (assoc configuration-data
                 :md-path (build-files/file-in-same-dir blog-config-file
                                                        (str filename ".md"))
                 :lang lang
                 :pdf-path (build-files/create-file-path output-pdf-dir
                                                         (str filename ".pdf"))
                 :html-path (build-files/create-file-path output-html-dir
                                                          (str filename
                                                               ".html")))))))

(defn blog-process
  "Process all customer materials directory as set in the configuration file"
  [customer-materials-dir output-html-dir output-pdf-dir]
  (build-log/info "Start to process blog files")
  (build-log/trace-map "Blog is processing all files in the following dirs"
                       :customer-materials-dir customer-materials-dir
                       :output-html-dir output-html-dir
                       :output-pdf-dir output-pdf-dir)
  (let [blog-config-files (build-files/search-files customer-materials-dir
                                                    "**.edn")]
    (build-log/debug-format "Process configuration files %s" blog-config-files)
    (if (empty? blog-config-files)
      (do (build-log/error "No blog file found") false)
      (do (doseq [blog-config-file blog-config-files]
            (build-log/trace-format
             "Process `%s blog configuration file` blog-config-file")
            (let [configuration-data (configuration-data blog-config-file
                                                         output-html-dir
                                                         output-pdf-dir)]
              (doseq [{:keys [pdf-path html-path md-path]} configuration-data]
                (if (and (build-files/modified-since blog-config-file
                                                     [pdf-path html-path])
                         (build-files/modified-since md-path
                                                     [pdf-path html-path]))
                  (build-log/trace-format
                   "Blog files `%s` and `%s` are uptodate"
                   pdf-path
                   html-path)
                  (do (build-log/debug-format
                       "Blog file `%s` and `%s` are regenerated from `%s`"
                       pdf-path
                       html-path
                       md-path)
                      (doseq [configuration-data-by-language configuration-data]
                        (configuration-data-by-language-to-html-pdf
                         configuration-data-by-language)))))))
          true))))
