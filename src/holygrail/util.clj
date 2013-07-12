(ns holygrail.util
  (:use [clojure.string :only [lower-case join capitalize]]))

(defn- to-camel-case [s]
  (let [matches (re-seq #"[a-zA-Z0-9]+" (lower-case s))]
    (join "" (cons (first matches) (map capitalize (rest matches))))))

(defn- sym-to-camel-case [sym]
  (symbol (to-camel-case (name sym))))

(defn java-method [form]
  (cons (sym-to-camel-case (first form)) (rest form)))

(defn route-args [args]
  (if (= :err-handler (first args))
    (cons (second args) (drop 2 args))
    (cons nil args)))
