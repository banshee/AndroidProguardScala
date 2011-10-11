$LOAD_PATH << "/Users/james/lib/asm-3.3.1/lib"

require 'rubygems'
require 'pathname'
require 'asm_support'
require 'required_classes'
require 'runnable_callable'
require 'pp'
require 'dependency_tracker'
require 'can_opener'

tracker = DependencyTracker.new
tracker.populate %w(/Users/james/lib/scala-2.9.1.final/lib/scala-library.jar)
dependencies = tracker.dependencies

pp "dependsare", dependencies

def hash_to_dot_entries h
  result = []
  h.each_pair do |k, v|
    v.keys.each do |v1|
      s =  %Q("#{k}" -> "#{v1}")
      result << s
    end
  end
  result
end

r = hash_to_dot_entries dependencies
puts "digraph {"
puts r.join ";\n"
puts "}"
