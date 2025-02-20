(ns spy
  (:require [nrepl.middleware :as mw]
            [nrepl.misc :refer [response-for]]
            [nrepl.transport :as t]))

(def active-spy (atom nil))

(defmacro spy+ [bindings & body]
  (let [bind-pairs (partition 2 bindings)
        syms (map first bind-pairs)
        vals (map second bind-pairs)
        locals-sym (gensym "locals")]
    `(let [~locals-sym (into {} (map vector '~syms [~@vals]))]
       (reset! (var spy/active-spy) {:ns (ns-name *ns*) :locals ~locals-sym})
       (let [~@(mapcat (fn [[s _]] [s `(get ~locals-sym '~s)]) bind-pairs)]
         (try 
           ~@body 
           (finally (reset! (var spy/active-spy) nil)))))))

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
