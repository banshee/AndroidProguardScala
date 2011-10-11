require 'java'
require 'asm.jar'
require 'pp'

module AsmSupport
  class RubyInterfaceImplementationBuilder
    def initialize visitor_class
      @visitor_class = visitor_class
    end

    def build visitor_object, file_input_stream
      begin
        construct = org.objectweb.asm::ClassReader.java_class.constructor(java.io.InputStream)
        class_reader = (construct.new_instance file_input_stream).to_java
        result = class_reader.accept(visitor_object, 0)
      rescue Exception => e
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
  end
end
