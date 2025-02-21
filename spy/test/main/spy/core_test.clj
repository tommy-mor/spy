(ns spy.core-test
  (:require [clojure.test :refer :all]
            [spy.core :refer [spy]]))

(deftest test-simple-let
  (spy
   (let [x 10
         y 20
         z (+ x y)]
     (is (= 10 x))
     (is (= 20 y))
     (is (= 30 z))
     (is (= 6000 (* x y z)))))


  (deftest test-destructuring
    (spy
     (let [{:keys [a b]} {:a 5 :b 15}
           sum (+ a b)]
       (is (= 5 a))
       (is (= 15 b))
       (is (= 20 sum)))))

  (deftest test-fn-args
    (spy
     ((fn [x y]
        (is (= 7 x))
        (is (= 3 y))
        (+ x y))
      7 3)))

  (deftest test-nested-lets
    (spy
     (let [a 1
           b 2]
       (let [c (+ a b)]
         (is (= 1 a))
         (is (= 2 b))
         (is (= 3 c))))))

  (deftest test-redefining-core
    (spy
     (let [count 5]
       (is (= 5 count)))))

  (deftest test-no-leakage-outside
    (spy
     (let [x 100])
    ;; After the block, `x` should still be available in REPL
    ;; But let's check it's NOT accessible if we eval this test file alone:
     (is (bound? (var x)))))

  (deftest test-return-value
    (is (= 6
           (spy
            (let [x 2
                  y 3]
              (* x y)))))))

(defn -main []
  (run-tests 'spy.core-test))