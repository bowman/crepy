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

(defn note-hz
  "convert note n C4=0, A4=5=440hz C5=7 to Hz values"
  ;; https://en.wikipedia.org/wiki/Piano_key_frequencies
  [n]
  (let [piano-map [:c4 40 :d4 42 :e4 44 :f4 45 :g4 47 :a5 49 :b5 51 :c5 52]
        key-num (filterv number? piano-map)
        k   (key-num n)]
    (* 440 (js/Math.pow 2 (/ (- k 49) 12)))))

(comment
  (note-hz 5) ; A4=440
  (note-hz 0) ; C4=261.6..
  (note-hz 7) ; C5=523.2511
  :rcf)

(defn- start-sound [_]
  ;; (when-not (:ac @db)
  ;;   (swap! db #(assoc db :ac (new js/window.AudioContext))))
  (when-not (:audio-context @db)
    (let [audio-context (new js/window.AudioContext)
          tone-node (new js/window.OscillatorNode audio-context)]
      ;; (.connect tone-node (.-destination audio-context))
      (.start tone-node)
      (swap! db #(assoc % :tone tone-node :audio-context audio-context))))

  (let [audio-context  (:audio-context @db)
        tone (:tone @db)
        audio-state (.-state audio-context)]
    (when (= audio-state "suspended") ; start or wake-up
      (.resume audio-context)) 
    (.connect tone (.-destination audio-context)))
  ;; (.start (:tone @db))
  )

(defn- stop-sound []
  ;; (.stop (:tone @db))
   (let [audio-context  (:audio-context @db)
         tone (:tone @db)]
     (.disconnect tone (.-destination audio-context)))
  )

(defn app []
  [:div
   [:img {:src ;"/images/closed.jpg"
          (str "/images/" (dir2fn (:dir @db)) ".jpg")
          :width "100%"
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
      (start-sound new-dir)
      (swap! db #(assoc % :dir new-dir))))
  )

(defn keyup [e]
  ;; (js/console.log e)
  (stop-sound)
  (swap! db #(assoc % :dir :none)))

;; start is called by init and after code reloading finishes
(defn ^:dev/after-load start []
  (js/console.log "start" db)
  (set! (.-onkeydown js/document) keydown)
  (set! (.-onkeyup js/document) keyup)

  (rdomc/render app-root [app]))

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
