#!/usr/bin/env bb

(require '[babashka.fs :as fs]
         '[clojure.string :as str])

(doseq [file (->> (fs/glob "." "**/*")
                  (filter fs/regular-file?)
                  (remove #(or (str/starts-with? (str %) ".")
                             (= (fs/file-name %) "prompt.txt")))
                  (map str)
                  sort)]
  (println (str "\n=== " file " ===\n"))
  (println (slurp file))) 
