(ns let-else
  (:require [clojure.tools.macro :refer (symbol-macrolet)]))


(defn partition-on
  "Applies f to each value in coll, splitting it each time f returns true.
  Returns a lazy seq of partitions."
  [f coll]
  (lazy-seq
   (when-let [s (seq coll)]
     (let [fst (first s)
           cf (complement f)
           run (cons fst (take-while cf (next s)))]
       (cons run (partition-on f (seq (drop (count run) s))))))))


(defmacro let?
  "Same behavior as let, except where a binding is followed by one or more of the following
   keyword clauses.

   :when <when>

   The rest of the bindings and the body are not evaluated if <when> evaluates to falsey,
   in which case let? returns the value of <else> if :else <else> is present, otherwise nil.

   :is <pred>
   :is-not <pred>

   The rest of the bindings and the body are not evaluated if <pred> applied to the value
   of the binding expression is falsey (for :is) or truthy (for :is-not). Works even with
   destructuring bindings. :when, :is, and :is-not can all be present on the same binding.

   :else <else>

   The value of <else> is the value of the let? if the associated :when or :is evaluates
   to falsey, or :is-not evaluates to truthy. In the absence of :when, :is, or :is-not,
   <else> becomes the value of the let? when the value of the binding expression is falsey.

   Without :when, :is, or :is-not clauses, the <else> is evaluated outside the context of
   the binding, otherwise inside, e.g.

   (let [a 4]
     (let? [a nil :else a]
       nil))
   => 4

   (let [a 4]
     (let? [a 3 :is even? :else (str a \" is not even\")]
       nil))
   => \"3 is not even\"

   :delay <truthy> following a binding of a symbol (not a destructuring form) delays
   evaluation of the binding value until it is actually used, in case there is a possibility
   it won't be used at all. :delay is ignored if the binding has any other keyword clauses.

   Alternatively, :delay may be specified as metadata preceding the symbol, e.g.
   (let? [^:delay x (foo)] ...)."

  [bindings & body]
  (let [bindings (partition 2 bindings)
        sections (partition-on #(not (keyword? (first %)))
                               bindings)

        reduce-fn
        (fn [body section]
          (let [[[name val] & opts] section

                opt-map (apply hash-map
                               (apply concat opts))

                {:keys [when is is-not else]} opt-map

                opt-keys (keys opt-map)

                ;; allow :else <falsey> alone to force a conditional
                else-exists? (some #{:else} opt-keys)

                temp-name (and (not (symbol? name))
                               (or is is-not)
                               (gensym))

                when-expand
                (clojure.core/when (some #{:when} opt-keys)
                  (list when))

                is-expand
                (clojure.core/when is
                  `((~is ~(or temp-name name))))

                is-not-expand
                (clojure.core/when is-not
                  `((not (~is-not ~(or temp-name name)))))

                when-is
                (let [asserts (concat when-expand is-expand is-not-expand)]
                  (clojure.core/when (seq asserts)
                    (list
                     (if (= 1 (count asserts))
                       (first asserts)
                       `(and ~@asserts)))))

                bind-fn
                (if temp-name
                  (fn [new-body]
                    `(let [~temp-name ~val
                           ~name ~temp-name]
                       ~new-body))
                  (fn [new-body]
                    `(let [~name ~val]
                       ~new-body)))]

            (cond (and when-is else-exists?)
                  (bind-fn
                   `(if ~@when-is
                      ~body
                      ~else))

                  when-is
                  (bind-fn
                   `(when ~@when-is
                      ~body))

                  else-exists?
                  `(if-let [~name ~val]
                     ~body
                     ~else)

                  (and (symbol? name)
                       (or (:delay (meta name))
                           (:delay opt-map)))
                  (let [delay-sym (gensym (str "delay-" name))]
                    `(let [~delay-sym (delay ~val)]
                       (symbol-macrolet [~name (force ~delay-sym)]
                                        ~body)))

                  :else
                  (bind-fn body))))]

    (reduce reduce-fn
            `(do ~@body)
            (reverse sections))))

(def truthy
  "Synonym for identity. In a let? form, :is truthy is clearer than :is identity."
  identity)
