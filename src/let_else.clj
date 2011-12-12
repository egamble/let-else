(ns let-else
  (:use [useful.seq :only (partition-between)]))


(defmacro let?
  "Same behavior as let, except where a binding is followed by :when <pred> or :else <else>
   or both, in either order.

   For a :when, the <pred> is evaluated after the associated binding is evaluated
   and must be truthy to continue evaluating the rest of the bindings and the body.
   If the <pred> is falsey, the <else> is the value of the let?, if present, or nil if not.

   For an :else without a :when, if the associated binding is falsey, <else> is the value of the let?."
  [bindings & body]
  (let [bindings (partition 2 bindings)
        sections (partition-between (fn [[[left] [right]]]
                                      (not (keyword? right)))
                                    bindings)]
    (reduce (fn [body section]
              (let [[[name val] & opts] section]
                (if-not opts
                  `(let [~name ~val]
                     ~body)
                  (let [{:keys [when else]}
                        (apply hash-map
                               (apply concat opts))]
                    (cond (nil? when)
                          `(if-let [~name ~val]
                             ~body
                             ~else)

                          (nil? else)
                          `(let [~name ~val]
                             (when ~when
                               ~body))

                          :else
                          (let [when-name (gensym)
                                val-name (gensym)]
                            `(let [[~when-name ~val-name] (let [~name ~val]
                                                            (if ~when
                                                              [true ~body]
                                                              [false]))]
                               (if ~when-name
                                 ~val-name
                                 ~else))))))))
            `(do ~@body)
            (reverse sections))))
