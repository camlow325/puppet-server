require 'open3'

PROJECT_ROOT = File.dirname(__FILE__)
ACCEPTANCE_ROOT = ENV['ACCEPTANCE_ROOT'] ||
  File.join(PROJECT_ROOT, 'acceptance')
PUPPET_SRC = File.join(PROJECT_ROOT, 'ruby', 'puppet')
PUPPET_LIB = File.join(PROJECT_ROOT, 'ruby', 'puppet', 'lib')
PUPPET_SPEC = File.join(PROJECT_ROOT, 'ruby', 'puppet', 'spec')
FACTER_LIB = File.join(PROJECT_ROOT, 'ruby', 'facter', 'lib')
PUPPET_SERVER_RUBY_SRC = File.join(PROJECT_ROOT, 'src', 'ruby', 'puppetserver-lib')

TEST_GEMS_DIR = File.join(PROJECT_ROOT, 'vendor', 'test_gems')
TEST_BUNDLE_DIR = File.join(PROJECT_ROOT, 'vendor', 'test_bundle')

RAKE_ROOT = File.expand_path(File.dirname(__FILE__))

GEM_SOURCE = ENV['GEM_SOURCE'] || "http://rubygems.delivery.puppetlabs.net"

def assemble_default_beaker_config
  if ENV["BEAKER_CONFIG"]
    return ENV["BEAKER_CONFIG"]
  end

  platform = ENV['PLATFORM']
  layout = ENV['LAYOUT']

  if platform and layout
    beaker_config = "#{ACCEPTANCE_ROOT}/config/beaker/jenkins/"
    beaker_config += "#{platform}-#{layout}.cfg"
  else
    abort "Must specify an appropriate value for BEAKER_CONFIG. See acceptance/README.md"
  end

  return beaker_config
end

def setup_smoke_hosts_config
  sh "bundle exec beaker-hostgenerator centos7-64m-64a > acceptance/scripts/hosts.cfg"
end

def basic_smoke_test(package_version)
  beaker = "PACKAGE_BUILD_VERSION=#{package_version}"
  beaker += " bundle exec beaker --debug --root-keys --repo-proxy"
  beaker += " --preserve-hosts always"
  beaker += " --type aio"
  beaker += " --helper acceptance/lib/helper.rb"
  beaker += " --options-file acceptance/config/beaker/options.rb"
  beaker += " --load-path acceptance/lib"
  beaker += " --config acceptance/scripts/hosts.cfg"
  beaker += " --keyfile ~/.ssh/id_rsa-acceptance"
  beaker += " --pre-suite acceptance/suites/pre_suite/foss"
  beaker += " --post-suite acceptance/suites/post_suite"
  beaker += " --tests acceptance/suites/tests/00_smoke"

  sh beaker
end

# TODO: this could be DRY'd up with the method above, but it seemed like it
# might make it a little harder to read and didn't seem worth the effort yet
def re_run_basic_smoke_test
  beaker = "bundle exec beaker --debug --root-keys --repo-proxy"
  beaker += " --preserve-hosts always"
  beaker += " --type aio"
  beaker += " --helper acceptance/lib/helper.rb"
  beaker += " --options-file acceptance/config/beaker/options.rb"
  beaker += " --load-path acceptance/lib"
  beaker += " --config acceptance/scripts/hosts.cfg"
  beaker += " --keyfile ~/.ssh/id_rsa-acceptance"
  beaker += " --tests acceptance/suites/tests/00_smoke"

  sh beaker
end

namespace :spec do
  task :init do
    if ! Dir.exists? TEST_GEMS_DIR
      ## Install bundler
      ## Line 1 launches the JRuby that we depend on via leiningen
      ## Line 2 programmatically runs 'gem install bundler' via the gem command that comes with JRuby
      gem_install_bundler = <<-CMD
      GEM_HOME='#{TEST_GEMS_DIR}' GEM_PATH='#{TEST_GEMS_DIR}' \
      lein run -m org.jruby.Main \
      -e 'load "META-INF/jruby.home/bin/gem"' install -i '#{TEST_GEMS_DIR}' --no-rdoc --no-ri bundler --source '#{GEM_SOURCE}'
      CMD
      sh gem_install_bundler

      path = ENV['PATH']
      ## Install gems via bundler
      ## Line 1 makes sure that our local bundler script is on the path first
      ## Line 2 tells bundler to use puppet's Gemfile
      ## Line 3 tells JRuby where to look for gems
      ## Line 4 launches the JRuby that we depend on via leiningen
      ## Line 5 runs our bundle install script
      bundle_install = <<-CMD
      PATH='#{TEST_GEMS_DIR}/bin:#{path}' \
      BUNDLE_GEMFILE='#{PUPPET_SRC}/Gemfile' \
      GEM_HOME='#{TEST_GEMS_DIR}' GEM_PATH='#{TEST_GEMS_DIR}' \
      lein run -m org.jruby.Main \
        -S bundle install --without extra development --path='#{TEST_BUNDLE_DIR}' --retry=3
      CMD
      sh bundle_install
    end
  end
end

