source ENV['GEM_SOURCE'] || "https://rubygems.org"

gem 'rake', :group => [:development, :test]
gem 'jira-ruby', :group => :development

group :test do
  gem 'rspec'
  gem 'beaker', '~>1.21.0'
  if ENV['GEM_SOURCE'] =~ /rubygems\.delivery\.puppetlabs\.net/
    gem 'sqa-utils', '0.12.1'
  end

  # beaker 1.21.0 requires google-api-client 0.7.1, which transitively requires
  # addressable ~> 2.3.  addressable, as of 2.5.0, started requiring the
  # public_suffix gem, which requires Ruby 2.  Pinning addressable to ~> 2.4.0
  # so that it will install on Ruby 1.9.3.
  gem 'addressable', '~> 2.4.0'

  # docker-api 1.32.0 requires ruby 2.0.0
  gem 'docker-api', '1.31.0'
end

if File.exists? "#{__FILE__}.local"
  eval(File.read("#{__FILE__}.local"), binding)
end
