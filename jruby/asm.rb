require 'rubygems'
require 'asm_support'
require 'asm_support/asm_visitor_harness'
require 'asm_support/dependency_signature_visitor'
require 'asm_support/printer_visitor'
require 'asm_support/interface_visitor'

include AsmSupport

# Default to PrinterVisitor
klass = PrinterVisitor

args = ARGV.map do |a|
  if a =~ /\*/
    Dir.glob a
  else
    a
  end
end.flatten

result = {}
# Loop through each .class file given on the command line
args.each do |arg|
  begin
    new_klass = eval arg
  rescue Exception => exc
  end
  if Class === new_klass
    klass = new_klass
  else
    builder = AsmVisitorHarness.new klass
    case arg
    when /\.class$/
      new_data = AsmVisitorHarness.build_for_filename klass, arg
      result.merge! new_data
    when /\.jar$/
      jarfile = java.util.jar.JarFile.new arg
      jarfile.entries.each do |e|
        if e.to_s =~ /\.class$/
          new_data = (builder.build_for_jar_entry jarfile, e)
          result.merge! new_data
        end
      end
    end
    pp result
  end
end
