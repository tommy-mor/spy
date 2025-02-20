#!/usr/bin/env bb

(require '[clojure.string :as str]
         '[babashka.fs :as fs])

(defn parse-file-section [content]
  (let [[header & contents] (str/split content #"\n")
        file-name (second (re-find #"=== (.*) ===" header))]
    {:file file-name
     :content (str/join "\n" contents)}))

(let [content (slurp "prompt.txt")
      [tree-part files-part] (str/split content #"\n\nFile contents:\n\n")
      file-sections (str/split files-part #"(?=\n===)")
      files (map parse-file-section (filter not-empty file-sections))]
  
  ;; Write each file back to disk
  (doseq [{:keys [file content]} files]
    ;; Ensure parent directories exist
    (fs/create-dirs (fs/parent file))
    ;; Write the file
    (spit file content)
    (println "Wrote:" file))) 