(ns puppetlabs.puppetserver.cli.jrubies
  (:require [puppetlabs.puppetserver.cli.subcommand :as cli]
            [puppetlabs.services.jruby.jruby-puppet-core :as core]
            [puppetlabs.services.jruby.jruby-puppet-agents :as agents]
            [puppetlabs.kitchensink.core :as ks]
            [slingshot.slingshot :as sling]
            [clojure.tools.logging :as log]
            [puppetlabs.trapperkeeper.config :as tk-config])
  (:import (clojure.lang ExceptionInfo)))

(defn jrubies-run!
  [config _]
  (tk-config/initialize-logging! config)
  (let [jruby-puppet-config (core/initialize-config config)
        pool-context (core/create-pool-context jruby-puppet-config
                                               nil
                                               identity)]
    (agents/prime-pool! pool-context jruby-puppet-config nil)
    (log/info "*** All done priming, sleeping for a bit")
    (Thread/sleep 60000)
    (log/info "*** Back awake, shut down time")))

;[{:keys [pool-state] :as pool-context} :- jruby-schemas/PoolContext
; config :- jruby-schemas/JRubyPuppetConfig
; profiler :- (schema/maybe PuppetProfiler)]

(defn jrubies-run-wrapper!
  [config options]
  (try
    (jrubies-run! config options)
    (catch ExceptionInfo e
      (let [exception-type (:type (.getData e))]
        (if (and exception-type
                 (keyword? exception-type)
                 (or (= (ks/without-ns exception-type) :cli-error)
                     (= (ks/without-ns exception-type) :cli-help)))
          (sling/throw+ e)
          (do
            ;; Non-cli error, recast to cli slingshot error
            (log/error e (.getMessage e))
            (sling/throw+
             {:type ::cli-error
              :message (.getMessage e)}
             e)))))))

(defn -main
  [& args]
  (cli/run jrubies-run-wrapper! args))
