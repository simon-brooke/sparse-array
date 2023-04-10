(ns sparse-array.core
  "Operations on sparse arrays.")

(declare put get)

(def ^:dynamic *safe-sparse-operations*
  "Whether spase array operations should be conducted safely, with careful
  checking of data conventions and exceptions thrown if expectations are not
  met. Normally `false`."
  false)

(defn- unsafe-sparse-operations?
  "returns `true` if `*safe-sparse-operations*` is `false`, and vice versa."
  []
  (not (true? *safe-sparse-operations*)))

(defn make-sparse-array
  "Make a sparse array with these `dimensions`. Every member of `dimensions`
  must be a keyword; otherwise, `nil` will be returned."
  [& dimensions]
  (when
    (and (pos? (count dimensions))
         (every? keyword? dimensions))
    {:dimensions (count dimensions)
     :coord (first dimensions)
     :content (if
                (empty? (rest dimensions))
                :data
                (rest dimensions))}))

(defn- safe-test-or-throw
  "If `v` is truthy or `*safe-sparse-operations*` is false, return `v`;
  otherwise, throw an `ExceptionInfo` with this `message` and the map `m`."
  [v message m]
  (if-not
    v
    (if
      *safe-sparse-operations*
      (throw (ex-info message m))
      v)
    v))

