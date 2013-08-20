(ns holygrail.test.eip.message-construction
  (:use [midje.sweet]
        [clojure.test]
        [holygrail.core]
        [holygrail.test-util]))

(facts "Event Message EIP"
  (fact "event message from endpoint"
    (defroute (make-context)
      (from "direct:source")
      (in-only)))

  (fact "event message to endpoint"
    (defroute (make-context)
      (from "direct:source")
      (to in-only "mock:dest")))

  (fact "event message on url"
    (defroute (make-context)
     (from "direct:source?exchangePattern=InOnly")
     (to "mock:dest"))))
