(ns automaton-build.tasks.launcher.bb-entrypoint-test
  (:require
   [automaton-build.log                          :as build-log]
   [automaton-build.tasks.launcher.bb-entrypoint :as sut]))

(comment
  (sut/-main ["blog" "-l" "trace" "-d"])
  (sut/-main ["apps" "-l" "trace" "-d"])
  (sut/-main ["clean" "-l" "trace" "-d"])
  (sut/-main ["clean-hard" "-l" "trace" "-d"])
  (sut/-main ["gha" "-l" "trace" "-d"])
  (sut/-main ["gha" "-l" "trace" "-d"])
  (build-log/set-min-level! :trace)
  ;
)
