(ns merpg.pxart.colors )

(defn number->color [n]
  {:pre [(<= 0 n 3)]}
  (case n
    0 "#000000"
    1 "#444444"
    2 "#888888"
    3 "#FFFFFF"
    :else "#FF0000"))
