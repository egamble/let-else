(ns let-else-test
  (:require [let-else :refer :all]
            [clojure.test :refer :all]))


(deftest else
  (are [vx vy r]
    (= r
       (let? [a 10
              x vx :else 1
              b 11
              y vy :else 2
              c 12]
         :body))
    
    nil   nil  1
    false nil  1
    3     nil  2
    3     4    :body))

(deftest else-nil
  (are [v r]
    (= r
       (let? [x v :else nil]
         :body))
    
    nil   nil
    1     :body))

(deftest else-context
  (are [v r]
    (= r
       (let [x 1]
         (let? [x v :else x]
           :body)))
    
    nil  1
    2    :body))

(deftest else-when-context
  (are [v r]
    (= r
       (let [x 1]
         (let? [x v :else x :when (odd? x)]
           :body)))
    
    2    2
    3    :body))

(deftest when
  (are [v r]
    (= r
       (let? [a 1
              x v :when (odd? x)
              b 2]
         :body))
    
    2    nil
    3    :body))

(deftest when-nil
  (are [v r]
    (= r
       (let? [x v :when nil]
         :body))
    
    1    nil
    nil  nil))


(deftest is-else
  (are [v r]
    (= r
       (let? [x v :else 1 :is not-empty]
         :body))
    
    ""   1
    "a"  :body))

(deftest is-not-else
  (are [v r]
    (= r
       (let? [x v :else 1 :is-not empty?]
         :body))
    
    ""   1
    "a"  :body))

(deftest is-is-not-when-else
  (are [v r]
    (= r
       (let? [x v :when (> x -1) :else 1 :is-not zero? :is even?]
         :body))
    
    0    1
    -2   1
    -3   1
    3    1
    2    :body))

(deftest is-truthy
  (are [v r]
    (= r
       (let? [x v :else 1 :is truthy]
         :body))
    
    nil   1
    false 1
    true  :body
    2     :body))

(deftest is-not-nil
  (are [v r]
    (= r
       (let? [x v :else 1 :is-not nil?]
         :body))
    
    nil   1
    false :body
    true  :body
    2     :body))

(deftest when-destructure
  (are [v r]
    (= r
       (let? [[a b] v :else 1 :when (< a b)]
         :body))
    
    [3 2] 1
    [2 3] :body))

(deftest is-destructure
  (are [v r]
    (= r
       (let? [[a b] v :else 1 :is (fn [[c d]] (< c d))]
         :body))
    
    [3 2] 1
    [2 3] :body))

(deftest delay
  (let [a (atom nil)]
    (let? [x (reset! a 1) :delay true]
      nil)
    (is (= @a nil))

    ;; :delay with other keywords is ignored
    (let? [x (reset! a 2) :delay true :else 10]
      nil)
    (is (= @a 2))

    (let? [x (reset! a 3) :delay true]
      x)
    (is (= @a 3))))
