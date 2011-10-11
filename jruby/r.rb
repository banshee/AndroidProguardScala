require 'java'
require 'guava.jar'
require 'fileutils'
require 'runnable_callable'
require 'can_opener'

def com
  Java::Com
end

jarfile = java.util.jar.JarFile.new "/Users/james/workspace/experiments/ScalaSample/app.jar"
target_directory = "/tmp/fnordx"

poolrunner = java.util.concurrent::Executors.newFixedThreadPool 10
lambdas_for_entries = jarfile.entries.map {|f| lambda {CanOpener.extract_file jarfile, f, target_directory}}
extraction_lambdas = RunnableCallable.to_runnable_callable lambdas_for_entries
results = poolrunner.invoke_all extraction_lambdas
results.each &:get
poolrunner.shutdown
