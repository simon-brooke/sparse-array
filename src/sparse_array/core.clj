(ns sparse-array.core)


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
    (and
      (map? x)
      (pos? (:dimensions x))
      (keyword? (:coord x))
      (if
        (coll? (:content x))
        (every?
          sparse-array?
          (map #(x %) (filter integer? (keys x))))
        (= (:content x) :data)))))

(defn put
  "Return a sparse array like this `array` but with this `value` at these
  `coordinates`. Returns `nil` if any coordinate is invalid."
  [array value & coordinates]
  (if
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
  but preferring values from `a2` where there is a conflict."
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

(defn dense-to-sparse
  "Return a sparse array representing the content of the dense array `x`,
  assuming these `coordinates` if specified."
  ([x]
   :TODO)
  ([x coordinates]
   :TODO))

(defn sparse-to-dense
  "Return a dense array representing the content of the sparse array `x`.
  If this blows up out of memory, that's strictly your problem."
  [x]
  :TODO)
