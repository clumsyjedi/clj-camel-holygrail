(ns holygrail.test.eip.message-routing
  (:use [holygrail.core]
        [clojure.test]
        [midje.sweet]
        [holygrail.test-util]))

(facts "Content based router EIP"
  (fact "Content based router using choice"
    (let [context (make-context)]
      (defroute context
        (from "direct:source")
        (choice)
        (when (predicate (= (get-body ex) "a")))
        (to "mock:a")
        (when (predicate (= (get-body ex) "b")))
        (to "mock:b")
        (otherwise)
        (to "mock:c")
        (end))

      (doseq [x '("a" "b" "c")]
        ((make-producer context) "direct:source" x)
        (let [mock-dest (make-endpoint context (str "mock:" x))]
          (received-counter mock-dest) => 1
          (get-body (first (received-exchanges mock-dest))) => x)))))

(facts "Message Filter EIP"
  (fact "Message filter on header"
    (let [context (make-context)
          produce (make-producer context)
          mock-dest (make-endpoint context "mock:dest")]
      (defroute context
        (from "direct:source")
        (filter (predicate (= "bar" (get-header ex :foo))))
        (to mock-dest))

      (produce "direct:source" "a" :headers {:foo "bar"})
      (produce "direct:source" "b" :headers {:foo "baz"})

      (received-counter mock-dest) => 1
      (get-body (first (received-exchanges mock-dest))) => "a")))


(facts "Dynamic Router EIP"
  (fact "Dynamic router with clojure expression"
    (let [context (make-context)
          produce (make-producer context)
          mock-dest-a (make-endpoint context "mock:dest")
          mock-dest-b (make-endpoint context "mock:dest")
          counter (atom 0)]
      (defroute context
        (from "direct:source")
        (dynamic-router (expression (swap! counter inc)
                                    (if (> @counter 1))
                                    (get-header ex :dest))))

      (produce "direct:source" "a" :headers {:dest "mock:a" :routed false})
      (produce "direct:source" "b" :headers {:dest "mock:b" })

      (received-counter mock-dest-a) => 1)))
