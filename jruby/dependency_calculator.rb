require 'rubygems'
require 'pathname'
require 'asm_support'
require 'required_classes'
require 'runnable_callable'

include AsmSupport

class DependencyCalculator
  def self.calculate files
    begin
      result = {}
      poolrunner = java.util.concurrent::Executors.newFixedThreadPool 100
      completion_service = java.util.concurrent.ExecutorCompletionService.new poolrunner
      futures = []

      current_file = ""

      builder = RubyInterfaceImplementationBuilder.new DependencySignatureVisitor
      files.each do |classfile|
        current_file = classfile
        case classfile
        when /\.jar$/
          jarfile = java.util.jar.JarFile.new classfile
          jarfile.entries.each do |e|
            if e.to_s =~ /\.class$/
              task = lambda {builder.build_for_jar_entry jarfile, e}
              task_as_callable = RunnableCallable.new task
              future = completion_service.submit task_as_callable
              futures << future
            end
          end
        when /\.class$/
          task = lambda {builder.build_for_filename classfile}
          task_as_callable = RunnableCallable.new task
          future = completion_service.submit task_as_callable
          futures << future
        end
      end

      while futures.pop do
        r = completion_service.take.get
        result.merge! r
      end
      result
    rescue Exception => e
      puts "Exception ----------x", e, current_file, "foo"
    ensure
      poolrunner.shutdownNow rescue nil
    end
  end
end
