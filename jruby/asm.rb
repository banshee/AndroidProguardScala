require 'rubygems'
require 'asm_support'
require 'asm_support/printer_visitor'
require 'rake'
require 'pp'

include AsmSupport

# Default to PrinterVisitor
klass = PrinterVisitor

args = ARGV.dup
args = args.map do |a|
  if a =~ /\*/
    Dir.glob a
  else
    a
  end
end
# Loop through each .class file given on the command line
args.flatten.each do |arg|
  begin
    new_klass = eval arg
  rescue Exception => exc
  end
  if Class === new_klass
    klass = new_klass
  else
    Dir.glob arg do |d|
      builder = RubyInterfaceImplementationBuilder.new klass
      case d
      when /\.class$/
        result = builder.build_for_filename d
        puts result
      when /\.jar$/
        jarfile = java.util.jar.JarFile.new d
        jarfile.entries.each do |e|
          if e.to_s =~ /\.class$/
            result = builder.build_for_jar_entry jarfile, e
            puts result
          end
        end
      end
    end
  end
end
