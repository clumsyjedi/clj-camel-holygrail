(ns holygrail.test-util
  (:use [holygrail.core])
  (:import [org.apache.camel.component.mock MockEndpoint]))

(defn received-counter [end]
  (.getReceivedCounter end))

(defn received-exchanges [end]
  (.getReceivedExchanges end))

(defn countdown-latch [n]
  (java.util.concurrent.CountDownLatch. n))

(defn wait [latch timeout]
  (.await latch timeout java.util.concurrent.TimeUnit/MILLISECONDS))

(defn countdown [latch]
  (.countDown latch))
