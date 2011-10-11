require 'pathname'
require 'asm_support'
require 'asm_support/jar_and_class_file_visitor'
require 'asm_support/dependency_signature_visitor'
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

      builder = JarAndClassFileVisitor.new DependencySignatureVisitor
      files.each do |classfile|
        current_file = classfile
        case classfile
        when /\.jar$/
          begin
            jarfile = java.util.jar.JarFile.new classfile
            jarfile.entries.each do |e|
              if e.to_s =~ /\.class$/
                task = lambda {builder.build_for_jar_entry jarfile, e}
                task_as_callable = RunnableCallable.new task
                future = completion_service.submit task_as_callable
                futures << future
              end
            end
          rescue Exception => e
            puts "Exception ----------xya", e, jarfile
          end
        when /\.class$/
          begin
            pp "about to create using build for filename"
            task = lambda {builder.build_for_filename classfile}
            task = lambda {pp 'in lambda here'}
            pp "created task lambda ", task
            task_as_callable = RunnableCallable.new task
            future = completion_service.submit task_as_callable
            pp "gotf future", future
            futures << future
          rescue Exception => e
            puts "Exception ----------xyb", e, jarfile
          end
        end
      end

      pp 'futruesarex', futures
      while a_future = futures.pop do
        begin
          pp 'futruesare', a_future, futures
          r = completion_service.take.get
          result.merge! r
        rescue Exception => e
          puts "Ignoring futures exception ----------xyd", e, e.backtrace
        end
      end
      result
    rescue Exception => e
      puts "Exception ----------xyc", e, current_file, e.backtrace, "---- complete"
    ensure
      poolrunner.shutdownNow rescue nil
    end
  end
end
