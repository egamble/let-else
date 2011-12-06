(ns let-else)


(defn- doify [body]
  (condp = (count body)
      0 nil
      1 (first body)
      (cons 'do body)))

(defn- let-else-expand
  "Recursive helper function for let-else. Not intended for use by itself."
  [bindings body]
  (if (empty? bindings)
    body

    (let [[name-1 expr-1 & bindings-1] bindings
          [name-2 expr-2 & bindings-2] bindings-1]
      (if (= :else name-2)
        `((if-let [~name-1 ~expr-1]
            ~(doify
              (let-else-expand bindings-2 body))
            ~expr-2))
        `((let [~name-1 ~expr-1]
            ~@(let-else-expand bindings-1 body)))))))

(defmacro let-else
  "Given a vector of bindings and a body, expands into nested lets.
   If a binding starts with :else, the surrounding let becomes an if-let and
   the value of the binding becomes the else expression of the if-let. E.g.
      (let-else
        [foo (f1) :else (e)
         bar (f2)]
        (b1)
        (b2))
   expands into
      (if-let [foo (f1)]
        (let [bar (f2)]
          (b1)
          (b2))
        (e))"
  [bindings & body]
  (doify
   (let-else-expand bindings body)))
