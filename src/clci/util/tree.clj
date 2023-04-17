(ns clci.util.tree
  "Tools to work with generic trees and specialized parser trees as returned from instaparse."
  (:require
    [clci.util.core :refer [mapv-r any]]))


;;
;; Generic tree manipulation functions ;;
;;

(defn leaf?
  "Test if the given `node` is a leaf."
  [node]
  (and (some? node) (not (coll? node))))


(defn- dissoc-nodes-impl
  "Implementation of `dissoc-nodes`."
  [node p]
  (if (leaf? node)
    node
    (when-not (p node)
      (mapv-r (fn [i-node] (dissoc-nodes-impl i-node p)) node nil?))))


(defn dissoc-nodes
  "Remove a node from an arbitrary tree.
   Takes a `tree` with a single root node and a predicate `p`.
   The predicate takes a tree node as argument.
   Traverses the tree depth-first and removes all nodes where `(p node)` is true."
  [tree p]
  (cond
    (nil? tree) nil
    ;; not a valid tree
    (not (coll? tree))
    nil
    ;; empty tree
    (empty? tree)
    '()
    ;; recursively dissoc node from tree
    :else
    (dissoc-nodes-impl tree p)))


(defn- update-nodes-impl
  "Implementation of `update-nodes`."
  [node f p]
  (let [keep-or-update (fn [n] (if (p n) (f n) n))]
    (if (leaf? node)
      (keep-or-update node)
      (map (fn [n] (update-nodes-impl n f p)) (keep-or-update node)))))


(defn update-nodes
  "Update the nodes of a tree.
   Takes a generic `tree`, a function `f` and an optional predicate `p`.
   Traverses the tree in depth-first left-right order. For each element,
   that are full nodes and leafs, applies test `(p node)` and if the test yields
   true, then applies `(f node)`. Applies `f` on nodes before traversing them
   recursively."
  ([tree f] (update-nodes tree f (fn [_] true)))
  ([tree f p]
   (cond
     (nil? tree) nil
     ;; not a valid tree
     (not (coll? tree))
     nil
     ;; empty tree
     (empty? tree)
     '()
     ;; recursively dissoc node from tree
     :else
     (update-nodes-impl tree f p))))


