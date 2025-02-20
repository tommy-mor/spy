(ns spy-test.core
  (:require [clojure.data.json :as json]
            [spy :refer [spy+]]))

;; Example 1: Cat Facts API
(defn get-cat-fact []
  (-> (slurp "https://catfact.ninja/fact")
      (json/read-str :key-fn keyword)))

;; Example 2: Simple Spy Demo
(defn spy-demo []
  (let [x 10
        y 20]
    (spy+ [x x
           y y
           z (+ x y)]
      (let [result (* x y z)]
        (println "Inside spy+ block:")
        (println "x =" x)
        (println "y =" y)
        (println "z =" z)
        result))))

;; Example 3: Fibonacci with Spy
(defn fibonacci [n]
  (let [x 1
        y 1]
    (spy+ [x x
           y y
           n n]
          (loop [a x
                 b y
                 count (- n 2)]
            (if (pos? count)
              (recur b (+ a b) (dec count))
              b)))))

(comment
  (+ 3 3)
  (get-cat-fact))
