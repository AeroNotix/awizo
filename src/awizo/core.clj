(ns awizo.core
  (:require [clojure.core.async :as async]
            [clojure.core.cache :as cache])
  (:import [java.nio.file StandardWatchEventKinds])
  (:import [java.nio.file WatchEvent$Kind])
  (:import [java.util Timer])
  (:import [java.util TimerTask]))


(def periodicity (long 5000))
(def CREATE StandardWatchEventKinds/ENTRY_CREATE)
(def DELETE StandardWatchEventKinds/ENTRY_DELETE)
(def MODIFY StandardWatchEventKinds/ENTRY_MODIFY)


(defn make-ref []
  (. clojure.lang.RT (nextID)))

(defn schedule-task [timer task]
  (swap!
    timer
    (fn [t]
      (doto t
        (.scheduleAtFixedRate task periodicity periodicity)))))

(defn poll [watch c]
  (if-let [watch-key (.poll watch)]
    (do
      (doseq [event (.pollEvents watch-key)]
        (async/go
         (async/>! c event)))
      (.reset watch-key))))

(defn string->path [p]
  (.toPath (clojure.java.io/file p)))

(defn path->watch [p]
  (.. p (getFileSystem) (newWatchService)))

(defn seq->event-array [events]
  (into-array WatchEvent$Kind events))

(defn attach-handler [p handler event-types]
  (let [c      (async/chan)
        path   (string->path p)
        watch  (path->watch path)
        events (seq->event-array event-types)
        uref   (make-ref)
        events-smoother (agent (cache/ttl-cache-factory {} :ttl 1000))
        timer (atom (Timer.))]
    (.register path watch events)
    (let [task (proxy [TimerTask] []
                 (run []
                   (do
                     (poll watch c))))]
      (schedule-task timer task))
    (async/go-loop
        [e (async/<! c)]
      (handler e)
      (recur (async/<! c))))
  nil)
