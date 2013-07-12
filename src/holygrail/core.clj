(ns holygrail.core
  (:import [org.apache.camel Exchange]
           [org.apache.camel ExchangePattern]
           [org.apache.camel Processor]
           [org.apache.camel Predicate]
           [org.apache.camel Expression]
           [org.apache.camel.impl DefaultCamelContext]
           [org.apache.camel.impl DefaultExchange]
           [org.apache.camel.impl DefaultMessage]
           [org.apache.camel.builder RouteBuilder]
           [org.apache.camel.builder DeadLetterChannelBuilder]
           [org.apache.camel.builder DefaultErrorHandlerBuilder]
           [org.apache.camel.processor SendProcessor]
           [org.apache.camel.processor.aggregate AggregationStrategy]
           [org.apache.camel.processor RecipientList]
           [org.apache.camel.processor ErrorHandler]
           [org.apache.camel.model.language HeaderExpression]
           [org.apache.camel.model.language SimpleExpression]
           [org.apache.camel.impl DefaultProducerTemplate]
           [org.apache.camel.impl DefaultConsumerTemplate]
           [uk.co.and.dailymail.hornetq HornetQConnectionFactory]
           [org.apache.activemq.camel.component ActiveMQComponent]
           [org.apache.camel.component.jms JmsConfiguration]
           [org.apache.camel.component.jms JmsComponent]
           [org.apache.camel.util CamelLogger] )
  (:require [clojure.tools.logging :as log]
            [holygrail.util :as util]))

(def http-uri-header (Exchange/HTTP_URI))
(def http-path-header (Exchange/HTTP_PATH))
(def exception-caught-header (Exchange/EXCEPTION_CAUGHT))
(def in-only (ExchangePattern/InOnly))
(def in-out (ExchangePattern/InOut))


(defn make-context
  "Create and start a DefaultCamelContext. Arguments are scheme -> component mappings"
  [& components]
  (let [context (DefaultCamelContext.)]
    (doseq [[scheme component] (apply hash-map components)]
      (.addComponent context (name scheme) component))
    (.start context)
    context))

(defn make-consumer
  "Create and start a DefaultConsumerTemplate"
  [context]
  (let [consumer (DefaultConsumerTemplate. context)]
    (.start consumer)
    (fn [dest & {:keys [timeout]}]

      (if timeout
          (.receiveBody consumer dest timeout)
          (.receiveBodyNoWait consumer dest)))))

(defn make-producer
  "Create and start a DefaultProducerTemplate"
  [context]
  (let [producer (DefaultProducerTemplate. context)]
    (.start producer)
    (fn [dest body & {:keys [ex-pattern] :or {ex-pattern in-only}}]
      (if (= in-only ex-pattern)
        (.sendBody producer dest body)
        (.requestBody producer dest body (.class Object))))))

(defmacro defroute
  "Creates a route from the provided context, error handler and body"
  [context & args]
  (let [[err-handler & body] (util/route-args args)
        body (map util/java-method body)]
    `(.addRoutes ~context
                 (proxy [RouteBuilder] []
                   (configure []
                     (.errorHandler ~'this ~err-handler)
                     (.. ~'this ~@body))))))

(defn make-endpoint [context url]
  (.getEndpoint context url))

; components
(defn activemq-component
  "Create an ActiveMQComponent and add it to the context"
  [conn-str]
  (ActiveMQComponent/activeMQComponent conn-str))


(defn hornetq-component
  "Create a hornetq JmsComponent and add it to the context"
  [conn-str]
  (let [[host port] (clojure.string/split conn-str #":")
        port (Long. port)
        conn-factory (HornetQConnectionFactory/makeConnectionFactory host port)
        jms-config (JmsConfiguration. conn-factory)]
    (JmsComponent. jms-config)))

; helper functions
(defn set-in-body [ex body]
  "Set the in message body"
  (.. ex (getIn) (setBody body)))

(defn set-out-body [ex body]
  "Set the out message body"
  (.. ex (getOut) (setBody body)))

(defn set-body [ex body]
  "Set the in message body"
  (set-in-body ex body))

(defn get-body [ex]
  "get the message body as a string"
  (.. ex (getIn) (getBody)))

(defn set-header
  "Useful for setting state inside processors"
  [ex k v]
  (.. ex (getIn) (setHeader (name k) v)))

(defn remove-header
  "Useful for removing state inside processors"
  [ex k]
  (.. ex (getIn) (removeHeader k )))

(defn get-header
  "Useful for getting state inside processors"
  [ex k]
  (.. ex (getIn) (getHeader (name k))))

(defn get-headers
  "Useful for getting state inside processors"
  [ex]
  (.. ex (getIn) (getHeaders)))

(defn get-exception
  "get the last caught exception from the message"
  [ex]
  (.getException ex))

; types and builders
(defn default-message []
  (DefaultMessage.))

(defmacro predicate
  "Creates a predicate for use in a camel/when clause"
  [& body]
  `(reify Predicate
     (matches [self ex]
       ~@body)))

(defmacro dead-letter-channel-builder [queue & body]
  (let [body (map util/java-method body)]
   (if (empty? body)
     `(DeadLetterChannelBuilder. ~queue)
     `(.. (DeadLetterChannelBuilder. ~queue) ~@body))))

(defmacro default-error-handler-builder [& body]
  (let [body (map util/java-method body)]
   (if (empty? body)
     `(DefaultErrorHandlerBuilder.)
     `(.. (DefaultErrorHandlerBuilder.) ~@body))))

(defmacro expression
  "Create a new Expression with the forms provided as the evaluate method"
  [& body]
  `(reify Expression
     (evaluate [this ex clazz]
       ~@body)))

(defn simple
  "Creates a Simple Expression"
  [expr]
  (SimpleExpression. expr))

(defn header
  "Creates a HeaderExpression"
  [s]
  (HeaderExpression. (name s)))


(defn recipient-list
  "Creates a RecipientList"
  [context expr]
  (RecipientList. context expr))

(defmacro aggregation-strategy
  "Creates an instance of an aggregation strategy with forms provided as
   the aggregate method"
  [& body]
  `(reify AggregationStrategy
   (aggregate
    [this a b]
     ~@body)))

(defmacro processor
  "Creates a new impl of org.apache.camel.Processor with the
   forms provided as the implementation"
  [& body]
  `(reify Processor
     (process [self ex]
       ~@body)))

; useful processors
(defn debug-processor
  "Logs a bit of info about the message"
  []
  (processor
   (log/warn "------------------------------------------")
   (log/warn "From endpoint:"   (.. ex getFromEndpoint getEndpointUri))
   (log/warn "Input headers:"   (.. ex getIn getHeaders))
   (log/warn "Input body type:" (type (.. ex getIn getBody)))
   (log/warn "Input body:"      (.. ex getIn getBody))
   (log/warn "Is Failed?:"      (.isFailed ex))
   (log/warn "Exception"        (.getException ex))
   (log/warn "Caught Exception" (get-header ex exception-caught-header))
   (log/warn "------------------------------------------")))

(defn forced-failure-processor
  "Throws an exception"
  []
  (processor
   (log/warn "Forced failure for message")
   (throw (Exception.))))

(defn slurp-processor
  "Converts the message body to a string from a stream. Useful since
   the jetty endpoint returns a single use input stream"
  []
  (processor
   (set-in-body ex (slurp (get-body ex)))))
