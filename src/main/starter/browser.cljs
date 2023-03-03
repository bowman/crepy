(ns starter.browser
  (:require
   ["react" :as react]
   ["react-dom/client" :as react-dom]))

;; start is called by init and after code reloading finishes
(defn ^:dev/after-load start []
  (js/console.log "start"))

(defn init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (js/console.log "init")
  (let [app-node (.getElementById js/document "app")
        root (react-dom/createRoot app-node)]
    (.render root "Hello Tycho!" app-node))
  (start))

;; this is called before any code is reloaded
(defn ^:dev/before-load stop []
  (js/console.log "stop"))
