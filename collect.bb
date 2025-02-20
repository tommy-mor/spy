#!/usr/bin/env bb

(require '[babashka.fs :as fs]
         '[babashka.process :refer [shell]])

(let [files (sort (map str (fs/glob "." "**/*.{clj,edn}")))
      tree (-> (shell {:out :string} "tree" ".") :out)]
  
  ;; Write the tree structure first
  (spit "prompt.txt" (str tree "\n\nFile contents:\n\n"))
  
  ;; Append each file's content in the loop
  (doseq [f files]
    (spit "prompt.txt"
          (format "\n=== %s ===\n\n%s\n" f (slurp f))
          :append true))
  
  ;; Open the current directory after writing is complete
  (shell "open" "prompt.txt")) 
