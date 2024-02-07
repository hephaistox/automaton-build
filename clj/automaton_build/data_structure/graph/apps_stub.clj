(ns automaton-build.data-structure.graph.apps-stub)

#_{:clj-kondo/ignore [:unresolved-namespace]}
(def apps-w-deps-stub
  [{:app-dir "ldir/app_stub"
    :app-name "app-stub"
    :monorepo {:app-dir "app_stub"}
    :publication {:as-lib 'hephaistox/app-stub}
    :deps-edn {:deps {'babashka/process {}
                      'hephaistox/automaton {}
                      'hephaistox/build {}}}}
   {:app-dir "ldir/everything"
    :app-name "everything"
    :monorepo {:app-dir "everything"}
    :publication {:as-lib 'hephaistox/everything}
    :deps-edn {:deps {}}}
   {:app-dir "ldir/base_app"
    :monorepo {:app-dir "base_app"}
    :publication {:as-lib 'hephaistox/base-app}
    :app-name "base-app"
    :deps-edn {:deps {'hephaistox/build {}}}}
   {:monorepo {:app-dir "build"}
    :app-dir "ldir/build_app"
    :app-name "build"
    :publication {:as-lib 'hephaistox/build}
    :deps-edn {:deps {}}}])
