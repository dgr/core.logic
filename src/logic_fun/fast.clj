(ns logic-fun.fast
  (:use [clojure.pprint :only [pprint]]))

;; Notes on how substitution works
;; 1) search until we find a value
;; 2) disallow addition substitutions that go back to the var we're looking for

(defn vec-last [v]
  (nth v (dec (count v))))

(deftype lvarT [])

(defn lvar? [x]
  (instance? lvarT x))

(defn lvar [] (lvarT.))

(defprotocol ISubstitutions
  (length [this])
  (ext [this s])
  (lookup [this v]))

(defn lookup* [ss v]
  (loop [v v a (vec-last (ss v)) ss ss]
    (cond
     (nil? a)  v
     (lvar? a) (recur a (vec-last (ss a)) ss)
     :else a)))

(defrecord Substitutions [ss order]
  ISubstitutions
  (length [this] (count order))
  (ext [this [a b :as s]]
       ;; check that we don't come back to the same var
       (Substitutions. (update-in ss [a] (fnil conj []) b)
                       (conj order a)))
  (lookup [this v]
          (lookup* ss v)))

(comment
  ;; remember backwards for us since we're using vectors
  [[x 1] [y 5] [x y]]
  
 ;; test
 (let [x  (lvar)
       y  (lvar)
       z  (lvar)
       ss (Substitutions. {x [1 y]
                           y [5]}
                          [x y x])
       ss (ext ss [x z])]
   (pprint ss)
   (println (length ss)))

 ;; not bad 500ms
 (dotimes [_ 10]
   (let [[x y z] [(lvar) (lvar) (lvar)]
         ss (Substitutions. {x [y 1] y [5]} [x y x])
         s  [x z]]
     (time
      (dotimes [_ 1e6]
        (ext ss s)))))

 ;; ~300ms, pretty good, neet to compare to naive
 (dotimes [_ 10]
   (let [[x y z] [(lvar) (lvar) (lvar)]
         ss (Substitutions. {x [y] y [z] z [5]} [x y z])]
     (time
      (dotimes [_ 1e6]
        (lookup ss x)))))

 ;; erg before we get ahead of ourselves
 ;; preventing circularity
)

;; we could store all vars in a map
(comment
  {'x [y]
   'y [5 1]}
  )

;; but what happens when we pop a substitution off ?
(comment
  ;; represents the above
  [[x y] [5 y] [y 1]]

  ;; we could construct the map each time, probably expensive
  ;; as declaring logic vars is quite common

  ;; but what would a better data structure look like
  ;; that allows us to preserve the backtracking component ?

  ;; we could keep a list of the current substitutions
  {'x [y]
   'y [5 1]}

  ;; subst-order
  ['x 'y 'y]

  ;; do we need to make a custom data structure for this?

  ;; we could hide it inside a datatype ?
  ;; since methods are already synchronized and they are opaque
  )

;; one holds the order
(def subst-order (ref '()))
o;; the other holds the mappings
(def substs (ref '{}))

(def empty-s [])

;; we could keep association lists for each var, refs
;; then we wouldn't have to walk the whole shebang

(defn walk [v s]
  (cond
   (lvar? v) (let [[_ rhs :as a] (some #(= v (first %)) s)]
               (cond
                a (walk rhs s)
                :else v))
   :else v))