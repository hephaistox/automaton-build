(ns automaton-build.project.impl.clojars-deploy)

(defn deploy
  "Deploys jar that is in `jar-path` with it's pom file located in `pom-path` to `repository`.
   [deps-deploy docs](https://github.com/slipset/deps-deploy)
   Params:
   * `jar-path` string
   * `pom-path` string
   * `repository` map consisting of key being an id for where to deploy and value being required data for deploy (e.g. map with :url, :username, :password) "
  [jar-path]
  ["clojure" "-X:deploy" ":artifact" jar-path]
  #_(deps-deploy/deploy {:installer :remote
                         :artifact jar-path
                         :sign-release? true
                         :pom-file pom-path
                         :repository repository}))
