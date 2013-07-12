(ns holygrail.test-util
  (:use [holygrail.core])
  (:import [org.apache.camel.component.mock MockEndpoint]))

(defn mock-endpoint [context url]
  (endpoint context url))

(defn received-counter [end]
  (.getReceivedCounter end))

(defn received-exchanges [end]
  (.getReceivedExchanges end))
