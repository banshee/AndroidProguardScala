require 'java'
require 'proguard_cache_requires'
require 'asm_support'
require 'asm_support/asm_visitor_harness'
require 'asm_support/dependency_signature_visitor'
require 'digest/sha1'
require 'proguardrunner'
require 'pathname'
require 'fileutils'
require 'jvm_entity'
require 'ostruct'
require 'proguard_cache_parameters'

java_package 'com.restphone.androidproguardscala.jruby'

class ProguardCacheRuby
  def proguard_output pattern, checksum
    pattern.sub("CKSUM", checksum)
  end

  # Given a directory, return a list of relative pathnames as strings
  # that are the .class  files
  def classfiles_relative_to_directory dir
    result = Dir.glob(dir + "/**/*.class")
    d = dir.to_pathname
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

  def build_dependencies_for_file dependency_directory, binary_file, forced_location = nil
    FileUtils.mkdir_p dependency_directory.dirname
    dependencies = AsmSupport::AsmVisitorHarness.build_for_filename(AsmSupport::DependencySignatureVisitor, binary_file.to_s)
    classnames = dependencies.keys
    classnames.each do |classname|
      dep_path = if forced_location
        forced_location
      else
        Pathname.new((dependency_directory + classname).to_s + ".class.proto_depend")
      end
      FileUtils.mkdir_p dep_path.dirname
      File.open(dep_path, "w") do |f|
        f.puts "classname: #{classname}"
        f.write dependencies[classname].keys.sort.uniq.join("\n")
      end
    end
    classnames
  end

  def calculate_classnames_in_cache_dir cache_dir
    filenames = Dir.glob(cache_dir.to_s + "/**/*.proto_depend")
    classnames = filenames.map do |filename|
      result = []
      File.open(filename) do |f|
        f.each_line do |l|
          l.strip!
          label, classname = l.split(": ")
          if label == "classname"
            result << (classname.split('/').join('.'))
          end
        end
      end
      result
    end
    classnames.flatten.sort.uniq
  end

  def build_dependency_files input_items, cache_dir
    cache_dir_pathname = Pathname.new cache_dir
    FileUtils.mkdir_p cache_dir
    result = []
    input_items.each do |d|
      dir_identifier = d.checksum
      cache_directory_with_checksum = cache_dir_pathname + dir_identifier
      case d
      when ClassDirectory
        classfiles = classfiles_relative_to_directory d
        classfiles.each do |cf|
          full_pathname_for_binary_file = d.to_pathname + cf
          full_pathname_for_dependency_file = cache_dir_pathname + dir_identifier + (cf.to_s + ".proto_depend")
          is_current = FileUtils.uptodate? full_pathname_for_dependency_file, [full_pathname_for_binary_file]
          if !is_current
            build_dependencies_for_file cache_directory_with_checksum, full_pathname_for_binary_file, full_pathname_for_dependency_file
          end
          result << full_pathname_for_dependency_file
        end
      when JarFile
        full_pathname_for_binary_file = d.to_pathname
        full_pathname_for_dependency_file = cache_dir_pathname + dir_identifier + (d.basename.to_s + ".class.proto_depend")
        is_current = FileUtils.uptodate? full_pathname_for_dependency_file, [full_pathname_for_binary_file]
        if !is_current
          classnames = build_dependencies_for_file cache_directory_with_checksum, full_pathname_for_binary_file
          result = result + classnames.map {|result_file| cache_directory_with_checksum + (result_file + ".class.proto_depend")}
        end
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
    input_entities = args.classFiles
    proguard_config_file = args.proguardProcessedConfFile
    destination_jar = args.outputJar
    cache_dir = args.cacheDir
    cached_jar = args.cachedJar

    proguard_config_file or raise "You must specify proguardProcessedConfFile"
    destination_jar or raise "You must specify a destination jar"
    cache_dir ||= "proguard_cache"

    proguard_dependency_files = build_dependency_files input_entities, cache_dir

    dependency_checksum = checksum_of_lines_in_files(proguard_dependency_files)

    proguard_destination_file = proguard_output cached_jar, dependency_checksum

    contents = unique_lines_in_files_as_string proguard_dependency_files
    File.open "#{cache_dir}/dependency_lines." + dependency_checksum, "w" do |f|
      f.write contents
    end

    ProguardCacheParameters.new :parent_parameters => args, :new_parameters => {
      :proguard_destination_file => proguard_destination_file,
      :proguard_config_file => proguard_config_file,
      :dependency_checksum => dependency_checksum,
      :destination_jar => destination_jar}
  end

  def run_proguard args
    destination_file = args.proguard_destination_file
    logger = args.logger
    config_file = args.proguard_config_file
    if !File.exists?(destination_file)
      logger.logMsg("Running proguard with config file " + config_file)
      ProguardRunner.execute_proguard(:config_file => config_file, :cksum => ".#{args.dependency_checksum}")
    end
    if File.exists?(destination_file)
      destination_jar = args.destination_jar
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
      defaults = args.proguardDefaults
      scala_library_jar = args.scalaLibraryJar
      f.puts "# scala-library.jar was calculated from the classpath"
      f.puts %Q[-injars "#{scala_library_jar}"(!META-INF/MANIFEST.MF)\n]
      f.puts "\n# The CKSUM string is significant - it will be replaced with an actual checksum"
      f.puts %Q[-outjar "#{args.cachedJar}"]
      args.classFiles.each do |classfile|
        f.puts %Q[-injar "#{classfile}"]
      end

      android_lib_jar = args.androidLibraryJar
      if android_lib_jar
        f.puts "\n# Android library calculated from classpath"
        f.puts %Q(-libraryjars "#{args.androidLibraryJar}")
      end

      extra_libs = args.extraLibs
      f.puts "\n# Extra libraries"
      (extra_libs || []).each do |lib|
        f.puts %Q(-libraryjars "#{lib}")
      end

      f.puts "\n# Builtin defaults"
      f.write defaults
      f.puts "\n# Inserting file #{args.proguardAdditionsFile} - possibly empty"
      if File.exists? args.proguardAdditionsFile
        additions_file = File.new args.proguardAdditionsFile
        f.write additions_file.read
      end

      f.puts "# Keep all non-scala classess"
      args.classnames.each do |classname|
        f.puts "-keep class #{classname} {*;}"
      end
      f.flush

      conf_file = args.proguardProcessedConfFile
      FileUtils.install f.path, conf_file, :mode => 0666, :verbose => false

      args.logger.logMsg("Created new proguard configuration at #{conf_file}")
    end
  end

  def build_dependency_files_and_final_jar args
    logger = args.logger
    setup_external_variables args
    update_and_load_additional_libs_ruby_file args
    classFiles = (args.classFiles + ($ADDITIONAL_LIBS || [])).sort.uniq
    classFiles.each do |i|
      raise "non-existant input directory: " + i.to_s unless File.exists? i.to_s
      puts "input directory: #{i}"
    end
    classFiles = classFiles.map {|f| JvmEntityBuilder.create f}
    new_parameters = ProguardCacheParameters.new :parent_parameters => args, :new_parameters => {:classFiles => classFiles}
    result = build_proguard_dependencies new_parameters
    all_classnames = calculate_classnames_in_cache_dir new_parameters.cacheDir
    parameters_after_calculating_classes = ProguardCacheParameters.new :parent_parameters => new_parameters, :new_parameters => {'classnames' => all_classnames}
    build_proguard_file parameters_after_calculating_classes
    run_proguard result
  end

  def update_and_load_additional_libs_ruby_file args
    additional_file = args.confDir.to_s + "/additional_libs.rb"
    if !File.exists? additional_file
      FileUtils.mkdir_p Pathname.new(additional_file).dirname
      File.open(additional_file, "w") do |f|
        f.puts "# Auto-generated sample file. "
        f.puts "# $WORKSPACE_DIR is set to the path for the current workspace"
        f.puts %Q{# $ADDITIONAL_LIBS = [$WORKSPACE_DIR + "/TestAndroidLibrary/bin/testandroidlibrary.jar"]}
      end
    end
    load additional_file
  end

  java_signature 'void clean_cache(String cacheDir)'

  def clean_cache cache_dir
    depend_files = Dir.glob(cache_dir.to_s + "/**/*.proto_depend")
    jar_files = Dir.glob(cache_dir.to_s + "/**/*.jar")
    dependency_lines = Dir.glob(cache_dir.to_s + "/**/dependency_lines*")
    (depend_files + jar_files + dependency_lines).each do |f|
      File.unlink f
    end
    # The checksum has 40 characters - only delete directories that are exactly that long
    dependency_directories = Dir.glob(cache_dir.to_s + "/*").select do |d|
      (File.basename d).length == 40
    end
    dependency_directories.each {|d| FileUtils.rm_rf d}
  end

  def setup_external_variables args
    $WORKSPACE_DIR = args.workspaceDir
    $PROJECT_DIR = args.projectDir
    $BUILDER_ARGS = args
    $ADDITIONAL_LIBS = []
  end
end

#  visitAsClass  -    -  com/restphone/uricurrency/UrlConnector$ConnectionResult  -  scala/Product  -  scala/Serializable
#visitMethodInsn  -  182  -  com/restphone/uricurrency/UrlConnector$$anonfun$1$$anonfun$apply$2$$anonfun$apply$3$$anonfun$apply$5  -  apply  -  (Lscala/Tuple4;)Lcom/restphone/uricurrency/UrlConnector$LiveConnection;
