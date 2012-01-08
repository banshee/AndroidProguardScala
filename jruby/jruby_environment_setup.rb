require 'java'
require 'pathname'

java_package 'com.restphone'

class JrubyEnvironmentSetup
  java_signature 'void addIvyDirectoryToLoadPath(String dir)'
  def self.add_ivy_directory_to_load_path dir
    all_jars = Dir.glob(dir + "/**/*.jar")
    all_jars.each do |j|
      f = Pathname.new j
      case j
      when /asm-all-3.3.1.jar/, /jruby-complete-1.6.5.1.jar/, /proguard-base-4.6.jar/
        $LOAD_PATH << f.parent
      end
    end
  end

  java_signature 'void addToLoadPath(String file)'
  def self.add_to_load_path file
    $LOAD_PATH << file
  end
end
