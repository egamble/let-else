The jar is available at https://clojars.org/egamble/let-else.

The `let?` macro has the same behavior as `let`, except where a binding is followed by `:when` _when_ or `:else` _else_ or both, in either order.

For a `:when`, the _when_ is evaluated after the associated binding is evaluated
and must be truthy to continue evaluating the rest of the bindings and the body.
If the _when_ is falsey, the _else_ is the value of the `let?`, if present, or `nil` if not.

E.g., these expressions with and without `let?` are equivalent:

```clojure
(let? [d 0 :when (> d 0) :else "error"
       n 3]
  (/ n d))

(let [d 0]
  (if (> d 0)
    (let [n 3]
      (/ n d))
    "error"))
```

For an `:else` without a `:when`, if the associated binding is falsey, _else_ is the value of the `let?`.

E.g., these expressions are equivalent:

```clojure
(let? [a (foo) :else nil
       b (bar) :else "error"]
  [a b])

(when-let [a (foo)]
  (if-let [b (bar)]
    [a b]
    "error"))
```

Note that `:else` clauses are evaluated outside the scope of the associated binding, e.g:

```clojure
(let [x 3]
  (let? [x false :else x]
    nil))
=> 3
```

`:delay` _truthy_ following a binding of a symbol (not a destructuring form) delays
evaluation of the binding value until it is actually used, in case there is a
possibility it won't be used at all.

Alternatively, `:delay` may be specified as metadata preceding the symbol, e.g.

```clojure
(let? [^:delay x (foo)]
  ...)
```

#####Updates:

Version 1.0.1:

Added a new keyword `:is`, which can only follow a symbol binding (not a destructuring form). `:is <pred>`, following the binding of `foo`, is equivalent to `:when (<pred> foo)`.

E.g., these two expressions are equivalent:

```clojure
(let? [a "foo" :is not-empty :else "error"]
       b "bar"]
  (str a b))

(let [a "foo"]
  (if (not-empty a)
    (let [b "bar"]
      (str a b)
      "error")))
```

