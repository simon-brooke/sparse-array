(ns sparse-array.core-test
  (:require [clojure.test :refer :all]
            [sparse-array.core :refer :all]))

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
    (is-not (sparse-array? []))
    (is-not (sparse-array? "hello"))
    ))

(deftest put-and-get
  (testing "get"
    (let [expected "hello"
          array {:dimensions 2,
                 :coord :x,
                 :content '(:y)
                 3 {:dimensions 1 :coord :y :content :data 4 "hello"}}
          actual (get array 3 4)]
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
      (is (= actual expected))))
  (testing "round trip, two dimensions"
    (let
      [expected "hello"
       actual (get (put (make-sparse-array :x :y) expected 3 4) 3 4)]
      (is (= actual expected))))
    (testing "round trip, three dimensions"
    (let
      [expected "hello"
       actual (get (put (make-sparse-array :x :y :z) expected 3 4 5) 3 4 5)]
      (is (= actual expected))))
    (testing "round trip, four dimensions"
    (let
      [expected "hello"
       actual (get (put (make-sparse-array :p :q :r :s) expected 3 4 5 6) 3 4 5 6)]
      (is (= actual expected)))))

(deftest merge-test
  (testing "merge, one dimension"
    (let
      [merged (merge-sparse-arrays
                (put (make-sparse-array :x) "hello" 3)
                (put (make-sparse-array :x) "goodbye" 4))]
      (is (= "hello" (get merged 3)))
      (is (= "goodbye" (get merged 4)))))
  (testing "merge, two dimensions"
    (let
      [merged (merge-sparse-arrays
                (put (make-sparse-array :x :y) "hello" 3 4)
                (put (make-sparse-array :x :y) "goodbye" 4 3))]
      (is (= "hello" (get merged 3 4)))
      (is (= "goodbye" (get merged 4 3))))))

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


