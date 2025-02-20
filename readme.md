# Spy Playground

**Spy Playground** lets you capture local variables at runtime using `spy+` and access them directly in your REPL. After calling a function wrapped with `spy+`, the variables become available for quick debugging and exploration.

> In hindsight, so much of what we hype up as “exploratory programming” in the REPL is really just coping with the lack of useful type information.
> -- [this post](https://discuss.ocaml.org/t/whats-your-development-workflow/10358/8)

> It's much easier for me to generalize from the concrete than conctretize from the general.
> -- a professor once told me

> The values of a program deserve to be tracked in git, not just the source of a program.
> -- my opinion
---

## How to Use

1. **Start the REPL:**  
   ```bash
   cd spy-test
   ./run.sh
   ```
2. **Connect to the REPL:**  
3. **Use `spy+` in Your Code:**  
   ```clojure
   (ns spy-test.core
     (:require [spy :refer [spy+]]))

   (defn spy-demo []
     (let [x 10
           y 20]
       (spy+ [x x
              y y
              z (+ x y)]
         (* x y z))))

   (spy-demo) ;; => 2000

   ;; Now, directly use captured variables in the REPL:
   (+ x y)   ;; => 30
   (* z 2)   ;; => 60
   ```

## For LLM Use

To share the entire codebase as a single script for an LLM, run:  
```bash
bb llm.bb
```  
This collects all files into `prompt.txt`—perfect for pasting into an LLM for quick analysis or debugging.
