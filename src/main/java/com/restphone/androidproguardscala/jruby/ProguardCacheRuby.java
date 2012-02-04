package com.restphone.androidproguardscala.jruby;

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.RubyClass;


public class ProguardCacheRuby extends RubyObject  {
    private static final Ruby __ruby__ = Ruby.getGlobalRuntime();
    private static final RubyClass __metaclass__;

    static {
        String source = new StringBuilder("require 'java'\n" +
            "require 'proguard_cache_requires'\n" +
            "require 'asm_support'\n" +
            "require 'asm_support/asm_visitor_harness'\n" +
            "require 'asm_support/dependency_signature_visitor'\n" +
            "require 'digest/sha1'\n" +
            "require 'proguardrunner'\n" +
            "require 'pathname'\n" +
            "require 'fileutils'\n" +
            "require 'jvm_entity'\n" +
            "\n" +
            "java_package 'com.restphone.androidproguardscala.jruby'\n" +
            "\n" +
            "class ProguardCacheRuby\n" +
            "  def proguard_output pattern, checksum\n" +
            "    pattern.sub(\"CKSUM\", checksum)\n" +
            "  end\n" +
            "\n" +
            "  # Given a directory, return a list of relative pathnames as strings\n" +
            "  # that are the .class  files\n" +
            "  def classfiles_relative_to_directory dir\n" +
            "    result = Dir.glob(dir + \"/**/*.class\")\n" +
            "    d = dir.to_pathname\n" +
            "    result.map {|f| Pathname.new f}.map {|f| f.relative_path_from d}.map(&:to_s)\n" +
            "  end\n" +
            "\n" +
            "  def unique_lines_in_files_as_string files\n" +
            "    (unique_lines_in_files files).join(\"\\n\")\n" +
            "  end\n" +
            "\n" +
            "  def unique_lines_in_files files\n" +
            "    result = files.map {|f| IO.read(f).split(/[\\n\\r]+/)}.flatten.sort.uniq\n" +
            "    # This is a hack, and makes the tool tied to just building scala libraries.  Factor it out.\n" +
            "    result.select {|x| x =~ %r(scala/)}\n" +
            "  end\n" +
            "\n" +
            "  def checksum_of_lines_in_files files\n" +
            "    file_contents = (unique_lines_in_files_as_string files)\n" +
            "    Digest::SHA1.hexdigest file_contents\n" +
            "  end\n" +
            "\n" +
            "  def build_dependencies_for_file dependency_directory, binary_file, forced_location = nil\n" +
            "    FileUtils.mkdir_p dependency_directory.dirname\n" +
            "    dependencies = AsmSupport::AsmVisitorHarness.build_for_filename(AsmSupport::DependencySignatureVisitor, binary_file.to_s)\n" +
            "    classnames = dependencies.keys\n" +
            "    classnames.each do |classname|\n" +
            "      dep_path = if forced_location\n" +
            "        forced_location\n" +
            "      else\n" +
            "        Pathname.new((dependency_directory + classname).to_s + \".class.proto_depend\")\n" +
            "      end\n" +
            "      FileUtils.mkdir_p dep_path.dirname\n" +
            "      File.open(dep_path, \"w\") do |f|\n" +
            "        f.puts \"classname: #{classname}\"\n" +
            "        f.write dependencies[classname].keys.sort.uniq.join(\"\\n\")\n" +
            "      end\n" +
            "    end\n" +
            "    classnames\n" +
            "  end\n" +
            "\n" +
            "  def calculate_classnames_in_cache_dir cache_dir\n" +
            "    filenames = Dir.glob(cache_dir.to_s + \"/**/*.proto_depend\")\n" +
            "    classnames = filenames.map do |filename|\n" +
            "      result = []\n" +
            "      File.open(filename) do |f|\n" +
            "        f.each_line do |l|\n" +
            "          l.strip!\n" +
            "          label, classname = l.split(\": \")\n" +
            "          if label == \"classname\"\n" +
            "            result << (classname.split('/').join('.'))\n" +
            "          end\n" +
            "        end\n" +
            "      end\n" +
            "      result\n" +
            "    end\n" +
            "    classnames.flatten.sort.uniq\n" +
            "  end\n" +
            "\n" +
            "  def build_dependency_files input_items, cache_dir\n" +
            "    cache_dir_pathname = Pathname.new cache_dir\n" +
            "    FileUtils.mkdir_p cache_dir\n" +
            "    result = []\n" +
            "    input_items.each do |d|\n" +
            "      dir_identifier = d.checksum\n" +
            "      cache_directory_with_checksum = cache_dir_pathname + dir_identifier\n" +
            "      case d\n" +
            "      when ClassDirectory\n" +
            "        classfiles = classfiles_relative_to_directory d\n" +
            "        classfiles.each do |cf|\n" +
            "          full_pathname_for_binary_file = d.to_pathname + cf\n" +
            "          full_pathname_for_dependency_file = cache_dir_pathname + dir_identifier + (cf.to_s + \".proto_depend\")\n" +
            "          is_current = FileUtils.uptodate? full_pathname_for_dependency_file, [full_pathname_for_binary_file]\n" +
            "          if !is_current\n" +
            "            build_dependencies_for_file cache_directory_with_checksum, full_pathname_for_binary_file, full_pathname_for_dependency_file\n" +
            "          end\n" +
            "          result << full_pathname_for_dependency_file\n" +
            "        end\n" +
            "      when JarFile\n" +
            "        full_pathname_for_binary_file = d.to_pathname\n" +
            "        full_pathname_for_dependency_file = cache_dir_pathname + dir_identifier + (d.basename.to_s + \".class.proto_depend\")\n" +
            "        is_current = FileUtils.uptodate? full_pathname_for_dependency_file, [full_pathname_for_binary_file]\n" +
            "        if !is_current\n" +
            "          classnames = build_dependencies_for_file cache_directory_with_checksum, full_pathname_for_binary_file\n" +
            "          result = result + classnames.map {|result_file| cache_directory_with_checksum + (result_file + \".class.proto_depend\")}\n" +
            "        end\n" +
            "      end\n" +
            "    end\n" +
            "    result\n" +
            "  end\n" +
            "\n" +
            "  \"Build a proguarded scala library.  Arguments are:\n" +
            "proguard_file: The proguard config file\n" +
            "destination_jar: The final, proguarded jar file\n" +
            "cache_jar_pattern: The file name of the cached jars\n" +
            "cache_dir: Where the cached jars are stored\n" +
            "\n" +
            "Example: jruby -S rake -T -v proguard[proguard_android_scala.config,proguard_cache/scala-proguard.jar]\n" +
            "\"\n" +
            "\n" +
            "  def build_proguard_dependencies args\n" +
            "    args = Hash[args]\n" +
            "\n" +
            "    input_entities = args['classFiles']\n" +
            "    proguard_config_file = args['proguardProcessedConfFile']\n" +
            "    destination_jar = args['outputJar']\n" +
            "    cache_dir = args['cacheDir']\n" +
            "    cached_jar = args['cachedJar']\n" +
            "\n" +
            "    proguard_config_file or raise \"You must specify proguardProcessedConfFile\"\n" +
            "    destination_jar or raise \"You must specify a destination jar\"\n" +
            "    cache_dir ||= \"proguard_cache\"\n" +
            "\n" +
            "    proguard_dependency_files = build_dependency_files input_entities, cache_dir\n" +
            "\n" +
            "    dependency_checksum = checksum_of_lines_in_files(proguard_dependency_files)\n" +
            "\n" +
            "    proguard_destination_file = proguard_output cached_jar, dependency_checksum\n" +
            "\n" +
            "    contents = unique_lines_in_files_as_string proguard_dependency_files\n" +
            "    File.open \"#{cache_dir}/dependency_lines.\" + dependency_checksum, \"w\" do |f|\n" +
            "      f.write contents\n" +
            "    end\n" +
            "\n" +
            "    {:proguard_destination_file => proguard_destination_file,\n" +
            "      :proguard_config_file => proguard_config_file,\n" +
            "      :dependency_checksum => dependency_checksum,\n" +
            "      :destination_jar => destination_jar}\n" +
            "  end\n" +
            "\n" +
            "  def run_proguard args\n" +
            "    destination_file = args[:proguard_destination_file]\n" +
            "    logger = args['logger']\n" +
            "    config_file = args[:proguard_config_file]\n" +
            "    if !File.exists?(destination_file)\n" +
            "      logger.logMsg(\"Running proguard with config file \" + config_file)\n" +
            "      ProguardRunner.execute_proguard(:config_file => config_file, :cksum => \".#{args[:dependency_checksum]}\")\n" +
            "    end\n" +
            "    if File.exists?(destination_file)\n" +
            "      destination_jar = args[:destination_jar]\n" +
            "      FileUtils.install destination_file, destination_jar, :mode => 0666, :verbose => false\n" +
            "      logger.logMsg(\"installed #{destination_file} to #{destination_jar}\")\n" +
            "    else\n" +
            "      logger.logError(\"No proguard output found at \" + destination_file)\n" +
            "      File.unlink destination_jar\n" +
            "    end\n" +
            "  end\n" +
            "\n" +
            "  def build_proguard_file args\n" +
            "    require 'tempfile'\n" +
            "    Tempfile.open(\"android_scala_proguard\") do |f|\n" +
            "      defaults = args['proguardDefaults']\n" +
            "      scala_library_jar = args['scalaLibraryJar']\n" +
            "      f.puts \"# scala-library.jar was calculated from the classpath\"\n" +
            "      f.puts %Q[-injars \"#{scala_library_jar}\"(!META-INF/MANIFEST.MF)\\n]\n" +
            "      f.puts \"\\n# The CKSUM string is significant - it will be replaced with an actual checksum\"\n" +
            "      f.puts %Q[-outjar \"#{args['cachedJar']}\"]\n" +
            "      args['classFiles'].each do |classfile|\n" +
            "        f.puts %Q[-injar \"#{classfile}\"]\n" +
            "      end\n" +
            "\n" +
            "      android_lib_jar = args['androidLibraryJar']\n" +
            "      if android_lib_jar\n" +
            "        f.puts \"\\n# Android library calculated from classpath\"\n" +
            "        f.puts %Q(-libraryjars \"#{args['androidLibraryJar']}\")\n" +
            "      end\n" +
            "\n" +
            "      extra_libs = args['extraLibs'] \n" +
            "      f.puts \"\\n# Extra libraries\"\n" +
            "      extra_libs.each do |lib|\n" +
            "        f.puts %Q(-libraryjars \"#{lib}\")\n" +
            "      end\n" +
            "\n" +
            "      f.puts \"\\n# Builtin defaults\"\n" +
            "      f.write defaults\n" +
            "      f.puts \"\\n# Inserting file #{args['proguardAdditionsFile']} - possibly empty\"\n" +
            "      if File.exists? args['proguardAdditionsFile']\n" +
            "        additions_file = File.new args['proguardAdditionsFile']\n" +
            "        f.write additions_file.read\n" +
            "      end\n" +
            "\n" +
            "      f.puts \"# Keep all non-scala classess\"\n" +
            "      args['classnames'].each do |classname|\n" +
            "        f.puts \"-keep class #{classname} {*;}\"\n" +
            "      end\n" +
            "      f.flush\n" +
            "\n" +
            "      conf_file = args['proguardProcessedConfFile']\n" +
            "      FileUtils.install f.path, conf_file, :mode => 0666, :verbose => false\n" +
            "\n" +
            "      args['logger'].logMsg(\"Created new proguard configuration at #{conf_file}\")\n" +
            "    end\n" +
            "  end\n" +
            "\n" +
            "  def build_dependency_files_and_final_jar args\n" +
            "    args = Hash[args]\n" +
            "    logger = args['logger']\n" +
            "    setup_external_variables args\n" +
            "    update_and_load_additional_libs_ruby_file args\n" +
            "    args['classFiles'] = (args['classFiles'] + ($ADDITIONAL_LIBS || [])).sort.uniq\n" +
            "    args['classFiles'].each do |i|\n" +
            "      raise \"non-existant input directory: \" + i.to_s unless File.exists? i.to_s\n" +
            "      puts \"input directory: #{i}\"\n" +
            "    end\n" +
            "    args['classFiles'] = args['classFiles'].map {|f| JvmEntityBuilder.create f}\n" +
            "    result = build_proguard_dependencies args\n" +
            "    all_classnames = calculate_classnames_in_cache_dir args['cacheDir']\n" +
            "    build_proguard_file(args.merge 'classnames' => all_classnames)\n" +
            "    run_proguard result.merge('logger' => logger)\n" +
            "  end\n" +
            "\n" +
            "  def update_and_load_additional_libs_ruby_file args\n" +
            "    additional_file = args['confDir'] + \"/additional_libs.rb\"\n" +
            "    if !File.exists? additional_file\n" +
            "      File.open(additional_file, \"w\") do |f|\n" +
            "        f.puts \"# Auto-generated sample file. \"\n" +
            "        f.puts \"# $WORKSPACE_DIR is set to the path for the current workspace\"\n" +
            "        f.puts %Q{# $ADDITIONAL_LIBS = [$WORKSPACE_DIR + \"/TestAndroidLibrary/bin/testandroidlibrary.jar\"]}\n" +
            "      end\n" +
            "    end\n" +
            "    load additional_file\n" +
            "  end\n" +
            "\n" +
            "  java_signature 'void clean_cache(String cacheDir)'\n" +
            "\n" +
            "  def clean_cache cache_dir\n" +
            "    depend_files = Dir.glob(cache_dir.to_s + \"/**/*.proto_depend\")\n" +
            "    jar_files = Dir.glob(cache_dir.to_s + \"/**/*.jar\")\n" +
            "    dependency_lines = Dir.glob(cache_dir.to_s + \"/**/dependency_lines*\")\n" +
            "    (depend_files + jar_files + dependency_lines).each do |f|\n" +
            "      File.unlink f\n" +
            "    end\n" +
            "  end\n" +
            "\n" +
            "  def setup_external_variables args\n" +
            "    $WORKSPACE_DIR = args['workspaceDir']\n" +
            "    $PROJECT_DIR = args['projectDir']\n" +
            "    $BUILDER_ARGS = args\n" +
            "    $ADDITIONAL_LIBS = []\n" +
            "  end\n" +
            "end\n" +
            "").toString();
        __ruby__.executeScript(source, "src/main/jruby/proguard_cache.rb");
        RubyClass metaclass = __ruby__.getClass("ProguardCacheRuby");
        metaclass.setRubyStaticAllocator(ProguardCacheRuby.class);
        if (metaclass == null) throw new NoClassDefFoundError("Could not load Ruby class: ProguardCacheRuby");
        __metaclass__ = metaclass;
    }

