;; -*- lexical-binding: t -*-

(require 'cider-client)
(require 'nrepl-client)

(defvar *merpg-debug* nil)
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
  (if (cider-connected-p)
      (progn
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
    (message "Cider not connected. cider-merpg cannot operate")))

(defun merpg-find-file (url)
  (interactive
   (list
    (read-file-name "Find file - start with ! to find merpg ns: " default-directory "" nil)))
  (if (string-prefix-p "!" url)
      (progn
	(setq real-url (replace-regexp-in-string "^!\\(.*\\)" "\\1" url))
	(message (concat "Finding file from ns " real-url "..."))
	(if (cider-connected-p)
	    (cider-nrepl-send-request (list "op" "find-file"
					    "ns" real-url
					    "debug?" (if *merpg-debug*
							 "true"
						       "false"))
				      (lambda (result)
					(if *merpg-debug*
					    (message (concat "Result from nrepl server: " (prin1-to-string result))))
					(if (equal (nrepl-dict-get result "status") '("create-asset"))
					    (let ((new-map-id (completing-read "Creating new script asset. Parent map id: " (nrepl-dict-get result "map-ids") nil 44)))
					      (cider-nrepl-send-request (list "op" "create-file"
									      "parent-id" new-map-id
									      "ns" real-url)
									(lambda (result)
									  (if (equal (nrepl-dict-get result "status") '("done"))
									      (merpg-find-file url)
									    (message (concat "Error while creating " real-url ". "
											     (if *merpg-debug*
												 (prin1-to-string result)
											       "Turn *merpg-debug* on to get more info")))))))
					  
					  (if (equal (nrepl-dict-get result "status") '("done"))
					      (progn
						
						(message (concat "Found ns " real-url ", opening..."))
						(setq buf-name (concat "MERPG: " real-url))
						(setq buf (get-buffer-create buf-name))
						(switch-to-buffer buf)
						(delete-region (point-min) (point-max))
						
						(if *merpg-debug*
						    (message (concat "Got a dict: " (prin1-to-string result))))
						(insert (nrepl-dict-get result "contents"))
						(clojure-mode)
						(merpg-edit-mode)
						(not-modified)
						(setq ns real-url)
						(if *merpg-debug*
						    (message (concat "Set ns to " ns)))
						(message (nrepl-dict-get result "notes")))
					    (message (concat "NS " real-url " not found in the running MERPG instance"))))))
	  (message "Cider not connected. cider-merpg cannot operate")))
    (find-file url)))

(defun to-kill-buffer ()
  (interactive)
  (if (or (not (buffer-modified-p))
  	       (and (buffer-modified-p)
  		    (y-or-n-p (concat "Buffer " (prin1-to-string (current-buffer)) " modified. Kill anyway?"))))
  	   (kill-buffer (current-buffer))))

(define-minor-mode merpg-edit-mode
  "An extension to CIDER. With cider connected to a live instance of MERPG (URL `https://github.com/feuery/memapper/'), this mode overrides C-x C-f and C-x C-s to be able to load and save script assets from the game"
  :lighter " merpg"
  :keymap (let ((map (make-sparse-keymap)))
	    (define-key map (kbd "C-x C-f") 'merpg-find-file)
	    (define-key map (kbd "C-x C-s") 'merpg-save-file)
	    (define-key map (kbd "C-x k") 'to-kill-buffer)
						   
	    map)
  :global nil)

;; (add-hook 'cider-repl-mode-hook 'merpg-edit-mode)

(provide 'cider-merpg)
