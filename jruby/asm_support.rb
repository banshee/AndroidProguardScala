require 'java'
require 'asm.jar'
require 'pp'

module AsmSupport
  # This lets me type org.objectweb.asm::... instead of Java::OrgObjectwebAsm::...
  def org
    Java::Org
  end

  java_import  org.objectweb.asm::Type

  module AllVisitorInterfaces
    include org.objectweb.asm::MethodVisitor
    include org.objectweb.asm::ClassVisitor
    include org.objectweb.asm::FieldVisitor
    include org.objectweb.asm::AnnotationVisitor
    include org.objectweb.asm.signature::SignatureVisitor
  end

  class GenericVisitor
    include AllVisitorInterfaces
    def initialize
      @result = ""
    end

    def get_result
      @result
    end

    # There are two visit methods, on AnnotationVisitor and ClassVisitor.
    # Split those into visitAsAnnotation and visitAsClass
    def visit *args
      if args.size == 2
        visitAsAnnotation *args
      else
        visitAsClass *args
      end
    end

    def method_missing name, *args
      @seen ||= {}
      if name.to_s =~ /^visit.*/
        if name.to_s =~ /visitCode/
          return nil
        end
        @seen[name] = 1
        #        pp "seenMethods: ", @seen
        return self
      end
      super
    end
  end

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
