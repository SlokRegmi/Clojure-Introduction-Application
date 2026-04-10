(ns my-playground.db
  (:require [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.java.io :as io]
            [my-playground.auth :as auth]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(defn now-str []
  (tf/unparse (tf/formatters :date-time-no-ms) (t/now)))

(defn db-path []
  (or (System/getProperty "my-playground.db.path")
      (System/getenv "MY_PLAYGROUND_DB_PATH")
      "data/my-playground.sqlite"))

(defn- ensure-db-dir! []
  (let [db-file (io/file (db-path))
        parent (.getParentFile db-file)]
    (when parent
      (.mkdirs parent))))

(defn- db-spec []
  {:dbtype "sqlite"
   :dbname (db-path)})

(defonce ^:private datasource
  (delay
    (ensure-db-dir!)
    (jdbc/get-datasource (db-spec))))

(defn ds []
  @datasource)

(defn last-insert-id [tx]
  (:id (jdbc/execute-one! tx
                          ["SELECT last_insert_rowid() AS id"]
                          {:builder-fn rs/as-unqualified-kebab-maps})))

(def ^:private schema-sql
  [(str "CREATE TABLE IF NOT EXISTS users ("
        "id INTEGER PRIMARY KEY AUTOINCREMENT, "
        "name TEXT NOT NULL, "
        "email TEXT NOT NULL UNIQUE, "
        "role TEXT NOT NULL, "
        "password_hash TEXT NOT NULL DEFAULT '', "
        "created_at TEXT NOT NULL"
        ");")

   (str "CREATE TABLE IF NOT EXISTS accounts ("
        "id INTEGER PRIMARY KEY AUTOINCREMENT, "
        "user_id INTEGER NOT NULL, "
        "account_number TEXT NOT NULL UNIQUE, "
        "account_type TEXT NOT NULL, "
        "balance NUMERIC NOT NULL DEFAULT 0, "
        "status TEXT NOT NULL DEFAULT 'active', "
        "created_at TEXT NOT NULL, "
        "updated_at TEXT NOT NULL"
        ");")

   (str "CREATE TABLE IF NOT EXISTS transfers ("
        "id INTEGER PRIMARY KEY AUTOINCREMENT, "
        "from_account_id INTEGER NOT NULL, "
        "to_account_id INTEGER NOT NULL, "
        "amount NUMERIC NOT NULL, "
        "mode TEXT NOT NULL, "
        "status TEXT NOT NULL, "
        "description TEXT, "
        "requested_by_user_id INTEGER NOT NULL, "
        "approved_by_user_id INTEGER, "
        "rejected_by_user_id INTEGER, "
        "created_at TEXT NOT NULL, "
        "updated_at TEXT NOT NULL, "
        "completed_at TEXT, "
        "rejected_at TEXT"
        ");")

   (str "CREATE TABLE IF NOT EXISTS ledger_entries ("
        "id INTEGER PRIMARY KEY AUTOINCREMENT, "
        "account_id INTEGER NOT NULL, "
        "entry_type TEXT NOT NULL, "
        "amount NUMERIC NOT NULL, "
        "balance_after NUMERIC NOT NULL, "
        "reference_type TEXT, "
        "reference_id INTEGER, "
        "description TEXT, "
        "created_at TEXT NOT NULL"
        ");")

   "CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);"
   "CREATE INDEX IF NOT EXISTS idx_transfers_from_account ON transfers(from_account_id);"
   "CREATE INDEX IF NOT EXISTS idx_transfers_to_account ON transfers(to_account_id);"
   "CREATE INDEX IF NOT EXISTS idx_transfers_status ON transfers(status);"
   "CREATE INDEX IF NOT EXISTS idx_ledger_account_id ON ledger_entries(account_id);"])

(def ^:private seed-users
  [{:name "Alice Johnson" :email "alice@example.com" :role "admin"  :password "admin123"  :created-at "2026-01-10"}
   {:name "Bob Smith"     :email "bob@example.com"   :role "editor" :password "editor123" :created-at "2026-02-14"}
   {:name "Carol White"   :email "carol@example.com" :role "viewer" :password "viewer123" :created-at "2026-03-01"}])

(def ^:private seed-accounts
  [{:user-id 1 :account-number "AC100001" :account-type "checking" :balance 5000.00}
   {:user-id 2 :account-number "AC100002" :account-type "checking" :balance 3200.00}
   {:user-id 3 :account-number "AC100003" :account-type "savings"  :balance 7100.00}])

(defn- table-count [tx table-name]
  (:c (jdbc/execute-one! tx
                         [(str "SELECT COUNT(*) AS c FROM " table-name)]
                         {:builder-fn rs/as-unqualified-kebab-maps})))

(defn- apply-schema! [tx]
  (doseq [sql schema-sql]
    (jdbc/execute! tx [sql])))

(defn- seed-data! [tx]
  (when (zero? (table-count tx "users"))
    (doseq [{:keys [name email role password created-at]} seed-users]
      (jdbc/execute! tx
                     ["INSERT INTO users (name, email, role, password_hash, created_at) VALUES (?, ?, ?, ?, ?)"
                      name email role (auth/hash-password password) created-at])))
  (when (zero? (table-count tx "accounts"))
    (let [now (now-str)]
      (doseq [{:keys [user-id account-number account-type balance]} seed-accounts]
        (jdbc/execute! tx
                       ["INSERT INTO accounts (user_id, account_number, account_type, balance, status, created_at, updated_at) VALUES (?, ?, ?, ?, 'active', ?, ?)"
                        user-id account-number account-type balance now now])))))

(defn init! []
  (jdbc/with-transaction [tx (ds)]
    (apply-schema! tx)
    (seed-data! tx))
  true)

(defn reset-for-tests! []
  (jdbc/with-transaction [tx (ds)]
    (doseq [sql ["DROP TABLE IF EXISTS ledger_entries"
                 "DROP TABLE IF EXISTS transfers"
                 "DROP TABLE IF EXISTS accounts"
                 "DROP TABLE IF EXISTS users"]]
      (jdbc/execute! tx [sql]))
    (apply-schema! tx)
    (seed-data! tx))
  true)
