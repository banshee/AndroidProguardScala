require 'java'
require 'proguard_cache_requires'
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
end
