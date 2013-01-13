(defproject lein-checkouts "1.1.0"
  	:description "Executes the given lein task on all checkout dependencies of the current project."
	:license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.4.0"]]}}
  :eval-in-leiningen true
)
