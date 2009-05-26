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

task :build => [:copylibs, "war/WEB-INF/classes"] + FileList["war/WEB-INF/classes/**/*.scala"].map {|i| i.sub(/scala$/, 'class') }

task :copylibs do
	dest = Pathname("war/WEB-INF/lib")
	dest.mkpath unless dest.exist?

	Pathname.glob([
		SCALA_HOME + "**/scala-library.jar",
		GAE_SDK + "lib/user/appengine-api-*-sdk-*.jar"
	].join("\0")) do |jar|
		next if (dest + jar.basename).exist?
		cp jar, dest
	end
end

file "war/WEB-INF/classes" => FileList["src/**/*"] do |t|
	p t
	classes = Pathname("war/WEB-INF/classes")
	cp_r "src", classes
end


rule ".class" => [".scala"] do |t|
	scalac "-d", "war/WEB-INF/classes", t.source
end
