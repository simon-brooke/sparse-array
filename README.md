# sparse-array

A Clojure library designed to manipulate sparse *arrays* - multi-dimensional spaces accessed by indices, but containing arbitrary values rather than just numbers. For sparse spaces which contain numbers only, you're better to use a *sparse matrix* library, [for example clojure.core.matrix](https://mikera.github.io/core.matrix/).

Arbitrary numbers of dimensions are supported, up to limits imposed by the JVM stack.

Available from Clojars: [![Clojars Project](https://img.shields.io/clojars/v/sparse-array.svg)](https://clojars.org/sparse-array)
Tests currently [![CircleCI](https://circleci.com/gh/simon-brooke/sparse-array.svg?style=svg)](https://circleci.com/gh/simon-brooke/sparse-array)

[![CircleCI](https://circleci.com/gh/simon-brooke/sparse-array.svg?style=svg)](https://circleci.com/gh/simon-brooke/sparse-array)

## Conventions

### Sparse arrays

For the purposes of this library, a sparse array shall be implemented as a map, such that all keys are non-negative members of the set of integers, except for the following keyword keys, all of which are expected to be present:

1. `:dimensions` The number of dimensions in this array, counting the present one (value expected to be a real number);
2. `:coord` The coordinate of the dimension represented by the current map (value expected to be a keyword);
3. `:content` What this map contains; if the value of `:dimensions` is one, then `:data`; otherwise, an ordered sequence of the coordinates of the dimensions below this one.

Thus an array with a single value 'hello' at coordinates x = 3, y = 4, z = 5 would be encoded:

```clojure
{:dimensions 3
 :coord :x
 :content [:y :z]
 3 {:dimensions 2
    :coord :y
    :content [:z]
    4 {:dimensions 1
       :coord :z
       :content :data
       5 "hello"
     }
    }
  }
```

### Errors and error-reporting

A dynamic variable, `*safe-sparse-operations*`, is provided to handle behaviour in error conditions. If this is `false`, bad data will generally not cause an exception to be thrown, and corrupt structures may be returned, thus:

```clojure
(put (make-sparse-array :x :y :z) "hello" 3) ;; insufficient coordinates specified

=> {:dimensions 3, :coord :x, :content (:y :z), 3 {:dimensions 2, :coord :y, :content (:z), nil {:dimensions 1, :coord :z, :content :data, nil nil}}}
```

However, if `*safe-sparse-operations*` is bound to `true`, exceptions will be thrown instead:

```clojure
(binding [*safe-sparse-operations* true]
  (put (make-sparse-array :x :y :z) "hello" 3))

ExceptionInfo Expected 3 coordinates; found 1  clojure.core/ex-info (core.clj:4617)
```

Sanity checking data is potentially expensive; for this reason `*safe-sparse-operations*` defaults to `false`, but you may wish to bind it to `true` especially while debugging.

### Dense arrays

For the purposes of conversion, a **dense array** is assumed to be a vector; a two dimensional dense array a vector of vectors; a three dimensional dense array a vector of vectors of vectors, and so on. For any depth `N`, all vectors at depth `N` must have the same arity. If these conventions are not respected conversion may fail.

## Usage

### make-sparse-array
`sparse-array.core/make-sparse-array ([& dimensions])`

Make a sparse array with these `dimensions`. Every member of `dimensions` must be a keyword; otherwise, `nil` will be returned.

e.g.

```clojure
(make-sparse-array :x :y :z)

=> {:dimensions 3, :coord :x, :content (:y :z)}

```

### sparse-array?

`sparse-array.core/sparse-array? ([x])`

`true` if `x` is a sparse array conforming to the conventions established by this library, else `false`.

### put

`sparse-array.core/put ([array value & coordinates])`

Return a sparse array like this `array` but with this `value` at these `coordinates`. Returns `nil` if any coordinate is invalid.

e.g.

```clojure
(put (put (make-sparse-array :x :y) "hello" 3 4) "goodbye" 4 3)

=> {:dimensions 2,
     :coord :x,
     :content (:y),
     3 {:dimensions 1, :coord :y, :content :data, 4 "hello"},
     4 {:dimensions 1, :coord :y, :content :data, 3 "goodbye"}}
```

### get

`sparse-array.core/get ([array & coordinates])`

Return the value in this sparse `array` at these `coordinates`.

### merge-sparse-arrays

`sparse-array.core/merge-sparse-arrays ([a1 a2])`

Return a sparse array taking values from sparse arrays `a1` and `a2`, but preferring values from `a2` where there is a conflict. `a1` and `a2` must have the **same** dimensions in the **same** order, or `nil` will be returned.

e.g.

```clojure
(merge-sparse-arrays
  (put (make-sparse-array :x) "hello" 3)
  (put (make-sparse-array :x) "goodbye" 4)))

=> {:dimensions 1, :coord :x, :content :data, 3 "hello", 4 "goodbye"}
```

### dense-to-sparse

`sparse-array.core/dense-to-sparse ([x] [x coordinates])`

Return a sparse array representing the content of the dense array `x`, assuming these `coordinates` if specified. *NOTE THAT* if insufficient values of `coordinates` are specified, the resulting sparse array will be malformed.

e.g.

```clojure
(dense-to-sparse [nil nil nil "hello" nil "goodbye"])

=> {:dimensions 1, :coord :i0, :content :data, 3 "hello", 5 "goodbye"}
```

### sparse-to-dense

`sparse-array.core/sparse-to-dense ([x] [x arity])`

Return a dense array representing the content of the sparse array `x`.

**NOTE THAT** this has the potential to consume very large amounts of memory.

e.g.

```clojure
(sparse-to-dense
  (put
    (put
      (make-sparse-array :x :y)
      "hello" 3 4)
    "goodbye" 4 3))

=> [[nil nil nil nil nil]
    [nil nil nil nil nil]
    [nil nil nil nil nil]
    [nil nil nil nil "hello"]
    [nil nil nil "goodbye" nil]]
```

### extract

The whole point of working with sparse arrays is because we wish to work with
interesting subsets of arrays the entirety of which would be too large to
conveniently handle; thus perhaps the most important operation is to be able
to extract a sparse subset of an array.

`sparse-array.extract/extract ([array function])`

Return a sparse subset of this `array` - which may be either sparse or
dense - comprising all those cells for which this `function` returns a
'truthy' value.

e.g.

```clojure
(extract [[[1 2 3][:one :two :three]["one" "two" "three"]]
         [[1 :two "three"]["one" 2 :three][:one "two" 3]]
         [[1.0 2.0 3.0][2/2 4/2 6/2]["I" "II" "III"]]]
         #(if
           (number? %)
           (= % 3)
           (= (name %) "three")))

=> {:dimensions 3,
     :coord :i0,
     :content (:i1 :i2),
     0
     {:dimensions 2,
      :coord :i1,
      :content (:i2),
      0 {:dimensions 1, :coord :i2, :content :data, 2 3},
      1 {:dimensions 1, :coord :i2, :content :data, 2 :three},
      2 {:dimensions 1, :coord :i2, :content :data, 2 "three"}},
     1
     {:dimensions 2,
      :coord :i1,
      :content (:i2),
      0 {:dimensions 1, :coord :i2, :content :data, 2 "three"},
      1 {:dimensions 1, :coord :i2, :content :data, 2 :three},
      2 {:dimensions 1, :coord :i2, :content :data, 2 3}},
     2
     {:dimensions 2,
      :coord :i1,
      :content (:i2),
      1 {:dimensions 1, :coord :i2, :content :data, 2 3}}}
```

### extract-from-dense

Note that the above example returns the default axis sequence `{i0, i1, i2...}`;
extracting from a sparse array will always retain the axes of the array
extracted from. Dense arrays, obviously, do not have explicit axes.

You may wish to specify a sequence of axes when extracting from a dense array.
A function is provided:

`sparse-array.extract/extract-from-dense ([array function] [array function axes])`

Return a subset of this dense `array` comprising all those cells for which
this `function` returns a 'truthy' value. Use these `axes` if provided.

e.g.

```clojure
(extract-from-dense
  [[[1 2 3][:one :two :three]["one" "two" "three"]]
  [[1 :two "three"]["one" 2 :three][:one "two" 3]]
  [[1.0 2.0 3.0][2/2 4/2 6/2]["I" "II" "III"]]]
  integer?
  '(:p :q :r))

=> {:dimensions 3,
     :coord :p,
     :content (:q :r),
     0
     {:dimensions 2,
      :coord :q,
      :content (:r),
      0 {:dimensions 1, :coord :r, :content :data, 0 1, 1 2, 2 3}},
     1
     {:dimensions 2,
      :coord :q,
      :content (:r),
      0 {:dimensions 1, :coord :r, :content :data, 0 1},
      1 {:dimensions 1, :coord :r, :content :data, 1 2},
      2 {:dimensions 1, :coord :r, :content :data, 2 3}},
     2
     {:dimensions 2,
      :coord :q,
      :content (:r),
      1 {:dimensions 1, :coord :r, :content :data, 0 1, 1 2, 2 3}}}
```

## License

Copyright © 2019 Simon Brooke

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
