require 'java'
require 'pp'
require 'asm_support'

module AsmSupport
  class DependencyVisitor < GenericVisitor
    def hash_with_subhash
      Hash.new {|hash, k| hash[k] = hash_with_subhash}
    end

    def initialize
      @result = hash_with_subhash
      @current_class = nil
    end

    def visitAsClass *args
      #      int version,
      #      int access,
      #      String name,
      #      String signature,
      #      String superName,
      #      String[] interfaces);
      name = args[2]
      super_name = args[4]
      interfaces = args[5]
      @current_class = name
      depends_on :class,  name, super_name
      interfaces.each do |i|
        depends_on :class,  name, i
      end
      self
    end

    def type_to_strings t
      case t.get_sort
      when org.objectweb.asm::Type::OBJECT
        [t.get_internal_name]
      when org.objectweb.asm::Type::ARRAY
        [t.get_element_type]
      else
        []
      end
    end

    def visit_method *args
      require 'pp'
      require 'thread'
      @@mutex ||= Mutex.new
      @@mutex.synchronize {
        pp "in visit method", args, :done
      }
      name = args[1]
      descriptor = args[2]
      return_type = Type.get_return_type descriptor
      (type_to_strings return_type).each do |t|
        depends_on :class,  @current_class, t
      end
      (Type.get_argument_types descriptor).each do |t|
        (type_to_strings t).each do |u|
          depends_on :class,  @current_class, u
        end
      end
      self
    end

    # Remove [[[L....; from class names
    def normalize_class_name c
      result = c.to_s
      result = result.sub %r(\[?L?), ''
      result = result.sub /;$/, ''
      result
    end

    def depends_on method_or_class, klass, d
      return unless d
      return self if klass == d
      k = normalize_class_name klass
      d = normalize_class_name d
      @result[method_or_class][k][d] = 1
      self
    end

    def type_in_second_argument *args
      depends_on :class,  @current_class, args[1]
      self
    end

    # There are several visitAnnotation methods.
    # AnnotationVisitor#visitAnnotation has the
    # descriptor in the second argument.
    #
    # The other calls take a String and a boolean,
    # with the descriptor in the first argument.
    def visit_annotation first_arg, second_arg, *args
      if String === second_arg
        depends_on :class,  @current_class, second_arg
      else
        depends_on :class,  @current_class, first_arg
      end
      self
    end

    # void visitMethodInsn(int opcode, String owner, String name, String desc);
    def visit_method_insn opcode, owner, name, desc
      @@mutex ||= Mutex.new
      @@mutex.synchronize {
        pp "in visit method insn", opcode, owner, name, desc
      }
      depends_on :method,  @current_class, owner
    end

    alias_method :visitTypeInsn, :type_in_second_argument
    alias_method :visitFieldInsn, :type_in_second_argument
    alias_method :visitEnum, :type_in_second_argument
    alias_method :visitFormalTypeParameter, :type_in_second_argument
    alias_method :visitTypeVariable, :type_in_second_argument
    alias_method :visitClassType, :type_in_second_argument
    alias_method :visitInnerClassType, :type_in_second_argument
  end
end
