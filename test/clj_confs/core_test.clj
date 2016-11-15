(ns clj-confs.core-test
  (:require [clojure.test :refer :all]
            [clj-confs.core :as sut]))

(defn temp-path
  []
  (.getAbsolutePath (java.io.File/createTempFile "temp" "")))

(deftest load-confs-with-nothing
  (let [conf (sut/load)]
    (is (thrown? AssertionError (conf :no-keys-in-empty-conf)))))

(deftest test-load-confs
  (let [path (temp-path)
        data {:a "foo"
              :b {:c "blah"}
              :d nil
              :e false}
        _ (spit path (str data))
        conf (sut/load path)]
    (testing "nil is illegal as a value"
      (is (thrown? AssertionError (conf :d))))
    (testing "false is fine as a value"
      (is (= false (conf :e))))
    (testing "keys"
      (is (= (conf :a) "foo")))
    (testing "nested keys"
      (is (= (conf :b :c) "blah")))
    (testing "nil values, like a key miss, throws an error"
      (is (thrown? AssertionError (conf :missing)))
      (is (thrown? AssertionError (conf :b :missing))))
    (testing "conf data is attached to meta"
      (is (= [data] (-> conf meta :confs))))))

(deftest test-load-confs-edn-str
  (let [path (temp-path)
        data {:a :default-val}]
    (spit path (str data))
    (testing "overriding a value from an edn string"
      (let [conf (sut/load "{:a :new-val}" path)]
        (is (= :new-val (conf :a)))))
    (testing "edn strings which read to nil are ignored"
      (let [conf (sut/load "   " path)]
        (is (= :default-val (conf :a)))))))

(deftest test-load-confs-multiple-paths
  (let [extra-conf (temp-path)
        base-conf (temp-path)]
    (testing "confs provided are searched left to right for values"
      (spit base-conf (str {:a :default-val}))
      (spit extra-conf (str {:a :new-val}))
      (let [conf (sut/load extra-conf base-conf)]
        (is (= :new-val (conf :a)))))
    (testing "the first value can be an edn-str"
      (spit base-conf (str {:a :default-val}))
      (spit extra-conf (str {:a :new-val}))
      (let [conf (sut/load "{:a :newest-val}" extra-conf base-conf)]
        (is (= :newest-val (conf :a)))))
    (testing "extra confs must be overriding values already defined in the last (base) conf"
      (spit extra-conf (str {:unknown-key :not-gonna-work}))
      (is (thrown? AssertionError (sut/load extra-conf base-conf))))))

(deftest load-just-empty-str
  (let [conf (sut/load "")]
    (is (thrown? AssertionError (conf :literally-anything)))))

(deftest test-keys-in
  (is (= [[1 2]
          [1 4]
          [1 6 7]]
         (sut/-keys-in {1 {2 :val
                           4 :val
                           6 {7 :val}}}))))
