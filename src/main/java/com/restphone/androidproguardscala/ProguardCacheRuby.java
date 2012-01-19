package com.restphone.androidproguardscala;

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
            "\n" +
            "java_package 'com.restphone.androidproguardscala'\n" +
            "\n" +
            "class ProguardCacheRuby\n" +
            "  def proguard_output pattern, checksum\n" +
            "    pattern.sub(\"CKSUM\", checksum)\n" +
            "  end\n" +
            "\n" +
            "  # Given a list of directories, return a hash of\n" +
            "  #   directory_name_checksum => [class and jar files relative to the directory]\n" +
            "  def binary_file_directories_to_cache_files dir_list\n" +
            "    dir_list.inject({}) do |memo, obj|\n" +
            "      dir_identifier = Digest::SHA1.hexdigest obj.gsub(\"/\", \"_\")\n" +
            "      memo.merge dir_identifier => binary_file_directory_to_cache_files(obj)\n" +
            "    end\n" +
            "  end\n" +
            "\n" +
            "  # Given a directory, return a list of relative pathnames as strings\n" +
            "  # that are the .class and .jar files\n" +
            "  def binary_file_directory_to_cache_files dir\n" +
            "    result = Dir.glob(dir + \"/**/*.class\")\n" +
            "    result = result + Dir.glob(dir + \"/**/*.jar\")\n" +
            "    d = Pathname.new dir\n" +
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
            "  def build_dependencies_for_file dependency_file, binary_file\n" +
            "    FileUtils.mkdir_p dependency_file.dirname\n" +
            "    dependencies = AsmSupport::AsmVisitorHarness.build_for_filename(AsmSupport::DependencySignatureVisitor, binary_file.to_s)\n" +
            "    File.open(dependency_file, \"w\") do |f|\n" +
            "      f.write dependencies.values.first.keys.sort.uniq.join(\"\\n\")\n" +
            "    end\n" +
            "  end\n" +
            "\n" +
            "  def build_dependency_files input_directories, cache_dir\n" +
            "    cache_dir_pathname = Pathname.new cache_dir\n" +
            "    FileUtils.mkdir_p cache_dir\n" +
            "    result = []\n" +
            "    input_directories.each do |d|\n" +
            "      dir_identifier = Digest::SHA1.hexdigest d.gsub(\"/\", \"_\")\n" +
            "      bin_files = binary_file_directory_to_cache_files d\n" +
            "      bin_files.each do |bf|\n" +
            "        full_pathname_for_binary_file = Pathname.new(d) + bf\n" +
            "        full_pathname_for_dependency_file = cache_dir_pathname + dir_identifier + (bf.to_s + \".proto_depend\")\n" +
            "        is_current = FileUtils.uptodate? full_pathname_for_dependency_file, [full_pathname_for_binary_file]\n" +
            "        if !is_current\n" +
            "          build_dependencies_for_file full_pathname_for_dependency_file, full_pathname_for_binary_file\n" +
            "        end\n" +
            "        result << full_pathname_for_dependency_file\n" +
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
            "    args = HashViaGet.new args\n" +
            "\n" +
            "    input_directories = args['classFiles']\n" +
            "    proguard_config_file = args['proguardProcessedConfFile']\n" +
            "    destination_jar = args['outputJar']\n" +
            "    cache_dir = args['cacheDir']\n" +
            "    cached_jar = args['cachedJar']\n" +
            "\n" +
            "    proguard_config_file or raise \"You must specify proguardProcessedConfFile\"\n" +
            "    destination_jar or raise \"You must specify a destination jar\"\n" +
            "    cache_dir ||= \"proguard_cache\"\n" +
            "\n" +
            "    proguard_dependency_files = build_dependency_files input_directories, cache_dir\n" +
            "\n" +
            "    dependency_checksum = checksum_of_lines_in_files(proguard_dependency_files + [proguard_config_file])\n" +
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
            "    logger.logMsg(\"Proguard output file is \" + destination_file)\n" +
            "    if File.exists?(destination_file)\n" +
            "      destination_jar = args[:destination_jar]\n" +
            "      FileUtils.install destination_file, destination_jar, :mode => 0666, :verbose => true\n" +
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
            "      f.puts %Q[-injars \"#{scala_library_jar}\"(!META-INF/MANIFEST.MF,!library.properties)]\n" +
            "      f.puts %Q[-outjar \"#{args['cachedJar']}\"]\n" +
            "      args['classFiles'].each do |classfile|\n" +
            "        f.puts %Q[-injar \"#{classfile}\"]\n" +
            "      end\n" +
            "      f.write defaults\n" +
            "      if File.exists? args['proguardAdditionsFile']\n" +
            "        additions_file = File.new args['proguardAdditionsFile']\n" +
            "        f.write additions_file.read\n" +
            "      end\n" +
            "      f.flush\n" +
            "      conf_file = args['proguardProcessedConfFile']\n" +
            "      FileUtils.install f.path, conf_file, :mode => 0666, :verbose => true\n" +
            "      args['logger'].logMsg(\"Created new proguard configuration at #{conf_file}\")\n" +
            "    end\n" +
            "  end\n" +
            "\n" +
            "  def build_dependency_files_and_final_jar args\n" +
            "    require 'hash_via_get'\n" +
            "    args = HashViaGet.new args\n" +
            "    logger = args['logger']\n" +
            "    args['classFiles'].each do |i|\n" +
            "      raise \"non-existant input directory: \" + i.to_s unless File.exists? i.to_s\n" +
            "      puts \"input directory: #{i}\"\n" +
            "    end\n" +
            "    build_proguard_file args\n" +
            "    result = build_proguard_dependencies args\n" +
            "    run_proguard result.merge('logger' => args['logger'])\n" +
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

    
    public Object binary_file_directories_to_cache_files(Object dir_list) {
        IRubyObject ruby_dir_list = JavaUtil.convertJavaToRuby(__ruby__, dir_list);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "binary_file_directories_to_cache_files", ruby_dir_list);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public Object binary_file_directory_to_cache_files(Object dir) {
        IRubyObject ruby_dir = JavaUtil.convertJavaToRuby(__ruby__, dir);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "binary_file_directory_to_cache_files", ruby_dir);
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

    
    public Object build_dependencies_for_file(Object dependency_file, Object binary_file) {
        IRubyObject ruby_dependency_file = JavaUtil.convertJavaToRuby(__ruby__, dependency_file);
        IRubyObject ruby_binary_file = JavaUtil.convertJavaToRuby(__ruby__, binary_file);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "build_dependencies_for_file", ruby_dependency_file, ruby_binary_file);
        return (Object)ruby_result.toJava(Object.class);

    }

    
    public Object build_dependency_files(Object input_directories, Object cache_dir) {
        IRubyObject ruby_input_directories = JavaUtil.convertJavaToRuby(__ruby__, input_directories);
        IRubyObject ruby_cache_dir = JavaUtil.convertJavaToRuby(__ruby__, cache_dir);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "build_dependency_files", ruby_input_directories, ruby_cache_dir);
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

}
