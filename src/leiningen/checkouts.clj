(ns leiningen.checkouts
  "Executes a leiningen task on all \"checkouts\" dependencies."
  {
   :author "Gunnar Völkel"
   :added "1.0"
  }
  (:require
    [leiningen.compile :as lc]
  )  
  (:use
    [leiningen.core :only (apply-task read-project task-not-found)]
    [clojure.string :only (join)]    
  )
  (:import
    java.io.File
  )
)


(defn error [msg-format & params]
  (println (apply format (str "ERROR: " msg-format) params))
  (System/exit 1)
)


(defn user-dir
  []
  (-> "user.dir" System/getProperty File.)
)


(defn list-checkouts
  [^File dir]
  (seq (.listFiles (File. dir "checkouts")))
)

  
(defn get-project-data
  [^File dir]
  (read-project (.getAbsolutePath (File. dir "project.clj")))
)


(defrecord Project [name, dir, data, checkouts])

(defn create-project
  [dir]
  (if-let [data (get-project-data dir)]
    (Project. (:name data), dir, data, nil)
    (error "There is no \"project.clj\" in directory \"%s\"!" (.getPath dir))
  )
)

(defn build-checkouts-map 
  [project]
  (loop [project-list [(create-project (-> project :root File.))], result-map {}]
    ; as long as we have another project ...
    (if-let [p (first project-list)] 
      ; ... we check whether the project is already in our result map ...
      (if (contains? result-map (:name p))
        ; ... then we simply continue with the enxt project ...
        (recur (rest project-list), result-map)
	      ; ... else if the project has checkouts ...
	      (if-let [checkouts (seq (map create-project (list-checkouts (:dir p))))]
	        ; ... then assoc the checkouts to the current project and add them to the project list ...
	        (recur
	          (apply conj (rest project-list) checkouts)
	          (assoc result-map  (:name p) (assoc p :checkouts (set (map :name checkouts))))           
	        )
	        ; ... else just add p to the result map
          (recur
            (rest project-list)
            (assoc result-map  (:name p) p)
          )
	      )
      )
      ; ... finally we return the result map
      result-map
    )
  )
)

(defn update-checkouts-map
  [checkouts-map, no-deps]
  (let [
        no-deps-names (map :name no-deps),
        checkouts-map (apply dissoc checkouts-map no-deps-names)
       ]
    (reduce
      (fn [cm, k] (update-in cm [k :checkouts] #(apply disj % no-deps-names))) 
      checkouts-map
      (keys checkouts-map) 
    )    
  )
)

(defn create-checkout-build-seq
  [checkouts-map]
  (loop [checkouts-map checkouts-map, build-seq []]
    (if (empty? checkouts-map)
      build-seq
      ; if there are checkouts with no sub-checkouts ...
      (if-let [no-deps (seq (filter #(empty? (:checkouts %)) (vals checkouts-map)))]
        ; ... then add them to the build sequence and update the checkouts-map          
        (recur (update-checkouts-map checkouts-map, no-deps), (apply conj build-seq no-deps))          
        ; ... else signal error.
        (error "There seem to be cyclic dependent checkouts!")
      )
    )
  )
)

(defn perform-lein-task
  [task-name, project]
  (apply-task task-name (:data project) [] task-not-found)
)

(def clean-task "clean")
(def deps-task "deps")
(def compile-task "compile")
(def jar-task "jar")
(def uberjar-task "uberjar")
(def install-task "install")


(def task-set #{clean-task, deps-task, compile-task, jar-task, uberjar-task, install-task})

(defn build-checkouts
  [build-seq, task]
  (let [
        build-last? (#{compile-task jar-task uberjar-task install-task} task),
        deps? (or (= task deps-task) build-last?)
       ]
	  (doseq [p (butlast build-seq)]
	    (println (format "[PROCESSING] project \"%s\"" (:name p)))
	    (perform-lein-task clean-task p)
      (when deps? (perform-lein-task deps-task p))
	    (when build-last? (perform-lein-task install-task p))
	  )
	  (let [p (last build-seq)]
	    (println (format "[PROCESSING] project \"%s\"" (:name p)))
	    (perform-lein-task clean-task p)
	    (when deps? (perform-lein-task deps-task p))
	    (when build-last? 
	        (perform-lein-task task p)	     
	    )
	    nil
	  )
  )
)


(defn checkouts
  "Executes a leiningen task on all checkout dependencies."
  [project, task]
  (if project
	  (if-let [task (task-set task)]
	    (-> project
	      build-checkouts-map 
	      create-checkout-build-seq
	      (build-checkouts task)
	    )
	    (error "No task specified! One of the following tasks has to be specified:\n* %s" (join "\n* " (sort (seq task-set))))
	  )
    (error "No project specified!")
  )
)
