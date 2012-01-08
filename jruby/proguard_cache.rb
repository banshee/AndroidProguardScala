require 'java'

require 'proguard_cache_requires'
require 'asm_support'
require 'asm_support/asm_visitor_harness'
require 'asm_support/dependency_signature_visitor'
require 'digest/sha2'
require 'proguardrunner'
require 'pathname'
require 'fileutils'

java_package 'com.restphone'

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

  #x = binary_file_directories_to_cache_files "/Users/james/.ivy2/cache/org.scala-tools.sbt"
  #pp x

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

  def build_proguard_dependencies input_directories, proguard_config_file, destination_jar, cache_dir = nil, cache_jar_pattern = nil
    proguard_config_file or raise "You must specify a proguard config file"
    destination_jar or raise "You must specify a destination jar"
    cache_jar_pattern ||= cache_dir + "/scala-library.CKSUM.jar"
    cache_dir ||= "proguard_cache"

    proguard_dependency_files = build_dependency_files input_directories, cache_dir

    dependency_checksum = checksum_of_lines_in_files(proguard_dependency_files + [proguard_config_file])

    proguard_destination_file = proguard_output cache_jar_pattern, dependency_checksum

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

  #  ProguardCache.new.build_dependency_files_and_final_jar %w(target/scala-2.9.1), "proguard_config/proguard_android_scala.config.unix", "/tmp/out.jar", "target/proguard_cache"
  def build_dependency_files_and_final_jar input_directories, proguard_config_file, destination_jar, cache_dir, cache_jar_pattern
    result = build_proguard_dependencies input_directories, proguard_config_file, destination_jar, cache_dir, cache_jar_pattern
    run_proguard result
  end
end
