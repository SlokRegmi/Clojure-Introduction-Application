(ns my-playground.banking
  (:require [clojure.string :as str]
            [my-playground.accounts :as accounts]
            [my-playground.db :as db]
            [my-playground.transfers :as transfers]
            [my-playground.users :as users]
            [next.jdbc :as jdbc]))

(defn- fail
  ([message]
   (fail message 400))
  ([message status]
   {:success false :error message :status status}))

(defn- to-amount [value]
  (try
    (let [parsed (cond
                   (instance? java.math.BigDecimal value) value
                   (number? value) (bigdec value)
                   (string? value) (bigdec (str/trim value))
                   :else nil)]
      (when parsed
        (.setScale parsed 2 java.math.RoundingMode/HALF_UP)))
    (catch Exception _
      nil)))

(defn- positive-amount? [amount]
  (pos? (.compareTo amount 0M)))

(defn- non-negative-amount? [amount]
  (not (neg? (.compareTo amount 0M))))

(defn- enough-balance? [balance amount]
  (not (neg? (.compareTo (bigdec balance) amount))))

(defn- active-account? [account]
  (= "active" (:status account)))

(defn list-accounts []
  (accounts/list-accounts))

(defn list-user-accounts [user-id]
  (accounts/list-user-accounts user-id))

(defn get-account [account-id]
  (accounts/get-account account-id))

(defn get-balance [account-id]
  (some-> (accounts/get-account account-id) :balance))

(defn create-account
  [{:keys [user-id account-type initial-balance]}]
  (let [amount (if (nil? initial-balance) 0M (to-amount initial-balance))]
    (cond
      (nil? user-id)
      (fail "user-id is required")

      (nil? (users/get-user user-id))
      (fail "User not found" 404)

      (nil? amount)
      (fail "Initial balance must be a valid decimal amount")

      (not (non-negative-amount? amount))
      (fail "Initial balance must be zero or greater")

      :else
      (jdbc/with-transaction [tx (db/ds)]
        (let [account (accounts/create-account! tx {:user-id user-id
                                                    :account-type account-type
                                                    :initial-balance amount})]
          (when (positive-amount? amount)
            (accounts/add-ledger-entry! tx {:account-id (:id account)
                                            :entry-type "deposit"
                                            :amount amount
                                            :balance-after (:balance account)
                                            :reference-type "account"
                                            :reference-id (:id account)
                                            :description "Initial balance"}))
          {:success true :account account})))))

(defn close-account [account-id]
  (jdbc/with-transaction [tx (db/ds)]
    (if-let [account (accounts/get-account tx account-id)]
      (cond
        (not= "active" (:status account))
        (fail "Account is already closed" 409)

        (not (zero? (.compareTo (:balance account) 0M)))
        (fail "Cannot close account with non-zero balance" 409)

        :else
        (do
          (accounts/update-status! tx account-id "closed")
          {:success true :message "Account closed successfully"}))
      (fail "Account not found" 404))))

(defn- execute-transfer! [tx transfer from-account to-account amount description]
  (let [from-balance (- (:balance from-account) amount)
        to-balance (+ (:balance to-account) amount)
        from-updated (accounts/update-balance! tx (:id from-account) from-balance)
        to-updated (accounts/update-balance! tx (:id to-account) to-balance)]
    (accounts/add-ledger-entry! tx {:account-id (:id from-account)
                                    :entry-type "transfer-out"
                                    :amount amount
                                    :balance-after (:balance from-updated)
                                    :reference-type "transfer"
                                    :reference-id (:id transfer)
                                    :description (or description "Transfer sent")})
    (accounts/add-ledger-entry! tx {:account-id (:id to-account)
                                    :entry-type "transfer-in"
                                    :amount amount
                                    :balance-after (:balance to-updated)
                                    :reference-type "transfer"
                                    :reference-id (:id transfer)
                                    :description (or description "Transfer received")})
    {:success true
     :transaction transfer
     :from-balance (:balance from-updated)
     :to-balance (:balance to-updated)}))

