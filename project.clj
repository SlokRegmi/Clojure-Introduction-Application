(defproject test-app "0.1.0-SNAPSHOT"
  :description "Test banking API application for learning and testing purposes"
  :url "https://example.com/test-app"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.2"]
                 [cheshire "5.11.0"]
                 [clj-time "0.15.2"]
                 [ring/ring-core "1.9.6"]
                 [ring/ring-jetty-adapter "1.9.6"]
                 [ring/ring-json "0.5.1"]
                 [compojure "1.7.1"]
                 [hiccup "1.0.5"]
                 [com.github.seancorfield/next.jdbc "1.3.955"]
                 [org.xerial/sqlite-jdbc "3.45.3.0"]]
  :main ^:skip-aot my-playground.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
