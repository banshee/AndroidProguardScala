require 'pathname'
require 'asm_support'
require 'pp'

include AsmSupport

module RequiredClasses
  # Returns an array of classes that are required for all classes given in
  # starting_points_hash.
  def self.extract_required_classes requirements_hash, starting_points_hash = {}, depth = 0
    new_requirements = {}
    starting_points_hash.keys.each do |s|
      if requirements_hash[s]
        requirements_hash[s].keys.each do |k|
          new_requirements[k] = 1
        end
      end
    end
    ending_requirements = starting_points_hash.merge new_requirements
    if ending_requirements.length == starting_points_hash.length
      ending_requirements.keys
    else
      extract_required_classes requirements_hash, ending_requirements, depth + 1
    end
  end

  def self.class_to_relative_filename c
    c.sub(/^\[?L?/, '').sub(/;$/, '').ext ".class"
  end

  def self.copy_required_files relative_file_paths, src_dir, dest_dir
    src_path = Pathname.new src_dir
    dest_path = Pathname.new dest_dir
    relative_file_paths.each do |f|
      srcfile = src_path + f
      dstfile = dest_path + f
      if File.exists? srcfile
        FileUtils.install_with_dir_if_missing srcfile, dstfile, :mode => 0444, :verbose => true
      else
        puts "missing: " + srcfile
      end
    end
  end
end
