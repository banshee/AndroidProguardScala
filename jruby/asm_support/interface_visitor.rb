require 'java'
require 'asm-all-3.3.1.jar'
require 'pp'

module AsmSupport
  class InterfaceVisitor < GenericVisitor
    def method_signature_to_java_signature sig
      types = Type.getArgumentTypes sig
      i = ?a.ord
      names = types.map do |t|
        result = "#{t.get_class_name} #{i.chr.to_s}"
        i += 1
        result
      end
      "(" + names.join(", ") + ")"
    end

    def visit_method *args
      signature = args[2]
      name = args[1]
      java_signature = method_signature_to_java_signature signature
      return_type = Type.get_return_type signature
      @result << %Q&java_signature "#{return_type.get_class_name} #{name} #{java_signature}"\n&
      @result << "def #{name} *args\nend\n\n"
      self
    end
  end
end
