(ns let-else)


(defn- regularize-bindings
  "Regularize the order of :when and :else. Supply a :when for each naked :else."
  [bindings]
  (let [pairs (partition 2 bindings)
        pair-groups (partition-by (comp keyword? first) pairs)
        pair-group-pairs (partition-all 2 pair-groups)]

    (flatten
     (map #(let [[name-pairs kwd-pairs] %]
             (if (nil? kwd-pairs) %
                 (let [kwds (apply hash-map (flatten kwd-pairs))]
                   [name-pairs
                    (cond (= 2 (count kwds))
                          [:when (:when kwds) :else (:else kwds)]

                          (:when kwds)
                          [:when (:when kwds)]

                          :else
                          [:when (first (last name-pairs)) :else (:else kwds)])])))
          pair-group-pairs))))

(defmacro let?-
  [bindings & body]
  (let [[bind [kwd1 expr1 & [kwd2 expr2 & more2 :as more1]]]
        (split-with (complement #{:when})
                    bindings)]
    `(let [~@bind]
       ~@(cond (= :else kwd2)
               (if more2
                 [`(if ~expr1
                     (let?- [~@more2] ~@body)
                     ~expr2)]
                 [`(if ~expr1
                     ~@body
                     ~expr2)])

               kwd2
               [`(when ~expr1
                   (let?- [~@more1] ~@body))]

               kwd1
               [`(when ~expr1 ~@body)]

               :else
               ~@body))))

(defmacro let?
  "Expands into a let, except where a binding is followed by :when <pred> or :else <else>
   or both, in either order.

   For a :when, the <pred> is evaluated after the associated binding is evaluated
   and must be truthy to continue evaluating the rest of the bindings and the body.
   If the <pred> is falsey, the <else> is the value of the let?, if present, or nil if not.

   For an :else without a :when, if the associated binding is falsey, <else> is the value of the let?."
  [bindings & body]
  `(let?-
    [~@(regularize-bindings bindings)]
    ~@body))
