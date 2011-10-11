require 'java'
require 'guava.jar'
require 'fileutils'

def com
  Java::Com
end

class RunnableCallable
  include java.lang.Runnable
  include java.util.concurrent.Callable
  def self.to_runnable_callable items
    items.map {|i| RunnableCallable.new i}
  end

  def initialize o
    @i = o
  end

  def run
    @i.call
  end

  def call x = nil
    @i.call x
  end
end
