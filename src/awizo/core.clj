(ns awizo.core
  (:import [java.nio.file StandardWatchEventKinds])
  (:import [java.nio.file WatchEvent$Kind]))

(def CREATE StandardWatchEventKinds/ENTRY_CREATE)
(def DELETE StandardWatchEventKinds/ENTRY_DELETE)
(def MODIFY StandardWatchEventKinds/ENTRY_MODIFY)

(defn string->path [p]
  (.toPath (clojure.java.io/file p)))

(defn path->watch [p]
  (.. p (getFileSystem) (newWatchService)))

(defn seq->event-array [events]
  (into-array WatchEvent$Kind events))

(defn attach-handler [p handler event-types]
  (let [path (string->path p)
        watch (path->watch path)
        events (seq->event-array event-types)]
    (.register path watch events)
    (while true
      (let [watch-key (.take watch)]
        (doseq [event (.pollEvents watch-key)]
          (handler event))
        (.reset watch-key)))))
