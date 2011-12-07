The let-else macro takes a vector of bindings and a body and expands into nested lets.
If a binding starts with :else, the surrounding let becomes an if-let and
 the value of the binding becomes the else expression of the if-let. E.g.

The let? macro expands into a let, except where a binding is followed by :when <pred> or :else <else> or both, in either order.

For a :when, the <pred> is evaluated after the associated binding is evaluated
and must be truthy to continue evaluating the rest of the bindings and the body.
If the <pred> is falsey, the <else> is the value of the let?, if present, or nil if not.

For an :else without a :when, if the associated binding is falsey, <else> is the value of the let?."

E.g.

```clojure
(let?
  [foo (f1) :when (w) :else (e)
   bar (f2)]
  (b1)
  (b2))
```
expands into

```clojure
(let [foo (f1)]
  (if (w)
    (let [bar (f2)]
      (b1)
      (b2))
    (e)))
```
and

```clojure
(let?
  [foo (f1) :else (e)
   bar (f2)]
  (b1)
  (b2))
```
expands into

```clojure
(let [foo (f1)]
  (if foo
    (let [bar (f2)]
      (b1)
      (b2))
    (e)))
```