(defn transfer
  [{:keys [from-account-id to-account-id amount description mode actor-user-id]}]
  (let [transfer-mode (if (= "request" (some-> mode str/lower-case)) "request" "instant")
        parsed-amount (to-amount amount)]
    (cond
      (nil? from-account-id)
      (fail "from-account-id is required")

      (nil? to-account-id)
      (fail "to-account-id is required")

      (= from-account-id to-account-id)
      (fail "Source and destination accounts must be different")

      (nil? parsed-amount)
      (fail "Amount must be a valid decimal number")

      (not (positive-amount? parsed-amount))
      (fail "Amount must be greater than zero")

      :else
      (jdbc/with-transaction [tx (db/ds)]
        (let [from-account (accounts/get-account tx from-account-id)
              to-account (accounts/get-account tx to-account-id)
              requested-by (or actor-user-id (some-> from-account :user-id))]
          (cond
            (nil? from-account)
            (fail "Source account not found" 404)

            (nil? to-account)
            (fail "Destination account not found" 404)

            (not (active-account? from-account))
            (fail "Source account is not active" 409)

            (not (active-account? to-account))
            (fail "Destination account is not active" 409)

            (and actor-user-id (nil? (users/get-user tx actor-user-id)))
            (fail "Actor user not found" 404)

            (= transfer-mode "request")
            (let [created (transfers/create-transfer! tx {:from-account-id from-account-id
                                                          :to-account-id to-account-id
                                                          :amount parsed-amount
                                                          :mode "request"
                                                          :status "pending"
                                                          :description description
                                                          :requested-by-user-id requested-by})]
              {:success true
               :transaction created
               :message "Transfer request created and waiting for approval"})

            (not (enough-balance? (:balance from-account) parsed-amount))
            (fail "Insufficient funds" 422)

            :else
            (let [created (transfers/create-transfer! tx {:from-account-id from-account-id
                                                          :to-account-id to-account-id
                                                          :amount parsed-amount
                                                          :mode "instant"
                                                          :status "completed"
                                                          :description description
                                                          :requested-by-user-id requested-by
                                                          :approved-by-user-id requested-by})]
              (assoc (execute-transfer! tx created from-account to-account parsed-amount description)
                     :message "Transfer completed"))))))))

(defn deposit
  [{:keys [account-id amount description]}]
  (let [parsed-amount (to-amount amount)]
    (cond
      (nil? account-id)
      (fail "account-id is required")

      (nil? parsed-amount)
      (fail "Amount must be a valid decimal number")

      (not (positive-amount? parsed-amount))
      (fail "Amount must be greater than zero")

      :else
      (jdbc/with-transaction [tx (db/ds)]
        (if-let [account (accounts/get-account tx account-id)]
          (if (active-account? account)
            (let [new-balance (+ (:balance account) parsed-amount)
                  updated (accounts/update-balance! tx account-id new-balance)
                  entry (accounts/add-ledger-entry! tx {:account-id account-id
                                                        :entry-type "deposit"
                                                        :amount parsed-amount
                                                        :balance-after (:balance updated)
                                                        :reference-type "deposit"
                                                        :reference-id nil
                                                        :description (or description "Deposit")})]
              {:success true :transaction entry :balance (:balance updated)})
            (fail "Account is not active" 409))
          (fail "Account not found" 404))))))

(defn withdraw
  [{:keys [account-id amount description]}]
  (let [parsed-amount (to-amount amount)]
    (cond
      (nil? account-id)
      (fail "account-id is required")

      (nil? parsed-amount)
      (fail "Amount must be a valid decimal number")

      (not (positive-amount? parsed-amount))
      (fail "Amount must be greater than zero")

      :else
      (jdbc/with-transaction [tx (db/ds)]
        (if-let [account (accounts/get-account tx account-id)]
          (cond
            (not (active-account? account))
            (fail "Account is not active" 409)

            (not (enough-balance? (:balance account) parsed-amount))
            (fail "Insufficient funds" 422)

            :else
            (let [new-balance (- (:balance account) parsed-amount)
                  updated (accounts/update-balance! tx account-id new-balance)
                  entry (accounts/add-ledger-entry! tx {:account-id account-id
                                                        :entry-type "withdrawal"
                                                        :amount parsed-amount
                                                        :balance-after (:balance updated)
                                                        :reference-type "withdrawal"
                                                        :reference-id nil
                                                        :description (or description "Withdrawal")})]
              {:success true :transaction entry :balance (:balance updated)}))
          (fail "Account not found" 404))))))

