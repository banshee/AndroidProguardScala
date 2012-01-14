require 'java'
require 'proguard_cache_requires'

module ProguardRunner
  import Java::proguard

  # args:
  # :config_file => filename
  # :cksum => string
  #
  def self.execute_proguard args
    config_file = java.io.File.new args[:config_file]

    cp = ConfigurationParser.new config_file

    configuration = Configuration.new

    cp.parse(configuration);

    program_jars = configuration.programJars
    (0...program_jars.size).each do |i|
      element = program_jars.get i
      name = element.name
      if name =~ /\.CKSUM/
        new_file = java.io.File.new name.sub(".CKSUM", args[:cksum])
        element.setFile new_file
      end
    end

    ProGuard.new(configuration).execute
  end
end

# ProguardRunner.execute_proguard :config_file => "proguard_android_scala.config", :tmp_replacement => ".fnord"
