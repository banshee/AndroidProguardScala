require 'rubygems'
require 'pathname'
require 'asm_support'
require 'required_classes'
require 'runnable_callable'
require 'pp'
require 'dependency_calculator'

include AsmSupport

class DependencyTracker
  def initialize
    @dependencies = {}
  end

  def add_dependencies dependency_hash
    @dependencies.merge! dependency_hash
  end

  def populate files
    @dependencies = DependencyCalculator.calculate files
  end

  def add_files files
    new_dependencies = DependencyCalculator.calculate files
    @dependencies.merge! new_dependencies
  end
  
  def calculate_classes starting_points_hash
    RequiredClasses.extract_required_classes @dependencies, starting_points_hash
  end
  
  def calculate_class_file_paths starting_points_hash
    classes = calculate_classes starting_points_hash
    classes.map {|c| RequiredClasses.class_to_relative_filename c}.sort
  end
  
  attr_accessor :dependencies
end

tracker = DependencyTracker.new
tracker.populate ARGV
paths = tracker.calculate_class_file_paths 'com/restphone/MySampleApp' => 1
puts "paths are", (paths.join "\n")

#dependencies = DependencyCalculator.calculate ARGV
#
#dependencies = existing_dependencies.add_dependencies dependencies
#
#pp "dependencies are", dependencies
#
#classes = RequiredClasses.extract_required_classes dependencies, {'scala/Predef$'  => 1, 'com/restphone/MySampleApp' => 1}
#relative_paths = classes.map {|c| RequiredClasses.class_to_relative_filename c}
#pp "classes are ", relative_paths.sort
## RequiredClasses.copy_required_files relative_paths, 'original_scala_classes', 'filtered_scala_classes'
