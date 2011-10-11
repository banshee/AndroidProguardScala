$LOAD_PATH.concat ["/Users/james/bin/s3sync",
"/Library/Frameworks/JRuby.framework/Versions/1.6.4/lib/ruby/site_ruby/1.8",
"/Library/Frameworks/JRuby.framework/Versions/1.6.4/lib/ruby/site_ruby/shared",
"/Library/Frameworks/JRuby.framework/Versions/1.6.4/lib/ruby/1.8",
"/Users/james/workspace/AndroidProguardScala/jruby",
"/Users/james/workspace/AndroidProguardScala/lib",
"."]

$CLASSPATH = ["/Users/james/workspace/AndroidProguardScala/lib/asm.jar"]

require 'java'
require 'pathname'
require 'asm_support'
require 'required_classes'
require 'runnable_callable'
require 'dependency_tracker'
require 'can_opener'


java_package 'com.banshee.androidproguardscala'
class DependencySignature
  def initialize
    @signature = []
    @signature_by_class = {}
    @tracker = DependencyTracker.new
  end
  
  def glob_list files
    files.map do |f|
      Dir.glob f.to_s
    end.flatten
  end

  # Add a list of class and jar files to the signature.
  # Returns a list of changes to the signature.
  java_signature 'String add_files(Iterable<String> files)'
  def add_files files
    pp "filesbefore", files.map(&:to_s)
    files = glob_list files
    require 'pp'
    pp "filesafter", files
    @tracker.populate files.map(&:to_s)
    @signature = @tracker.combined_dependencies
    @signature.join("\n")
  end
  
  attr_accessor :signature
end
