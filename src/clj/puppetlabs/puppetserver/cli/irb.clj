(ns puppetlabs.puppetserver.cli.irb
  (:import (org.jruby RubyInstanceConfig)
           (org.jruby Main)
           (java.util HashMap))
  (:require [puppetlabs.kitchensink.core :as ks]
            [slingshot.slingshot :refer [try+]]
            [puppetlabs.services.jruby.jruby-puppet-core :as jruby-puppet-core]
            [puppetlabs.trapperkeeper.config :as tk-config]))

(defn parse-cli-args!
  "Parses the command-line arguments using `puppetlabs.kitchensink.core/cli!`.
      --config <config file or directory>"
  [cli-args]
  (let [specs       [["-c" "--config CONFIG-PATH"
                      (str "Path to a configuration file or directory of configuration files. "
                           "See the documentation for a list of supported file types.")]]
        required    [:config]]
    (ks/cli! cli-args specs required)))

(defn run!
  [config args]
  (let [load-path-args    (map #(str "-I" %)
                            (get-in config [:os-settings
                                            :ruby-load-path]))
        file-to-load      (str "jar:file:"
                                (-> (class *ns*)
                                    .getProtectionDomain
                                    .getCodeSource
                                    .getLocation
                                    .getPath)
                                "!/META-INF/jruby.home/bin/irb")
        args              (into-array String
                                     (concat ["-e"
                                              (str "load '"
                                                file-to-load
                                                "'")
                                              (str "-I"
                                                jruby-puppet-core/ruby-code-dir)
                                              "--"]
                                             load-path-args
                                             args))
        jruby-config      (RubyInstanceConfig.)
        env-with-gem-home (doto (-> jruby-config (.getEnvironment) (HashMap.))
                            (.put "GEM_HOME"
                                  (get-in config
                                          [:jruby-puppet :gem-home])))]
    (.setEnvironment jruby-config env-with-gem-home)
    (-> (Main. jruby-config)
        (.run args))))

(defn load-tk-config
  [cli-data]
  (let [debug? (or (:debug cli-data) false)]
    (if-not (contains? cli-data :config)
      {:debug debug?}
      (-> (:config cli-data)
          (tk-config/load-config)
          (assoc :debug debug?)))))

(defn -main
  [& args]
  (try+
    (let [[config extra-args] (-> (or args '())
                                  (parse-cli-args!))]
      (run! (load-tk-config config) extra-args))
    (catch map? m
      (println (:message m))
      (case (ks/without-ns (:type m))
        :cli-error (System/exit 1)
        :cli-help  (System/exit 0)))
    (finally
      (shutdown-agents))))
