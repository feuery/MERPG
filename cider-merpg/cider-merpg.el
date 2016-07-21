(require 'cider-client)
(require 'nrepl-client)

(setq *merpg-debug* nil)

(setq lexical-binding t)

(defun merpg-find-file (url)
  (interactive "sFind ns: ")
  (message (concat "Finding file from ns " url "..."))
  (cider-nrepl-send-request (list "op" "find-file"
				  "ns" url
				  "debug?" (if *merpg-debug*
					       "true"
					     "false"))
			    (lambda (result)
			      (message (concat "Found ns " url ", opening..."))
			      
			      (let* ((buf-name (concat "MERPG: " url))
				     (buf (get-buffer-create buf-name)))
				(set-buffer buf)
				(insert (nrepl-dict-get result "contents"))
				(switch-to-buffer buf)
				(clojure-mode)
				;; (enable cider-merpg-mode)
				(message (nrepl-dict-get result "notes"))))))
				;; (switch-to-buffer buf)

			      ;; (message (concat "Got result " (prin1-to-string result))))))

;; Debugger entered--Lisp error: (void-variable n)
 
(merpg-find-file "merpg.core.lol")
