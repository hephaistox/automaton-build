# automaton-build
Automaton build library streamlines the creation, development, testing, and publishing of the projects.

<img src="docs/img/automaton_duck.png" width=130 alt="automaton picture"> 

> If every tool, when ordered, or even of its own accord, could do the work that befits it, just as the creations of Daedalus moved of themselves, or the tripods of Hephaestus went of their own accord to their sacred work, if the shuttle would weave and the plectrum touch the lyre without a hand to guide them, master-craftsmen would have no need of assistants and masters no need of slaves ~ Aristotle, Politics 1253b

## Motivation
While working with multiple projects in the organization, many things are repetitive, like running repl, formatting and linting the code, versioning, CICD etc. This library aims to simplify and automate this work. 

## Quick start
To integrate automaton-build into your project
1. Create `bb.edn` file at the root of your project with the following content:

``` clojure
{:deps {org.clojars.hephaistox/automaton-build #:mvn{:version "1.0.2"}} 
:tasks {-base-deps {:doc "Dependencies for a task using bb"
                    :extra-deps {org.clojure/tools.cli {:mvn/version "1.1.230"}}}
        :requires [[automaton-build.tasks.common :as tasks-common]]}}
```
Which will:
* Add this library (automaton-build) dependency to enable its features.
* Add the `-base-deps` task. Which will be used to declare whatever is common to all tasks. It starts with `-` so it is not shown in the task list.
* Add the `requires` that enables `tasks-common` namespace for all tasks so you don't have to repeat it.

2. Add your custom tasks or use pre-defined ones from the `automaton-build.tasks` directory.

Example task for starting REPL:
```clojure
 repl {:depends [-base-deps]
       :requires [[automaton-build.tasks.2 :as tasks-2]]
       :extra-deps {djblue/portal {:mvn/version "0.52.2"}}
       :doc "Launch repl"
       :enter (tasks-common/enter tasks-2/cli-opts (current-task))
       :task (tasks-2/start-repl [:common-test :env-development-repl :build])}
```

## Tasks configuration
Some tasks may require additional configuration. Set up a `project.edn` file in your project root to customize task behavior. 
For an example refer to the forbidden-words report task [automaton-build.tasks.tasks.3](src/bb/automaton_build/tasks/3.clj) and [project.edn file](project.edn) 

## Compatibility of dependencies
To use external dependency (`:extra-deps` alias), dependency needs to compile to the GraalVM or be included in Babashka.
[For a list of compatible libraries see the Babashka documentation](https://github.com/babashka/babashka/blob/master/doc/projects.md)

In case the library is not supported, but has CLI capabilities, you can include it as an alias in the project `deps.edn` and just call cmd with "clojure -M/X:alias". 
[Example of it can be found in documentation task that uses codox library](src/bb/automaton_build/tasks/doc.clj)

## See documentation
This library is heavily based on the usage of [babashka tasks](https://book.babashka.org/#_tasks_api). 

If you're interested in more detail about the approach we took while developing this library look at [design decisions document](docs/design_decision.md)

[For detailed API documentation click here](https://hephaistox.github.io/automaton-build/latest).

License information can be found in [LICENSE file](LICENSE.md)
Copyright © 2020-2024 Hephaistox