(defn sparse-array?
  "`true` if `x` is a sparse array conforming to the conventions established
  by this library, else `false`."
  ;; TODO: sparse-array? should not throw exceptions even when
  ;; *safe-sparse-operations* is true, since we may use to test
  ;; whether an object is a sparse array. The place to throw the exceptions
  ;; (if required) is after it has failed.
  ([x]
   (apply
     sparse-array?
     (cons
       x
       (cons
         (:coord x)
         (when
           (coll? (:content x))
           (:content x))))))
  ([x & axes]
   (and
     (safe-test-or-throw
       (map? x)
       "Array must be a map" {:array x})
     (safe-test-or-throw
       (and (integer? (:dimensions x)) (pos? (:dimensions x)))
       (str "The value of `:dimensions` must be a positive integer, not " (:dimensions x))
       {:array x})
     (safe-test-or-throw
       (keyword? (:coord x))
       (str "The value of `:coord` must be a keyword, not " (:coord x))
       {:array x})
     (safe-test-or-throw
       (= (:coord x) (first axes))
       (str "The value of `:coord` must be " (first axes) ", not " (:coord x))
       {:array x})
     (if
       (empty? (rest axes))
       (safe-test-or-throw
         (= (:content x) :data)
         "If there are no further axes the value of `:content` must be `:data`"
         {:array x})
       (and
         (= (:content x) (rest axes))
         (every?
           sparse-array?
           (map #(x %) (filter integer? (keys x)))))))))

(defn- unsafe-put
  [array value coordinates]
  (cond
    (every?
      #(and (integer? %) (or (zero? %) (pos? %)))
      coordinates)
    (assoc
      array
      (first coordinates)
      (if
        (= :data (:content array))
        value
        (apply
          put
          (cons
            (or
              (array (first coordinates))
              (apply make-sparse-array (:content array)))
            (cons value (rest coordinates))))))))

(defn put
  "Return a sparse array like this `array` but with this `value` at these
  `coordinates`. Returns `nil` if any coordinate is invalid."
  [array value & coordinates]
  (cond
    (nil? value)
    nil
    (unsafe-sparse-operations?)
    (unsafe-put array value coordinates)
    (not (sparse-array? array))
    (throw (ex-info "Sparse array expected" {:array array}))
    (not= (:dimensions array) (count coordinates))
    (throw
      (ex-info
        (str "Expected " (:dimensions array) " coordinates; found " (count coordinates))
        {:array array
         :coordinates coordinates}))
    (not
      (every?
        #(and (integer? %) (or (zero? %) (pos? %)))
        coordinates))
    (throw
      (ex-info
        "Coordinates must be zero or positive integers"
        {:array array
         :coordinates coordinates
         :invalid (remove #(and (pos? %) (integer? %)) coordinates)}))
    :else
    (unsafe-put array value coordinates)))

(defn- unsafe-get
  ;; TODO: I am CERTAIN there is a more elegant solution to this.
  [array coordinates]
  (let [v (array (first coordinates))]
    (cond
      (= :data (:content array))
      v
      (nil? v)
      nil
      :else
      (apply get (cons v (rest coordinates))))))

(defn get
  "Return the value in this sparse `array` at these `coordinates`."
  [array & coordinates]
  (cond
    (unsafe-sparse-operations?)
    (unsafe-get array coordinates)
    (not (sparse-array? array))
    (throw (ex-info "Sparse array expected" {:array array}))
    (not (every?
           #(and (integer? %) (or (zero? %) (pos? %)))
           coordinates))
    (throw
      (ex-info
        "Coordinates must be zero or positive integers"
        {:array array
         :coordinates coordinates
         :invalid (remove #(and (pos? %) (integer? %)) coordinates)}))
    (not (= (:dimensions array) (count coordinates)))
    (throw
      (ex-info
        (str "Expected " (:dimensions array) " coordinates; found " (count coordinates))
        {:array array
         :coordinates coordinates}))
    :else
    (unsafe-get array coordinates)))

(defn dense-dimensions
  "How many usable dimensions (represented as vectors) does the dense array
  `x` have?"
  [x]
  (if
    (vector? x)
    (if
      (every? vector? x)
      (inc (apply min (map dense-dimensions x)))
      ;; `min` is right here, not `max`, because otherwise
      ;; we will get malformed arrays. Be liberal with what you
      ;; consume, conservative with what you return!
      1)
    0))

(defn dense-array?
  "Basically, any vector can be considered as a dense array of one dimension.
  If we're seeking a dense array of more than one dimension, the number of
  dimensions should be specified as `d`."
  ([x]
   (vector? x))
  ([x d]
   (and (vector? x) (< d (dense-dimensions x)))))

(defn merge-sparse-arrays
  "Return a sparse array taking values from sparse arrays `a1` and `a2`,
  but preferring values from `a2` where there is a conflict. `a1` and `a2`
  must have the **same** dimensions in the **same** order, or `nil` will
  be returned."
  [a1 a2]
  (cond
    (nil? a1) a2
    (nil? a2) a1
    (not (= (:content a1) (:content a2)))
    ;; can't reasonably merge arrays with different dimensions
    nil
    (= :data (:content a1))
    (merge a1 a2)
    (or (unsafe-sparse-operations?) (and (sparse-array? a1) (sparse-array? a2)))
    (reduce
      merge
      a2
      (map
        #(assoc a2 % (merge-sparse-arrays (a1 %) (a2 %)))
        (filter
          integer?
          (set
            (concat
              (keys a1)
              (keys a2))))))))

(defn merge-dense-with-sparse
  "Merge this dense array `d` with this sparse array `s`, returning a new
  dense array with the same arity as `d`, preferring values from `s` where
  there is conflict"
  [d s]
  (apply
    vector
    (map
      #(cond
         (= :data (:content s))
         (or (s %2) %1)
         (nil? (s %2))
         %1
         :else
         (merge-dense-with-sparse %1 (s %2)))
      d
      (range))))

(defn merge-arrays
  "Merge two arrays `a1`, `a2`, which may be either dense or sparse but which
  should have the same number of axes and compatible dimensions, and return a
  new dense array preferring values from `a2`."
  [a1 a2]
  (cond
    (dense-array? a2)
    a2 ;; if a2 is dense, no values from a1 will be returned
    (sparse-array? a1)
    (cond
      (sparse-array? a2)
      (merge-sparse-arrays a1 a2)
      *safe-sparse-operations*
      (throw
        (ex-info
          "Object passed as array is neither dense not sparse"
          {:array a2})))
    (dense-array? a1)
    (cond
      (sparse-array? a2)
      (merge-dense-with-sparse a1 a2)
      *safe-sparse-operations*
      (throw
        (ex-info
          "Object passed as array is neither dense not sparse"
          {:array a2})))
    *safe-sparse-operations*
    (throw
      (ex-info
        "Object passed as array is neither dense not sparse"
        {:array a1}))))

(defn dense-to-sparse
  "Return a sparse array representing the content of the dense array `x`,
  assuming these `axes` if specified. *NOTE THAT* if insufficient
  values of `axes` are specified, the resulting sparse array will
  be malformed."
  ([x]
   (dense-to-sparse x (map #(keyword (str "i" %)) (range))))
  ([x axes]
   (let
     [dimensions (dense-dimensions x)]
     (reduce
       merge
       (apply make-sparse-array (take dimensions axes))
       (map
         (fn [i v] (if (nil? v) nil (hash-map i v)))
         (range)
         (if
           (> dimensions 1)
           (map #(dense-to-sparse % (rest axes)) x)
           x))))))

(defn arity
  "Return the arity of the sparse array `x`."
  [x]
  (inc (apply max (filter integer? (keys x)))))

(defn child-arity
  "Return the largest arity among the arities of the next dimension layer of
  the sparse array `x`."
  [x]
  (apply
    max
    (cons
      -1 ;; if no children are sparse arrays, we should return 0ß
      (map
        arity
        (filter sparse-array? (vals x))))))

(defn sparse-to-dense
  "Return a dense array representing the content of the sparse array `x`.

  **NOTE THAT** this has the potential to consume very large amounts of memory."
  ([x]
   (sparse-to-dense x (arity x)))
  ([x arity]
   (if
     (map? x)
     (let [a (child-arity x)]
       (apply
         vector
         (map
           #(let [v (x %)]
              (if
                (= :data (:content x))
                v
                (sparse-to-dense v a)))
           (range arity))))
     (apply vector (repeat arity nil)))))


