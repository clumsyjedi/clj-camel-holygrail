(ns holygrail.core
  (:import [org.apache.camel Exchange]
           [org.apache.camel ExchangePattern]
           [org.apache.camel Processor]
           [org.apache.camel Predicate]
           [org.apache.camel Expression]
           [org.apache.camel.impl DefaultCamelContext]
           [org.apache.camel.impl DefaultExchange]
           [org.apache.camel.builder RouteBuilder]
           [org.apache.camel.builder DeadLetterChannelBuilder]
           [org.apache.camel.builder DefaultErrorHandlerBuilder]
           [org.apache.camel.processor SendProcessor]
           [org.apache.camel.processor.aggregate AggregationStrategy]
           [org.apache.camel.processor RecipientList]
           [org.apache.camel.model.language HeaderExpression]
           [org.apache.camel.model.language SimpleExpression]
           [org.apache.camel.impl DefaultProducerTemplate])
  (:require [clojure.tools.logging :as log]
            [holygrail.util :as util]))

(def http-uri-header (Exchange/HTTP_URI))
(def http-path-header (Exchange/HTTP_PATH))


(defn make-context
  "Create and start a DefaultCamelContext. Arguments are scheme -> component mappings"
  [& components]
  (let [context (DefaultCamelContext.)]
    (doseq [[scheme component] components]
      (.addComponent context scheme component))
    (.start context)
    context))

(defn make-producer
  "Create and start a DefaultProducerTemplate"
  [context]
  (let [producer (DefaultProducerTemplate. context)]
    (.start producer)
    (fn [dest body]
      (.sendBody producer dest body))))

(defmacro defroute
  "Creates a route from the provided context, error handler and body"
  [context err-handler & body]
  (let [body (map util/java-method body)]
    `(.addRoutes ~context
                 (proxy [RouteBuilder] []
                   (configure []
                     (.errorHandler ~'this ~err-handler)
                     (.. ~'this ~@body))))))
; helper functions
(defn set-in-body [ex body]
  "Set the message body"
  (.. ex (getIn) (setBody body)))

(defn set-out-body [ex body]
  "Set the out message body"
  (.. ex (getOut) (setBody body)))

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

; types and builders

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



(defprotocol Splitter
  (split [this body]))

(defmacro splitter [& body]
  `(reify Splitter
    (split [this body]
      ~@body)))

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
   (log/info "------------------------------------------")
   (log/info "From endpoint:"   (.. ex getFromEndpoint getEndpointUri))
   (log/info "Input headers:"   (.. ex getIn getHeaders))
   (log/info "Input body type:" (type (.. ex getIn getBody)))
   (log/info "Input body:"      (.. ex getIn getBody))
   (log/info "------------------------------------------")))

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
