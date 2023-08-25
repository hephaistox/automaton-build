(ns automaton-build.env-setup
  "Environment data setup for monorepo")

(def env-setup
  "Set directories, accounts depending on the environment where we build"
  {:archi {:dir "../docs/archi/"}
   :customer-materials {:dir "../docs/customer_materials"
                        :tmp-html-dir "../tmp/pdf/htmls"
                        :tmp-dir "../tmp/pdf"}

   :container-repo {:assembly-subdir "containers"
                    :account "hephaistox"
                    :cc {:source-dir "container_images/cc_image"
                         :repo-name "cc-image"}
                    :gha {:source-dir "container_images/gha_image"
                          :repo-name "gha-image"}}
   :published-apps {:dir "../tmp/apps"
                    :code-subdir "code-pub"}
   :documentation {:codox "docs/codox"
                   :code-stats "../docs/code/stats.md"
                   :code-subdir "docs/code"}
   :log {:spitted-edns "../tmp/logs/spitted"}
   :tests {:tmp-dirs "../tmp/tests"}})
