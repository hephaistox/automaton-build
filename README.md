# automaton-build
The project used by all other Hephaistox projects to build, publish, and update them (e.g. cicd, babashka tasks, files, and version management)

## Project design decisions
`automaton-build` has the following objectives:
* being usable for all kinds of projects
   * Description:
      * shadow-cljs is not necessary or meaningful for all projects
      * even if there are shadow-cljs, there may be no target build in that project (like automaton-web, automaton-core, ...)
   * Rationale: 
      * most of those features are the same for all projects, our one source of truth apply to it
   * Consequences: 
      * Each project has full control on what's the list of tasks, their name, comments, 
      * We can simply reuse tasks
* project flexibility
  * Description:
     * Even if all projects could comply to the standard tasks, it is flexible to add its own, or change the existing ones
  * Rationale:
     * even if most of the time all projects are identical, we may have some specificites due to certain technologies, 
     * or some specificies for some customers,
  * Consequences :
      * `build_config.edn` is different for each app, and contains main differentiators
      * the code of each task could be wrapped or rewritten
* bb.edn are as simple as possible, so update, copy of them is quite simple
  * Description:
    * to have only the description of the tasks to use
  * Rationale:
    * as few code there is in bb.edn, as it is easy to copy paste a new version in it,
  * Consequences:
    * nearly all code is stored in automaton_build

## How to use the project
* Adding tasks in a project consists in is
   * task selection 
     * reuse a task in a project is simple, just pick a function with `opts` parameter in tasks directories
     * and add that entry in the `bb.edn` file
   * parameters updating in `build_config.edn`

## Creates a new tasks
* First prepare all the building blocks in the appropriate namespaces
* Then, a task should be created in the `tasks` directory, in an existing namespace or a new one, the grouping should be done logically so that the tasks are easy to find, 
* By default, tasks are added without the `:clj` options, meaning that everything is run in babashka, you'll need to change it to `:clj` to execute it in clojure mode if
   * the dependencies are not compatible with babashka
   * so the function should be changed to a namespace with a `_clj` suffix, so that the babashka functions are not polluted with the dependency
* Consider to add this task in `la` for testing

License information can be found in [LICENSE file](LICENSE.md)
Copyright © 2020-2024 Anthony Caumond, Mateusz Mazurczak
