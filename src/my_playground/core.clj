(ns my-playground.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [my-playground.db :as db]
            [my-playground.routes :refer [app-routes]])
  (:gen-class))

(defn- session-secret []
  (let [s (or (System/getenv "SESSION_SECRET") "mysecretkey12345")]
    (.getBytes s "UTF-8")))

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})
      wrap-json-response
      wrap-params
      (wrap-session {:store (cookie-store {:key (session-secret)})})))

(defn -main [& _args]
  (db/init!)
  (println "Starting Banking Application server on http://localhost:3000")
  (run-jetty app {:port 3000 :join? true}))
