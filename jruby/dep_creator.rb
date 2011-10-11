require 'dependency_signature'
require 'pp'

signature = DependencySignature.new

def glob_list files
  files.map do |f|
    Dir.glob f
  end.flatten
end

files = glob_list ARGV
signature.add_files files
pp signature.signature
