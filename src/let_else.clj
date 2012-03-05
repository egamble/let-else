(ns let-else
  (:use [useful.seq :only (partition-between)]
        [clojure.tools.macro :only (symbol-macrolet)]))


(defmacro let?
  "Same behavior as let, except where a binding is followed by :when <when> or :else <else>
   or both, in either order.

   For a :when, the <when> is evaluated after the associated binding is evaluated
   and must be truthy to continue evaluating the rest of the bindings and the body.
   If the <when> is falsey, the <else> is the value of the let?, if present, or nil if not.

   For an :else without a :when, if the associated binding is falsey, <else> is the value of the let?.

   :delay <truthy> following a binding of a symbol (not a destructuring form)
   delays evaluation of the binding value until it is actually used, in case there
   is a possibility it won't be used at all.

   Alternatively, :delay may be specified as metadata preceding the symbol, e.g.
   (let? [^:delay x (foo)] ...).

   :delay is ignored if there is an :else with no :when on the same binding.

   :is <pred>, following the binding of a symbol foo, is equivalent to :when (<pred> foo)."

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

                {:keys [when is else]} opt-map

                ;; allows :else <falsey>
                else-exists (some #{:else} (keys opt-map))

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
                       ~new-body)))

                is-expanded
                (clojure.core/when is
                 (if (symbol? name)
                   `(~is ~name)
                   (throw (Exception. ":is <pred> doesn't work with destructuring bindings; use :when <exp> instead"))))

                when-is
                (if (and when is-expanded)
                  `(and ~when ~is-expanded)
                  (or when is-expanded))]

            (cond (and when-is else-exists)
                  (let [delay-else-sym (gensym "delay-else")]
                    `(let [~delay-else-sym (delay ~else)]
                       ~(delay-fn
                         `(if ~when-is
                            ~body
                            (force ~delay-else-sym)))))

                  when-is
                  (delay-fn
                   `(when ~when-is
                      ~body))

                  else-exists
                  ;; no delay possible in this case
                  `(if-let [~name ~val]
                     ~body
                     ~else)

                  :else
                  (delay-fn body))))]

    (reduce reduce-fn
            `(do ~@body)
            (reverse sections))))

