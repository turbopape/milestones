Milestones - The Thinking Tasks Scheduler
=============================================

[![License MIT](https://img.shields.io/badge/License-MIT-blue.svg)](http://opensource.org/licenses/MIT)
[![Build Status](https://travis-ci.org/turbopape/milestones.svg?branch=master)](https://travis-ci.org/turbopape/milestones)
[![Gratipay](https://img.shields.io/gratipay/turbopape.svg)](https://gratipay.com/turbopape/)
[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.turbopape/milestones.svg)](https://clojars.org/org.clojars.turbopape/milestones)

<img src="./logo.jpg"
 alt="Automagic logo" title="The Robot and the Bunny" align="right" />

> "Any sufficiently advanced technology is indistinguishable from magic"
- According to Clarke's 3rd Law


Milestones is a Clojure and ClojureScript library that needs only your project's task description in order to generate the best possible schedule for you. This is based on the priorities of the tasks that you have set (in terms of fields in tasks, more about this in a second).

Constraints on tasks are:
- Resources (i.e, which resource is needed to perform a particular task),
- The task's duration
- Predecessors (i.e, which tasks need to be done before a particular task can be fired).

Based on the above constraints, Milestones either generates
the schedule (if it does not detect scheduling errors) or shows you any kind
of error it may have found.

Tasks are basically build up out of a map containing IDs as keys and information about
the tasks as values. Information about a task is itself a map of
associating fields to values; here is an example:

```Clojure
{ 1 { :task-name "A description about this task"
      :resource-id 2
      :duration 5 :priority 1}

  2 {:task-name "A description about this task"
      :resource-id 1
      :duration 4
      :priority 1
      :predecessors [1]} }
```

Milestones tries to detect any circular dependencies (tasks
that depend on themselves or tasks that end up depending on
themselves). The task's definition must be a directed
non-cyclical graph.

Tasks (that are not milestones) without a resource-id won't be scheduled and will be reported as erroneous.


Special tasks with `:is-milestone "whatever"` are milestones. They are assigned a random user
and a duration 1, so they can enter the computation like ordinary tasks.
They must have predecessors, otherwise they will be reported as erroneous.

If nothing went wrong during the generation process, the output of Milestones is a schedule. It will be
comprised of the very same tasks mapped with a `:begin` field, telling us when to begin each task.
The time for each task is represented as an integer value.


```Clojure
{ 1 { :task-name "A description about this task"
      :resource-id 2
      :duration 5
      :priority 1
      :begin 0}

  2 {:task-name "A description about this task"
     :resource-id 1
     :duration 4
     :priority 1
     :predecessors [1] :begin 5} }
```
## Installation

You can grab Milestones from clojars. Using Leiningen, you put the dependency in the **:dependencies** section in your project.clj:

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.turbopape/milestones.svg)](https://clojars.org/org.clojars.turbopape/milestones)

## Usage

Start the library using the **schedule** function.
Pass a map to it containing tasks and a vector containing the
properties that define how the scheduler will prioritize the tasks.
Priorities at the left are considered first. The lower the number, the earlier it is scheduled.
Say you want to schedule tasks with lower `:priority` and then lower `:duration`, first do:

```Clojure
    (schedule tasks [:priority :duration])
```

It returns tasks with **`:begin`** fields, or an error

```Clojure
    {:errors nil

    :result {1 {**:begin** }}}
```

Or:

```Clojure
    {:errors {:reordering-errors reordering-errors
             :tasks-w-predecessors-errors tasks-predecessors-errors
             :tasks-cycles tasks-cycles
             :milestones-w-no-predecessors milestones-w-no-predecessors}

     :result nil}
```

### Sample Case

For example, if you have tasks defined to:

```Clojure
    {
    1 {:task-name "Bring bread"
         :resource-id "mehdi"
         :duration 5
         :priority 1
         :predecessors []}

    2 {:task-name "Bring butter"
       :resource-id "rafik"
       :duration 5
       :priority 1
       :predecessors []}

    3 {:task-name "Put butter on bread"
       :resource-id "salma"
       :duration 3
       :priority 1
       :predecessors [1 2]}

    4 {:task-name "Eat toast"
       :resource-id "rafik"
        :duration 4
        :priority 1
        :predecessors [3]}

    5 {:task-name "Eat toast"
        :resource-id "salma"
        :duration 4
        :priority 1
        :predecessors [3]}

    ;; now some milestones
    6 {:task-name "Toasts ready"
        :is-milestone true
        :predecessors [3]}}
```
you would want to run

```Clojure
    (schedule tasks [:duration])
```

and you'd have:

```Clojure
     {:error nil,
      :result {
      ;;tasks with :begin field, i.e at what time shall they be fired.
      1
      {:achieved 5,
       :begin 1,
       :task-name "Bring bread",
       :resource-id "mehdi",
       :duration 5,
       :priority 1,
       :predecessors []},
      2
      {:achieved 5,
       :begin 1,
       :task-name "Bring butter",
       :resource-id "rafik",
       :duration 5,
       :priority 1,
       :predecessors []},
      3
      {:resource-id "salma",
       :achieved 3,
       :duration 3,
       :predecessors [1 2],
       :begin 6,
       :task-name "Put butter on bread",
       :priority 1},
      4
      {:resource-id "rafik",
       :achieved 4,
       :duration 4,
       :predecessors [3],
       :begin 9,
       :task-name "Eat toast",
       :priority 1},
      5
      {:resource-id "salma",
       :achieved 4,
       :duration 4,
       :predecessors [3],
       :begin 9,
       :task-name "Eat toast",
       :priority 1},
      6
      {:resource-id :milestone-user21667,
       :achieved 1,
       :duration 1,
       :predecessors [3],
       :begin 9,
       :task-name "Toasts ready",
       :is-milestone true}}}
```

You can then pass this to another program to render as a Gantt chart (ours is coming soon).
You should have `:achieved` equal to `:duration`, or Milestones will not be able to schedule all of the tasks (this
should not happen).

### Errors

 Error Map Key                 |  What it means
-------------------------------|-----------------------------
:unable-to-schedule            | Something made it impossible for the recursive algorithm to terminate...
:reordering-errors             | { 1 [:priority],...} You gave priority to tasks according to fields (:priority) which some tasks (1) lack).
:tasks-w-predecessors-errors   | :{6 [13],...} These tasks have these non-existent predecessors.
:tasks-w-no-resources          | [1,... These tasks are not milestones and are not assigned to any resource.
:tasks-cycles                  | [[1 2 3]... Set of tasks that are in
a cycle. In this example, 2 depends on 1, 2 on 3 and 3 on 1.
:milestones-w-no-predecessors  | [1 2...  These milestones don't have predecessors.

## History

The concept of auto-magic project scheduling is inspired from **the great**
[Taskjuggler.](http://www.taskjuggler.org).

The first prototype of Milestones was built as an entry to the Clojure
Cup 2014. You can find the code and some technical explanation of the
algorithms in use (core.async, etc...)
[here.](https://github.com/turbopape/milestones-clojurecup2014)

as of version 0.2.X and above, milestone uses a purely functional
algorithm using the same logic of assigning work units, but simply
relying on recur to advance the system status.

Although the prototype showcases the main idea, this repository is the official one, i.e, contains latest versions and is more thoroughly tested.

## Code Of Conduct
Please note that this project is released with a [Contributor Code of Conduct](./CODE_OF_CONDUCT.md). By participating in this project you agree to abide by its terms.

## License and Credits

Copyright © 2016 Rafik Naccache and [Contributors](./CONTRIBUTORS.md). Distributed under the terms of the [MIT License] (https://github.com/turbopape/milestones/blob/master/LICENSE).

All Libraries used in this project (see project.clj) are owned by their respective authors and their respective licenses apply.

The Automagic Logo - Labeled "The Robot and The Bunny" was created by my friend Chakib Daoud.
