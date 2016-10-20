CHANGELOG
==========
## 0.3.0
* Add GANTT Charts generation Thanks to [Google Charts Lib](https://developers.google.com/chart/interactive/docs/gallery/ganttchart)
* Add support for NLP syntax parsing in clojurescript -
  Using
  [nlp-compromise](https://github.com/nlp-compromise/nlp_compromise)
* Created a little Web Play Ground.

## 0.2.1
* Fix output if the task is a milestone, don't show the random
  generated user.

## 0.2.0
* Add ClojureScript support thanks to the use of reader conditionals,
  the same namespaces are used.
* Removed dependencies to loom and combinatorics. Use own [Tarjan's
  algorithm](https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm) implementation.
* Removed reliance on core.async, now using a purely functional
  implementation using loop/recur (So ClojureScript Support is
  possible).
* Added new error type : `:unable-to-schedule`

## 0.1.4
* The main channel is buffered. No need to wait for a place to put
on the work of a resource, there is enough room for all of them.
* Fix Code Style.

## 0.1.3
* Now Errors for non existent tasks show a map { task-id [missing task-id]...

## 0-1.0 - 0.1.2 
* Version Bumps to allow clojars to update.
* Add support for milestones
* Add support for detailed error messages
* Add support for cyclic graphs

