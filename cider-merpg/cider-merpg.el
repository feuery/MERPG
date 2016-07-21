(require 'cider-client)
(require 'nrepl-client)

(setq *merpg-debug* nil)

(setq lexical-binding t)

(make-variable-buffer-local (defvar ns "" "Buffer-local var to track the ns of the currently opened merpg script asset's buffer"))

(replace-regexp-in-string "^!\\(.*\\)" "\\1" "!merpg.core.lol")

(defun merpg-find-file (url)
  (interactive "sFind file: ")
  (if (string-prefix-p "!" url)
      (let ((real-url (replace-regexp-in-string "^!\\(.*\\)" "\\1" url)))
	(message (concat "Finding file from ns " real-url "..."))
	(cider-nrepl-send-request (list "op" "find-file"
					"ns" real-url
					"debug?" (if *merpg-debug*
						     "true"
						   "false"))
				  (lambda (result)
				    (message (concat "Found ns " real-url ", opening..."))
				    
				    (let* ((buf-name (concat "MERPG: " real-url))
					   (buf (get-buffer-create buf-name)))
				      (set-buffer buf)
				      (insert (nrepl-dict-get result "contents"))
				      (switch-to-buffer buf)
				      (setq ns real-url)				
				      (clojure-mode)
				      ;; (enable cider-merpg-mode)
				      (message (nrepl-dict-get result "notes"))))))
    (find-file url)))

(define-minor-mode merpg-edit-mode
  "An extension to CIDER. With cider connected to a live instance of MERPG (URL `https://github.com/feuery/memapper/'), this mode overrides C-x C-f and C-x C-s to be able to load and save script assets from the game"
  :lighter " merpg"
  :keymap (let ((map (make-sparse-keymap)))
	    (define-key map (kbd "C-x C-f") 'merpg-find-file)
	    map)
  :global nil)
  
