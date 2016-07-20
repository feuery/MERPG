(require 'cider-client)

(defun merpg-find-file (url)
  (interactive "sFind ns: ")
  (cider-nrepl-send-request (list "op" "find-file"
				  "ns" url)
			    (lambda (result)
			      (message (concat "Got result " (prin1-to-string result))))))

(merpg-find-file "merpg.nrepl.middleware")
