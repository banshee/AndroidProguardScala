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
