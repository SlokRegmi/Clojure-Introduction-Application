(ns my-playground.transfers
  (:require [my-playground.db :as db]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(defn- builder [result-set opts]
  (rs/as-unqualified-kebab-maps result-set opts))

(defn- to-transfer [row]
  (when row
    (update row :amount #(if (nil? %) 0M (bigdec %)))))

(defn- fetch-transfer [conn transfer-id]
  (-> (jdbc/execute-one! conn
                         ["SELECT id, from_account_id, to_account_id, amount, mode, status, description, requested_by_user_id, approved_by_user_id, rejected_by_user_id, created_at, updated_at, completed_at, rejected_at FROM transfers WHERE id = ?"
                          transfer-id]
                         {:builder-fn builder})
      to-transfer))

(defn get-transfer
  ([transfer-id]
   (fetch-transfer (db/ds) transfer-id))
  ([conn transfer-id]
   (fetch-transfer conn transfer-id)))

(defn list-transfers []
  (->> (jdbc/execute! (db/ds)
                      ["SELECT id, from_account_id, to_account_id, amount, mode, status, description, requested_by_user_id, approved_by_user_id, rejected_by_user_id, created_at, updated_at, completed_at, rejected_at FROM transfers ORDER BY id DESC"]
                      {:builder-fn builder})
       (map to-transfer)
       vec))

(defn list-pending-transfers []
  (->> (jdbc/execute! (db/ds)
                      ["SELECT id, from_account_id, to_account_id, amount, mode, status, description, requested_by_user_id, approved_by_user_id, rejected_by_user_id, created_at, updated_at, completed_at, rejected_at FROM transfers WHERE status = 'pending' ORDER BY id DESC"]
                      {:builder-fn builder})
       (map to-transfer)
       vec))

(defn list-account-transfers [account-id]
  (->> (jdbc/execute! (db/ds)
                      ["SELECT id, from_account_id, to_account_id, amount, mode, status, description, requested_by_user_id, approved_by_user_id, rejected_by_user_id, created_at, updated_at, completed_at, rejected_at FROM transfers WHERE from_account_id = ? OR to_account_id = ? ORDER BY id DESC"
                       account-id account-id]
                      {:builder-fn builder})
       (map to-transfer)
       vec))

(defn create-transfer!
  ([tx {:keys [from-account-id to-account-id amount mode status description requested-by-user-id approved-by-user-id]}]
   (let [now (db/now-str)
         transfer-status (or status (if (= mode "request") "pending" "completed"))
         completed-at (when (= transfer-status "completed") now)]
     (jdbc/execute! tx
                    ["INSERT INTO transfers (from_account_id, to_account_id, amount, mode, status, description, requested_by_user_id, approved_by_user_id, created_at, updated_at, completed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                     from-account-id to-account-id amount mode transfer-status description requested-by-user-id approved-by-user-id now now completed-at])
     (fetch-transfer tx (db/last-insert-id tx))))
  ([attrs]
   (jdbc/with-transaction [tx (db/ds)]
     (create-transfer! tx attrs))))

(defn update-transfer-status! [tx transfer-id {:keys [status approved-by-user-id rejected-by-user-id]}]
  (let [now (db/now-str)
        completed-at (when (= status "completed") now)
        rejected-at (when (= status "rejected") now)
        result (jdbc/execute-one! tx
                                  ["UPDATE transfers SET status = ?, approved_by_user_id = ?, rejected_by_user_id = ?, updated_at = ?, completed_at = ?, rejected_at = ? WHERE id = ?"
                                   status approved-by-user-id rejected-by-user-id now completed-at rejected-at transfer-id])]
    (when (pos? (or (:next.jdbc/update-count result) 0))
      (fetch-transfer tx transfer-id))))
