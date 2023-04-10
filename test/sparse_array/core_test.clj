(ns sparse-array.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [sparse-array.core :refer [*safe-sparse-operations* 
                                       dense-dimensions dense-to-sparse
                                       get make-sparse-array
                                       merge-arrays put 
                                       sparse-array? sparse-to-dense]]))

(deftest creation-and-testing
  (testing "Creation and testing."
    (is (sparse-array? (make-sparse-array :x :y :z)))
    (is (sparse-array? {:dimensions 2,
                        :coord :x,
                        :content '(:y),
                        3 {:dimensions 1,
                           :coord :y,
                           :content :data,
                           4 "hello"},
                        4 {:dimensions 1,
                           :coord :y,
                           :content :data,
                           3 "goodbye"}}))
    (is (= (sparse-array? []) false))
    (is (= (sparse-array? "hello") false))
    (is (=
          (sparse-array?
            (dissoc (make-sparse-array :x :y :z) :dimensions))
          false)
        "All mandatory keywords must be present: dimensions")
    (is (=
          (sparse-array?
            (dissoc (make-sparse-array :x :y :z) :coord))
          false)
        "All mandatory keywords must be present: coord")
    (is (=
          (sparse-array?
            (dissoc (make-sparse-array :x :y :z) :content))
          false)
        "All mandatory keywords must be present: content")
    (is (=
          (sparse-array? {:dimensions 2,
                          :coord :x,
                          :content '(:y),
                          3 {:dimensions 1,
                             :coord :y,
                             :content :data,
                             4 "hello"},
                          4 {:dimensions 1,
                             :coord :y,
                             :content :data,
                             3 "goodbye"}
                          5 :foo})
          false)
          "Can't have data in a non-data layer")
    ))

(deftest testing-safe
  (testing "Checking that correct exceptions are thrown when `*safe-sparse-operations*` is true"
    (binding [*safe-sparse-operations* true]
      (is
        (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"Array must be a map"
            (sparse-array? [])))
      (is
        (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"The value of `:dimensions` must be a positive integer, not .*"
          (sparse-array?
            (dissoc (make-sparse-array :x :y :z) :dimensions)))
        "All mandatory keywords must be present: dimensions")
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"The value of `:coord` must be a keyword, not .*"
          (sparse-array?
            (dissoc (make-sparse-array :x :y :z) :coord))))
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"If there are no further axes the value of `:content` must be `:data`"
          (sparse-array?
            (dissoc (make-sparse-array :x :y :z) :content)))
        "All mandatory keywords must be present: content")
    (is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"Array must be a map"
          (sparse-array? {:dimensions 2,
                          :coord :x,
                          :content '(:y),
                          3 {:dimensions 1,
                             :coord :y,
                             :content :data,
                             4 "hello"},
                          4 {:dimensions 1,
                             :coord :y,
                             :content :data,
                             3 "goodbye"}
                          5 :foo}))
          "Can't have data in a non-data layer"))))

