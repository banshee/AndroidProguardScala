require 'java'
require 'guava.jar'
require 'fileutils'
require 'runnable_callable'

def com
  Java::Com
end

class CanOpener
  def self.extract_file jarfile, zipfile_entry, target_dir_root
    unless zipfile_entry.is_directory
      destination_file = java.io.File.new target_dir_root, zipfile_entry.to_s
      unless destination_file.exists?
        begin
          destination_directory = File.dirname destination_file.to_s
          FileUtils.mkdir_p destination_directory
          destination_file_stream = java.io.FileOutputStream.new destination_file
          input_stream = jarfile.get_input_stream zipfile_entry
          com.google.common.io.ByteStreams.copy input_stream, destination_file_stream
        ensure
          input_stream.close rescue nil
          destination_file_stream.close rescue nil
        end
      end
    end
  end

  def self.zip_entries_matching jarfile, matching_entries
    jarfile.entries.select do |e|
      matching_entries.find do |f|
        case f
        when String
          f.to_s == e.to_s
        when Regexp
          f =~ e.to_s
        end
      end
    end
  end
end
