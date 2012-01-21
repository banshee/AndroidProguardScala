require 'java'
require 'proguard_cache_requires'
require 'asm_support'
require 'asm_support/asm_visitor_harness'
require 'asm_support/dependency_signature_visitor'
require 'digest/sha1'
require 'proguardrunner'
require 'pathname'
require 'fileutils'

java_package 'com.restphone.androidproguardscala'

class ProguardCacheRuby
  def proguard_output pattern, checksum
    pattern.sub("CKSUM", checksum)
  end

  # Given a list of directories, return a hash of
  #   directory_name_checksum => [class and jar files relative to the directory]
  def binary_file_directories_to_cache_files dir_list
    dir_list.inject({}) do |memo, obj|
      dir_identifier = Digest::SHA1.hexdigest obj.gsub("/", "_")
      memo.merge dir_identifier => binary_file_directory_to_cache_files(obj)
    end
  end

  # Given a directory, return a list of relative pathnames as strings
  # that are the .class and .jar files
  def binary_file_directory_to_cache_files dir
    result = Dir.glob(dir + "/**/*.class")
    result = result + Dir.glob(dir + "/**/*.jar")
    d = Pathname.new dir
    result.map {|f| Pathname.new f}.map {|f| f.relative_path_from d}.map(&:to_s)
  end

  def unique_lines_in_files_as_string files
    (unique_lines_in_files files).join("\n")
  end

  def unique_lines_in_files files
    result = files.map {|f| IO.read(f).split(/[\n\r]+/)}.flatten.sort.uniq
    # This is a hack, and makes the tool tied to just building scala libraries.  Factor it out.
    result.select {|x| x =~ %r(scala/)}
  end

  def checksum_of_lines_in_files files
    file_contents = (unique_lines_in_files_as_string files)
    Digest::SHA1.hexdigest file_contents
  end

  def build_dependencies_for_file dependency_file, binary_file
    FileUtils.mkdir_p dependency_file.dirname
    dependencies = AsmSupport::AsmVisitorHarness.build_for_filename(AsmSupport::DependencySignatureVisitor, binary_file.to_s)
    File.open(dependency_file, "w") do |f|
      f.write dependencies.values.first.keys.sort.uniq.join("\n")
    end
  end

  def build_dependency_files input_directories, cache_dir
    cache_dir_pathname = Pathname.new cache_dir
    FileUtils.mkdir_p cache_dir
    result = []
    input_directories.each do |d|
      dir_identifier = Digest::SHA1.hexdigest d.gsub("/", "_")
      bin_files = binary_file_directory_to_cache_files d
      bin_files.each do |bf|
        full_pathname_for_binary_file = Pathname.new(d) + bf
        full_pathname_for_dependency_file = cache_dir_pathname + dir_identifier + (bf.to_s + ".proto_depend")
        is_current = FileUtils.uptodate? full_pathname_for_dependency_file, [full_pathname_for_binary_file]
        if !is_current
          build_dependencies_for_file full_pathname_for_dependency_file, full_pathname_for_binary_file
        end
        result << full_pathname_for_dependency_file
      end
    end
    result
  end

  "Build a proguarded scala library.  Arguments are:
proguard_file: The proguard config file
destination_jar: The final, proguarded jar file
cache_jar_pattern: The file name of the cached jars
cache_dir: Where the cached jars are stored

