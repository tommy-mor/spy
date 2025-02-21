# spy

`spy` lets you capture local variables at runtime and access them directly in your REPL. Wrap a block with `spy`, and every subexpression becomes instantly evaluable—no manual debugging hacks required.

> *"In hindsight, so much of what we hype up as 'exploratory programming' in the REPL is really just coping with the lack of useful type information."*  
> — [this post](https://discuss.ocaml.org/t/whats-your-development-workflow/10358/8)  

> *"It's much easier for me to generalize from the concrete than concretize from the general."*  
> — a professor once told me  

---
## Why its cool
Clojure’s REPL shines by merging edit time and runtime, but it’s held back by slow feedback and missing type info. You load namespaces manually, rerun code to see data shapes, and lose context between runs. spy fights back:

- **Direct Subexpression Evaluation:** Every variable in a spy block is instantly REPL-accessible. No copying, no printing—just type the name and see the value.
- **Real Context:** Values come from actual runtime execution, not mocks or guesses. You’re exploring the exact state of your program, not hypothesizing about types.

Compare this to type systems like OCaml: write an expression, and within milliseconds, the IDE shows its type `(int, list string)`. That’s fast—but abstract. spy gets you close to that speed with values, not types. After one eval, you don’t wonder if processed is a map—you see `{:fact "Cats have 9 lives" :length 15}` and can play with it. It’s an improvement over types: concrete, immediate, and REPL-ready.
---

## Example: Cat Facts

Here’s a real-world example fetching a cat fact from an API:  

```clojure
(require '[clojure.data.json :as json])

(spy
  (let [data (slurp "https://catfact.ninja/fact")
        processed (json/read-str data :key-fn keyword)
        {:keys [fact]} processed]
    (str "your fact is: " fact)))
```

After evaluating this:  

```clojure
;; Instantly accessible in the REPL:
data       ;; => "{\"fact\": \"Cats have 9 lives\", \"length\": 15}"
processed  ;; => {:fact "Cats have 9 lives" :length 15}
fact       ;; => "Cats have 9 lives"

;; Run any subexpression in-place, ithout modifying it
(str "your fact is: " fact) ;; => "your fact is: Cats have 9 lives"

;; Experiment live:
(+ (:length processed) 10) ;; => 25
```

No print statements, no manual `def`s—just instant access to every step of the computation. You can write expressions on the data without rerunning the api call!

---

## How It Works: Before and After

**spy** is a macro that transforms your code to define all local variables globally. Here’s what happens:  

### Before (your code):  

```clojure
(let [x 10
      y 20
      z (+ x y)]
  (* x y z))
```

### After (macro expansion):  

```clojure
(let [x 10
      y 20
      z (+ x y)]
  (def x x)
  (def y y)
  (def z z)
  (* x y z))
```

After evaluation, `x`, `y`, and `z` are available in your REPL as `10`, `20`, and `30`. You can mess around with them instantly:  

```clojure
(* z 2) ;; => 60
```

---

## Motivation

I’ve leaned on the “inline def” trick—`(def x x)`—for years to debug and develop interactively. (See great write-ups [here](https://blog.michielborkent.nl/inline-def-debugging.html) and [here](https://cognitect.com/blog/2017/6/5/repl-debugging-no-stacktrace-required).) It’s perfect for REPL workflows: define function args inline, run a test or comment block, and send the function body to the REPL as you tweak it. Values flow through your expressions milliseconds after writing them.  

But it’s tedious:  
- Manually writing `(def arg1 arg1)` for every variable is a chore.  
- You’ve got to clean them up before committing to avoid code smell.  

**spy** automates this pattern, making it effortless and keeping your code clean. It’s the inline `def` hack on autopilot.  

---

## Trade-Offs

Yes, **spy** clogs up the global namespace with `def`s—just like `(def varname varname)` does. This means that you can't ever have a local variable named `count`, because then it will overwrite the `count` var, and mess up other code that expects `count` to be a function. Using spy you have to be more careful about naming your local variables.

---

## How to Use

1. **Add spy to your project** (details TBD—stick it in your `deps.edn` or copy `core.clj` for now).  
2. **Require it:**  

```clojure
(require '[spy.core :refer [spy]])
```

3. **Wrap any block:**  

```clojure
(spy
  (let [a 5
        b (* a 2)]
    (+ a b)))
```

4. **Evaluate and explore:**  

```clojure
a ;; => 5
b ;; => 10
(+ a b) ;; => 15
```

## Comparison to type systems.

In typed languages, you write an expression and get millisecond-level feedback like List<Integer>, offering instant but abstract type info. With spy, you eval once and get near-instant access to concrete values like [1, 2, 3, 4]—richer for exploration, though it needs that initial run. It’s an improvement over types: instead of just knowing the shape, you see the real data and can evaluate subexpressions in context immediately. Think of it as trading static safety for dynamic power, bringing Clojure’s REPL workflow tantalizingly close to type-system speed with deeper insight.