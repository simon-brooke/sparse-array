(defproject sparse-array "0.1.0"
  :description "A Clojure library designed to manipulate sparse *arrays* - multi-dimensional spaces accessed by indices, but containing arbitrary values rather than just numbers. For sparse spaces which contain numbers only, you're better to use a *sparse matrix* library, for example [clojure.core.matrix](https://mikera.github.io/core.matrix/)."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]

  :plugins [[lein-codox "0.10.4"]
            [lein-release "1.0.5"]]


  ;; `lein release` doesn't play nice with `git flow release`. Run `lein release` in the
  ;; `develop` branch, then merge the release tag into the `master` branch.

  :release-tasks [["vcs" "assert-committed"]
                  ["clean"]
                  ["test"]
                  ["codox"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ;; ["vcs" "tag"] -- not working, problems with secret key
                  ["uberjar"]
                  ["install"]
                  ["deploy" "clojars"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]])

