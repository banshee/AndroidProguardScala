require 'java'
require 'pp'

module AsmSupport
  class DependencySignatureVisitor < GenericVisitor
    def hash_with_subhash
      Hash.new {|hash, k| hash[k] = hash_with_subhash}
    end

    def initialize
      @result = hash_with_subhash
      @current_class = "builder startup"
      @tracked_methods = [
        :visitAnnotation,
        :visitTypeInsn,
        :visitFieldInsn,
        :visitEnum,
        :visitFormalTypeParameter,
        :visitTypeVariable,
        :visitClassType,
        :visitMethodInsn]
    end

    def visitAsClass *args
      #      int version,
      #      int access,
      #      String name,
      #      String signature,
      #      String superName,
      #      String[] interfaces);
      name = args[2]
      @current_class = name
      depends_on :visitAsClass, *args
    end

    def method_missing name, *args
      if @tracked_methods.include? name
        depends_on name, *args
        return self
      end
      super
    end

    def depends_on method_name, *args
      tags = args.map do |a|
        case a
        when Enumerable
          a.to_a
        else
          a
        end
      end
      @result[@current_class][([method_name] + tags).join("*")] = 1
      self
    end
  end
end
