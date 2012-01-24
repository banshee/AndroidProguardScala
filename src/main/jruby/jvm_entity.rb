require 'java'
require 'pathname'
require 'fileutils'

java_package 'com.restphone.androidproguardscala'

class JvmEntity
  def initialize f
    @entity = f
  end

  java_signature "IRubyObject to_s()"

  def to_s
    @entity.to_s
  end

  def to_pathname
    Pathname.new @entity
  end

  def checksum
    Digest::SHA1.hexdigest @entity.to_s.gsub("/", "_")
  end

  def basename
    to_pathname.basename
  end

  def method_missing(name, *args, &block)
    @entity.send(name, *args, &block)
  end
end

class JarFile < JvmEntity
end

class ClassFile < JvmEntity
end

class ClassDirectory < JvmEntity
end

class UnknownEntity < JvmEntity
end

class JvmEntityBuilder
  def self.create f
    case f
    when /\.jar$/i
      JarFile.new f
    when /\.class$/i
      ClassFile.new f
    else
      if File.directory? f
        ClassDirectory.new f
      end
    end
  end
end
