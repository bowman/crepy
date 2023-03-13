(ns starter.browser
  (:require
   [reagent.core :as reagent]
   [reagent.dom.client :as rdomc]
   ))

(defonce app-root (rdomc/create-root (.getElementById js/document "app")))
(defonce db (reagent/atom {:dir :none}))

(def dir2fn
  {:none "closed"
   :left "left"
   :right "right"
   :up "up"
   :down "down"})

(defn boo []
  [:div
   [:img {:src ;"/images/closed.jpg"
          (str "/images/" (dir2fn (:dir @db)) ".jpg")
          :width 300
          :on-click (fn [e]
                      (js/console.log e)
                      (js/console.log db))
          }]])

(defn keydown [e]
  ;; (js/console.log e)
  (let [key->dir {"ArrowUp" :up
                  "ArrowDown" :down
                  "ArrowLeft" :left
                  "ArrowRight" :right}
        key (-> e .-key)
        new-dir (key->dir key)]
    (when new-dir
      (js/console.log new-dir)
      (swap! db #(assoc % :dir new-dir))))
  )

(defn keyup [e]
  ;; (js/console.log e)
  (swap! db #(assoc % :dir :none)))

;; start is called by init and after code reloading finishes
(defn ^:dev/after-load start []
  (js/console.log "start" db)
  (set! (.-onkeydown js/document) keydown)
  (set! (.-onkeyup js/document) keyup)
  (rdomc/render app-root [boo]))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (js/console.log "init")
  (start))

;; this is called before any code is reloaded
(defn ^:dev/before-load stop []
  (js/console.log "stop"))

(identity [init stop])
