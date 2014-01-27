(ns awizo.core
  (:require [clojure.core.async :as async])
  (:import [java.nio.file StandardWatchEventKinds])
  (:import [java.nio.file WatchEvent$Kind])
  (:import [java.util Timer])
  (:import [java.util TimerTask]))


(defn timer-error-handler [timer ex]
  (doseq [ste (.getStackTrace ex)]
    (println ste)))
(def timer       (agent (Timer.)))
(set-error-handler! timer timer-error-handler)
(def periodicity (long 500))

(def CREATE StandardWatchEventKinds/ENTRY_CREATE)
(def DELETE StandardWatchEventKinds/ENTRY_DELETE)
(def MODIFY StandardWatchEventKinds/ENTRY_MODIFY)


(defn schedule-task [task]
  (send-off
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

(defn close [c]
  )

(defn string->path [p]
  (.toPath (clojure.java.io/file p)))

(defn path->watch [p]
  (.. p (getFileSystem) (newWatchService)))

(defn seq->event-array [events]
  (into-array WatchEvent$Kind events))

(defn attach-handler [p handler event-types]
  (let [c     (async/chan)
        path  (string->path p)
        watch (path->watch path)
        events (seq->event-array event-types)]
    (.register path watch events)
    (let [task (proxy [TimerTask] []
                 (run []
                   (poll watch c)))]
      (schedule-task task))
    c))
