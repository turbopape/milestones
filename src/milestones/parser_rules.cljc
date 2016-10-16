(ns milestones.parser-rules)

(def rules
  [
   '(:predecessors
     #{:Question}
     #{:Task}
     #{:Noun :Value}
     #{:Noun}
     #{:Verb :PastTense}
     :resource-id
     #{:Noun}
     :duration
     #{:Verb :Modal}
     #{:Verb :Infinitive}
     #{:Noun :Plural :Date}
     :task-id
     #{:Preposition}
     #{:Task}
     #{:Noun :Value}
     :task-name
     #{:Preposition}
     #{:Noun}
     #{:Preposition}
     #{:Verb :Infinitive}
     #{:Noun})
   '(:task-id
     #{:Conjunction}
     #{:Task}
     #{:Noun :Value}
     :resource-id
     #{:Noun}
     :task-name
     #{:Verb :FutureTense}
     #{:Preposition}
     #{:Verb :Infinitive}
     #{:Noun}
     :duration
     #{:Preposition}
     #{:Noun :Plural :Date}
     :dependencies
     #{:Preposition :Condition}
     #{:Task :Condition}
     #{:Noun :Value :Condition})
   '(:task-id
     #{:Task}
     #{:Noun :Value}
     :resource-id
     #{:Noun}
     :task-name
     #{:Verb :Modal}
     #{:Verb :Infinitive}
     #{:Noun}
     :duration
     #{:Preposition }
     #{:Noun :Plural :Date}
     :predecessors
     #{:Preposition :Condition}
     #{:Task :Condition}
     #{:Noun :Value :Condition})])
