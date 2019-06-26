(ns sparse-array.core)

(def ^:dynamic *safe-sparse-operations* false)

(defn make-sparse-array
  "Make a sparse array with these `dimensions`. Every member of `dimensions`
  must be a keyword; otherwise, `nil` will be returned."
  [& dimensions]
  (if
    (and (pos? (count dimensions))
         (every? keyword? dimensions))
    {:dimensions (count dimensions)
     :coord (first dimensions)
     :content (if
                (empty? (rest dimensions))
                :data
                (rest dimensions))}))

(defn sparse-array?
  "`true` if `x` is a sparse array conforming to the conventions established
  by this library, else `false`."
  ([x]
   (apply
     sparse-array?
     (cons
       x
       (cons
         (:coord x)
         (if
           (coll? (:content x))
           (:content x))))))
  ([x & axes]
   (and
     (map? x)
     (pos? (:dimensions x))
     (keyword? (:coord x))
     (= (:coord x) (first axes))
     (if
       (rest axes)
       (and
         (= (:content x) (rest axes))
         (every?
           sparse-array?
           (map #(x %) (filter integer? (keys x)))))
       (= (:content x) :data)))))

(defn put
  "Return a sparse array like this `array` but with this `value` at these
  `coordinates`. Returns `nil` if any coordinate is invalid."
  [array value & coordinates]
  (cond
    (and *safe-sparse-operations* (sparse-array? array))
    (throw (ex-info "Sparse array expected" {:array array}))
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
            (cons value (rest coordinates))))))
    *safe-sparse-operations*
    (throw
      (ex-info
        "Coordinates must be zero or positive integers"
        {:coordinates coordinates
         :invalid (remove integer? coordinates)}))))

(defn get
  "Return the value in this sparse `array` at these `coordinates`."
  ;; TODO: I am CERTAIN there is a more elegant solution to this.
  [array & coordinates]
  (if
    (= :data (:content array))
    (array (first coordinates))
    (apply get (cons (array (first coordinates)) (rest coordinates)))))

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
    :else
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

(defn dense-to-sparse
  "Return a sparse array representing the content of the dense array `x`,
  assuming these `coordinates` if specified. *NOTE THAT* if insufficient
  values of `coordinates` are specified, the resulting sparse array will
  be malformed."
  ([x]
   (dense-to-sparse x (map #(keyword (str "i" %)) (range))))
  ([x coordinates]
   (let
     [dimensions (dense-dimensions x)]
     (reduce
       merge
       (apply make-sparse-array (take dimensions coordinates))
       (map
         (fn [i v] (if (nil? v) nil (hash-map i v)))
         (range)
         (if
           (> dimensions 1)
           (map #(dense-to-sparse % (rest coordinates)) x)
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