(deftest put-and-get
  (testing "get"
    (let [expected "hello"
          array {:dimensions 2,
                 :coord :x,
                 :content '(:y)
                 3 {:dimensions 1 :coord :y :content :data 4 "hello"}}
          actual (get array 3 4)]
      (is (= actual expected)))
    (let [expected nil
          array {:dimensions 2,
                 :coord :x,
                 :content '(:y)
                 3 {:dimensions 1 :coord :y :content :data 4 "hello"}}
          actual (get array 4 3)]
      (is (= actual expected))))
  (testing "put"
    (let [expected "hello"
          array {:dimensions 2,
                 :coord :x,
                 :content '(:y)}
          actual (((put array expected 3 4) 3) 4)]
      (is (= actual expected))))
  (testing "round trip, one dimension"
    (let
      [expected "hello"
       actual (get (put (make-sparse-array :x) expected 3) 3)]
      (is (= actual expected)))
    (binding [*safe-sparse-operations* true]
      ;; enabling error handling shouldn't make any difference
      (let
        [expected "hello"
         actual (get (put (make-sparse-array :x) expected 3) 3)]
        (is (= actual expected)))))
  (testing "round trip, two dimensions"
    (let
      [expected "hello"
       actual (get (put (make-sparse-array :x :y) expected 3 4) 3 4)]
      (is (= actual expected)))
    (binding [*safe-sparse-operations* true]
      ;; enabling error handling shouldn't make any difference
    (let
      [expected "hello"
       actual (get (put (make-sparse-array :x :y) expected 3 4) 3 4)]
      (is (= actual expected)))))
    (testing "round trip, three dimensions"
    (let
      [expected "hello"
       actual (get (put (make-sparse-array :x :y :z) expected 3 4 5) 3 4 5)]
      (is (= actual expected))))
    (testing "round trip, four dimensions"
    (let
      [expected "hello"
       actual (get (put (make-sparse-array :p :q :r :s) expected 3 4 5 6) 3 4 5 6)]
      (is (= actual expected))))
  (testing "Error handling, number of dimensions"
    (binding [*safe-sparse-operations* true]
      (is
        (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"Expected 3 coordinates; found 2"
          (put (make-sparse-array :x :y :z) "hello" 3 4)))
      (is
        (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"Expected 3 coordinates; found 4"
          (put (make-sparse-array :x :y :z) "hello" 3 4 5 6)))
      (is
        (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"Expected 3 coordinates; found 2"
          (get (make-sparse-array :x :y :z) 3 4)))
      (is
        (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"Expected 3 coordinates; found 4"
          (get (make-sparse-array :x :y :z) 3 4 5 6))))))

(deftest merge-test
  (testing "merge, sparse arrays, one dimension"
    (let
      [merged (merge-arrays
                (put (make-sparse-array :x) "hello" 3)
                (put (make-sparse-array :x) "goodbye" 4))]
      (is (= "hello" (get merged 3)))
      (is (= "goodbye" (get merged 4)))))
  (testing "merge, sparse arrays, two dimensions"
    (let
      [merged (merge-arrays
                (put (make-sparse-array :x :y) "hello" 3 4)
                (put (make-sparse-array :x :y) "goodbye" 4 3))]
      (is (= "hello" (get merged 3 4)))
      (is (= "goodbye" (get merged 4 3)))))
  (testing "merge, dense with sparse, two dimensions")
  (let [dense [[[nil nil nil][nil nil nil][nil nil nil]]
               [[nil nil nil][nil nil nil][nil nil nil]]
               [[nil nil nil][nil nil nil][nil nil nil]]]
        sparse (put (put (make-sparse-array :x :y :z) "hello" 0 0 0) "goodbye" 2 2 2)
        expected [[["hello" nil nil] [nil nil nil] [nil nil nil]]
                  [[nil nil nil] [nil nil nil] [nil nil nil]]
                  [[nil nil nil] [nil nil nil] [nil nil "goodbye"]]]
        actual (merge-arrays dense sparse)]
    (is (= actual expected))))

(deftest dense-to-sparse-tests
  (testing "dense-to-sparse, one dimension"
    (let [dense [nil nil nil "hello" nil "goodbye"]
          sparse (dense-to-sparse dense)]
      (is (= "hello" (get sparse 3)))
      (is (= "goodbye" (get sparse 5))))))

(deftest dense-dimensions-tests
  (testing "dense-dimensions"
    (is (= 0 (dense-dimensions 1)))
    (is (= 1 (dense-dimensions [1 2 3])))
    (is (= 2 (dense-dimensions [[1 2 3][2 4 6][3 6 9]])))
    (is (= 3
           (dense-dimensions
             [[[1 2 3][2 4 6][3 6 9]]
              [[2 4 6][4 16 36][6 12 18]]
              [[3 6 9][18 96 (* 6 36 3)][100 200 300]]])))))

(deftest sparse-to-dense-tests
  (testing "Conversion from sparse to dense arrays"
    (let [expected [[nil nil nil nil nil]
                    [nil nil nil nil nil]
                    [nil nil nil nil nil]
                    [nil nil nil nil "hello"]
                    [nil nil nil "goodbye" nil]]
          actual (sparse-to-dense {:dimensions 2,
                                   :coord :x,
                                   :content '(:y),
                                   3 {:dimensions 1,
                                      :coord :y,
                                      :content :data,
                                      4 "hello"},
                                   4 {:dimensions 1,
                                      :coord :y,
                                      :content :data,
                                      3 "goodbye"}})]
      (is (= actual expected)))))


