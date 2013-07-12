(ns holygrail.test.eip
  (:use [clojure.test]
        [holygrail.core]
        [midje.sweet]
        [holygrail.test-util]))

(facts "Pipeline EIP"
  (fact "pipeline of endpoints"
    (let [context (make-context)]

      (defroute context
        (from "direct:source")
        (pipeline (into-array ["mock:a" "mock:b" "mock:c"])))

      ((make-producer context) "direct:source" "msg")
      (received-counter (mock-endpoint context "mock:a")) => 1
      (received-counter (mock-endpoint context "mock:b")) => 1
      (received-counter (mock-endpoint context "mock:c")) => 1)))
