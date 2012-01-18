$LOAD_PATH << "/Users/james/workspace/AndroidProguardScala/lib"
$LOAD_PATH << "/Users/james/workspace/AndroidProguardScala/src/main/jruby"
require "asm-3.3.1.jar"
require "proguard-base-4.6.jar"
require 'proguard_cache'
require 'jruby_environment_setup'

input_directories = ['bin']

p = ProguardCacheRuby.new
p.build_dependency_files_and_final_jar "classFiles" => ['bin'],
"proguardDefaults" => "some defaults go here\n",
"proguardAdditionsFile" => 'proguard_cache/proguard_additions.conf',
"proguardProcessedConfFile" => 'proguard_cache/proguard.conf',
"cachedJar" => 'proguard_cache/scala.CKSUM.jar',
"outputJar" => 'lib/scala_proguard.jar',
"scalaLibraryJar" => '/Users/james/src/scala-ide/org.scala-ide.sdt.core.tests/test-workspace/classpath/lib/2.9.x/scala-library.jar'

#val parameters = Map(
#  "classFiles" -> outputFoldersFiles,
#  "proguardDefaults" -> proguardDefaults,
#  "proguardAdditionsFile" -> proguardAdditionsFile,
#  "proguardProcessedConfFile" -> proguardProcessedConfFile,
#  "cachedJar" -> cachedJar,
#  "outputJar" -> outputJar)
