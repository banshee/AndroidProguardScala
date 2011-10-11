require 'java'

# You'll need to replace GEM_HOME with your own gem directory
ENV['GEM_HOME'] = "/Users/james/jrubygems"

# Replace with the output of pp $LOAD_PATH from jirb
$LOAD_PATH.concat ["/Users/james/bin/s3sync",
  "/Library/Frameworks/JRuby.framework/Versions/1.6.4/lib/ruby/site_ruby/1.8",
  "/Library/Frameworks/JRuby.framework/Versions/1.6.4/lib/ruby/site_ruby/shared",
  "/Library/Frameworks/JRuby.framework/Versions/1.6.4/lib/ruby/1.8",
  "/Users/james/experimements/AsmSample/libs",
  "/Users/james/workspace",
  "."]

$CLASSPATH << "/Users/james/workspace/JrubyEclipsePlugin/bin"

require 'asm_support'

class FilterCallback
  include AsmSupport
  include  Java::ComRestphoneJrubyeclipse::IJrubyFilter


  # TBD This works, but isn't really correct.  See
  # http://stackoverflow.com/questions/7542704/in-an-eclipse-plugin-whats-the-right-way-to-get-the-class-file-that-correspond
  # for how to improve this.
  def selection_to_path selection
    path = selection.get_paths.first

    output_location = path.get_first_segment.get_output_location # IPath
    root = org.eclipse.core.resources::ResourcesPlugin.getWorkspace().get_root
    folder = root.get_folder output_location
    folder.get_location.to_s

    path_to_file_as_string = path.get_last_segment.get_path.to_s
    path_to_file_as_string[/\.java$/] = ".class"
    path_elements_of_java_file = path_to_file_as_string.split("/")[3..-1]

    all_elements = folder.get_location.to_s.split("/") + path_elements_of_java_file

    File.join(all_elements)
  end

  def do_filter selection
    begin

      file = "/Users/james/experimements/AsmSample/bin/com/restphone/classSignature/MtdVisitor.class"
      
      file = selection_to_path selection
      

      builder = RubyInterfaceImplementationBuilder.new InterfaceVisitor

      builder.build_for_filename file
    rescue Exception => e
      e.methods.sort.join("\n") + e.class.ancestors.join("\n")
      "got ruby exception " + e.message + " " + e.backtrace.join("\n")
    end
  end
end

# eclipse_callback is a special name.  Define an #eclipse_callback method in your file, and the plugin
# will call it to create an Java::ComRestphoneJrubyeclipse::IJrubyFilter object.  It must return
# an object that implements the Java::ComRestphoneJrubyeclipse::IJrubyFilter interface.
def eclipse_callback
  FilterCallback.new
end

