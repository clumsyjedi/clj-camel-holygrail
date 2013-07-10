(ns holygrail.test.core
  (:use [clojure.test]
        [midje.sweet]
        [holygrail.core])
  (require [holygrail.component :as component]))

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
      @counter => 1))

  (fact "AMQ connect"
    (let [context (make-context :amq (component/activemq "tcp://127.0.0.1:61616"))
          produce (make-producer context)
          counter (atom 0)]
      (defroute context
        (default-error-handler-builder)
        (from "amq:test")
        (process (processor
                  (swap! counter inc))))
      (produce "amq:test" "foo")
      (Thread/sleep 500)
      @counter => 1)))
