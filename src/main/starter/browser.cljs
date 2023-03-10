(ns starter.browser
  (:require
  ;;  ["react" :as react]
  ;;  [reagent.core :as r]
  ;;  [reagent.dom :as rdom]
   [reagent.dom.client :as rdomc]
  ;;  ["react-dom/client" :as react-dom]
   ))

;; start is called by init and after code reloading finishes
(defn ^:dev/after-load start []
  (js/console.log "start"))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (js/console.log "init")
  (let [app-node (.getElementById js/document "app")
        root (rdomc/create-root app-node)]
    (rdomc/render root
                  [:div
                   [:h1 "Hello World"]
                   [:img {:src "/images/closed.jpg" :width 200}]
                   "bye"]))
  (start))

;; this is called before any code is reloaded
(defn ^:dev/before-load stop []
  (js/console.log "stop"))

(identity [init stop])
