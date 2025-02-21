**spy** lets you capture local variables at runtime using `spy` and access them directly in your REPL. After calling a function wrapped with `spy`, the variables become available for quick debugging and exploration.

> In hindsight, so much of what we hype up as “exploratory programming” in the REPL is really just coping with the lack of useful type information.
> -- [this post](https://discuss.ocaml.org/t/whats-your-development-workflow/10358/8)

> It's much easier for me to generalize from the concrete than conctretize from the general.
> -- a professor once told me

> The values of a program deserve to be tracked in git, not just the source of a program.
> -- my opinion
---

## How to Use
1. **Use `spy+` in Your Code:**  
   ```clojure
   (require [spy.core :refer [spy]])

   ;; Example 1: Simple let bindings 
   (spy
     (let [x 10
           y 20
           z (+ x y)]
       (println "x =" x)
       (println "y =" y)
       (println "z =" z)
       (* x y z)))

   ;; Example 2: Destructuring
   (spy 
    (let [{:keys [a b]} {:a 1 :b 2}
          sum (+ a b)]
      (println "sum =" sum)
      sum))

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
