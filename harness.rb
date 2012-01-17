$LOAD_PATH << "/Users/james/workspace/AndroidProguardScala/lib"
$LOAD_PATH << "/Users/james/workspace/AndroidProguardScala/src/main/jruby"
require "asm-3.3.1.jar"
require "proguard-base-4.6.jar"
require 'proguard_cache'


input_directories = ['bin']

p = ProguardCacheRuby.new
p.build_dependency_files_and_final_jar "classFiles" => ['bin'], proguardDefaults => 'proguard_cache/proguard_defaults.conf',
  "proguardBasicConfFile" => 'proguard_cache/proguard_basic.conf',
  "proguardProcessedConfFile" => 'proguard_cache/proguard.conf',
  "cachedJar" => 'proguard_cache/scala.CKSUM.jar',
  "outputJar" => 'lib/scala_proguard.jar'

#    "classFiles" -> outputFoldersFiles,
#    "proguardDefaults" -> proguardDefaults,
#    "proguardConfFile" -> proguardConfFile,
#    "proguardProcessedConfFile" -> proguardProcessedConfFile,
#    "cachedJar" -> cachedJar,
#    "outputJar" -> outputJar)
