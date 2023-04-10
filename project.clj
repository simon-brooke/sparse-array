(defproject sparse-array "0.3.0-SNAPSHOT"
  :aot :all
  :cloverage {:output "docs/cloverage"}
  :codox {:metadata {:doc "**TODO**: write docs"
                     :doc/format :markdown}
          :output-path "docs/codox"
          :source-uri "https://github.com/simon-brooke/sparse-array/blob/master/{filepath}#L{line}"}

  :dependencies [[org.clojure/clojure "1.11.1"]]

  :description "A Clojure library designed to manipulate sparse *arrays* - multi-dimensional spaces accessed by indices, but containing arbitrary values rather than just numbers. For sparse spaces which contain numbers only, you're better to use a *sparse matrix* library, for example [clojure.core.matrix](https://mikera.github.io/core.matrix/)."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cloverage "1.2.2"]
            [lein-codox "0.10.7"]
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
                  ;; ["deploy" "clojars"] -- also not working now. Security tightened at Clojars?
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]]
  :url "https://simon-brooke.github.io/sparse-array/docs/")

