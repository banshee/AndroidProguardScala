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
    args = HashViaGet.new args

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
    if !File.exists?(args[:proguard_destination_file])
      ProguardRunner.execute_proguard(:config_file => args[:proguard_config_file], :cksum => ".#{args[:dependency_checksum]}")
    end
    FileUtils.install args[:proguard_destination_file], args[:destination_jar], :mode => 0666, :verbose => true
  end

  # Given
  def build_proguard_file args
    require 'tempfile'
    Tempfile.open("android_scala_proguard") do |f|
      defaults = args['proguardDefaults']
      scala_library_jar = args['scalaLibraryJar']
      f.puts %Q[-injars "#{scala_library_jar}"(!META-INF/MANIFEST.MF,!library.properties)]
      f.puts %Q[-outjar "#{args['cachedJar']}"]
      args['classFiles'].each do |classfile|
        f.puts %Q[-injar "#{classfile}"]
      end
      f.write defaults
      if File.exists? args['proguardAdditionsFile']
        additions_file = File.new args['proguardAdditionsFile']
        f.write additions_file.read
      end
      f.flush
      FileUtils.install f.path, args['proguardProcessedConfFile'], :mode => 0666, :verbose => true
    end
  end

  #  ProguardCache.new.build_dependency_files_and_final_jar %w(target/scala-2.9.1), "proguard_config/proguard_android_scala.config.unix", "/tmp/out.jar", "target/proguard_cache"
  def build_dependency_files_and_final_jar args
    #    "classFiles" -> outputFoldersFiles,
    #    "proguardDefaults" -> proguardDefaults,
    #    "proguardConfFile" -> proguardConfFile,
    #    "proguardProcessedConfFile" -> proguardProcessedConfFile,
    #    "cachedJar" -> cachedJar,
    #    "outputJar" -> outputJar)
    require 'hash_via_get'
    args = HashViaGet.new args
    pp "args are ", args
    args['classFiles'].each do |i|
      raise "non-existant input directory: " + i.to_s unless File.exists? i.to_s
      puts "input directory: #{i}"
    end
    build_proguard_file args
    result = build_proguard_dependencies args
    pp 'reesultis', result
    run_proguard result
  end
end
