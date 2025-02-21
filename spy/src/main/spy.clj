(ns spy
  (:require [nrepl.middleware :as mw]
            [nrepl.misc :refer [response-for]]
            [nrepl.transport :as t]
            [clojure.walk :as walk]))

(defn should-spy? [sym]
  (not (re-matches #"map__\d+" (str sym))))

(defn inject-spy-defs [form]
  (walk/postwalk
   (fn [form]
     (cond 
       ;; Handle let* bindings
       (and (seq? form) (= (first form) 'let*))
       (let [bvec (second form)
             body (drop 2 form)
             syms (filter should-spy? (take-nth 2 bvec))
             def-forms (map (fn [sym] `(def ~sym ~sym)) syms)]
         `(let* ~bvec 
            ~@def-forms
            ~@body))

       ;; Handle fn* forms
       (and (seq? form) (= (first form) 'fn*))
       (let [arglists (rest form)  ; List of ([args] body) pairs
             spy-arglist (fn [[args & body]]  ; Transform each arglist
                           (let [def-forms (map (fn [sym] `(def ~sym ~sym)) args)]
                             `(~args ~@def-forms ~@body)))]
         `(fn* ~@(map spy-arglist arglists)))

       :else form))
   form))
(comment
  (inject-spy-defs (walk/macroexpand-all 
                    '(let [f (fn [x]
                               (let [{:keys [a b]} {:a 3 :b 4}]
                                 (+ a b)))]
                       (let [z 3]
                         (f z)))))
  (walk/macroexpand-all 
   '(defn foo [{:keys [a b]} m]
      (+ a b)))
  (def final '(let [f (fn [x]
                        (def x x)
                        (let [{:keys [a b]} {:a 3 :b 4}]
                          (def a a)
                          (def b b)
                          (+ a b)))]
                (def f f)
                (let [z 3]
                  (def z z)
                  (f z)))))

(macroexpand '(let [{:keys [a b c]} 3] x))

(def active-spy (atom nil))

(defmacro spy+ [bindings & body]
  (let [bind-pairs (partition 2 bindings)
        syms (map first bind-pairs)
        vals (map second bind-pairs)
        locals-sym (gensym "locals")]
    `(let [~locals-sym (into {} (map vector '~syms [~@vals]))]
       (reset! spy/active-spy {:ns (ns-name *ns*) :locals ~locals-sym})
       (let [~@(mapcat (fn [[s _]] [s `(get ~locals-sym '~s)]) bind-pairs)]
         ~@body))))

(defn inject-spy [form]
  (println "inject-spy called with form:" form)
  (try
    (if-let [{:keys [locals]} @active-spy]
      (do
        (println "active-spy locals found:" locals)
        `(let [~@(mapcat identity locals)] ~form))
      (do
        (println "no active-spy locals")
        form))
    (catch Exception e
      (println "Error in inject-spy:" (.getMessage e))
      (println "Current namespace:" (str *ns*))
      form)))

(defn spy-middleware [h]
  (fn [{:keys [op code transport] :as msg}]
    (println "\nspy-middleware handling message with op:" op)
    (println "Message details:" msg)
    (case op
      "eval" (try
              (let [form (inject-spy (read-string code))]
                (println "evaluating modified form:" form)
                (binding [*ns* (or (some-> msg :ns symbol find-ns) *ns*)]
                  (println "in namespace:" (str *ns*))
                  (t/send transport (response-for msg
                                                :status :done
                                                :value (pr-str (eval form))))))
              (catch Exception e
                (println "Error in eval:" (.getMessage e))
                (println "Stack trace:" (with-out-str (. e printStackTrace)))
                (t/send transport (response-for msg
                                              :status :error
                                              :error (.getMessage e)))))
      (do
        (println "passing message to next handler")
        (h msg)))))

(mw/set-descriptor! #'spy-middleware {:expects #{"eval"}})
