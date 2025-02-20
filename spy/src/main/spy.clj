(ns spy)

(def active-spy (atom nil))

(defmacro spy+ [bindings & body]
  `(let [locals# (zipmap '~(take-nth 2 bindings) [(do ~@(take-nth 2 (rest bindings)))])]
     (reset! active-spy {:ns (ns-name *ns*) :locals locals#})
     (try ~@body (finally (reset! active-spy nil)))))
