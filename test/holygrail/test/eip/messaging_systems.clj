(ns holygrail.test.eip.messaging-systems
  (:use [clojure.test]
        [holygrail.core]
        [midje.sweet]
        [holygrail.test-util]))

(facts "Message Channel EIP"
  (fact "message channel"
    (instance? org.apache.camel.Endpoint
               (make-endpoint (make-context) "mock:endpoint"))))

(facts "Message EIP"
  (fact "default message"
    (instance? org.apache.camel.impl.DefaultMessage
               (default-message))))

(facts "Pipeline EIP"
  (fact "pipeline of endpoints"
    (let [context (make-context)]

      (defroute context
        (from "direct:source")
        (pipeline (into-array ["mock:a" "mock:b" "mock:c"])))

      ((make-producer context) "direct:source" "msg")
      (received-counter (make-endpoint context "mock:a")) => 1
      (received-counter (make-endpoint context "mock:b")) => 1
      (received-counter (make-endpoint context "mock:c")) => 1))

  (fact "pipeline of processors"
    (let [context (make-context)]
      (defroute context
        (from "direct:source")
        (process (processor (set-body ex "a")))
        (process (processor (set-body ex (str (get-body ex) "b"))))
        (process (processor (set-body ex (str (get-body ex) "c"))))
        (to "mock:dest"))

      ((make-producer context) "direct:source" "msg")
      (let [mock-dest (make-endpoint context "mock:dest")]
        (received-counter mock-dest) => 1
        (get-body (first (received-exchanges mock-dest))) => "abc"))))


(facts "Message Router EIP"
  (fact "message router with predicates"
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

(facts "Message Translator EIP"
  (fact "message router with processors"
    (let [context (make-context)]
      (defroute context
        (from "direct:source")
        (process (processor (set-body ex "new body")))
        (to "mock:dest"))

      ((make-producer context) "direct:source" "old body")
      (let [mock-dest (make-endpoint context "mock:dest")]
        (received-counter mock-dest) => 1
        (get-body (first (received-exchanges mock-dest))) => "new body")))

  (fact "message router with expressions"
    (let [context (make-context)]
      (defroute context
        (from "direct:source")
        (transform (expression "new body"))
        (to "mock:dest"))

      ((make-producer context) "direct:source" "old body")
      (let [mock-dest (make-endpoint context "mock:dest")]
        (received-counter mock-dest) => 1
        (get-body (first (received-exchanges mock-dest))) => "new body"))))


(facts "Message Endpoint EIP"
  (fact "message endpoint"
    (instance? org.apache.camel.Endpoint
               (make-endpoint (make-context) "mock:endpoint"))))
