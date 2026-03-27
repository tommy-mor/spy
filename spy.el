(defface clojure-spy-inline-face
  '((t (:foreground "orange" :slant italic)))
  "Face for inline spy values."
  :group 'clojure)

(defface clojure-spy-top-overlay-face
  '((t (:foreground "magenta" :weight bold :slant italic)))
  "Face for the top-of-defn instrumenting overlay."
  :group 'clojure)

(defvar clojure-spy-overlays nil
  "List of active inline spy overlays.")

(defvar clojure-spy-top-overlays nil
  "List of active top-of-defn overlays for instrumenting messages.")

(defun clojure-spy-top-overlay-add-at-defn (start-marker)
  "Add a persistent overlay at the top of the defn starting at START-MARKER."
  (let ((ov (make-overlay start-marker start-marker)))
    (overlay-put ov 'after-string
                 (propertize "Instrumenting...\n" 'face 'clojure-spy-top-overlay-face))
    (push ov clojure-spy-top-overlays)
    ov))

(defun clojure-spy-overlay-show ()
  "Show spy value inline under symbol at point, on next line, persistent until C-g."
  (interactive)
  (let* ((sym (thing-at-point 'symbol t))
         (val (cider-nrepl-sync-request:eval
               (format "(when-let [v (ns-resolve 'spy '%s)] (var-get v))" sym)))
         (text (nrepl-dict-get val "value"))
         (ov (make-overlay (line-end-position) (line-end-position))))
    (overlay-put ov 'after-string
                 (propertize (concat "\n" text)
                             'face 'clojure-spy-inline-face))
    (push ov clojure-spy-overlays)))

(defun clojure-spy-overlay-hide ()
  "Remove all spy overlays."
  (interactive)
  (mapc #'delete-overlay clojure-spy-overlays)
  (setq clojure-spy-overlays nil))

(defun clojure-spy-wrap-defn ()
  "Wrap the nearest (defn or (defn-) form with (spy ...)."
  (interactive)
  (save-excursion
    (when (re-search-backward "^[[:space:]]*(defn-?" nil t)
      (let* ((start (point-marker))
             (end (save-excursion
                    (end-of-defun)
                    (point-marker))))
        (goto-char end)
        (insert ")")
        (goto-char start)
        (insert "(spy/spy\n")))))

(defun clojure-spy-unwrap-defn ()
  "Remove surrounding (spy ...) around the nearest defn or defn- form."
  (interactive)
  (save-excursion
    ;; go to nearest defn
    (when (re-search-backward "^[[:space:]]*(defn-?" nil t)
      (let* ((defn-start (point))
             spy-open spy-close)
        ;; move up to the opening ( of spy
        (ignore-errors
          (goto-char defn-start)
          (backward-up-list)
	  (when (looking-at "(spy/spy\\_>")
            (setq spy-open (point))
            ;; move forward to matching paren of spy
            (forward-sexp)
            (setq spy-close (point))
            ;; delete closing paren first
            (goto-char (1- spy-close))
            (delete-char 1)
            ;; delete opening (spy
            (goto-char spy-open)
            (delete-char 8)
            ;; remove any whitespace/newlines after opening
            (when (looking-at "[[:space:]\n]+")
              (delete-region (point) (match-end 0)))))))))

(defun clojure-spy-defn-at-point ()
  "Instrument the defn at point dynamically via spy-runtime and show top overlay."
  (interactive)
  (save-excursion
    (when (re-search-backward "^[[:space:]]*(defn-?" nil t)
      (let* ((start (point-marker))
             (end (save-excursion (end-of-defun) (point-marker)))
             ;; Find the function's name (symbol) first.
             (fn-name (save-excursion
                        (goto-char start)
                        (when (re-search-forward "(defn-?\\s-+\\(\\_<[^[:space:]]+\\_>\\)" end t)
                          (match-string 1))))
             (defn-form (buffer-substring-no-properties start end))
             ;; Call the two-argument version of spy-runtime, passing the
             ;; fully-qualified symbol and the function's source text.
             (expr (if fn-name
                       (format "(spy/spy-runtime '%s %S)"
                               (concat (cider-current-ns) "/" fn-name) ; Create qualified symbol
                               defn-form)
                     ;; Fallback if we can't find the name (e.g. anonymous defn)
                     (format "(spy/spy-runtime nil %S)" defn-form)))
             (overlay (clojure-spy-top-overlay-add-at-defn start)))
        (cider-nrepl-sync-request:eval expr)
        (message "Instrumented %s" (or fn-name "function"))))))

(defun clojure-spy-unspy (arg)
  "Unspy the defn at point, or all defns with a C-u prefix argument."
  (interactive "P")
  (if arg
      ;; C-u prefix → unspy all global state
      (progn
        (cider-nrepl-sync-request:eval "(spy/clear!)")
        (mapc #'delete-overlay clojure-spy-top-overlays)
        (setq clojure-spy-top-overlays nil)
        (message "Unspy all successful"))
    ;; No prefix → unspy only the defn at point by re-evaluating it.
    (save-excursion
      (when (re-search-backward "^[[:space:]]*(defn-?" nil t)
        (let* ((start (point-marker))
               (end (save-excursion (end-of-defun) (point-marker)))
               (defn-form (buffer-substring-no-properties start end))
               ;; To unspy, just re-evaluate the original form.
               ;; This overwrites the instrumented version in the REPL.
               (expr defn-form)
               (overlay-to-remove
                (seq-find (lambda (ov)
                            (= (overlay-start ov) start))
                          clojure-spy-top-overlays)))
          (when overlay-to-remove
            (delete-overlay overlay-to-remove)
            (setq clojure-spy-top-overlays
                  (delq overlay-to-remove clojure-spy-top-overlays)))
          (cider-nrepl-sync-request:eval expr)
          (message "Unspy at point successful"))))))

;; --- Minor Mode Definition ---

(defvar spy-clojure-mode-map
  (let ((map (make-keymap)))
    (define-key map (kbd "C-c q") #'clojure-spy-defn-at-point)
    (define-key map (kbd "C-c y") #'clojure-spy-unspy)
    (define-key map (kbd "C-c w") #'clojure-spy-wrap-defn)
    (define-key map (kbd "C-c W") #'clojure-spy-unwrap-defn)
    (define-key map (kbd "C-c o") #'clojure-spy-overlay-show)
    (define-key map (kbd "C-c O") #'clojure-spy-overlay-hide)
    map)
  "Keymap for spy-clojure-mode.")

(define-minor-mode spy-clojure-mode
  "A minor mode for spying on Clojure code."
  :init-value nil
  :lighter " Spy"
  :keymap spy-clojure-mode-map)

(add-hook 'clojure-mode-hook #'spy-clojure-mode)
