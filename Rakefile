#!rake

require "rake"
require 'rake/clean'
require "pathname"
require "fileutils"

include FileUtils::Verbose

SCALA      = Pathname(`which scala`.chomp)
SCALA_HOME = SCALA + "../../share/scala"
GAE_SDK    = Pathname.glob("#{ENV['HOME']}/sdk/appengine-java-sdk-*")[0]

CLEAN.include [%w[war/WEB-INF/lib/* war/WEB-INF/classes]]

@classpath  = ENV['CLASSPATH'].split(':')
@classpath << Pathname.glob(GAE_SDK + "lib/**/*.jar")
@classpath << SCALA_HOME + "lib/scala-library.jar"
@classpath << SCALA_HOME + "lib/scala-compiler.jar"
@classpath.flatten!

warn "classpath:"
@classpath.each do |path|
	warn path
end

ENV['CLASSPATH'] = @classpath.join(":")

def scalac(*args)
	sh "scalac", *args
end

task :default => :run

task :run => [:build] do
	sh GAE_SDK + "bin/dev_appserver.sh", "war"
end

task :build => [:copylibs] do
	classes = Pathname("war/WEB-INF/classes")

	cp_r "src", classes
	# file, rule を使うようにする
	scalac "-d", classes, *Pathname.glob("src/**/*.scala")
end

task :copylibs do
	dest = Pathname("war/WEB-INF/lib")
	dest.mkpath unless dest.exist?

	cp Pathname.glob(SCALA_HOME + "**/scala-library.jar"), dest
	cp Pathname.glob(GAE_SDK    + "lib/user/appengine-api-*-sdk-*.jar"), dest
end

