The jar is available at https://clojars.org/egamble/let-else.

The `let?` macro has the same behavior as `let`, except where a binding is followed by one or more of the keyword clauses described below, in any order.

### Motivation

I often find myself writing a series of `let` bindings,
where some or all of the bindings have assertions associated with them that
would stop further binding if the assertions failed. Conceptually, that pattern
really feels like it should be a single `let` form, but in practice it
has to be implemented with a bunch of nested `lets`, `when-lets`, `if-lets`,
etc. So `let?` allows me to write code for that pattern as the single
`let` that it wants to be.

A contrived example:

```clojure
(when-let [a foo]
  (let [b bar]
    (when (even? b)
      (let [c baz]
        (when (> b c)
          (let [d qux]
            (f a b c d)))))))
```

becomes:

```clojure
(let? [a foo :else nil
       b bar :is even?
       c baz :when (> b c)
       d qux]
  (f a b c d))
```

### Keyword clauses

#### `:when` _when_

The rest of the bindings and the body are not evaluated if _when_ evaluates to falsey,
in which case `let?` returns the value of _else_ if `:else` _else_ is present, otherwise nil.

E.g., these expressions with and without `let?` are equivalent:

```clojure
(let? [d 5 :when (> d 0) :else "error"
       n 3]
  (/ n d))

(let [d 5]
  (if (> d 0)
    (let [n 3]
      (/ n d))
    "error"))
```

#### `:is` _pred_ and `:is-not` _pred_

The rest of the bindings and the body are not evaluated if _pred_ applied to the value
of the binding expression is falsey (for `:is`) or truthy (for `:is-not`). Works even with
destructuring bindings. `:when`, `:is`, and `:is-not` can all be present on the same binding.

E.g., these three expressions are equivalent:

```clojure
(let? [a "foo" :is not-empty :else "error"
       b "bar"]
  (str a b))

(let? [a "foo" :is-not empty? :else "error"
       b "bar"]
  (str a b))

(let [a "foo"]
  (if (not-empty a)
    (let [b "bar"]
      (str a b))
    "error"))
```

#### `:else` _else_

The value of _else_ is the value of the `let?` if the associated `:when` or `:is` evaluates
to falsey, or `:is-not` evaluates to truthy.

For an `:else` without a `:when`, `:is`, or `:is-not`, if the value of the binding expression
is falsey, _else_ is the value of the `let?`.

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

With `:when`, `:is`, or `:is-not` clauses, the _else_ is evaluated inside the context of
the binding, otherwise outside, e.g.

```clojure
(let [a 4]
  (let? [a 3 :is even? :else (str a " is not even")]
    nil))
=> "3 is not even"

(let [a 4]
  (let? [a nil :else a]
    nil))
=> 4
```

#### `:delay` _truthy_

A `:delay` clause following a binding of a symbol (not a destructuring form) delays
evaluation of the binding value until it is actually used, in case there is a possibility
it won't be used at all. `:delay` is ignored if the binding has any other keyword clauses.

Alternatively, `:delay` may be specified as metadata preceding the symbol, e.g.

```clojure
(let? [^:delay x (foo)]
  ...)
```

is equivalent to:

```clojure
(let? [x (foo) :delay true]
  ...)
```

### Alternatives to `:else nil`

The keyword clause `:else nil` with no other keyword clauses is equivalent to `when-let`.
Some people find `:else nil` awkward, since nil is already the value of `when-let` when
the binding value is falsey. An equivalent keyword clause is `:is truthy`.
`let-else/truthy` is defined as a synonym of `clojure.core/identity`.

A not-quite-equivalent keyword clause is `:is-not nil?`, which distinguishes between nil
and not-nil rather than falsey and truthy.

### Updates:

#### Version 1.0.1:

Added the new keyword clause `:is` _pred_.

#### Version 1.0.2:

Fixed the behavior of `:else` _falsey_ which was incorrectly being ignored.

#### Version 1.0.3:

* Added the new keyword clause `:is-not` _pred_.
* Changed the behavior of `:else` _else_ in the presence of `:when`, `:is`, or `:is-not` so that _else_ is evaluated inside the context of the binding. `:else` without other keyword clauses is still evaluated outside the binding context.
* `:delay` is now ignored when other keyword clauses are present.

#### Version 1.0.4

Defined `truthy` as a synonym of `identity`, for use in the clause `:is truthy`.

#### Version 1.0.5

Updated Clojure dependency to 1.5.0.

#### Version 1.0.6

Removed dependency on [flatland/useful](https://github.com/flatland/useful) and updated Clojure dependency to 1.5.1.

#### Version 1.0.7

Added unit tests. Fixed the behavior of `:when nil` which was being ignored. Updated Clojure dependency to 1.6.0.
