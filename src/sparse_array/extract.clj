(ns sparse-array.extract
  "Extracting interesting data from sparse arrays."
  (:require [sparse-array.core :refer [*safe-sparse-operations*
                                       dense-array? dense-dimensions
                                       make-sparse-array sparse-array?]]))

;;; The whole point of working with sparse arrays is to work with interesting
;;; subsets of arrays most of which are uninteresting. To extract an
;;; interesting subset from an array, we're going to need an extract function.

(defn- extract-from-sparse
  "Return a subset of this sparse `array` comprising all those cells for which
  this `function` returns a 'truthy' value."
  [array function]
  (reduce
    merge
    (apply
      make-sparse-array
      (cons
        (:coord array)
        (when (coll? (:content array)) (:content array))))
    (map
      #(if
         (= :data (:content array))
         (when (function (array %)) {% (array %)})
         (let [v (extract-from-sparse (array %) function)]
           (if
             (empty?
               (filter integer? (keys v)))
             nil
             {% v})))
      (filter integer? (keys array)))))

(defn extract-from-dense
  "Return a subset of this dense `array` comprising all those cells for which
  this `function` returns a 'truthy' value. Use these `axes` if provided."
  ([array function]
   (extract-from-dense array function (map #(keyword (str "i" %)) (range))))
  ([array function axes]
   (let
     [dimensions (dense-dimensions array)]
     (reduce
       merge
       (apply make-sparse-array (take dimensions axes))
       (if
         (= dimensions 1)
         (map
           (fn [i v] (when (function v) (hash-map i v)))
           (range)
           array)
         (map
           (fn [i v] (if (empty? (filter integer? (keys v))) nil (hash-map i v)))
           (range)
           (map #(extract-from-dense % function (rest axes)) array)))))))

(defn extract
  "Return a sparse subset of this `array` - which may be either sparse or
  dense - comprising all those cells for which this `function` returns a
  'truthy' value."
  [array function]
  (cond
    (sparse-array? array)
    (extract-from-sparse array function)
    (dense-array? array)
    (extract-from-dense array function)
    *safe-sparse-operations*
    (throw
      (ex-info
        "Argument passed as `array` is neither sparse nor dense."
        {:array array}))))

