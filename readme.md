# spy

`spy` lets you capture local variables at runtime and access them directly in your REPL. Wrap a block with `spy`, and every subexpression becomes instantly evaluable.

> *"In hindsight, so much of what we hype up as 'exploratory programming' in the REPL is really just coping with the lack of useful type information."*  
> — [this post](https://discuss.ocaml.org/t/whats-your-development-workflow/10358/8)  

> *"It's much easier for me to generalize from the concrete than concretize from the general."*  
> — a professor once told me  

---

## Example: Cat Facts

Here's a real-world example fetching a cat fact from an API:  

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

**spy** is a macro that transforms your code to define all local variables globally. Here's what happens:  

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

I've leaned on the "inline def" trick—`(def x x)`—for years to debug and develop interactively. (See great write-ups [here](https://blog.michielborkent.nl/inline-def-debugging.html) and [here](https://cognitect.com/blog/2017/6/5/repl-debugging-no-stacktrace-required).) 

But it's tedious:  
- Manually writing `(def arg1 arg1)` for every variable is a chore.  
- You've got to clean them up before committing to avoid code smell.  

**spy** automates this pattern, making it effortless and keeping your code clean. It's the inline `def` hack on autopilot.  

---

## Trade-Offs

Yes, **spy** clogs up the global namespace with `def`s—just like `(def varname varname)` does. This means that you can't ever have a local variable named `count`, because then it will overwrite the `count` var, and mess up other code that expects `count` to be a function. Using spy you have to be more careful about naming your local variables.

---

## How to Use

1. **Add spy to your project** by adding this to your `deps.edn`:

```clojure
{:deps {io.github.tommy-mor/spy {:git/url "https://github.com/tommy-mor/spy"
                                 :git/sha "COMMIT-SHA-HERE"}}}
```

2. **Require it:**  

```clojure
(require '[spy.core :refer [spy]])
```

3. **Wrap any block:**  

```clojure
;; instrument expression/function
(spy
   (defn test-fn [a {:keys [b c]}]
     (+ a b c)))

;; initalize values
(test-fn 10 {:b 20 :c 30})
```

4. **Evaluate and explore:**  

```clojure
a ;; => 10
b ;; => 20
c ;; => 30
(+ a b c) ;; => 60
```

## Comparison to type systems.

In typed languages, you write an expression and get millisecond-level feedback like `List<Integer>`, offering instant but abstract type info. With spy, you eval once and get near-instant access to concrete values like `[1, 2, 3, 4]`—richer for exploration, though it needs that initial run. It's an improvement over types: instead of just knowing the shape, you see the real data and can evaluate subexpressions in context immediately. Types have other benefits that spy does not have.
