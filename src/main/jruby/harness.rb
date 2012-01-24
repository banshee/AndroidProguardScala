$LOAD_PATH << "/Users/james/workspace/AndroidProguardScala/lib"
$LOAD_PATH << "/Users/james/workspace/AndroidProguardScala/src/main/jruby"
require "asm-3.3.1.jar"
require "proguard-base-4.6.jar"
require 'proguard_cache'
require 'jruby_environment_setup'

base = "/Users/james/runtime-EclipseApplicationwithEquinoxWeaving/AndroidTest"
input_directories = [base + "/bin"]

class Lgr
  def logMsg s
    puts s
  end
end

p = ProguardCacheRuby.new
p.build_dependency_files_and_final_jar "classFiles" =>  input_directories,
"proguardDefaults" => "some defaults go here\n",
"proguardAdditionsFile" => base + '/proguard_cache/proguard_additions.conf',
"proguardProcessedConfFile" => base + '/proguard_cache/proguard.conf',
"workspaceDir" => "/Users/james/runtime-EclipseApplicationwithEquinoxWeaving",
"cachedJar" => base + '/proguard_cache/scala.CKSUM.jar',
"outputJar" => base + '/lib/scala_proguard.jar',
"confDir" => base + "/proguard_cache_conf",
'logger' => Lgr.new,
"cacheDir" => base + "/proguard_cache",
"scalaLibraryJar" => '/Users/james/src/scala-ide/org.scala-ide.sdt.core.tests/test-workspace/classpath/lib/2.9.x/scala-library.jar'

#cacheDir => " /Users/james/runtime-EclipseApplicationwithEquinoxWeaving/AndroidTest/proguard_cache"
#proguardProcessedConfFile => " /Users/james/runtime-EclipseApplicationwithEquinoxWeaving/AndroidTest/proguard_cache_conf/proguard_postprocessed.conf
#logger => " com.restphone.androidproguardscala.AndroidProguardScalaBuilder$$anon$1@28c61840
#classFiles => " [Ljava.lang.String;@656226a9
#proguardAdditionsFile => " /Users/james/runtime-EclipseApplicationwithEquinoxWeaving/AndroidTest/proguard_cache_conf/proguard_additions.conf
#outputJar => " /Users/james/runtime-EclipseApplicationwithEquinoxWeaving/AndroidTest/lib/scala_library.min.jar
#cachedJar => " /Users/james/runtime-EclipseApplicationwithEquinoxWeaving/AndroidTest/proguard_cache/scala-library.CKSUM.jar
#scalaLibraryJar => " /Users/james/workspace/.metadata/.plugins/org.eclipse.pde.core/Eclipse Application with Equinox Weaving/org.eclipse.osgi/bundles/808/1/.cp/lib/scala-library.jar
#androidLibraryJar => " /Users/james/lib/android-sdk-mac_86/platforms/android-8/android.jar
#confDir => " /Users/james/runtime-EclipseApplicationwithEquinoxWeaving/AndroidTest/proguard_cache_conf
#proguardDefaults => " ""
#projectDir => " /Users/james/runtime-EclipseApplicationwithEquinoxWeaving/AndroidTest
