(ns my-playground.routes
  (:require [clojure.string :as str]
            [compojure.core :refer [defroutes GET POST PUT DELETE]]
            [compojure.route :as route]
            [ring.util.response :refer [response not-found redirect status]]
            [my-playground.banking :as banking]
            [my-playground.frontend :as frontend]
            [my-playground.users :as users]))

(defn- ok [body]
  (response body))

(defn- error-response [message code]
  (-> (response {:error message})
      (status code)))

(defn- bad-request [message]
  (error-response message 400))

(defn- parse-int [value]
  (cond
    (integer? value) value
    (number? value) (int value)
    (string? value) (try
                      (Integer/parseInt value)
                      (catch Exception _ nil))
    :else nil))

(defn- service-response [result success-code]
  (if (:success result)
    (-> (response (dissoc result :success))
        (status success-code))
    (error-response (:error result) (or (:status result) 400))))

(defn- session-user [request]
  (get-in request [:session :user]))

(defn- require-auth [request handler]
  (if (session-user request)
    (handler request)
    (redirect "/login")))

(defn- require-write
  "Requires editor or admin role. Call inside require-auth."
  [request handler]
  (if (contains? #{"editor" "admin"} (:role (session-user request)))
    (handler request)
    (error-response "Insufficient permissions — editor or admin role required" 403)))

(defn- require-admin
  "Requires admin role. Call inside require-auth."
  [request handler]
  (if (= "admin" (:role (session-user request)))
    (handler request)
    (error-response "Insufficient permissions — admin role required" 403)))

(defroutes app-routes
  ;; ── Auth ──────────────────────────────────────────────────────────────────
  (GET "/login" request
    (if (session-user request)
      (redirect "/")
      {:status  200
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :body    (frontend/login-page nil)}))

  (POST "/login" request
    (let [params   (:params request)
          email    (str/trim (or (get params "email") ""))
          password (or (get params "password") "")]
      (if-let [user (users/authenticate-user email password)]
        (-> (redirect "/")
            (assoc :session {:user {:id   (:id user)
                                    :name (:name user)
                                    :role (:role user)}}))
        {:status  200
         :headers {"Content-Type" "text/html; charset=utf-8"}
         :body    (frontend/login-page "Invalid email or password.")})))

  (POST "/logout" _request
    (-> (redirect "/login")
        (assoc :session nil)))

  ;; ── Main UI ───────────────────────────────────────────────────────────────
  (GET "/" request
    (require-auth request
      (fn [req]
        {:status  200
         :headers {"Content-Type" "text/html; charset=utf-8"}
         :body    (frontend/page (session-user req))})))

  ;; ── Users API ─────────────────────────────────────────────────────────────
  (GET "/api/users" request
    (require-auth request
      (fn [_] (ok (vec (users/list-users))))))

  (GET "/api/users/:id" [id :as request]
    (require-auth request
      (fn [_]
        (if-let [user-id (parse-int id)]
          (if-let [user (users/get-user user-id)]
            (ok user)
            (not-found {:error "User not found"}))
          (bad-request "User id must be a number")))))

  (POST "/api/users" {body :body :as request}
    (require-auth request
      (fn [req]
        (require-admin req
          (fn [_]
            (let [{:keys [name email role]} body
                  clean-name  (some-> name str str/trim)
                  clean-email (some-> email str str/trim)]
              (if (or (str/blank? clean-name) (str/blank? clean-email))
                (bad-request "name and email are required")
                (try
                  (-> (users/create-user {:name clean-name :email clean-email :role role})
                      (ok)
                      (status 201))
                  (catch Exception _
                    (error-response "Could not create user (email may already exist)" 409))))))))))

  (PUT "/api/users/:id" [id :as {body :body :as request}]
    (require-auth request
      (fn [req]
        (require-admin req
          (fn [_]
            (if-let [user-id (parse-int id)]
              (if-let [updated (users/update-user user-id body)]
                (ok updated)
                (not-found {:error "User not found"}))
              (bad-request "User id must be a number")))))))

  (DELETE "/api/users/:id" [id :as request]
    (require-auth request
      (fn [req]
        (require-admin req
          (fn [_]
            (if-let [user-id (parse-int id)]
              (if (users/delete-user user-id)
                (ok {:message "Deleted"})
                (not-found {:error "User not found"}))
              (bad-request "User id must be a number")))))))

  ;; ── Accounts API ──────────────────────────────────────────────────────────
  (GET "/api/accounts" request
    (require-auth request
      (fn [_] (ok (vec (banking/list-accounts))))))

  (GET "/api/users/:user-id/accounts" [user-id :as request]
    (require-auth request
      (fn [_]
        (if-let [parsed-user-id (parse-int user-id)]
          (ok (vec (banking/list-user-accounts parsed-user-id)))
          (bad-request "user-id must be a number")))))

  (GET "/api/accounts/:id" [id :as request]
    (require-auth request
      (fn [_]
        (if-let [account-id (parse-int id)]
          (if-let [account (banking/get-account account-id)]
            (ok account)
            (not-found {:error "Account not found"}))
          (bad-request "Account id must be a number")))))

  (GET "/api/accounts/:id/balance" [id :as request]
    (require-auth request
      (fn [_]
        (if-let [account-id (parse-int id)]
          (if-let [balance (banking/get-balance account-id)]
            (ok {:account-id account-id :balance balance})
            (not-found {:error "Account not found"}))
          (bad-request "Account id must be a number")))))

  (POST "/api/accounts" {body :body :as request}
    (require-auth request
      (fn [req]
        (require-write req
          (fn [_]
            (let [{:keys [user-id account-type initial-balance]} body
                  parsed-user-id (parse-int user-id)]
              (if (nil? parsed-user-id)
                (bad-request "user-id is required and must be a number")
                (service-response (banking/create-account {:user-id         parsed-user-id
                                                           :account-type    account-type
                                                           :initial-balance initial-balance})
                                  201))))))))

  (PUT "/api/accounts/:id/close" [id :as request]
    (require-auth request
      (fn [req]
        (require-write req
          (fn [_]
            (if-let [account-id (parse-int id)]
              (service-response (banking/close-account account-id) 200)
              (bad-request "Account id must be a number")))))))

  ;; ── Transactions API ──────────────────────────────────────────────────────
  (GET "/api/transactions" request
    (require-auth request
      (fn [_] (ok (vec (banking/list-transactions))))))

  (GET "/api/accounts/:id/transactions" [id :as request]
    (require-auth request
      (fn [_]
        (if-let [account-id (parse-int id)]
          (ok (vec (banking/get-account-transactions account-id)))
          (bad-request "Account id must be a number")))))

  (GET "/api/accounts/:id/history" [id :as request]
    (require-auth request
      (fn [_]
        (if-let [account-id (parse-int id)]
          (ok (vec (banking/get-account-transactions account-id)))
          (bad-request "Account id must be a number")))))

  (GET "/api/transactions/:id" [id :as request]
    (require-auth request
      (fn [_]
        (if-let [transaction-id (parse-int id)]
          (if-let [transaction (banking/get-transaction transaction-id)]
            (ok transaction)
            (not-found {:error "Transaction not found"}))
          (bad-request "Transaction id must be a number")))))

  (POST "/api/transactions/transfer" {body :body :as request}
    (require-auth request
      (fn [req]
        (require-write req
          (fn [req2]
            (let [{:keys [from-account-id to-account-id amount description mode]} body
                  from-id   (parse-int from-account-id)
                  to-id     (parse-int to-account-id)
                  actor-id  (:id (session-user req2))]
              (if (or (nil? from-id) (nil? to-id))
                (bad-request "from-account-id and to-account-id are required and must be numbers")
                (service-response (banking/transfer {:from-account-id from-id
                                                     :to-account-id   to-id
                                                     :amount          amount
                                                     :description     description
                                                     :mode            mode
                                                     :actor-user-id   actor-id})
                                  201))))))))

  (POST "/api/transfers" {body :body :as request}
    (require-auth request
      (fn [req]
        (require-write req
          (fn [req2]
            (let [{:keys [from-account-id to-account-id amount description mode]} body
                  from-id   (parse-int from-account-id)
                  to-id     (parse-int to-account-id)
                  actor-id  (:id (session-user req2))]
              (if (or (nil? from-id) (nil? to-id))
                (bad-request "from-account-id and to-account-id are required and must be numbers")
                (service-response (banking/transfer {:from-account-id from-id
                                                     :to-account-id   to-id
                                                     :amount          amount
                                                     :description     description
                                                     :mode            mode
                                                     :actor-user-id   actor-id})
                                  201))))))))

  (POST "/api/transactions/deposit" {body :body :as request}
    (require-auth request
      (fn [req]
        (require-write req
          (fn [_]
            (let [{:keys [account-id amount description]} body
                  parsed-account-id (parse-int account-id)]
              (if (nil? parsed-account-id)
                (bad-request "account-id is required and must be a number")
                (service-response (banking/deposit {:account-id  parsed-account-id
                                                    :amount      amount
                                                    :description description})
                                  201))))))))

  (POST "/api/transactions/withdraw" {body :body :as request}
    (require-auth request
      (fn [req]
        (require-write req
          (fn [_]
            (let [{:keys [account-id amount description]} body
                  parsed-account-id (parse-int account-id)]
              (if (nil? parsed-account-id)
                (bad-request "account-id is required and must be a number")
                (service-response (banking/withdraw {:account-id  parsed-account-id
                                                     :amount      amount
                                                     :description description})
                                  201))))))))

  (GET "/api/transactions/pending" [actor-user-id :as request]
    (require-auth request
      (fn [req]
        (let [actor-id (or (when actor-user-id (parse-int actor-user-id))
                           (:id (session-user req)))]
          (ok (vec (banking/list-pending-transfers actor-id)))))))

  (GET "/api/transfers/pending" [actor-user-id :as request]
    (require-auth request
      (fn [req]
        (let [actor-id (or (when actor-user-id (parse-int actor-user-id))
                           (:id (session-user req)))]
          (ok (vec (banking/list-pending-transfers actor-id)))))))

  (POST "/api/transactions/:id/approve" [id :as request]
    (require-auth request
      (fn [req]
        (require-write req
          (fn [req2]
            (let [transfer-id (parse-int id)
                  actor-id    (:id (session-user req2))]
              (if (nil? transfer-id)
                (bad-request "transfer id must be a number")
                (service-response (banking/approve-transfer {:transfer-id   transfer-id
                                                             :actor-user-id actor-id})
                                  200))))))))

  (POST "/api/transfers/:id/approve" [id :as request]
    (require-auth request
      (fn [req]
        (require-write req
          (fn [req2]
            (let [transfer-id (parse-int id)
                  actor-id    (:id (session-user req2))]
              (if (nil? transfer-id)
                (bad-request "transfer id must be a number")
                (service-response (banking/approve-transfer {:transfer-id   transfer-id
                                                             :actor-user-id actor-id})
                                  200))))))))

  (POST "/api/transactions/:id/reject" [id :as request]
    (require-auth request
      (fn [req]
        (require-write req
          (fn [req2]
            (let [transfer-id (parse-int id)
                  actor-id    (:id (session-user req2))]
              (if (nil? transfer-id)
                (bad-request "transfer id must be a number")
                (service-response (banking/reject-transfer {:transfer-id   transfer-id
                                                            :actor-user-id actor-id})
                                  200))))))))

  (POST "/api/transfers/:id/reject" [id :as request]
    (require-auth request
      (fn [req]
        (require-write req
          (fn [req2]
            (let [transfer-id (parse-int id)
                  actor-id    (:id (session-user req2))]
              (if (nil? transfer-id)
                (bad-request "transfer id must be a number")
                (service-response (banking/reject-transfer {:transfer-id   transfer-id
                                                            :actor-user-id actor-id})
                                  200))))))))

  (route/not-found {:error "Not found"}))
