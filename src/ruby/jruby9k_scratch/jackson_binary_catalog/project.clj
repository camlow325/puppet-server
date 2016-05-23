(defproject puppetlabs.scratch.jruby9k/jackson-binary-catalog "0.0.1"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cheshire "5.6.1"]
                 [commons-io "2.5"]
                 [commons-lang "2.6"]]

  :pedantic? :abort

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]

  :profiles {:dev {:source-paths  ["dev"]}}

  )
