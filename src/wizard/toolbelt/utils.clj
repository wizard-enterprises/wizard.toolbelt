(ns wizard.toolbelt.utils
  (:require [clojure.string :as str]
            [tupelo.core :refer [append glue fetch-in drop-at]]))

(defn glue+
  [& xs]
  (let [list? (every? (some-fn list? seq?) xs)
        xs (filter some? xs)
        glued (if (empty? xs) [] (apply glue xs))]
    (if list?
      (into () xs)
      glued)))

(defn index-by
  [list-of-maps id]
  (into
   {}
   (map (juxt id identity)
        list-of-maps)))

(defn not-
  [pred & args]
  (apply (complement pred) args))

(defn- intern-item-from
  [here-ns ns name item]
  (let [item-meta (try (meta item) (catch Exception e {}))
        item      (deref item)]
    (intern (ns-name here-ns)
            (with-meta name item-meta)
            item)))

(defn intern-all-from
  [here-ns ns]
  (doseq [[name val] (ns-publics ns)]
    (intern-item-from here-ns ns name val)))

(defn contains-all?
  [m & xs]
  (every? (partial contains? m) xs))

(defmacro def-
  [sym value]
  `(def ~(with-meta sym {:private true}) ~value))

(defn key-by-value [m value]
  (some (fn [[k v]] (if (= v value) k)) m))

(defn remove-trailing-slash
  [uri]
  (if (every? #(% uri "/") [not= str/ends-with?])
    (subs uri 0 (dec (count uri)))
    uri))

(defn deep-merge
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (if (every? coll? maps)
      (let [colls (filter #(not- empty? %) maps)]
        (if (= 1 (count colls))
          (first colls)
          (apply glue+ colls)))
      (last maps))))

(defn contains-in?
  [m keyseq]
  (let [not-found (Object.)]
    (not= not-found (get-in m keyseq not-found))))

(defmacro if-let*
  ([bindings then]
   `(if-let* ~bindings ~then nil))
  ([bindings then else]
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-let* ~(drop 2 bindings) ~then ~else)
        ~else)
     then)))

(defn =either
  [thing & options]
  (some #(= thing %) options))

(defn ns-hash-map
  [ns]
  (into {} (ns-publics ns)))

(defn update-last
  [coll f & args]
  (append (or (butlast coll) [])
          (apply f (last coll) args)))

(defn subtract-lists
  [l1 l2]
  (let [a (group-by identity l1)
        b (group-by identity l2)]
    (mapcat #(repeat
              (-
               (count (second %))
               (count (get b (key %))))
              (key %)) a)))

(defn that-is [pred thing]
  (if (pred thing) thing nil))

(defn that-is-not [pred thing]
  (if-not (pred thing) thing nil))

(defn- walk-some
  ([pred data]
   (walk-some pred data ::nil))
  ([pred data default]
   (try
     (clojure.walk/postwalk
      #(if (pred %)
         (throw (ex-info "!!Found!!" {:data %}))
         %)
      data)
     default
     (catch clojure.lang.ExceptionInfo e
       (if (= "!!Found!!" (ex-message e))
         (:data (ex-data e))
         (throw e))))))

(defn find-walk
  [pred data]
  (let [walked (walk-some pred data ::not-found)]
    (if (= ::not-found walked)
      (throw (ex-info "Not found in walk" {}))
      walked)))

(defn call-fn-in
  [x key-path & args]
  (apply (fetch-in x key-path) args))

(defn index-of
  [x v]
  (->> v
       (map-indexed #(first {%1 %2}))
       (some #(when (if (fn? x)
                      (= true (x (val %)))
                      (= x (val %)))
                (key %)))))

(defn without-first
  [coll x]
  (let [index (index-of x coll)]
    (if (nil? index)
      coll
      (drop-at coll (index-of x coll)))))

(defn px->rem
  ([px]
   (px->rem 16 px))
  ([rem-base px]
   (-> (re-matches #"^(\d+)px$" px)
       second
       read-string
       (/ rem-base)
       float
       (str "rem"))))

(defn element-tag
  [tag]
  (->> tag first name (re-matches #"^(.+?)(?:[\.\#].+)?$") second keyword))

(defn is-tag?
  [x tag]
  (= tag (element-tag x)))

(defn merge-attrs
  [attrs-base attrs]
  (let [class (get attrs "class")
        style (get attrs "style")
        attrs (dissoc attrs "class" "style")]
    (cond-> attrs
      true          (merge attrs-base attrs)
      (some? class) (update "class"
                            #(->> [% class] (filter some?) (str/join " ")))
      (some? style) (update "style" deep-merge style))))

(defn current-context-class-loader
  ([] (current-context-class-loader (Thread/currentThread)))
  ([thread] (.getContextClassLoader thread)))
