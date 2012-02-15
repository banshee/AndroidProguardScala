package com.restphone.androidproguardscala.jruby;

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.RubyClass;


public class ProguardCacheJava extends RubyObject  {
    private static final Ruby __ruby__ = Ruby.getGlobalRuntime();
    private static final RubyClass __metaclass__;

    static {
        String source = new StringBuilder("require 'java'\n" +
            "require 'proguard_cache.rb'\n" +
            "\n" +
            "java_package 'com.restphone.androidproguardscala.jruby'\n" +
            "\n" +
            "class ProguardCacheJava\n" +
            "  def initialize *args\n" +
            "    @ruby_object = ProguardCacheRuby.new\n" +
            "  end\n" +
            "  \n" +
            "  def build_dependency_files_and_final_jar *args\n" +
            "    @ruby_object.build_dependency_files_and_final_jar *args\n" +
            "  end\n" +
            "\n" +
            "  java_signature 'void clean_cache(String cacheDir)'\n" +
            "\n" +
            "  def clean_cache cache_dir\n" +
            "    @ruby_object.clean_cache cache_dir\n" +
            "  end\n" +
            "end\n" +
            "").toString();
        __ruby__.executeScript(source, "src/main/jruby/proguard_cache_java_interop.rb");
        RubyClass metaclass = __ruby__.getClass("ProguardCacheJava");
        metaclass.setRubyStaticAllocator(ProguardCacheJava.class);
        if (metaclass == null) throw new NoClassDefFoundError("Could not load Ruby class: ProguardCacheJava");
        __metaclass__ = metaclass;
    }

    /**
     * Standard Ruby object constructor, for construction-from-Ruby purposes.
     * Generally not for user consumption.
     *
     * @param ruby The JRuby instance this object will belong to
     * @param metaclass The RubyClass representing the Ruby class of this object
     */
    private ProguardCacheJava(Ruby ruby, RubyClass metaclass) {
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
        return new ProguardCacheJava(ruby, metaClass);
    }

    
    public  ProguardCacheJava(Object args) {
        this(__ruby__, __metaclass__);
        IRubyObject ruby_args = JavaUtil.convertJavaToRuby(__ruby__, args);
        RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "initialize", ruby_args);

    }

    
    public Object build_dependency_files_and_final_jar(Object args) {
        IRubyObject ruby_args = JavaUtil.convertJavaToRuby(__ruby__, args);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "build_dependency_files_and_final_jar", ruby_args);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public void clean_cache(String cacheDir) {
        IRubyObject ruby_cacheDir = JavaUtil.convertJavaToRuby(__ruby__, cacheDir);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "clean_cache", ruby_cacheDir);
        return;

    }

}
