(ns my-playground.accounts
  (:require [my-playground.db :as db]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(defn- builder [result-set opts]
  (rs/as-unqualified-kebab-maps result-set opts))

(defn- to-account [row]
  (when row
    (update row :balance #(if (nil? %) 0M (bigdec %)))))

(defn- to-ledger [row]
  (-> row
      (update :amount #(if (nil? %) 0M (bigdec %)))
      (update :balance-after #(if (nil? %) 0M (bigdec %)))))

(defn- account-number-exists? [conn candidate]
  (pos? (:c (jdbc/execute-one! conn
                               ["SELECT COUNT(*) AS c FROM accounts WHERE account_number = ?" candidate]
                               {:builder-fn builder}))))

(defn- generate-account-number [conn]
  (loop []
    (let [candidate (str "AC" (+ 100000000 (rand-int 900000000)))]
      (if (account-number-exists? conn candidate)
        (recur)
        candidate))))

(defn- fetch-account [conn account-id]
  (-> (jdbc/execute-one! conn
                         ["SELECT id, user_id, account_number, account_type, balance, status, created_at, updated_at FROM accounts WHERE id = ?"
                          account-id]
                         {:builder-fn builder})
      to-account))

(defn get-account
  ([account-id]
   (fetch-account (db/ds) account-id))
  ([conn account-id]
   (fetch-account conn account-id)))

(defn list-accounts []
  (->> (jdbc/execute! (db/ds)
                      ["SELECT id, user_id, account_number, account_type, balance, status, created_at, updated_at FROM accounts ORDER BY id"]
                      {:builder-fn builder})
       (map to-account)
       vec))

(defn list-user-accounts [user-id]
  (->> (jdbc/execute! (db/ds)
                      ["SELECT id, user_id, account_number, account_type, balance, status, created_at, updated_at FROM accounts WHERE user_id = ? ORDER BY id"
                       user-id]
                      {:builder-fn builder})
       (map to-account)
       vec))

(defn create-account!
  ([tx {:keys [user-id account-type initial-balance]}]
   (let [now (db/now-str)
         balance (if (nil? initial-balance) 0M (bigdec initial-balance))
         account-number (generate-account-number tx)]
     (jdbc/execute! tx
                    ["INSERT INTO accounts (user_id, account_number, account_type, balance, status, created_at, updated_at) VALUES (?, ?, ?, ?, 'active', ?, ?)"
                     user-id account-number (or account-type "checking") balance now now])
     (fetch-account tx (db/last-insert-id tx))))
  ([attrs]
   (jdbc/with-transaction [tx (db/ds)]
     (create-account! tx attrs))))

(defn update-balance! [tx account-id new-balance]
  (let [result (jdbc/execute-one! tx
                                  ["UPDATE accounts SET balance = ?, updated_at = ? WHERE id = ?"
                                   new-balance (db/now-str) account-id])]
    (when (pos? (or (:next.jdbc/update-count result) 0))
      (fetch-account tx account-id))))

(defn update-status! [tx account-id new-status]
  (let [result (jdbc/execute-one! tx
                                  ["UPDATE accounts SET status = ?, updated_at = ? WHERE id = ?"
                                   new-status (db/now-str) account-id])]
    (when (pos? (or (:next.jdbc/update-count result) 0))
      (fetch-account tx account-id))))

(defn add-ledger-entry! [tx {:keys [account-id entry-type amount balance-after reference-type reference-id description]}]
  (jdbc/execute! tx
                 ["INSERT INTO ledger_entries (account_id, entry_type, amount, balance_after, reference_type, reference_id, description, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                  account-id entry-type amount balance-after reference-type reference-id description (db/now-str)])
  (-> (jdbc/execute-one! tx
                         ["SELECT id, account_id, entry_type, amount, balance_after, reference_type, reference_id, description, created_at FROM ledger_entries WHERE id = ?"
                          (db/last-insert-id tx)]
                         {:builder-fn builder})
      to-ledger))

(defn list-history []
  (->> (jdbc/execute! (db/ds)
                      ["SELECT id, account_id, entry_type, amount, balance_after, reference_type, reference_id, description, created_at FROM ledger_entries ORDER BY id DESC"]
                      {:builder-fn builder})
       (map to-ledger)
       vec))

(defn list-account-history [account-id]
  (->> (jdbc/execute! (db/ds)
                      ["SELECT id, account_id, entry_type, amount, balance_after, reference_type, reference_id, description, created_at FROM ledger_entries WHERE account_id = ? ORDER BY id DESC"
                       account-id]
                      {:builder-fn builder})
       (map to-ledger)
       vec))
