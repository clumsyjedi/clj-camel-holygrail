(ns holygrail.test-util
  (:use [holygrail.core])
  (:import [org.apache.camel.component.mock MockEndpoint]))

(defn received-counter [end]
  (.getReceivedCounter end))

(defn received-exchanges [end]
  (.getReceivedExchanges end))
