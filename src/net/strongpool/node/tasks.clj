(ns net.strongpool.node.tasks
  (:require
   [clojure.java.shell :as shell]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [net.strongpool.node.config :as config]))

;; TODO stream shell output

(defn checked-sh [& args]
  (let [res (apply shell/sh args)]
    (if (not= 0 (:exit res))
      (throw (ex-info "Error returned from shell command" res))
      res)))

;; TODO determine why 'bash -c' is needed to get $PATH right

;; TODO add 'dc' (docker-compose) command

(defn arweave-args [config]
  (cond-> ""
    (:mine? config)
    (str " mine")

    (and (:mine? config) (:miner-address config))
    (str " mining_addr " (:miner-address config) " ")

    (get-in config [:arweave :extra-args])
    (str (str/join " " (get-in config [:arweave :extra-args])) " ")

    (get-in config [:arweave :peers])
    (str (->> (get-in config [:arweave :peers])
              (str/join " peer ")
              (str "peer ")))))

(defn start []
  (when-let [config (config/validated-load)]
    (let [args (arweave-args config)
          cmd (str "ARWEAVE_ARGS='" args "' docker-compose up -d")]
      (print "Staring Stronpool node... ")
      (checked-sh "bash" "-c" cmd)
      (println "started."))))

(defn stop []
  (print "Stopping Stronpool node... ")
  (checked-sh "bash" "-c" "docker-compose exec -d arweave /arweave/bin/stop")
  (println "stopped.") ;; TODO wait for arweave service to actually stop
  )

(defn logs []
  (-> (checked-sh "bash" "-c" "docker-compose logs")
      :out
      println))

(defn validate-config []
  (when-let [config (config/validated-load)]
    (println "Valid Strongpool config:")
    (pprint config)))