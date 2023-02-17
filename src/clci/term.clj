(ns clci.term
  "Utilities for the terminal.")

;;
;; Set colors for text in the terminal
;; Based on: https://github.com/trhura/clojure-term-colors by github user 'trhura'
;; published under the Eclipse Public License.
;;

(defn- escape-code
  "Generate the escape code for a specific color."
  [i]
  (str "\033[" i "m"))

(def colors'
  "All available colors by their name."
  [:grey :red :green :yellow :blue :magenta :cyan :white])

(def colors
  "Color map for text."
  (zipmap colors'
          (map escape-code
               (range 30 38))))

(def highlights
  "Color map for text highlights."
  (zipmap colors'
          (map escape-code
               (range 40 48))))

(def reset
  "Escape code"
  (escape-code 0))

(defn with-c
  "Build a text string with a specific color.
	Resets to the default color after the text if not explicitly forbidden."
  ([c s] (with-c c s false))
  ([c s no-reset] (if no-reset (str (get colors c) s) (str (get colors c) s reset))))

(defn with-h
  "Build a text string with a specific highlight color.
	Resets to the default color after the text if not explicitly forbidden."
  ([c s] (with-h c s false))
  ([c s no-reset] (if no-reset (str (get highlights c) s) (str (get highlights c) s reset))))

(defn grey
  "Shorthand for `(with-color :grey s)`."
  ([s] (with-c :grey s))
  ([s no-reset] (with-c :grey s no-reset)))

(defn red
  "Shorthand for `(with-color :red s)`."
  ([s] (with-c :red s))
  ([s no-reset] (with-c :red s no-reset)))

(defn green
  "Shorthand for `(with-color :green s)`."
  ([s] (with-c :green s))
  ([s no-reset] (with-c :green s no-reset)))

(defn yellow
  "Shorthand for `(with-color :yellow s)`."
  ([s] (with-c :yellow s))
  ([s no-reset] (with-c :yellow s no-reset)))

(defn blue
  "Shorthand for `(with-color :blue s)`."
  ([s] (with-c :blue s))
  ([s no-reset] (with-c :blue s no-reset)))

(defn magenta
  "Shorthand for `(with-color :magenta s)`."
  ([s] (with-c :magenta s))
  ([s no-reset] (with-c :magenta s no-reset)))

(defn cyan
  "Shorthand for `(with-color :cyan s)`."
  ([s] (with-c :cyan s))
  ([s no-reset] (with-c :cyan s no-reset)))

(defn white
  "Shorthand for `(with-color :white s)`."
  ([s] (with-c :white s))
  ([s no-reset] (with-c :white s no-reset)))
