(ns my-playground.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.memory :refer [memory-store]]
            [my-playground.db :as db]
            [my-playground.routes :refer [app-routes]])
  (:gen-class))

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})
      wrap-json-response
      wrap-params
      (wrap-session {:store (memory-store)})))

(defn -main [& _args]
  (db/init!)
  (println "Starting Banking Application server on http://localhost:3000")
  (run-jetty app {:port 3000 :join? true}))
