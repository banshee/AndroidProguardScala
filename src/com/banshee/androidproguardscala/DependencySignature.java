package com.banshee.androidproguardscala;

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.RubyClass;


public class DependencySignature extends RubyObject  {
    private static final Ruby __ruby__ = Ruby.getGlobalRuntime();
    private static final RubyClass __metaclass__;

    static {
        String source = new StringBuilder("$LOAD_PATH.concat [\"/Users/james/bin/s3sync\",\n" +
            "\"/Library/Frameworks/JRuby.framework/Versions/1.6.4/lib/ruby/site_ruby/1.8\",\n" +
            "\"/Library/Frameworks/JRuby.framework/Versions/1.6.4/lib/ruby/site_ruby/shared\",\n" +
            "\"/Library/Frameworks/JRuby.framework/Versions/1.6.4/lib/ruby/1.8\",\n" +
            "\"/Users/james/workspace/AndroidProguardScala/jruby\",\n" +
            "\"/Users/james/workspace/AndroidProguardScala/lib\",\n" +
            "\".\"]\n" +
            "\n" +
            "$CLASSPATH = [\"/Users/james/workspace/AndroidProguardScala/lib/asm.jar\"]\n" +
            "\n" +
            "require 'java'\n" +
            "require 'pathname'\n" +
            "require 'asm_support'\n" +
            "require 'required_classes'\n" +
            "require 'runnable_callable'\n" +
            "require 'dependency_tracker'\n" +
            "require 'can_opener'\n" +
            "\n" +
            "\n" +
            "java_package 'com.banshee.androidproguardscala'\n" +
            "class DependencySignature\n" +
            "  def initialize\n" +
            "    @signature = []\n" +
            "    @signature_by_class = {}\n" +
            "    @tracker = DependencyTracker.new\n" +
            "  end\n" +
            "  \n" +
            "  def glob_list files\n" +
            "    files.map do |f|\n" +
            "      Dir.glob f.to_s\n" +
            "    end.flatten\n" +
            "  end\n" +
            "\n" +
            "  # Add a list of class and jar files to the signature.\n" +
            "  # Returns a list of changes to the signature.\n" +
            "  java_signature 'String add_files(Iterable<String> files)'\n" +
            "  def add_files files\n" +
            "    pp \"filesbefore\", files.map(&:to_s)\n" +
            "    files = glob_list files\n" +
            "    require 'pp'\n" +
            "    pp \"filesafter\", files\n" +
            "    @tracker.populate files.map(&:to_s)\n" +
            "    @signature = @tracker.combined_dependencies\n" +
            "    @signature.join(\"\\n\")\n" +
            "  end\n" +
            "  \n" +
            "  attr_accessor :signature\n" +
            "end\n" +
            "").toString();
        __ruby__.executeScript(source, "dependency_signature.rb");
        RubyClass metaclass = __ruby__.getClass("DependencySignature");
        metaclass.setRubyStaticAllocator(DependencySignature.class);
        if (metaclass == null) throw new NoClassDefFoundError("Could not load Ruby class: DependencySignature");
        __metaclass__ = metaclass;
    }

    /**
     * Standard Ruby object constructor, for construction-from-Ruby purposes.
     * Generally not for user consumption.
     *
     * @param ruby The JRuby instance this object will belong to
     * @param metaclass The RubyClass representing the Ruby class of this object
     */
    private DependencySignature(Ruby ruby, RubyClass metaclass) {
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
        return new DependencySignature(ruby, metaClass);
    }

    
    public  DependencySignature() {
        this(__ruby__, __metaclass__);

        RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "initialize");

    }

    
    public Object glob_list(Object files) {
        IRubyObject ruby_files = JavaUtil.convertJavaToRuby(__ruby__, files);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "glob_list", ruby_files);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public String add_files(Iterable files) {
        IRubyObject ruby_files = JavaUtil.convertJavaToRuby(__ruby__, files);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "add_files", ruby_files);
        return (String)ruby_result.toJava(String.class);

    }

}
