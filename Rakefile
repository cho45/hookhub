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

@classpath  = (ENV['CLASSPATH'] || "").split(':')
@classpath << SCALA_HOME + "lib/scala-library.jar"
@classpath << SCALA_HOME + "lib/scala-compiler.jar"
@classpath << Pathname.glob("#{ENV['HOME']}/lib/java/**/*.jar")
@classpath << Pathname.glob(GAE_SDK + "lib/**/*.jar")
@classpath.flatten!
puts @classpath
ENV['CLASSPATH'] = @classpath.join(":")

def src(ext='', pre='')
	FileList["src/**/*.scala"].map {|i|
		pre + i.sub(/^src\//, '').sub(/\..+?$/, ext)
	}
end

def scalac(*args)
	sh "scalac", *args
rescue => e
	puts e
end

task :default => :run

task :run => [:build] do
	sh GAE_SDK + "bin/dev_appserver.sh", "war"
end

task :build => [:copylibs, "war/WEB-INF/classes"] + src('.class', 'war/WEB-INF/classes/')

task :copylibs do
	dest = Pathname("war/WEB-INF/lib")
	dest.mkpath unless dest.exist?

	Pathname.glob([
		"lib/*.jar",
		SCALA_HOME + "**/scala-library.jar",
		GAE_SDK + "lib/user/appengine-api-*-sdk-*.jar"
	].join("\0")) do |jar|
		next if (dest + jar.basename).exist?
		cp jar, dest
	end
end

file "war/WEB-INF/classes" do |t|
	classes = Pathname("war/WEB-INF/classes")
#	classes.rmtree if classes.exist?
#	classes.mkpath
	cp_r "src", classes
end

src.each do |s|
	file "war/WEB-INF/classes/#{s}.class" => "src/#{s}.scala" do |t|
		scalac "-d", "war/WEB-INF/classes", "src/#{s}.scala"
	end
end

