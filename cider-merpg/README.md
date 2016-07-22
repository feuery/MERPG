# cider-merpg
The emacs minor-mode for editing script assets inside a running merpg-instance. For more details, refer to the [blog](http://yearofourlord.blogspot.fi/2016/07/thoughts-on-scripting-game-engine.html).

# Dependencies 
Requires a modern CIDER installed. The exact version number is specified in the [project.clj's](https://github.com/feuery/memapper/blob/master/project.clj) [cider/cider-nrepl] - dependency.

# Installation
I have no idea how to package this script in a package.el compatible format, nor do I yet wish to find out how to push this to melpa, nor do I have the resources to run package.el - compatible server myself. Thus installing this needs a bit of .emacs - hacking.

First clone the repository somewhere. Then add this to your .emacs:

```elisp
;; or wherever you 'git clone'd the project. This is the path to the .el
(add-to-list 'load-path "~/Dropbox/merpg/cider-merpg")
(require 'cider-merpg)

;; Not relevant unless you're using cider only on merpg-projects
(add-hook 'cider-repl-mode-hook 'merpg-edit-mode)
```

# License
Look at the [main README's](https://github.com/feuery/memapper#license) license
