Milestones - The Thinking Tasks Scheduler
=============================================

[![License GPL 3](http://img.shields.io/badge/license-GPL%203-green.svg)](http://www.gnu.org/licenses/gpl-3.0.txt)
[![Build Status](https://travis-ci.org/automagictools/milestones.svg?branch=master)](https://travis-ci.org/automagictools/milestones)
[![Gratipay](https://img.shields.io/gratipay/turbopape.svg)](https://gratipay.com/turbopape/)
[![Paypal](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=rafik%2enaccache%40gmail%2ecom&lc=US&item_name=Automagic%20Tools&item_number=automagictools&no_note=0&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHostedGuest)


![Automagical Tools Logo][Logo]
[Logo]:./logo.jpg

>"Any sufficiently advanced technology is indistinguishable from magic"
>>[Clarke's 3rd Law](https://en.wikipedia.org/wiki/Clarke%27s_three_laws)

Milestones is a Clojure library That only needs your project tasks description in order to generate the best possible schedule for you, based on priorities of scheduling you set (in terms of fields in tasks, more about this in a second).

Constraints are resource : i.e, which resource is needed to perform task, the task duration, and predecessors, i.e, which tasks need to be done before a particular task can be fired.

Based on the above constraints specification, Milestones generates the Schedule if it does not detect scheduling errors, or tells you why it was not able to do so.

Tasks are basically a map containing ids as keys and information about
the tasks as values (maps). Here is an example :

        { 1 { :task-name "A description about this task" 
        :resource-id 2 
        :duration 5 :priority 1 }
    
        2 {:task-name "A description about this task" 
          :resource-id 1 
          :duration 4 
          :priority 1 
          :predecessors [1]} }

Milestones tries to detect any circular dependencies, that is, tasks
that depend on themselves or on tasks that end up depending on
themselves, actually, the tasks definition must be a directed non
cyclical graph.

Tasks (that are not milestones) without resource-ids won't be scheduled.Maybe we'll issue a warning for such tasks.

Special tasks with  :is-milestone "whatever" are milestones, they are assigned a random user
and a duration 1, so they can enter the computation like ordinary tasks. 
They must have predecessors, else they will be reported as erroneous.

The output of Milestones is a schedule, that is, if it's possible, the
tasks map, with a :begin field, telling us when to begin each task.
	
	{ 1 { :task-name "A description about this task" :resource-id 2
		:duration 5 :priority 1 **:begin 0**}

	  2 {:task-name "A description about this task" :resource-id 1
        :duration 4 :priority 1 :predecessors [1] **:begin 5**}}


## Usage

You fire the library using the schedule function , 
you pass to it a map containing  tasks and a vector containing the 
properties you want the scheduler to use to give higher priorities to tasks (
less is higher priority) like so (if you want to schedule tasks with lower _:priority_ then lower _:duration_ first:

        (schedule tasks [:priority :duration])

It gives you back tasks with begin fields, or an error map.
        {:error nil , :result {1 {**:begin** }}}
OR
      {:error {:reordering-errors reordering-errors
             :tasks-predecessors-errors tasks-predecessors-errors
             :tasks-cycles tasks-cycles
             :milestones-w-no-predecessors milestones-w-no-predecessors},
     :result nil}

### Sample Case

for example, if you have tasks def'd to:

	     { 1 {:task-name "Bring bread"
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
                     :predecessors [3]
                  }}

you would want to run

	(schedule tasks [:duration])

and you'd have :

     {:error nil,
       :result
       {1
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

Which you can pass to another program to render as a GANTT program (ours is coming soon.)
You should have :achieved equal to :duration, or the program was not able to schedule all of the task - This
should not happen by the way.

### Errors 

 Error Map Key                 |  What it means
-------------------------------|-----------------------------
:reordering-errors             | { 1 [:missing-priority-field],...} You gave priority to tasks according to fields (:priority) which some tasks (1) lack)
:tasks-predecessors-errors     | [1 , 2.... these tasks have non-existent predecessors 
:tasks-w-no-resources          | [1,... These tasks are no milestones and are not assigned to any resource
:tasks-cycles                  | [[1 2] [3 5]... Couple of tasks that are in a cycle : 1 depends on 2, and 2 on 1
:milestones-w-no-predecessors | [1 2...  | These milestones don't have predecessors


## History

The concept of auto-magic project scheduling is inspired from **the great**
[Taskjuggler](http://www.taskjuggler.org). 

A first prototype of Milestones was built as an entry to the Clojure
Cup 2014. You can find the code and some technical explanation of the
algorithms in use (core.async, etc...) 
[here](https://github.com/turbopape/milestones-clojurecup2014).

Although the protorype showcases the main idea, this repository is the official one, i.e, contains latest versions and is more thoroughly tested.

## License and Credits

Copyright © 2014 rafik Naccache and Contributors.

Distributed under the GNU GPL v3.

All used Libraries in this project (see project.clj) pertain to their
respective authors and their respective licenses apply.

The automagic Logo is created by my friend Chakib Daoud.
