# Design decisions

* Being usable for all kinds of projects
    *   Description:
        * All projects don't have the same features, some don't have cljs, some have cljs for cljc but no css, ...
    * Rationale: 
        * Aligning all projects with all technology will soon lead to a mess.
        * But some technology are really common to numerous projects, like linting, so our one source of truth apply to it
        * And they may have some flavors, with different parameter values, different behaviors and so on.
    * Consequences: 
        * Each project builds it own `bb.edn` task and its own code
        * But it is also leveraging functions from this library.

* Project flexibility
    * Description:
        * Even if all projects could comply to the standard tasks, it is flexible to add its own, or change the existing ones
    * Rationale:
        * even if most of the time all projects are identical, we may have some specificites due to certain technologies, 
        * or some specificies for some customers,
    * Consequences :
        * All parameters that are project specific are in `bb.edn`, most probably hard coded in some calls of automaton-build function calls.
        * Parameters reused by more than one task of the same project should be put in `project.edn`, so they are clearely shared.
        * Others are hard coded in the function itself.

* Documentation should be as close as possible from the usage
    * Description
        * When an error or an error suspicion happen when calling a task, it should display what's wrong and how to fix it, o
    * Rationale:
        * In the tasks, at runtime, we exactly know the context and can tell exactly what's interesting for the end user.
        * In other documentation, an effort is needed to read it, and it may be misaligned.
        * Having documentation displayed enforce its update.
    * Consequences:
        * Tasks are managing errors and display expected behavior.

* Cli separation of concerns:
    * Description
        We separate the following concerns:
        * Assembling all features together
        * Displaying error messages
        * Doing actually something
    * Rationale
        * They're may be more than one printing technology / flavor.
        * Reworks are easier todo
    * Consequence:
        * Wf directory contains tasks, really focused on their assembling concern.
        * code, data, doc, are low level functions, they should not care about data validation and logging.
        * echo/xxx/vcs.clj is for instance the wrapper for the vcs technology when we'd like to display it with the `xxx` technology.
