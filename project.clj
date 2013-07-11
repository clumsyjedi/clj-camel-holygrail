(def camel-version "2.10.3")

(defproject clj-camel-holygrail "0.1.0" 
  :description "Apache Camel DSL in Clojure"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.apache.camel/camel-core ~camel-version]
                 [org.apache.camel/camel-jetty ~camel-version]
                 [org.apache.camel/camel-jms ~camel-version]
                 [org.apache.activemq/activemq-camel "5.8.0"]
                 [org.jboss.netty/netty "3.2.7.Final"]
                 [uk.co.and.dailymail/hornetq-connector "1.0"]
                 [org.clojure/tools.logging "0.2.6"]]
  :source-paths ["src"]
  :repositories {"snapshots" {:url "http://10.251.76.32:8081/nexus/content/repositories/snapshots"
                              :username "admin" :password "admin123"}
                 "releases" {:url "http://10.251.76.32:8081/nexus/content/repositories/releases"
                             :username "admin" :password "admin123" }
                 "thirdparty" {:url "http://10.251.76.32:8081/nexus/content/repositories/thirdparty"}}
  :lein-release {:deploy-via :shell
                 :shell ["./bin/release.sh"]}
  :profiles {:dev {:dependencies [[midje "1.5.1"]
                                  [midje-junit-formatter "0.1.0-SNAPSHOT"]]
                   :plugins [[lein-midje "3.0.0"]]}})
