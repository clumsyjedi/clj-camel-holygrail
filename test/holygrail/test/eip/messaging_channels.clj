(ns holygrail.test.eip.messaging-channels
  (:use [holygrail.core]
        [holygrail.test-util]
        [clojure.test]
        [midje.sweet]))

(facts "Point to Point Channel EIP"
  (fact "point-to-point"
    (let [context (make-context)]
      (defroute context
        (from "direct:source")
        (to "seda:dest"))

      ; only one consumer should get a message
      (let [consume-a (make-consumer context)
            consume-b (make-consumer context)
            produce (make-producer context)]

        (produce "direct:source" "body")

        (let [results [(consume-a "seda:dest" :timeout 5)
                       (consume-b "seda:dest" :timeout 5)]]
          (count (remove nil? results))
          => 1)))))
