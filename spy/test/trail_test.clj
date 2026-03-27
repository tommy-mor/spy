(ns trail-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [tommy-mor.trail :as t]))

(def snap
  [{:id "e1" :role "textbox" :name "add todo" :visible true}
   {:id "e2" :role "listitem" :name "buy milk" :checked false :visible true}
   {:id "e3" :role "listitem" :name "walk dog" :checked true :visible true}
   {:id "e4" :role "checkbox" :name nil :checked false :visible true :listeners ["click"]}
   {:id "e5" :role "checkbox" :name nil :checked true :visible true :listeners ["click"]}
   {:id "e6" :role "button" :name "Delete" :visible true :listeners ["click"]}
   {:id "e7" :role "status" :name "1 item left" :visible true}
   {:id "e8" :role "navigation" :name "filters"
    :children [{:id "e8a" :role "link" :name "All" :url "/all"}
               {:id "e8b" :role "link" :name "Active" :url "/active"}
               {:id "e8c" :role "link" :name "Completed"}]}])

;; 1. Basic filtering

(deftest test-role
  (let [result (t/validate [(t/role "listitem")] snap)]
    (is (:ok result))
    (is (= 2 (count (:matched result))))
    (is (every? #(= "listitem" (:role %)) (:matched result)))))

(deftest test-name=
  (let [result (t/validate [(t/name= "Delete")] snap)]
    (is (:ok result))
    (is (= 1 (count (:matched result))))
    (is (= "e6" (:id (first (:matched result)))))))

(deftest test-name-match
  (let [result (t/validate [(t/name-match #"(?i)dog")] snap)]
    (is (:ok result))
    (is (= 1 (count (:matched result))))
    (is (= "walk dog" (:name (first (:matched result)))))))

(deftest test-checked-unchecked
  (testing "checked filters to checked items"
    (let [result (t/validate [(t/role "listitem") t/checked] snap)]
      (is (:ok result))
      (is (= 1 (count (:matched result))))
      (is (= "walk dog" (:name (first (:matched result)))))))
  (testing "unchecked filters to unchecked items"
    (let [result (t/validate [(t/role "listitem") t/unchecked] snap)]
      (is (:ok result))
      (is (= 1 (count (:matched result))))
      (is (= "buy milk" (:name (first (:matched result))))))))

;; 2. Path composition — multi-step narrowing

(deftest test-path-composition
  (let [result (t/validate [(t/role "checkbox") t/checked (t/listens "click")] snap)]
    (is (:ok result))
    (is (= 1 (count (:matched result))))
    (is (= "e5" (:id (first (:matched result)))))))

(deftest test-visible-interactive
  (let [result (t/validate [t/visible t/interactive] snap)]
    (is (:ok result))
    (is (= 3 (count (:matched result))))
    (is (= #{"e4" "e5" "e6"} (set (map :id (:matched result)))))))

;; 3. Failure messages — trail shows up with candidate names

(deftest test-failure-message
  (let [result (t/validate [(t/role "listitem") t/checked (t/name= "nonexistent")] snap)]
    (is (:fail result))
    (is (= ["role(listitem)" "checked" "name(\"nonexistent\")"] (:fail result)))
    (is (clojure.string/includes? (:message result) "role(listitem) > checked > name(\"nonexistent\")"))
    (is (clojure.string/includes? (:message result) "0 matches"))
    (is (clojure.string/includes? (:message result) "walk dog"))))

(deftest test-failure-no-role
  (let [result (t/validate [(t/role "dialog")] snap)]
    (is (:fail result))
    (is (clojure.string/includes? (:message result) "role(dialog)"))
    (is (clojure.string/includes? (:message result) "0 matches"))))

;; 4. Quantifiers

(deftest test-count=
  (testing "count= passes when count matches"
    (let [result (t/validate [(t/role "listitem") (t/count= 2)] snap)]
      (is (:ok result))
      (is (= 2 (count (:matched result))))))
  (testing "count= fails when count doesn't match"
    (let [result (t/validate [(t/role "listitem") (t/count= 5)] snap)]
      (is (:fail result))
      (is (clojure.string/includes? (:message result) "count=5")))))

(deftest test-count>=
  (let [result (t/validate [(t/role "checkbox") (t/count>= 2)] snap)]
    (is (:ok result))
    (is (= 2 (count (:matched result)))))
  (let [result (t/validate [(t/role "checkbox") (t/count>= 10)] snap)]
    (is (:fail result))))

;; 5. Structural — any-child, all-children

(deftest test-any-child
  (testing "any-child matches parent with qualifying children"
    (let [result (t/validate [(t/role "navigation") (t/any-child (t/has-attr :url))] snap)]
      (is (:ok result))
      (is (= 1 (count (:matched result))))
      (is (= "e8" (:id (first (:matched result)))))))
  (testing "any-child fails when no child matches"
    (let [result (t/validate [(t/role "navigation") (t/any-child (t/role "button"))] snap)]
      (is (:fail result)))))

(deftest test-all-children
  (testing "all-children fails when not all children match"
    (let [result (t/validate [(t/role "navigation") (t/all-children (t/has-attr :url))] snap)]
      (is (:fail result) "e8c has no :url so all-children should fail")))
  (testing "all-children passes when all children match"
    (let [result (t/validate [(t/role "navigation") (t/all-children (t/role "link"))] snap)]
      (is (:ok result)))))

;; 6. Logical — any-of, none-of

(deftest test-any-of
  (let [result (t/validate [(t/any-of (t/role "button") (t/role "textbox"))] snap)]
    (is (:ok result))
    (is (= 2 (count (:matched result))))
    (is (= #{"e1" "e6"} (set (map :id (:matched result)))))))

(deftest test-none-of
  (let [result (t/validate [(t/none-of (t/role "listitem") (t/role "checkbox"))] snap)]
    (is (:ok result))
    (is (= 4 (count (:matched result))))
    (is (not-any? #(#{"listitem" "checkbox"} (:role %)) (:matched result)))))

;; 7. validate-all — multiple paths, collect only failures

(deftest test-validate-all
  (let [paths [[(t/role "textbox") (t/count= 1)]
               [(t/role "dialog")]
               [(t/role "listitem") (t/count= 2)]]
        failures (t/validate-all paths snap)]
    (is (= 1 (count failures)))
    (is (= ["role(dialog)"] (:fail (first failures))))))

;; 7b. label navigator

(deftest test-label
  (testing "label passes through without filtering"
    (let [result (t/validate [(t/label "setup") (t/role "listitem")] snap)]
      (is (:ok result))
      (is (= 2 (count (:matched result))))))
  (testing "label appears in trail on failure"
    (let [result (t/validate [(t/label "after adding") (t/role "dialog")] snap)]
      (is (:fail result))
      (is (= ["after adding" "role(dialog)"] (:fail result)))
      (is (clojure.string/includes? (:message result) "after adding > role(dialog)"))))
  (testing "label at end of path"
    (let [result (t/validate [(t/role "listitem") (t/label "should have 5")] snap)]
      (is (:ok result))
      (is (= ["role(listitem)" "should have 5"] (:ok result))))))

;; 8. Edge cases

(deftest test-empty-input
  (let [result (t/validate [(t/role "button")] [])]
    (is (:fail result))
    (is (clojure.string/includes? (:message result) "0 matches"))))

(deftest test-empty-path
  (let [result (t/validate [] snap)]
    (is (:ok result))
    (is (= (count snap) (count (:matched result))))))

(deftest test-no-matching-role
  (let [result (t/validate [(t/role "nonexistent")] snap)]
    (is (:fail result))
    (is (= ["role(nonexistent)"] (:fail result)))))

(defn -main [& _]
  (let [{:keys [fail error]} (run-tests 'trail-test)]
    (System/exit (if (and (zero? fail) (zero? error)) 0 1))))
