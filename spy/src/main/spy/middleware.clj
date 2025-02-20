(ns spy.middleware
  (:require [nrepl.middleware :as mw]
            [nrepl.misc :refer [response-for]]
            [nrepl.transport :as t]
            [spy :refer [active-spy]]))

(defn inject-spy [form]
  (if-let [{:keys [locals]} @active-spy]
    `(let [~@(mapcat identity locals)] ~form)
    form))

(defn spy-middleware [h]
  (fn [{:keys [op code transport] :as msg}]
    (case op
      "eval" (let [form (inject-spy (read-string code))]
               (t/send transport (response-for msg
                                               :status :done
                                               :value (pr-str (eval form)))))
      (h msg))))

(mw/set-descriptor! #'spy-middleware {:expects #{"eval"}})
