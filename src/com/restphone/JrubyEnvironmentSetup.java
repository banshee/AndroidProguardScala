package com.restphone;

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.RubyClass;


public class JrubyEnvironmentSetup extends RubyObject  {
    private static final Ruby __ruby__ = Ruby.getGlobalRuntime();
    private static final RubyClass __metaclass__;

    static {
        String source = new StringBuilder("require 'java'\n" +
            "\n" +
            "#$LOAD_PATH << \"/Users/james/workspace/AndroidProguardScala/jruby\"\n" +
            "#$LOAD_PATH << \"/Users/james/src/jruby/lib/ruby/1.8/\"\n" +
            "\n" +
            "java_package 'com.restphone'\n" +
            "\n" +
            "class JrubyEnvironmentSetup\n" +
            "  java_signature 'void addJrubyJarfile(String pathToJrubyCompleteJarfile)'\n" +
            "  def self.add_jruby_jarfile jruby_complete_jarfile\n" +
            "    require 'jruby'\n" +
            "    require jruby_complete_jarfile\n" +
            "    ruby_paths =\n" +
            "    if JRuby.runtime.is1_9\n" +
            "      %w{ site_ruby/1.9 site_ruby/shared site_ruby/1.8 1.9 }\n" +
            "    else\n" +
            "      %w{ site_ruby/1.8 site_ruby/shared 1.8 }\n" +
            "    end\n" +
            "    ruby_paths.each do |path|\n" +
            "      full_path = jruby_complete_jarfile + \"!/META-INF/jruby.home/lib/ruby/#{path}\"\n" +
            "      full_path = \"META-INF/jruby.home/lib/ruby/#{path}\"\n" +
            "      $LOAD_PATH << full_path unless $LOAD_PATH.include?(full_path)\n" +
            "    end\n" +
            "    puts \"load path: \" + $LOAD_PATH.join(\"\\n,\\n\")\n" +
            "  end\n" +
            "\n" +
            "  java_signature 'void addToLoadPath(String file)'\n" +
            "\n" +
            "  def self.add_to_load_path file\n" +
            "    $LOAD_PATH << file\n" +
            "    puts \"new load path: \" + $LOAD_PATH.join(\",\")\n" +
            "  end\n" +
            "\n" +
            "  java_signature 'void addIvyDirectoryToLoadPath(String dir)'\n" +
            "\n" +
            "  def self.add_ivy_directory_to_load_path dir\n" +
            "    require 'pathname'\n" +
            "    all_jars = Dir.glob(dir.to_s + \"/**/*.jar\")\n" +
            "    all_jars.each do |j|\n" +
            "      f = Pathname.new j\n" +
            "      case j\n" +
            "      when /asm-all-3.3.1.jar/, /proguard-base-4.6.jar/\n" +
            "        $LOAD_PATH << f.parent\n" +
            "        puts \"new load path: \" + $LOAD_PATH.join(\",\")\n" +
            "      end\n" +
            "    end\n" +
            "  end\n" +
            "end\n" +
            "").toString();
        __ruby__.executeScript(source, "jruby/jruby_environment_setup.rb");
        RubyClass metaclass = __ruby__.getClass("JrubyEnvironmentSetup");
        metaclass.setRubyStaticAllocator(JrubyEnvironmentSetup.class);
        if (metaclass == null) throw new NoClassDefFoundError("Could not load Ruby class: JrubyEnvironmentSetup");
        __metaclass__ = metaclass;
    }

    /**
     * Standard Ruby object constructor, for construction-from-Ruby purposes.
     * Generally not for user consumption.
     *
     * @param ruby The JRuby instance this object will belong to
     * @param metaclass The RubyClass representing the Ruby class of this object
     */
    private JrubyEnvironmentSetup(Ruby ruby, RubyClass metaclass) {
        super(ruby, metaclass);
    }

    /**
     * A static method used by JRuby for allocating instances of this object
     * from Ruby. Generally not for user comsumption.
     *
     * @param ruby The JRuby instance this object will belong to
     * @param metaclass The RubyClass representing the Ruby class of this object
     */
    public static IRubyObject __allocate__(Ruby ruby, RubyClass metaClass) {
        return new JrubyEnvironmentSetup(ruby, metaClass);
    }
        
    /**
     * Default constructor. Invokes this(Ruby, RubyClass) with the classloader-static
     * Ruby and RubyClass instances assocated with this class, and then invokes the
     * no-argument 'initialize' method in Ruby.
     *
     * @param ruby The JRuby instance this object will belong to
     * @param metaclass The RubyClass representing the Ruby class of this object
     */
    public JrubyEnvironmentSetup() {
        this(__ruby__, __metaclass__);
        RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "initialize");
    }

    
    public static void addJrubyJarfile(String pathToJrubyCompleteJarfile) {
        IRubyObject ruby_pathToJrubyCompleteJarfile = JavaUtil.convertJavaToRuby(__ruby__, pathToJrubyCompleteJarfile);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), __metaclass__, "add_jruby_jarfile", ruby_pathToJrubyCompleteJarfile);
        return;

    }

    
    public static void addToLoadPath(String file) {
        IRubyObject ruby_file = JavaUtil.convertJavaToRuby(__ruby__, file);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), __metaclass__, "add_to_load_path", ruby_file);
        return;

    }

    
    public static void addIvyDirectoryToLoadPath(String dir) {
        IRubyObject ruby_dir = JavaUtil.convertJavaToRuby(__ruby__, dir);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), __metaclass__, "add_ivy_directory_to_load_path", ruby_dir);
        return;

    }

}
