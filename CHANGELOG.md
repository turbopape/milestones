CHANGELOG
==========
## 0.1.4
* Now the main channel is buffered. No need to wait for a place to put
on the work of a resource, there is enough room for all of them.
* Eliminated a "silly" (<!! (go <! ... reminiscent of the days from
  Clojure Cup.

## 0.1.3
* Now Errors for non existent tasks show a map { task-id [missing task-id]...

## 0-1.0 - 0.1.2 
* Version Bumps to allow clojars to update.
* Now supports milestones
* Now supports detailed error messages
* Now detects cyclic graphs

