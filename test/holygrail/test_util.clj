(ns holygrail.test-util
  (:import [org.apache.camel.component.mock MockEndpoint]))

(defn mock-endpoint [context url]
  (.getEndpoint context url))

(defn received-counter [end]
  (.getReceivedCounter end))
