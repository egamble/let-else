(ns let-else
  (:use [useful.seq :only (partition-between)]
        [clojure.tools.macro :only (symbol-macrolet)]))


(defmacro let?
  "Same behavior as let, except where a binding is followed by :when <pred> or :else <else>
   or both, in either order.

   For a :when, the <pred> is evaluated after the associated binding is evaluated
   and must be truthy to continue evaluating the rest of the bindings and the body.
   If the <pred> is falsey, the <else> is the value of the let?, if present, or nil if not.

   For an :else without a :when, if the associated binding is falsey, <else> is the value of the let?.

   A :delay <truthy> clause following a binding of a symbol delays evaluation of the
   binding value until it is actually used, in case there is a possibility it won't be
   used at all. The :delay clause may instead be specified as metadata preceding the
   symbol, e.g. (let? [^:delay <name> <value>] <body>).

   :delay is ignored if there is an :else with no :when on the same binding."
  [bindings & body]
  (let [bindings (partition 2 bindings)
        sections (partition-between (fn [[[left] [right]]]
                                      (not (keyword? right)))
                                    bindings)

        reduce-fn
        (fn [body section]
          (let [[[name val] & opts] section

                opt-map (apply hash-map
                               (apply concat opts))

                {:keys [when else]} opt-map

                delay? (and (symbol? name)
                            (or (:delay (meta name))
                                (:delay opt-map)))

                delay-fn
                (if delay?
                  (let [delay-sym (gensym (str "delay-" name))]
                    (fn [new-body]
                      `(let [~delay-sym (delay ~val)]
                         (symbol-macrolet [~name (force ~delay-sym)]
                                          ~new-body))))
                  (fn [new-body]
                    `(let [~name ~val]
                       ~new-body)))]

            (cond (and when else)
                  (let [delay-else-sym (gensym "delay-else")]
                    `(let [~delay-else-sym (delay ~else)]
                       ~(delay-fn
                         `(if ~when
                            ~body
                            (force ~delay-else-sym)))))

                  when
                  (delay-fn
                   `(when ~when
                      ~body))

                  else
                  ;; no delay possible in this case
                  `(if-let [~name ~val]
                     ~body
                     ~else)

                  :else
                  (delay-fn body))))]

    (reduce reduce-fn
            `(do ~@body)
            (reverse sections))))