Example: jruby -S rake -T -v proguard[proguard_android_scala.config,proguard_cache/scala-proguard.jar]
"

  def build_proguard_dependencies args
    args = Hash[args]

    input_directories = args['classFiles']
    proguard_config_file = args['proguardProcessedConfFile']
    destination_jar = args['outputJar']
    cache_dir = args['cacheDir']
    cached_jar = args['cachedJar']

    proguard_config_file or raise "You must specify proguardProcessedConfFile"
    destination_jar or raise "You must specify a destination jar"
    cache_dir ||= "proguard_cache"

    proguard_dependency_files = build_dependency_files input_directories, cache_dir

    dependency_checksum = checksum_of_lines_in_files(proguard_dependency_files + [proguard_config_file])

    proguard_destination_file = proguard_output cached_jar, dependency_checksum

    contents = unique_lines_in_files_as_string proguard_dependency_files
    File.open "#{cache_dir}/dependency_lines." + dependency_checksum, "w" do |f|
      f.write contents
    end

    {:proguard_destination_file => proguard_destination_file,
      :proguard_config_file => proguard_config_file,
      :dependency_checksum => dependency_checksum,
      :destination_jar => destination_jar}
  end

  def run_proguard args
    destination_file = args[:proguard_destination_file]
    logger = args['logger']
    config_file = args[:proguard_config_file]
    if !File.exists?(destination_file)
      logger.logMsg("Running proguard with config file " + config_file)
      ProguardRunner.execute_proguard(:config_file => config_file, :cksum => ".#{args[:dependency_checksum]}")
    end
    logger.logMsg("Proguard output file is " + destination_file)
    if File.exists?(destination_file)
      destination_jar = args[:destination_jar]
      FileUtils.install destination_file, destination_jar, :mode => 0666, :verbose => false
      logger.logMsg("installed #{destination_file} to #{destination_jar}")
    else
      logger.logError("No proguard output found at " + destination_file)
      File.unlink destination_jar
    end
  end

  def build_proguard_file args
    require 'tempfile'
    Tempfile.open("android_scala_proguard") do |f|
      defaults = args['proguardDefaults']
      scala_library_jar = args['scalaLibraryJar']
      f.puts "# scala-library.jar was calculated from the classpath"
      f.puts %Q[-injars "#{scala_library_jar}"(!META-INF/MANIFEST.MF)\n]
      f.puts "\n# The CKSUM string is significant - it will be replaced with an actual checksum"
      f.puts %Q[-outjar "#{args['cachedJar']}"]
      args['classFiles'].each do |classfile|
        f.puts %Q[-injar "#{classfile}"]
      end

      android_lib_jar = args['androidLibraryJar']
      if android_lib_jar
        f.puts "\n# Android library calculated from classpath"
        f.puts %Q(-libraryjars "#{args['androidLibraryJar']}")
      end

      f.puts "\n# Builtin defaults"
      f.write defaults
      f.puts "\n# Inserting file #{args['proguardAdditionsFile']} - possibly empty"
      if File.exists? args['proguardAdditionsFile']
        additions_file = File.new args['proguardAdditionsFile']
        f.write additions_file.read
      end
      f.flush
      conf_file = args['proguardProcessedConfFile']
      FileUtils.install f.path, conf_file, :mode => 0666, :verbose => false
      args['logger'].logMsg("Created new proguard configuration at #{conf_file}")
    end
  end

  def build_dependency_files_and_final_jar args
    require 'hash_via_get'
    args = Hash[args]
    logger = args['logger']
    setup_external_variables args
    update_and_load_additional_libs_ruby_file args
    args['classFiles'] = args['classFiles'] + ($ADDITIONAL_LIBS || [])
    args['classFiles'].each do |i|
      raise "non-existant input directory: " + i.to_s unless File.exists? i.to_s
      puts "input directory: #{i}"
    end
    build_proguard_file args
    result = build_proguard_dependencies args
    run_proguard result.merge('logger' => logger)
  end

  def update_and_load_additional_libs_ruby_file args
    additional_file = args['confDir'] + "/additional_libs.rb"
    if !File.exists? additional_file
      File.open(additional_file, "w") do |f|
        f.write "# Auto-generated sample file. "
        f.write "# $WORKSPACE_DIR is set to the path for the current workspace"
        f.write %Q{$ADDITIONAL_LIBS = [$WORKSPACE_DIR + "/TestAndroidLibrary/bin/testandroidlibrary.jar"]}
      end
    end
    load additional_file
  end

  def setup_external_variables args
    $WORKSPACE_DIR = args['workspaceDir']
    $ADDITIONAL_LIBS = []
  end
end
