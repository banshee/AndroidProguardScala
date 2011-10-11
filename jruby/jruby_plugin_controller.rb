require 'java'

java_package 'com.restphone.jrubyeclipse'

class JrubyPluginController
  def create_filter ruby_file_name
    ruby_file_contents = IO.read ruby_file_name
    eval ruby_file_contents
    eclipse_callback
  end
end
