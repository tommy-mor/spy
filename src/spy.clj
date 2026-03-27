(ns spy
  (:require [clojure.walk :as walk]))

;; The spy namespace IS the store. Values are intern'd here as vars.
;; Access them as spy/x, spy/result, etc. No atoms, no helpers.

(defn should-spy? [sym]
  (and (symbol? sym)
       (not= '& sym)
       (not (re-matches #"map__\d+|&" (str sym)))))

(defn inject-spy-interns
  "Walk form, inject (intern 'spy 'sym sym) for let bindings and fn args."
  [form]
  (walk/postwalk
   (fn [f]
     (cond
        ;; let / let*
       (and (seq? f) (#{'let 'let*} (first f)))
       (let [bvec (second f)
             body (drop 2 f)
             syms (->> (partition 2 bvec)
                       (mapcat (fn [[binding _]]
                                 (let [acc (atom [])]
                                   (walk/postwalk
                                    (fn [x] (when (should-spy? x) (swap! acc conj x)) x)
                                    binding)
                                   (distinct @acc)))))]
         `(let* ~bvec
            ~@(map (fn [s] `(intern '~'spy '~s ~s)) syms)
            ~@body))

        ;; fn / fn* / defn / defn-
       (and (seq? f) (#{'fn 'fn* 'defn 'defn-} (first f)))
       (let [named? (symbol? (second f))
             bodies (let [rst (if named? (drop 2 f) (rest f))]
                      (if (vector? (first rst)) [rst] rst))]
         `(~(first f)
           ~@(when named? [(second f)])
           ~@(map (fn [[args & body]]
                    (let [syms (let [acc (atom [])]
                                 (walk/postwalk
                                  (fn [x] (when (should-spy? x) (swap! acc conj x)) x)
                                  args)
                                 (distinct @acc))]
                      `(~args
                        (let [~@(mapcat (fn [s] [s s]) syms)]
                          ~@(map (fn [s] `(intern '~'spy '~s ~s)) syms)
                          (do ~@body)))))
                  bodies)))

       :else f))
   form))

(defmacro spy
  "Wrap code so all let bindings and fn args are intern'd into the spy namespace.
   Access values as spy/x, spy/result, etc."
  [& body]
  (inject-spy-interns (walk/macroexpand-all `(do ~@body))))

(defn spy!
  "Instrument all fns in the current namespace. On each call, args and return value
   are intern'd into spy. Access as spy/argname, spy/fnname<."
  []
  (let [target-ns *ns*]
    (doseq [[sym v] (ns-interns target-ns)
            :when (and (fn? @v) (not (:macro (meta v))))]
      (let [original @v
            arg-names (first (:arglists (meta v)))]
        (alter-var-root v
                        (fn [f]
                          (fn [& args]
                            (doseq [[n val] (map vector (or arg-names (range)) args)]
                              (intern 'spy (if (symbol? n) n (symbol (str "arg" n))) val))
                            (let [result (apply f args)]
                              (intern 'spy (symbol (str sym "<")) result)
                              result))))
        (alter-meta! v assoc ::original original)))))

(defn unspy!
  "De-instrument all fns in current namespace. Captured values stay in spy ns for inspection."
  []
  (doseq [[sym v] (ns-interns *ns*)
          :when (::original (meta v))]
    (alter-var-root v (constantly (::original (meta v))))
    (alter-meta! v dissoc ::original)))

(def ^:private spy-own-syms
  #{'should-spy? 'inject-spy-interns 'spy 'spy! 'unspy! 'clear! 'spy-own-syms})

(defn clear!
  "Wipe all captured values from the spy namespace."
  []
  (doseq [[sym _] (ns-interns 'spy)
          :when (not (spy-own-syms sym))]
    (ns-unmap 'spy sym)))
