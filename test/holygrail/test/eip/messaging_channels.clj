(ns holygrail.test.eip.messaging-channels
  (:use [holygrail.core]
        [holygrail.test-util]
        [clojure.test]
        [midje.sweet]))

(facts "Point to Point Channel EIP"
  (fact "point to point channel"
    (let [context (make-context)]

      (defroute context
        (from "seda:source")
        (to "mock:dest"))

      ((make-producer context) "seda:source" "body")

      ; this is hacky
      (Thread/sleep 25)

      (received-counter (make-endpoint context "mock:dest"))
      => 1)))

(facts "Publish Subscribe Channel EIP"
  (fact "publish/subscribe"
    (let [context (make-context)]

      (defroute context
        (from "seda:source?multipleConsumers=true")
        (to "mock:dest"))

      (defroute context
        (from "seda:source?multipleConsumers=true")
        (to "mock:dest"))

      ((make-producer context) "seda:source" "body")

      ; this is hacky
      (Thread/sleep 25)

      (received-counter (make-endpoint context "mock:dest"))
      => 2)))

(facts "Dead Letter Channel EIP"
  (fact "DLQ error handler"
    (let [context (make-context)]

      (defroute context
        :err-handler (dead-letter-channel "mock:dlq"
                      (maximum-redeliveries 2))
        (from "direct:source")
        (process (processor (throw (Exception. "foo")))))

      ((make-producer context) "direct:source" "body")
      (received-counter (make-endpoint context "mock:dlq"))
      => 1)))
