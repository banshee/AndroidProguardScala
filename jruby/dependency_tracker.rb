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

  def to_json
    result = ""
    @dependencies.keys.sort.each do |classname|
      result << %Q("#{classname}":\n  [\n) 
      result << @dependencies[classname].keys.sort.map do |signature|
        %Q(  "#{signature}")
      end.join(",\n")
      result << "\n  ]\n"
    end
    result
  end
  
  attr_accessor :dependencies
end
