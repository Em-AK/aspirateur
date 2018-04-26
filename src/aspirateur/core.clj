(ns aspirateur.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.core.async :as async]))

(defn video-link?
  [s]
  (string/includes? s "video-download-link"))

(defn extract-episode
  [s]
  (zipmap
    '(:url :title :id)
    (rest (re-find #"(https://\S*)\".*>\s(.*#(\d*):.*)\s</a" s)))) ; !regex

(def episodes
  (with-open [reader (io/reader "resources/links.html")]
    (->> reader
         (line-seq) ; lazy != easy
         (filter video-link?)
         (map extract-episode)
         (vec))))

(defn download-episode
  [episode]
  (do
    (with-open [in (io/input-stream (:url episode))
                out (io/output-stream (:title episode))]
      (io/copy in out))
    (:id episode)))

(defn download-all
  []
  (let [out-chan (async/chan)
        workers-count 2]
    (async/pipeline-blocking workers-count
                             out-chan 
                             (map download-episode)
                             (async/to-chan episodes))
    (async/<!! (async/into [] out-chan))))

