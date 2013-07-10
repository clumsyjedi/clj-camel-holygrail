(ns holygrail.test.core
  (:use [clojure.test]
        [midje.sweet]
        [holygrail.core]))

(facts "Camel sanity checks"
  (fact "Produce and route"
    (let [context (make-context)
          produce (make-producer context)
          counter (atom 0)]
      (defroute context
        (default-error-handler-builder)
        (from "direct:test")
        (process (processor
                  (swap! counter inc))))
      (produce "direct:test" "foo")
      @counter => 1)))
