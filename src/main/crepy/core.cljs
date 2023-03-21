(ns crepy.core
  (:require
   [reagent.core :as reagent]
   [reagent.dom.client :as rdomc]
   ))

(defonce app-root (rdomc/create-root (.getElementById js/document "app")))
(defonce db (reagent/atom {:dir :none}))
(def log js/console.log)

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

(def num-notes 8) ; 0-based
(defn dir->note [dir shift-key]
  (when-let
   [base-note
    ({:down  0
      :left  1
      :up    2
      :right 3} dir)]
    (if shift-key (+ 4 base-note) base-note)))

(defn- setup-notes-rec [audio-context note notes]
  (let [tone (new js/window.OscillatorNode audio-context #js {"frequency" (note-hz note)})
        gain (new js/window.GainNode audio-context #js {"gain" 0})]
    ; tone -> gain -> destination (separately!)
    (.connect tone gain)
    (.connect gain (.-destination audio-context))
    (.start tone) ; always play, control with gain
    (let [new-notes (conj notes {:gain gain :tone tone :note note})]
      (if (= (inc note) num-notes)
        new-notes
        (setup-notes-rec audio-context (inc note) new-notes)))))

(defn- setup-notes [audio-context]
  (setup-notes-rec audio-context 0 []))

; Must be called after a user action, safe to repeat call
; :notes and ready :audio-context in @db
(defn- setup-sound []
  (when-not (:audio-context @db)
    (let [audio-context (new js/window.AudioContext)
          notes (setup-notes audio-context)]
      (swap! db #(assoc % :notes notes :audio-context audio-context))))

  (let [{audio-context :audio-context} @db]
    (when (= (.-state audio-context) "suspended") ; start or wake-up each time
      (.resume audio-context))))

(defn- start-sound [note]
  (setup-sound)
  (let [{notes :notes} @db
        {gain :gain} (notes note)]
    (set!  (.-value (.-gain gain)) 1.0)
  ))

(defn- stop-sound [note]
  (let [{notes :notes} @db
        {gain :gain} (notes note)]
    (set!  (.-value (.-gain gain)) 0.0)
  ))

(defn log-touch [e]
  (log e)
  (log (.-changedTouches e))
  (log (.-target e))
  (.stopPropagation e)
  )
(defn start-touch [dir]
  (fn [e]
    (log "Start "dir)
    (start-sound (dir->note dir (.-shiftKey e)))
    (swap! db #(assoc % :dir dir))
    #_(log-touch e)))
(defn end-touch [dir]
  (fn [e]
    (log "end" dir)
    (stop-sound (dir->note dir (.-shiftKey e)))
      (swap! db #(assoc % :dir :none))
    #_(log-touch e)))

(defn app []
  [:div {:id "div-img"}
   [:img {:src (str "images/" (dir2fn (:dir @db)) ".jpg")
          :class "crepe" }]
  [:svg {:view-box "0 0 756 1008"}:on-touch-start log-touch
   [:circle {:id "oj" :on-touch-start log-touch :cx 580 :cy 230 :r 90 :fill "orange"}]
   [:rect {:id "r-down"  :on-touch-start (start-touch :down) :on-touch-end (end-touch :down)
           :x 376 :y 600 :width 300 :height 300 :fill "green" :transform "rotate(45, 376, 600)"}]
   [:rect {:id "r-left"  :on-touch-start (start-touch :left) :on-touch-end (end-touch :left)
           :x 376 :y 600 :width 300 :height 300 :fill "blue" :transform "rotate(135, 376, 600)"}]
   [:rect {:id "r-up"    :on-touch-start (start-touch :up) :on-touch-end (end-touch :up)
           :x 376 :y 600 :width 300 :height 300 :fill "red" :transform "rotate(225, 376, 600)"}]
   [:rect {:id "r-right" :on-touch-start (start-touch :right) :on-touch-end (end-touch :right)
           :x 376 :y 600 :width 300 :height 300 :fill "purple" :transform "rotate(315, 376, 600)"}]
   [:circle {:id "egg" :on-touch-start log-touch :cx 376 :cy 600 :r 90 :fill "yellow"}]]
   ])


(def key->dir
  {"ArrowUp" :up
   "ArrowDown" :down
   "ArrowLeft" :left
   "ArrowRight" :right})

(defn keydown [e]
  (.preventDefault e)
  (let [key (.-key e)
        shift-key (.-shiftKey e)
        new-dir (key->dir key)]
    (when new-dir
      (start-sound (dir->note new-dir shift-key))
      (swap! db #(assoc % :dir new-dir))))
  )

(defn keyup [e]
  (.preventDefault e)
  (let [key (-> e .-key)
        shift-key (.-shiftKey e)
        new-dir (key->dir key)]
    (when new-dir
      (stop-sound (dir->note new-dir shift-key))
      (swap! db #(assoc % :dir :none)))))

;; start is called by init and after code reloading finishes
(defn ^:dev/after-load start []
  (set! (.-onkeydown js/document) keydown)
  (set! (.-onkeyup js/document) keyup)

  (rdomc/render app-root [app]))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (log "init")
  (start))

;; this is called before any code is reloaded
(defn ^:dev/before-load stop []
  (let [{audio-context :audio-context} @db]
    (when audio-context
      (.close audio-context)
      (reset! db {:dir :none}))) ; release audio system resources?
  (log "stop" @db))

(comment
  (def nn ((:notes @db) 0))
  (set! js/window.nn nn)
  :rcf)

(identity [stop]) ; shush warnings