(comment
  "Example how to use update-nodes.
   Update a tree with nodes that have non homogenous typed values.
   Also the tree is very unbalanced."
  
  (def int-ast
    '(:AST (:A 1 2 3 (:B 4 5) (:C (:D 7 8) 6))))

  (update-nodes int-ast (fn [n] (cond 
                                  (int? n) (inc n) 
                                  (keyword? n) (keyword (str (name n) "'")) 
                                  :else n)))
)


(defn conj-node-first
  "Add a new new node to a tree.
   Takes a `tree`, a predicate `p` and a `node`.
   Traverses the tree in depth-first left-right order. If the given predicate
   `p` evaluates true for a tree node, then the given `node` is inserted at
   the start of the tree node.
   !!! example
      
      ```clojure
      (def tree '(:ROOT (:A 1 2 3 (:B 4 5) (:C (:D 7 8) 6))))
   
      (def p? (fn [node] (when (coll? node) (when (= :B (first node))))))

      (conj-node-first tree p? '(:NEW 11 13 17))
      ;; -> (:ROOT (:A 1 2 3 ((:NEW 11 13 17) :B 4 5) (:C (:D 7 8) 6)))
      ```
   "
  [tree p node]
  (let [f (fn [tree-node]
            (concat [node] tree-node))]
    (update-nodes tree f p)))


(comment
  "Example how to use `conj-node-first`:"
  (def tree '(:ROOT (:A 1 2 3 (:B 4 5) (:C (:D 7 8) 6))))

  (def p? (fn [node] (when (coll? node) (= :B (first node)))))

  (conj-node-first tree p? '(:NEW 11 13 17))
)


(defn conj-node-last
  "Add a new new node to a tree.
   Takes a `tree`, a predicate `p` and a `node`.
   Traverses the tree in depth-first left-right order. If the given predicate
   `p` evaluates true for a tree node, then the given `node` is inserted at
   the end of the tree node.
   !!! example
      
      ```clojure
      (def tree '(:ROOT (:A 1 2 3 (:B 4 5) (:C (:D 7 8) 6))))
   
      (def p? (fn [node] (when (coll? node) (when (= :B (first node))))))

      (conj-node-last tree p? '(:NEW 11 13 17))
      ;; -> (:ROOT (:A 1 2 3 (:B 4 5 (:NEW 11 13 17)) (:C (:D 7 8) 6)))
      ```
   "
  [tree p node]
  (let [f (fn [tree-node]
            (concat tree-node [node]))]
    (update-nodes tree f p)))


(comment
  "Example how to use `conj-node-last`:"
  (def tree '(:ROOT (:A 1 2 3 (:B 4 5) (:C (:D 7 8) 6))))

  (def p? (fn [node] (when (coll? node) (= :B (first node)))))

  (conj-node-last tree p? [:NEW 11 13 17])
)


(defn conj-node-after
  "Add a new new node to a tree.
   Takes a `tree`, a predicate `p` and a `node`.
   Traverses the tree in depth-first left-right order. If the given predicate
   `p` evaluates true for a tree node, then the given `node` is inserted after
   the tree node.
   !!! example
      
      ```clojure
      (def tree '(:ROOT (:A 1 2 3 (:B 4 5) (:C (:D 7 8) 6))))
   
      (def p? (fn [node] (= node :B)))

      (conj-node-after tree p? [:NEW 11 13 17])
      ;; -> (:ROOT (:A 1 2 3 (:B (:NEW 11 13 17) 4 5) (:C (:D 7 8) 6)))
      ```
   "
  [tree p node]
  (let [p-parent (fn [p-node] (any p p-node))
        f (fn [tree-node]
            ;; tree-node
            (if (leaf? tree-node)
              tree-node ; TODO
              (loop [head       (first tree-node)
                     tail       (rest tree-node)
                     tree-node' []]
                (cond
                  ;; all nodes visited in current tree-node
                  (nil? head)
                  tree-node'
                  ;; current visiting node is a match, add the new node after and recur
                  (p head)
                  (recur (first tail) (rest tail) (conj tree-node' head node))
                  ;; no match, keep node and continue
                  :else
                  (recur (first tail) (rest tail) (conj tree-node' head))))))]
    (update-nodes tree f p-parent)))


(comment
  "Example how to use `conj-node-last`:"
  (def tree '(:ROOT (:A 1 2 3 (:B 4 5) (:C (:D 7 8) 6))))

  (def p? (fn [node] (= node :B)))

  (conj-node-after tree p? [:NEW 11 13 17])
)


;;
;; Tree manipulation functions specific for instaparse ASTs ;;
;;

(comment
	(def example-ast
	  "An example AST."
	  [:AST
	   [:PREAMBLE
	    [:PARAGRAPH
	     [:TEXT "All notable changes to this project will be documented in this file."]]
	    [:PARAGRAPH
	     [:TEXT "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)."]]]
	   [:UNRELEASED
	    [:OTHER
	     [:SHORT-DESC
	      [:TEXT "update build pipeline for releases "]
	      [:ISSUE-REF [:ISSUE-ID "11"]]]]]
	   [:RELEASE
	    [:RELEASE-HEAD
	     [:RELEASE-TAG "0.17.0"]
	     [:DATE "2023-04-14"]]
	    [:ADDED
	     [:LONG-DESC
	      [:PARAGRAPH
	       [:TEXT "new git hook mechanism "]
	       [:ISSUE-REF [:ISSUE-ID "96"]]]
	      [:PARAGRAPH
	       [:TEXT "This commit adds a new mechanism how to use git hooks in projects using clci. A hook will send a trigger which is then processed using the clci workflow mechanism."]]
	      [:PARAGRAPH
	       [:TEXT "And one more line with a linebreak."]
	       [:TEXT "So now what?"]]]]]])
  )


(defn ast-node-type=
  "Test if a given `node` of an AST is of specific type `t`."
  [node t]
  (when (coll? node)
    (= t (first node))))


(defn ast-update-nodes-with-type
  "Update an AST nodes with a specific type.
   Takes an `ast` as returned from instaparse, a function `f` used to
   update the tree nodes and the `node-type` that specifies the node type
   that will be updated.
   Uses `update-nodes` internally to traverse and update the tree."
  [ast f node-type]
  (let [p (fn [node]
            (cond
              (nil? node) false
              (coll? node) (= node-type (first node))
              :else false))]
    (update-nodes
      ast
      f
      p)))


(comment
  "Example of `ast-update-nodes-with-type` to replace
   the body of all `:TEXT` nodes with \"Hodor\"."
  (ast-update-nodes-with-type
    example-ast 
    (fn [_] [:TEXT "Hodor"]) :TEXT)
)


(defn ast-dissoc-nodes
  "Remove nodes from an AST.
   Takes an `ast` as returned by instaparse and the `node-type` as keyword
   that will be removed from the tree.
   Uses the generic `dissoc-nodes` function."
  [ast node-type]
  (dissoc-nodes
    ast
    (fn [node]
      (= (first node) node-type))))


(comment
  "Example how to use `ast-dissoc-node`."
  (ast-dissoc-nodes example-ast :UNRELEASED)
)


(defn ast-insert-after
  "Insert a new node into an AST.
   Takes an `ast` as returned by instaparse and the `after-node` which is the 
   keyword identifying the type of a node. Also takes the `node` that will be 
   inserted into the tree after the node matching the `after-node` identifier.
   !!! example
      
      Add a new Release node after the Unreleased node:
      ```clojure
      (def new-release 
        [:RELEASE [:RELEASE-HEAD [:RELEASE-TAG \"0.17.0\"] [:DATE \"2023-04-14\"]]
          [:ADDED [:SHORT-DESC [:TEXT \"implements tree manipulation for abstract syntax trees.\"]]]])
   
      (ast-insert-after example-ast :UNRELEASED new-release)
      ```"
  [ast after-node node]
  (conj-node-after
    ast
    (fn [n]
      (when (coll? n)
        (= after-node (first n))))
    node))


(comment
  "Example how to use `ast-insert-after`:"
  (def new-release 
        [:RELEASE [:RELEASE-HEAD [:RELEASE-TAG "0.123.0"] [:DATE "2023-04-26"]]
          [:ADDED [:SHORT-DESC [:TEXT "implements tree manipulation for abstract syntax trees."]]]])
  
  (ast-insert-after example-ast :UNRELEASED new-release)
  )


(defn- ast-contains?-node-impl
  "Implementation of `ast-contains-node?`."
  [ast p]
  (if (leaf? ast)
    false
    (or
      (p ast)
      (any true? (map (fn [node] (ast-contains?-node-impl node p)) (rest ast))))))


(defn ast-contains-node?
  "Test if a given `ast` contains a node of type `node-type`.
   Traverses the tree in depth-first left-right order.
   **Note**: Traverses all nodes before returning."
  [ast node-type]
  (let [p (fn [node] (when (coll? node) (= node-type (first node))))]
    (cond
      (nil? ast) nil
      ;; not a valid tree
      (not (coll? ast))
      nil
      ;; empty tree
      (empty? ast)
      '()
      ;; recursively dissoc node from tree
      :else
      (ast-contains?-node-impl ast p))))


(comment
  "Example how to use `ast-contains-node?`:"
	(ast-contains-node? example-ast :RELEASE)
  ;; -> true
  (ast-contains-node? example-ast :HODOR)
  ;; -> false
  )


(defn- nil-or-empty?
  "Test if given element `e` is nil or an empty collection."
  [e]
  (cond
    (nil? e) true
    (empty? e) true
    :else false))


(defn- ast-get-node-impl
  "Implementation of `ast-get-node`."
  [ast p]
  (cond
    ;; leaf node, can stop here
    (leaf? ast) nil
    ;; current node is the one to get
    (p ast) ast
    ;; continue recursively with children
    :else
    (first (mapv-r (fn [n] (ast-get-node-impl n p)) ast nil-or-empty?))))


(defn ast-get-node
  "Get the node of a given `ast` as produced by instaparse matching the `node-type`.
   Returns the first element found. Traverses the tree in a depth-first
   left-to-right order."
  [ast node-type]
  (let [p (fn [node] (when (coll? node) (= node-type (first node))))]
    (cond
      (nil? ast) nil
      ;; not a valid tree
      (not (coll? ast))
      nil
      ;; empty tree
      (empty? ast)
      '()
      ;; recursively dissoc node from tree
      :else
      (ast-get-node-impl ast p))))


(comment
  "Example how to use `ast-get-node`:"
  (ast-get-node example-ast :OTHER))
