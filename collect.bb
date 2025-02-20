#!/usr/bin/env bb

(require '[babashka.fs :as fs]
         '[babashka.process :refer [shell]])

(let [files (sort (map str (fs/glob "." "**/*.{clj,edn,md,txt}")))
      tree (-> (shell {:out :string} "tree" ".") :out)
      content (str tree "\n\nFile contents:\n\n"
                   (apply str
                     (for [f files]
                       (format "\n=== %s ===\n\n%s\n" f (slurp f)))))]
  
  (shell {:in content} "pbcopy")) 