desc "Run rspec tests"
task :spec => ["spec:init"] do
  ## Run RSpec via our JRuby dependency
  ## Line 1 tells bundler to use puppet's Gemfile
  ## Line 2 tells JRuby where to look for gems
  ## Line 3 launches the JRuby that we depend on via leiningen
  ## Line 4 adds all our Ruby source to the JRuby LOAD_PATH
  ## Line 5 runs our rspec wrapper script
  ## <sarcasm-font>dang ole real easy man</sarcasm-font>
  run_rspec_with_jruby = <<-CMD
    BUNDLE_GEMFILE='#{PUPPET_SRC}/Gemfile' \
    GEM_HOME='#{TEST_GEMS_DIR}' GEM_PATH='#{TEST_GEMS_DIR}' \
    lein run -m org.jruby.Main \
      -I'#{PUPPET_LIB}' -I'#{PUPPET_SPEC}' -I'#{FACTER_LIB}' -I'#{PUPPET_SERVER_RUBY_SRC}' \
      ./spec/run_specs.rb
  CMD
  sh run_rspec_with_jruby
end

namespace :test do

  namespace :acceptance do
    desc "Run beaker based acceptance tests"
    task :beaker do |t, args|

      # variables that take a limited set of acceptable strings
      type = ENV["BEAKER_TYPE"] || "pe"

      # variables that take pathnames
      beakeropts = ENV["BEAKER_OPTS"] || ""
      presuite = ENV["BEAKER_PRESUITE"] || "#{ACCEPTANCE_ROOT}/suites/pre_suite/#{type}"
      postsuite = ENV["BEAKER_POSTSUITE"] || ""
      helper = ENV["BEAKER_HELPER"] || "#{ACCEPTANCE_ROOT}/lib/helper.rb"
      testsuite = ENV["BEAKER_TESTSUITE"] || "#{ACCEPTANCE_ROOT}/suites/tests"
      loadpath = ENV["BEAKER_LOADPATH"] || ""
      options = ENV["BEAKER_OPTIONSFILE"] || "#{ACCEPTANCE_ROOT}/config/beaker/options.rb"

      # variables requiring some assembly
      config = assemble_default_beaker_config

      beaker = "beaker "

      beaker += " -c #{config}"
      beaker += " --helper #{helper}"
      beaker += " --type #{type}"

      beaker += " --options-file #{options}" if options != ''
      beaker += " --load-path #{loadpath}" if loadpath != ''
      beaker += " --pre-suite #{presuite}" if presuite != ''
      beaker += " --post-suite #{postsuite}" if postsuite != ''
      beaker += " --tests " + testsuite if testsuite != ''

      beaker += " " + beakeropts

      sh beaker
    end

    desc "Do an ezbake build, and then a beaker smoke test off of that build, preserving the vmpooler host"
    task :bakeNbeak do
      package_version = nil

      Open3.popen3("lein with-profile ezbake ezbake build 2>&1") do |stdin, stdout, stderr, thread|
        # sleep 5
        # puts "STDOUT IS: #{stdout}"
        success = true
        stdout.each do |line|
          if match = line.match(%r|^Your packages will be available at http://builds.delivery.puppetlabs.net/puppetserver/(.*)$|)
            package_version = match[1]
          elsif line =~ /^Packaging FAILURE\s*$/
            success = false
          end
          puts line
        end
        exit_code = thread.value
        if success == true
          puts "PACKAGE VERSION IS #{package_version}"
        else
          puts "\n\nPACKAGING FAILED!  exit code is '#{exit_code}'.  STDERR IS:"
          puts stderr.read
          exit 1
        end
      end

      begin
        setup_smoke_hosts_config()
        basic_smoke_test(package_version)
      rescue => e
        puts "\n\nJOB FAILED; PACKAGE VERSION WAS: #{package_version}\n\n"
        raise e
      end
    end

    desc "Do a basic smoke test, using the package version specified by PACKAGE_BUILD_VERSION, preserving the vmpooler host"
    task :smoke do
      package_version = ENV["PACKAGE_BUILD_VERSION"]
      unless package_version
        STDERR.puts("'smoke' task requires PACKAGE_BUILD_VERSION environment variable")
        exit 1
      end
      setup_smoke_hosts_config()
      basic_smoke_test(package_version)
    end

    desc "Re-run the basic smoke test on the host preserved from a previous run of the 'smoke' task"
    task :resmoke do
      re_run_basic_smoke_test()
    end

  end
end

build_defs_file = File.join(RAKE_ROOT, 'ext', 'build_defaults.yaml')
if File.exist?(build_defs_file)
  begin
    require 'yaml'
    @build_defaults ||= YAML.load_file(build_defs_file)
  rescue Exception => e
    STDERR.puts "Unable to load yaml from #{build_defs_file}:"
    raise e
  end
  @packaging_url  = @build_defaults['packaging_url']
  @packaging_repo = @build_defaults['packaging_repo']
  raise "Could not find packaging url in #{build_defs_file}" if @packaging_url.nil?
  raise "Could not find packaging repo in #{build_defs_file}" if @packaging_repo.nil?

  namespace :package do
    desc "Bootstrap packaging automation, e.g. clone into packaging repo"
    task :bootstrap do
      if File.exist?(File.join(RAKE_ROOT, "ext", @packaging_repo))
        puts "It looks like you already have ext/#{@packaging_repo}. If you don't like it, blow it away with package:implode."
      else
        cd File.join(RAKE_ROOT, 'ext') do
          %x{git clone #{@packaging_url}}
        end
      end
    end
    desc "Remove all cloned packaging automation"
    task :implode do
      rm_rf File.join(RAKE_ROOT, "ext", @packaging_repo)
    end
  end
end

begin
  load File.join(RAKE_ROOT, 'ext', 'packaging', 'packaging.rake')
rescue LoadError
end
