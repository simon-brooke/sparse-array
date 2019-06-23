# sparse-array

A Clojure library designed to manipulate sparse *arrays* - multi-dimensional spaces accessed by indices, but containing arbitrary values rather than just numbers. For sparse spaces which contain numbers only, you're better to use a *sparce matrix* library, for example [clojure.core.matrix](https://mikera.github.io/core.matrix/).

## Conventions:

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

At the present stage of development, where the expectations of an operation are violated, `nil` is returned and no exception is thrown. However, it's probable that later there will be at least the option of thowing specific exceptions, as otherwise debugging could be tricky.

## Usage

FIXME

## License

Copyright © 2019 Simon Brooke

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
