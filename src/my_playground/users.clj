(ns my-playground.users
  (:require [my-playground.auth :as auth]
            [my-playground.db :as db]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(defn- builder [result-set opts]
  (rs/as-unqualified-kebab-maps result-set opts))

(defn- fetch-user [conn user-id]
  (jdbc/execute-one! conn
                     ["SELECT id, name, email, role, created_at FROM users WHERE id = ?" user-id]
                     {:builder-fn builder}))

(defn list-users []
  (jdbc/execute! (db/ds)
                 ["SELECT id, name, email, role, created_at FROM users ORDER BY id"]
                 {:builder-fn builder}))

(defn get-user
  ([user-id]
   (fetch-user (db/ds) user-id))
  ([conn user-id]
   (fetch-user conn user-id)))

(defn create-user [{:keys [name email role]}]
  (jdbc/with-transaction [tx (db/ds)]
    (jdbc/execute! tx
                   ["INSERT INTO users (name, email, role, created_at) VALUES (?, ?, ?, ?)"
                    name email (or role "viewer") (db/now-str)])
    (fetch-user tx (db/last-insert-id tx))))

(defn update-user [user-id fields]
  (if-let [existing (fetch-user (db/ds) user-id)]
    (let [allowed (select-keys fields [:name :email :role])
          name (or (:name allowed) (:name existing))
          email (or (:email allowed) (:email existing))
          role (or (:role allowed) (:role existing))]
      (jdbc/execute! (db/ds)
                     ["UPDATE users SET name = ?, email = ?, role = ? WHERE id = ?"
                      name email role user-id])
      (fetch-user (db/ds) user-id))
    nil))

(defn delete-user [user-id]
  (let [result (jdbc/execute-one! (db/ds)
                                  ["DELETE FROM users WHERE id = ?" user-id])]
    (pos? (or (:next.jdbc/update-count result) 0))))

(defn authenticate-user
  "Returns the user map if email and password match, nil otherwise."
  [email password]
  (jdbc/execute-one! (db/ds)
                     ["SELECT id, name, email, role, created_at FROM users WHERE email = ? AND password_hash = ?"
                      email (auth/hash-password password)]
                     {:builder-fn builder}))
