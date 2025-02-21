(ns spy.core
  (:require [clojure.walk :as walk]))

(defn should-spy? [sym]
  (not (re-matches #"map__\d+" (str sym))))

(defn inject-spy-defs [form]
  (walk/postwalk
    (fn [form]
      (cond 
        (and (seq? form) (= (first form) 'let*))
        (let [bvec (second form)
              body (drop 2 form)
              syms (filter should-spy? (take-nth 2 bvec))
              def-forms (map (fn [sym] `(def ~sym ~sym)) syms)]
          `(let* ~bvec 
             ~@def-forms
             ~@body))

        (and (seq? form) (= (first form) 'fn*))
        (let [arglists (rest form)
              spy-arglist (fn [[args & body]]
                           (let [def-forms (map (fn [sym] `(def ~sym ~sym)) args)]
                             `(~args ~@def-forms ~@body)))]
          `(fn* ~@(map spy-arglist arglists)))

        :else form))
    form))

(defmacro spy [& body]
  (let [expanded (walk/macroexpand-all `(do ~@body))]
    (inject-spy-defs expanded)))

;; Test examples:
(comment
  ;; Example 1: Simple let bindings 
  (spy
    (let [x 10
          y 20
          z (+ x y)]
      (println "x =" x)
      (println "y =" y)
      (println "z =" z)
      (* x y z)))

  ;; Example 2: Destructuring
  (spy 
   (let [{:keys [a b]} {:a 1 :b 2}
         sum (+ a b)]
     (println "sum =" sum)
     sum)))



