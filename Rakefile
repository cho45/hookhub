#!rake

require "rake"
require 'rake/clean'
require "pathname"
require "fileutils"

include FileUtils::Verbose

SCALA      = Pathname(`which scala`.chomp)
SCALA_HOME = SCALA + "../../"
GAE_SDK    = Pathname.glob("#{ENV['HOME']}/sdk/appengine-java-sdk-*")[0]

CLEAN.include [%w[war/WEB-INF/lib/* war/WEB-INF/classes .buildtime]]

@classpath  = (ENV['CLASSPATH'] || "").split(':')
@classpath << SCALA_HOME + "lib/scala-library.jar"
@classpath << SCALA_HOME + "lib/scala-compiler.jar"
@classpath << Pathname.glob("#{ENV['HOME']}/lib/java/**/*.jar")
@classpath << Pathname.glob(GAE_SDK + "lib/**/*.jar")
@classpath << Pathname.glob("lib/*.jar")
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
end

def screen_info(i)
	puts "k:%s\\" % i
	system("screen", "-X", "echo", i)
end

def build_all
	buildtime = File.mtime(".buildtime").to_i rescue 0
	srcs = src(".scala", "src/").select {|f|
		File.mtime(f).to_i > buildtime
	}
	ret = nil
	unless srcs.empty?
		t = Time.now
		screen_info "scalac #{srcs.join(' ')}"
		ret = scalac "-d", "war/WEB-INF/classes", *srcs
		screen_info "done"

		touch ".buildtime" unless File.exist?(".buildtime")
		File.utime t, t, ".buildtime"
	end
	ret
rescue => e
	screen_info e.to_s
	sleep 3
	ret = nil
end

task :default => :run

task :deploy do
	sh GAE_SDK + "bin/appcfg.sh", "update", "war"
end

task :run => [:build] do
	pid = nil
	END {
		Process.kill(:INT, -pid)
	}
	loop do
		if !pid || build_all
			Process.kill(:INT, -pid) if pid
			pid = fork {
				Process.setpgrp
				exec(GAE_SDK + "bin/dev_appserver.sh", "war")
			}
		end
		sleep 1
	end
end

task :build => [:copylibs, "war/WEB-INF/classes"] do
	build_all
end

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


