(ns tommy-mor.trail
  (:require [clojure.string :as str]))

;; Core engine — ~15 lines

(defn validate
  "Walk a path of steps over elements. Returns {:ok trail :matched results} or {:fail trail :message ...}"
  [path els]
  (loop [remaining els
         steps path
         trail []]
    (if (empty? steps)
      {:ok trail :matched remaining}
      (let [{:keys [name match]} (first steps)
            result (match remaining)
            trail (conj trail name)]
        (if (seq result)
          (recur result (rest steps) trail)
          {:fail trail
           :message (str (str/join " > " trail)
                         ": 0 matches"
                         (when (seq remaining)
                           (str " (had " (count remaining) " candidates: "
                                (mapv :name remaining) ")")))})))))

;; Convenience: validate multiple paths, collect failures
(defn validate-all [paths els]
  (keep (fn [path]
          (let [result (validate path els)]
            (when (:fail result) result)))
        paths))

;; -- Filter navigators --

(defn role [r]
  {:name (str "role(" r ")")
   :match (fn [els] (filter #(= r (:role %)) els))})

(defn name= [n]
  {:name (str "name(\"" n "\")")
   :match (fn [els] (filter #(= n (:name %)) els))})

(defn name-match [pat]
  {:name (str "name(~" pat ")")
   :match (fn [els] (filter #(when-let [n (:name %)] (re-find pat n)) els))})

(def checked
  {:name "checked"
   :match (fn [els] (filter :checked els))})

(def unchecked
  {:name "unchecked"
   :match (fn [els] (remove :checked els))})

(def visible
  {:name "visible"
   :match (fn [els] (filter :visible els))})

(def interactive
  {:name "interactive"
   :match (fn [els] (filter #(seq (:listeners %)) els))})

(defn listens [event]
  {:name (str "listens(" event ")")
   :match (fn [els] (filter #(contains? (set (:listeners %)) event) els))})

(defn has-attr [k]
  {:name (str "has(" (clojure.core/name k) ")")
   :match (fn [els] (filter #(contains? % k) els))})

(defn attr= [k v]
  {:name (str (clojure.core/name k) "=" (pr-str v))
   :match (fn [els] (filter #(= v (get % k)) els))})

;; -- Quantifier navigators --

(defn count= [n]
  {:name (str "count=" n)
   :match (fn [els] (if (= n (count els)) els []))})

(defn count>= [n]
  {:name (str "count>=" n)
   :match (fn [els] (if (>= (count els) n) els []))})

(defn count<= [n]
  {:name (str "count<=" n)
   :match (fn [els] (if (<= (count els) n) els []))})

;; -- Structural navigators --

(defn any-child
  "Keeps elements where at least one child matches the sub-path"
  [& sub-path]
  {:name (str "any-child(" (str/join " > " (map :name sub-path)) ")")
   :match (fn [els]
            (filter (fn [el]
                      (let [result (validate sub-path (or (:children el) []))]
                        (:ok result)))
                    els))})

(defn all-children
  "Keeps elements where ALL children match the sub-path"
  [& sub-path]
  {:name (str "all-children(" (str/join " > " (map :name sub-path)) ")")
   :match (fn [els]
            (filter (fn [el]
                      (let [children (or (:children el) [])]
                        (and (seq children)
                             (every? (fn [child]
                                       (:ok (validate sub-path [child])))
                                     children))))
                    els))})

;; -- Logical combinators --

(defn any-of
  "Keeps elements matching ANY of the given steps"
  [& steps]
  {:name (str "any-of(" (str/join ", " (map :name steps)) ")")
   :match (fn [els]
            (filter (fn [el]
                      (some #(seq ((:match %) [el])) steps))
                    els))})

(defn none-of
  "Keeps elements matching NONE of the given steps"
  [& steps]
  {:name (str "none-of(" (str/join ", " (map :name steps)) ")")
   :match (fn [els]
            (filter (fn [el]
                      (not-any? #(seq ((:match %) [el])) steps))
                    els))})

;; -- Annotation navigators --

(defn label
  "Passthrough navigator that adds a name to the trail without filtering."
  [n]
  {:name n
   :match identity})

(defn absent
  "Passes when the sub-step matches NOTHING. Keeps all elements unchanged."
  [step]
  {:name (str "absent(" (:name step) ")")
   :match (fn [els]
            (if (empty? ((:match step) els))
              els
              []))})
