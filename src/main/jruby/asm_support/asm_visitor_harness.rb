require 'java'
require 'proguard_cache_requires'

module AsmSupport
  class AsmVisitorHarness
    def initialize visitor_class
      @visitor_class = visitor_class
    end

    def build visitor_object, file_input_stream
      begin
        construct = org.objectweb.asm::ClassReader.java_class.constructor(java.io.InputStream)
        class_reader = (construct.new_instance file_input_stream).to_java
        result = class_reader.accept(visitor_object, 0)
      rescue Exception => e
        require 'pp'
        pp "EXCEPTION ------------------------------------", e
        pp e.backtrace
      end
      result
    end

    def name_to_inputstream filename
      f = java.io.File.new filename
      java.io.FileInputStream.new f
    end

    def build_for_filename filename
      inputstream = name_to_inputstream filename
      build_for_inputstream inputstream
    end

    def build_for_jar_entry jarfile, jarentry
      i = jarfile.get_input_stream jarentry
      build_for_inputstream i
    end

    def build_for_inputstream inputstream
      visitor = @visitor_class.new
      build visitor, inputstream
      visitor.get_result
    end

    def self.build_for_filename klass, filename
      o = AsmVisitorHarness.new klass
      case filename
      when /\.class$/
        o.build_for_filename filename
      when /\.jar$/
        result = {}
        jarfile = java.util.jar.JarFile.new filename
        jarfile.entries.each do |e|
          if e.to_s =~ /\.class$/
            new_data = (o.build_for_jar_entry jarfile, e)
            result.merge! new_data
          end
        end
        result
      end
    end
  end
end
