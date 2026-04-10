(ns my-playground.auth)

(defn hash-password
  "Returns the SHA-256 hex digest of the given string.
   Demo only — use bcrypt in production."
  [s]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")
        bytes  (.digest digest (.getBytes (str s) "UTF-8"))]
    (apply str (map #(format "%02x" %) bytes))))
