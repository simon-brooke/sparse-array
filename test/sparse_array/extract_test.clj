(ns sparse-array.extract-test
  (:require [clojure.test :refer :all]
            [sparse-array.core :refer [dense-to-sparse get]]
            [sparse-array.extract :refer :all]))


(deftest sparse-tests
  (testing "extraction from sparse array"
    (let  [dense [[[1 2 3][:one :two :three]["one" "two" "three"]]
                  [[1 :two "three"]["one" 2 :three][:one "two" 3]]
                  [[1.0 2.0 3.0][2/2 4/2 6/2]["I" "II" "III"]]]
           sparse (dense-to-sparse dense '(:a :b :c))
           integers (extract sparse integer?)
           strings (extract sparse string?)
           keywords (extract sparse keyword?)
           threes (extract sparse #(if
                                     (number? %)
                                     (= % 3)
                                     (= (name %) "three")))]
      (map
        #(let [expected nil
              actual (get (extract sparse map?)  %1 %2 %3)]
          (is (= actual expected) "there are no cells of which `map?` is true"))
        (range 3)
        (range 3)
        (range 3))
      (let [expected 1
            actual (get integers 0 0 0)]
        (is (= actual expected)))
      (let [expected nil
            actual (get keywords 0 0 0)]
        (is (= actual expected)))
      (let [expected nil
            actual (get strings 0 0 0)]
        (is (= actual expected)))
      (let [expected nil
            actual (get threes 0 0 0)]
        (is (= actual expected)))
      (let [expected 1
            actual (get integers 0 0 2)]
        (is (= actual expected)))
      (let [expected nil
            actual (get keywords 0 0 2)]
        (is (= actual expected)))
      (let [expected "three"
            actual (get strings 0 0 2)]
        (is (= actual expected)))
      (let [expected "three"
            actual (get threes 0 0 2)]
        (is (= actual expected))))))

(deftest dense-tests
  (testing "extraction from dense array"
    (let  [dense [[[1 2 3][:one :two :three]["one" "two" "three"]]
                  [[1 :two "three"]["one" 2 :three][:one "two" 3]]
                  [[1.0 2.0 3.0][2/2 4/2 6/2]["I" "II" "III"]]]
           integers (extract dense integer?)
           strings (extract dense string?)
           keywords (extract dense keyword?)
           threes (extract dense #(if
                                     (number? %)
                                     (= % 3)
                                     (= (name %) "three")))]
      (map
        #(let [expected nil
              actual (get (extract dense map?)  %1 %2 %3)]
          (is (= actual expected) "there are no cells of which `map?` is true"))
        (range 3)
        (range 3)
        (range 3))
      (let [expected 1
            actual (get integers 0 0 0)]
        (is (= actual expected)))
      (let [expected nil
            actual (get keywords 0 0 0)]
        (is (= actual expected)))
      (let [expected nil
            actual (get strings 0 0 0)]
        (is (= actual expected)))
      (let [expected nil
            actual (get threes 0 0 0)]
        (is (= actual expected)))
      (let [expected 1
            actual (get integers 0 0 2)]
        (is (= actual expected)))
      (let [expected nil
            actual (get keywords 0 0 2)]
        (is (= actual expected)))
      (let [expected "three"
            actual (get strings 0 0 2)]
        (is (= actual expected)))
      (let [expected "three"
            actual (get threes 0 0 2)]
        (is (= actual expected))))))