    /**
     * Standard Ruby object constructor, for construction-from-Ruby purposes.
     * Generally not for user consumption.
     *
     * @param ruby The JRuby instance this object will belong to
     * @param metaclass The RubyClass representing the Ruby class of this object
     */
    private ProguardCacheRuby(Ruby ruby, RubyClass metaclass) {
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
        return new ProguardCacheRuby(ruby, metaClass);
    }
        
    /**
     * Default constructor. Invokes this(Ruby, RubyClass) with the classloader-static
     * Ruby and RubyClass instances assocated with this class, and then invokes the
     * no-argument 'initialize' method in Ruby.
     *
     * @param ruby The JRuby instance this object will belong to
     * @param metaclass The RubyClass representing the Ruby class of this object
     */
    public ProguardCacheRuby() {
        this(__ruby__, __metaclass__);
        RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "initialize");
    }

    
    public Object proguard_output(Object pattern, Object checksum) {
        IRubyObject ruby_pattern = JavaUtil.convertJavaToRuby(__ruby__, pattern);
        IRubyObject ruby_checksum = JavaUtil.convertJavaToRuby(__ruby__, checksum);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "proguard_output", ruby_pattern, ruby_checksum);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public Object classfiles_relative_to_directory(Object dir) {
        IRubyObject ruby_dir = JavaUtil.convertJavaToRuby(__ruby__, dir);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "classfiles_relative_to_directory", ruby_dir);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public Object unique_lines_in_files_as_string(Object files) {
        IRubyObject ruby_files = JavaUtil.convertJavaToRuby(__ruby__, files);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "unique_lines_in_files_as_string", ruby_files);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public Object unique_lines_in_files(Object files) {
        IRubyObject ruby_files = JavaUtil.convertJavaToRuby(__ruby__, files);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "unique_lines_in_files", ruby_files);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public Object checksum_of_lines_in_files(Object files) {
        IRubyObject ruby_files = JavaUtil.convertJavaToRuby(__ruby__, files);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "checksum_of_lines_in_files", ruby_files);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public Object build_dependencies_for_file(Object dependency_directory, Object binary_file, Object forced_location) {
        IRubyObject ruby_dependency_directory = JavaUtil.convertJavaToRuby(__ruby__, dependency_directory);
        IRubyObject ruby_binary_file = JavaUtil.convertJavaToRuby(__ruby__, binary_file);
        IRubyObject ruby_forced_location = JavaUtil.convertJavaToRuby(__ruby__, forced_location);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "build_dependencies_for_file", ruby_dependency_directory, ruby_binary_file, ruby_forced_location);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public Object calculate_classnames_in_cache_dir(Object cache_dir) {
        IRubyObject ruby_cache_dir = JavaUtil.convertJavaToRuby(__ruby__, cache_dir);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "calculate_classnames_in_cache_dir", ruby_cache_dir);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public Object build_dependency_files(Object input_items, Object cache_dir) {
        IRubyObject ruby_input_items = JavaUtil.convertJavaToRuby(__ruby__, input_items);
        IRubyObject ruby_cache_dir = JavaUtil.convertJavaToRuby(__ruby__, cache_dir);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "build_dependency_files", ruby_input_items, ruby_cache_dir);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public Object build_proguard_dependencies(Object args) {
        IRubyObject ruby_args = JavaUtil.convertJavaToRuby(__ruby__, args);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "build_proguard_dependencies", ruby_args);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public Object run_proguard(Object args) {
        IRubyObject ruby_args = JavaUtil.convertJavaToRuby(__ruby__, args);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "run_proguard", ruby_args);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public Object build_proguard_file(Object args) {
        IRubyObject ruby_args = JavaUtil.convertJavaToRuby(__ruby__, args);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "build_proguard_file", ruby_args);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public Object build_dependency_files_and_final_jar(Object args) {
        IRubyObject ruby_args = JavaUtil.convertJavaToRuby(__ruby__, args);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "build_dependency_files_and_final_jar", ruby_args);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public Object update_and_load_additional_libs_ruby_file(Object args) {
        IRubyObject ruby_args = JavaUtil.convertJavaToRuby(__ruby__, args);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "update_and_load_additional_libs_ruby_file", ruby_args);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public void clean_cache(String cacheDir) {
        IRubyObject ruby_cacheDir = JavaUtil.convertJavaToRuby(__ruby__, cacheDir);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "clean_cache", ruby_cacheDir);
        return;

    }

    
    public Object setup_external_variables(Object args) {
        IRubyObject ruby_args = JavaUtil.convertJavaToRuby(__ruby__, args);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "setup_external_variables", ruby_args);
        return (Object)ruby_result.toJava(Object.class);

    }

}
