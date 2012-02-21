require 'java'
require 'proguard_cache.rb'

java_package 'com.restphone.androidproguardscala.jruby'

class ProguardCacheJava
  def initialize *args
    @ruby_object = ProguardCacheRuby.new
    jruby_jarfile = args.first
    @@jruby_initialized ||= JrubyEnvironmentSetup.add_jruby_jarfile(jruby_jarfile)
  end
  
  java_signature 'void buildDependencyFilesAndFinalJar(ProguardCacheParameters params)'
  def build_dependency_files_and_final_jar *args
    @ruby_object.build_dependency_files_and_final_jar *args
  end

  java_signature 'void clean_cache(String cacheDir)'

  def clean_cache cache_dir
    @ruby_object.clean_cache cache_dir
  end
end
