(ns milestones.core
  (:require [milestones.dyna-scheduler :refer [schedule]]
            [milestones.nlp-tools :refer [nlp guess-tasks-from-str]]
            [milestones.browser-charts :refer [draw-gantt!]]
            [dommy.core :as dommy :refer-macros [sel1]]
            [goog.dom :as dom]
            [goog.events :as events]
            ))

(def schedule-btn (dom/getElement "schedule"))

(def error-zone (sel1 [:body :#portfolio :#dzone]))

(defn schedule-and-show!
  [schedule-start
   default-duration-unit
   in-div-id]
  (let [tasks-str (.-value (.getElementById  js/document "default-template"   ))
      
        tasks  (guess-tasks-from-str tasks-str [:predecessors :priority])
       scheduled (schedule tasks [:priority])]
   (if-let [err (get scheduled :errors)]
     (do
       (.error js/console err )
       (-> error-zone
           (dommy/set-text! "Couldn't schedule... plz retry !")
           (dommy/add-class! :alert-danger)))
       
       
       (do
         (-> error-zone
             (dommy/set-text! "Successfully scheduled!")
             (dommy/remove-class! :alert-danger)
             (dommy/add-class! :alert-success))
         
         (draw-gantt!
          (:result  scheduled)
          schedule-start
          default-duration-unit
          in-div-id)))))

(events/listen schedule-btn "click" #(schedule-and-show! (.format  (js/moment))
                                                         "hours"
                                                         "gantt-chart")) 













