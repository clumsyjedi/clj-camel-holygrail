(ns holygrail.component
  (:import [uk.co.and.dailymail.hornetq HornetQConnectionFactory]
           [org.apache.activemq.camel.component ActiveMQComponent]
           [org.apache.camel.component.jms JmsConfiguration]
           [org.apache.camel.component.jms JmsComponent]))

(defn activemq
  "Create an ActiveMQComponent and add it to the context"
  [conn-str]
  (ActiveMQComponent/activeMQComponent conn-str))


(defn hornetq
  "Create a hornetq JmsComponent and add it to the context"
  [conn-str]
  (let [[host port] (clojure.string/split conn-str #":")
        port (Long. port)
        conn-factory (HornetQConnectionFactory/makeConnectionFactory host port)
        jms-config (JmsConfiguration. conn-factory)]
    (JmsComponent. jms-config)))
