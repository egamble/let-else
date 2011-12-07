The let? macro expands into a let, except where a binding is followed by :when _pred_ or :else _else_ or both, in either order.

For a :when, the _pred_ is evaluated after the associated binding is evaluated
and must be truthy to continue evaluating the rest of the bindings and the body.
If the _pred_ is falsey, the _else_ is the value of the let?, if present, or nil if not.

For an :else without a :when, if the associated binding is falsey, _else_ is the value of the let?.

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
