# Monorepo tools
This subproject contains code factorized for CI/CD, it is meant for:
* Development using `everything` project,
* To manage all `bb` tasks of all customer projects,
* Manage `bb` tasks of automaton projects
* Manage all complimentary tasks, like templating, docker image creation and so on.
* Clojure root directory is where all clojure code should be stored. So `everything` project can gather all of it
* Root directory is different as some commands should be executed from there.
