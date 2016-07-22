;; -*- lexical-binding: t -*-

(require 'cider-client)
(require 'nrepl-client)

(setq *merpg-debug* nil)
(setq lexical-binding t)

(make-variable-buffer-local (defvar ns "" "Buffer-local var to track the ns of the currently opened merpg script asset's buffer"))

(replace-regexp-in-string "^!\\(.*\\)" "\\1" "!merpg.core.lol")

(make-variable-buffer-local (defvar buf-name ""))
(make-variable-buffer-local (defvar buf))

(defun clear-modified ()
  (interactive)
  (set-buffer-modified-p nil))

(defun merpg-save-file ()
  (interactive)
  (message (concat "Saving " ns))
  (unless (string= ns "")
    (lexical-let ((current-buf (current-buffer)))
      (cider-nrepl-send-request (list "op" "save-file"
				      "ns" ns
				      "contents" (buffer-substring-no-properties (point-min) (point-max)))
				(lambda (result)
				  (with-current-buffer current-buf
				    (message (prin1-to-string result))
				    (clear-modified)
				    (message (concat "Saved " ns)))))
      (message "Sent save request")))
  (when (string= ns "")
    (message "ERROR: ns is \"\"")))



(defun merpg-find-file (url)
  (interactive
   (list
    (read-file-name "Find file - start with ! to find merpg ns: " default-directory "" nil)))
  (if (string-prefix-p "!" url)
      (progn
	(setq real-url (replace-regexp-in-string "^!\\(.*\\)" "\\1" url))
	(message (concat "Finding file from ns " real-url "..."))
	(cider-nrepl-send-request (list "op" "find-file"
					"ns" real-url
					"debug?" (if *merpg-debug*
						     "true"
						   "false"))
				  (lambda (result)
				    (message (concat "Found ns " real-url ", opening..."))
				    (setq buf-name (concat "MERPG: " real-url))
				    (setq buf (get-buffer-create buf-name))
				    (switch-to-buffer buf)
				    ;; contents on nil
				    (if *merpg-debug*
					(message (concat "Got a dict: " (prin1-to-string result))))
				    (insert (nrepl-dict-get result "contents"))
				    (clojure-mode)
				    (merpg-edit-mode)
				    (not-modified)
				    (setq ns real-url)
				    (if *merpg-debug*
					(message (concat "Set ns to " ns)))
				    (message (nrepl-dict-get result "notes")))))
    (find-file url)))

(define-minor-mode merpg-edit-mode
  "An extension to CIDER. With cider connected to a live instance of MERPG (URL `https://github.com/feuery/memapper/'), this mode overrides C-x C-f and C-x C-s to be able to load and save script assets from the game"
  :lighter " merpg"
  :keymap (let ((map (make-sparse-keymap)))
	    (define-key map (kbd "C-x C-f") 'merpg-find-file)
	    (define-key map (kbd "C-x C-s") 'merpg-save-file)
	    map)
  :global nil)

(add-hook 'cider-repl-mode-hook 'merpg-edit-mode)
