(ns my-playground.core-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [my-playground.banking :as banking]
            [my-playground.db :as db]))

(use-fixtures :once
  (fn [f]
    (System/setProperty "my-playground.db.path" "target/test/my-playground-test.sqlite")
    (f)))

(use-fixtures :each
  (fn [f]
    (db/reset-for-tests!)
    (f)))

(deftest deposit-and-withdraw-flow
  (let [created (banking/create-account {:user-id 1 :account-type "checking" :initial-balance "200.00"})
        account-id (get-in created [:account :id])
        deposit-result (banking/deposit {:account-id account-id :amount "80.00" :description "Cash in"})
        withdraw-result (banking/withdraw {:account-id account-id :amount "35.00" :description "ATM"})
        final-account (banking/get-account account-id)]
    (is (:success created))
    (is (:success deposit-result))
    (is (:success withdraw-result))
    (is (= 245.00M (:balance final-account)))))

(deftest request-transfer-approval-flow
  (let [from-created (banking/create-account {:user-id 1 :account-type "checking" :initial-balance "500.00"})
        to-created (banking/create-account {:user-id 2 :account-type "checking" :initial-balance "100.00"})
        from-id (get-in from-created [:account :id])
        to-id (get-in to-created [:account :id])
        request-result (banking/transfer {:from-account-id from-id
                                          :to-account-id to-id
                                          :amount "75.00"
                                          :mode "request"
                                          :actor-user-id 1
                                          :description "Invoice payment"})
        transfer-id (get-in request-result [:transaction :id])
        approve-result (banking/approve-transfer {:transfer-id transfer-id :actor-user-id 2})
        from-final (banking/get-account from-id)
        to-final (banking/get-account to-id)]
    (is (:success request-result))
    (is (= "pending" (get-in request-result [:transaction :status])))
    (is (:success approve-result))
    (is (= "completed" (get-in approve-result [:transaction :status])))
    (is (= 425.00M (:balance from-final)))
    (is (= 175.00M (:balance to-final)))))

(deftest sender-cannot-approve-own-request-when-not-admin
  (let [from-created (banking/create-account {:user-id 2 :account-type "checking" :initial-balance "350.00"})
        to-created (banking/create-account {:user-id 1 :account-type "savings" :initial-balance "25.00"})
        from-id (get-in from-created [:account :id])
        to-id (get-in to-created [:account :id])
        request-result (banking/transfer {:from-account-id from-id
                                          :to-account-id to-id
                                          :amount "40.00"
                                          :mode "request"
                                          :actor-user-id 2
                                          :description "Request test"})
        transfer-id (get-in request-result [:transaction :id])
        approval-result (banking/approve-transfer {:transfer-id transfer-id :actor-user-id 2})]
    (is (:success request-result))
    (is (false? (:success approval-result)))
    (is (= 403 (:status approval-result)))))

(deftest instant-transfer-insufficient-funds
  (let [from-created (banking/create-account {:user-id 2 :account-type "checking" :initial-balance "20.00"})
        to-created (banking/create-account {:user-id 3 :account-type "checking" :initial-balance "0.00"})
        from-id (get-in from-created [:account :id])
        to-id (get-in to-created [:account :id])
        transfer-result (banking/transfer {:from-account-id from-id
                                           :to-account-id to-id
                                           :amount "500.00"
                                           :mode "instant"
                                           :actor-user-id 2
                                           :description "Too big"})]
    (is (false? (:success transfer-result)))
    (is (= 422 (:status transfer-result)))))
