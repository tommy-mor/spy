(ns tommy-mor.spy-test
  (:require [clojure.test :refer :all]
            [tommy-mor.spy :refer [spy]]))

(deftest test-simple-let
  (spy
   (let [x 10
         y 20
         z (+ x y)]
     (is (= 6000 (* x y z)))))
  (is (= x 10))
  (is (= y 20))
  (is (= z 30)))



(deftest test-destructuring
  (spy
   (is (= 21 (let [{:keys [a b]} {:a 5 :b 15}
                   sum (+ a b)]
               (inc sum))))
   (is (= sum 20))
   (is (= a 5))
   (is (= b 15))))

(deftest test-fn-args
  (spy ((fn [x y] (+ x y)) 7 3))
  (is (= x 7))
  (is (= y 3)))

(deftest test-nested-lets
  (spy
   (let [a 1
         b 2]
     (is (nil? (resolve 'c)))
     (let [c (+ a b)]
       (inc c))))
  (is (= c 3)))

(deftest test-redefining-core
  ;; watch out 
  (is (= (count [1 2 3]) 3))
  
  (spy
   (let [count 5]
     (is (= 5 count))))

  (is (thrown? Exception (count 3))))

(deftest test-redefining-core
  (spy
   (defn test-fn [a {:keys [b c]}]
     (+ a b c)))
  (is (= 306 (test-fn 101 {:b 102 :c 103})))
  (is (= 306 (+ a b c))))

(defn -main []
  (run-tests 'tommy-mor.spy-test))