(defn list-transactions []
  (accounts/list-history))

(defn get-account-transactions [account-id]
  (accounts/list-account-history account-id))

(defn get-transaction [transaction-id]
  (transfers/get-transfer transaction-id))

(defn list-pending-transfers
  ([]
   (transfers/list-pending-transfers))
  ([actor-user-id]
   (if (nil? actor-user-id)
     (transfers/list-pending-transfers)
     (let [actor (users/get-user actor-user-id)
           pending (transfers/list-pending-transfers)]
       (cond
         (nil? actor)
         []

         (= "admin" (:role actor))
         pending

         :else
         (->> pending
              (filter (fn [transfer-row]
                        (let [to-account (accounts/get-account (:to-account-id transfer-row))]
                          (= actor-user-id (:user-id to-account)))))
              vec))))))

(defn- allowed-to-approve? [tx actor-user-id transfer-row]
  (let [actor (users/get-user tx actor-user-id)
        to-account (accounts/get-account tx (:to-account-id transfer-row))]
    (and actor
         to-account
         (or (= actor-user-id (:user-id to-account))
             (= "admin" (:role actor))))))

(defn approve-transfer
  [{:keys [transfer-id actor-user-id]}]
  (cond
    (nil? transfer-id)
    (fail "transfer-id is required")

    (nil? actor-user-id)
    (fail "actor-user-id is required")

    :else
    (jdbc/with-transaction [tx (db/ds)]
      (if-let [transfer-row (transfers/get-transfer tx transfer-id)]
        (cond
          (not= "pending" (:status transfer-row))
          (fail "Only pending transfers can be approved" 409)

          (not (allowed-to-approve? tx actor-user-id transfer-row))
          (fail "You are not allowed to approve this transfer" 403)

          :else
          (let [from-account (accounts/get-account tx (:from-account-id transfer-row))
                to-account (accounts/get-account tx (:to-account-id transfer-row))
                amount (:amount transfer-row)]
            (cond
              (nil? from-account)
              (fail "Source account not found" 404)

              (nil? to-account)
              (fail "Destination account not found" 404)

              (not (active-account? from-account))
              (fail "Source account is not active" 409)

              (not (active-account? to-account))
              (fail "Destination account is not active" 409)

              (not (enough-balance? (:balance from-account) amount))
              (fail "Insufficient funds" 422)

              :else
              (let [updated-transfer (transfers/update-transfer-status! tx transfer-id {:status "completed"
                                                                                        :approved-by-user-id actor-user-id})]
                (assoc (execute-transfer! tx updated-transfer from-account to-account amount (:description transfer-row))
                       :message "Transfer approved")))))
        (fail "Transfer not found" 404)))))

(defn reject-transfer
  [{:keys [transfer-id actor-user-id]}]
  (cond
    (nil? transfer-id)
    (fail "transfer-id is required")

    (nil? actor-user-id)
    (fail "actor-user-id is required")

    :else
    (jdbc/with-transaction [tx (db/ds)]
      (if-let [transfer-row (transfers/get-transfer tx transfer-id)]
        (cond
          (not= "pending" (:status transfer-row))
          (fail "Only pending transfers can be rejected" 409)

          (not (allowed-to-approve? tx actor-user-id transfer-row))
          (fail "You are not allowed to reject this transfer" 403)

          :else
          (let [updated-transfer (transfers/update-transfer-status! tx transfer-id {:status "rejected"
                                                                                    :rejected-by-user-id actor-user-id})]
            {:success true
             :transaction updated-transfer
             :message "Transfer rejected"}))
        (fail "Transfer not found" 404)))))
