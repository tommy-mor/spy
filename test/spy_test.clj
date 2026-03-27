(ns spy-test
  (:require [clojure.test :refer :all]
            [spy :refer [spy clear!]]))

(defn spy-val [sym]
  (when-let [v (ns-resolve 'spy sym)]
    (var-get v)))

(deftest test-simple-let
  (spy (let [x 10 y 20 z (+ x y)] z))
  (is (= (spy-val 'x) 10))
  (is (= (spy-val 'y) 20))
  (is (= (spy-val 'z) 30))
  (clear!))

(deftest test-fn-args
  (spy ((fn [x y] (+ x y)) 7 3))
  (is (= (spy-val 'x) 7))
  (is (= (spy-val 'y) 3))
  (clear!))

(deftest test-anonymous-fn
  ;; #() shorthand — flat (fn* [args] body), not multi-arity form
  (spy (let [nums [1 2 3]
             doubled (mapv #(* 2 %) nums)]
         doubled))
  (is (= (spy-val 'nums) [1 2 3]))
  (is (= (spy-val 'doubled) [2 4 6]))
  (clear!))

(deftest test-destructuring
  (spy (let [{:keys [a b]} {:a 5 :b 15}
             sum (+ a b)]
         sum))
  (is (= (spy-val 'a) 5))
  (is (= (spy-val 'b) 15))
  (is (= (spy-val 'sum) 20))
  (clear!))

(defn -main [] (run-tests 'spy-test))
