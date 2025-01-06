# automaton-build

Automaton build library streamlines the creation, development, testing, and publishing of the automaton and cust-apps projects (see [definitions](https://github.com/hephaistox/hephaistox/blob/main/README.md)).

<img src="docs/img/automaton_duck.png" width=130 alt="automaton duck picture"> 

> If every tool, when ordered, or even of its own accord, could do the work that befits it, just as the creations of Daedalus moved of themselves, or the tripods of Hephaestus went of their own accord to their sacred work, if the shuttle would weave and the plectrum touch the lyre without a hand to guide them, master-craftsmen would have no need of assistants and masters no need of slaves ~ Aristotle, Politics 1253b

[Detailed API documentation](https://hephaistox.github.io/automaton-build/latest).

## Motivation

It's an hephaistox objective to be able to spread codebase among different reusable projects. As technical diversity is limited in our project, it is possible to write once most of that tasks in advance.

This project makes possible to quickly create a project and maintain its modifications through time. 

* Advantage: 
   * Same approach than other hephaistox projects
* Disadvantages: 
   * Backward compatilibity is now a question for code written in this project
   * It doesn't solve for now the diversity of technology if it happens

### Alternative approaches

Possible alternative approaches are :

* Templating. Creates project templates copied to build a new project with all cicd in it.
    * Advantage: 
      * Simple to start with
    * Disadvantages: 
       * Makes more complicated, near impossible any future updates as code is copied in all templates and all their copies, 
       * Specificities of each project will always lead to be mixed-up.
* Build constraints on the target projects and build one `automaton-build` leveraging that constraints.
    * Advantage:
       * Classical approach.
    * Disavantage:
       * Don't know how to solve this issue without generalizing all cicd technologies.

## Main features

* Premade tasks for our technical stack
* Build clojure, bb and clojurescript projects
* Test, lint and format project code
* For cli user interaction, execute commands with feedbacks on the cli with two modes: `heading` for a sequence of tasks organizeed as a tree, `actions` which suits for long lasting actions which feedbacks may be intertwine
* Simplify and standardize cli options
* Manage many projects as one project
   * Create one configuration file (deps.edn, shadow-cljs.edn, ....)
   * Deploy one and update others

## Design decision

See [design decision page](docs/design_decisions.md)


---

See license information in [LICENSE file](LICENSE.md) Copyright © 2020-2024 Hephaistox
