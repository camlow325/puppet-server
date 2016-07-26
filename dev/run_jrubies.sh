#!/bin/bash

set -e 

while true;
do
   #lein jrubies --config ~/puppet-scratch/puppetserver.conf   
   java -Xms2g -Xmx2g -Djruby.debug.loadService=true -cp ./target/puppet-server-release.jar clojure.main -m puppetlabs.puppetserver.cli.jrubies --config ~/puppet-scratch/puppetserver.conf
done
