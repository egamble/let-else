The let-else macro takes a vector of bindings and a body and expands into nested lets.
If a binding starts with :else, the surrounding let becomes an if-let and
 the value of the binding becomes the else expression of the if-let. E.g.

```clojure
(let-else
  [foo (f1) :else (e)
   bar (f2)]
  (b1)
  (b2))
```
expands into

```clojure
(if-let [foo (f1)]
  (let [bar (f2)]
    (b1)
    (b2))
  (e))
```