require 'rubygems'
require 'pathname'
require 'asm_support'
require 'required_classes'
require 'runnable_callable'
require 'dependency_tracker'
require 'can_opener'

tracker = DependencyTracker.new
tracker.populate ARGV
puts tracker.to_json

#
#paths = tracker.calculate_class_file_paths 'com/restphone/MySampleApp' => 1
#puts "paths are", (paths.join "\n")
#
#jarfile = java.util.jar.JarFile.new '/Users/james/lib/scala-2.9.1.final/lib/scala-library.jar'
#entries = CanOpener.zip_entries_matching jarfile, paths
#entries.each do |e|
#  CanOpener.extract_file jarfile, e, 'scalastuff'
#end
