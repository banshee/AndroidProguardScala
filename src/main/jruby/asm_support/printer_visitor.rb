require 'java'

module AsmSupport
  class PrinterVisitor < GenericVisitor
    def asm_args_to_string args
      result = (args || [nil]).map do |a|
        case a
        when String
          %Q("#{a}")
        when Java::OrgObjectwebAsm::Label
          "LabelForSwitchEtc"
        when Java::OrgObjectwebAsm::Attribute
          a.get_labels ? (asm_args_to_string a.get_labels) : "no_labels"
        when Enumerable, ArrayJavaProxy
          if a.length > 0
            "[" + a.map {|v| v.to_s}.join(", ") + "]"
          else
            "[]"
          end
        when nil
          "nil"
        else
          a.pretty_inspect.gsub "\n", ""
        end
      end.map {|x| x[0..300]}
      result.length > 1 ? result.join(", ") : result
    end

    def visitAsAnnotation *args
      case args.first
      when "bytes"
        "#{args.first.length} bytes"
      else
        asm_args_to_string args
      end
    end

    def method_missing name, *args
      name_string = name.to_s
      if name_string =~ /visit.*/
        @result << "#{name}:\t#{asm_args_to_string args}" << "\n"
        self
      else
        super
      end
    end
  end
end
