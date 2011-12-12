The `let?` macro expands into a `let`, except where a binding is followed by `:when` _pred_ or `:else` _else_ or both, in either order.

For a `:when`, the _pred_ is evaluated after the associated binding is evaluated
and must be truthy to continue evaluating the rest of the bindings and the body.
If the _pred_ is falsey, the _else_ is the value of the `let?`, if present, or `nil` if not.

For an `:else` without a `:when`, if the associated binding is falsey, _else_ is the value of the `let?`.

Note that `:else` clauses are evaluated outside the scope of the associated binding, e.g:

```clojure
(let [x 3]
  (let? [x false :else x]
    nil))
=> 3
```

The jar is available at https://clojars.org/egamble/let-else.

