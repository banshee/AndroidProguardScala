package com.banshee.androidproguardscala;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import proguard.ClassPath;
import proguard.ClassPathEntry;
import proguard.Configuration;
import proguard.ConfigurationParser;
import proguard.ParseException;
import proguard.ProGuard;

import com.google.common.io.Files;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class AndroidProguardScalaBuilder extends IncrementalProjectBuilder {
  public IPath proguardConfigFile() {
    // TODO This should be user-configurable
    IPath q = this.getProject()
        .getLocation()
        .append("proguard_android_scala.config");
    return q;
  }

  public Configuration proguardConfiguration(IPath proguardConfigFile)
      throws IOException,
      ParseException,
      CoreException {
    if (proguardConfigFile.toFile().exists()) {
      ConfigurationParser parser = new ConfigurationParser(proguardConfigFile.toFile());
      Configuration proguardConfiguration = new Configuration();
      parser.parse(proguardConfiguration);
      return proguardConfiguration;
    } else {
      final String msg = "FATAL: You must have a ProGuard config file at "
          + proguardConfigFile.toOSString();
      // IMarker m = ResourcesPlugin.getWorkspace()
      // .getRoot()
      // .createMarker(IMarker.PROBLEM);
      // m.setAttribute(IMarker.MESSAGE, msg);
      final Status status = new Status(IStatus.ERROR, BUILDER_ID, msg);
      throw new CoreException(status);
    }
  }

  private String computeProguardSignature() {
    // TODO use the JRuby signature here so we don't run proguard on every build
    return "";
  }

  private ProguardTask createProguardBuildTask(String signature)
      throws IOException,
      ParseException,
      CoreException {
    IPath proguardConfigFile = proguardConfigFile();
    final Configuration proConfig = proguardConfiguration(proguardConfigFile);
    final File tempOutputFile = File.createTempFile("proguard_temp_file",
        ".jar");
    tempOutputFile.deleteOnExit();
    String originalName = replaceTmpProguardOutput(proConfig, tempOutputFile);
    final ListenableFuture<File> proguardFuture = listeningExecutorService.submit(new Callable<File>() {
      @Override
      public File call() throws Exception {
        File result = executeProguard(proConfig, tempOutputFile);
        return result;
      }
    });
    return new ProguardTask(signature,
        proguardFuture,
        tempOutputFile,
        destinationFile(originalName));
  }

  private File destinationFile(String originalName) {
    return new File(originalName.replace(".TMP", ""));
  }

  private
      void
      moveProguardOutputToFinalDestination(File file, File destination)
          throws IOException {
    Files.move(file, destination);
  }

  /**
   * @param proConfig
   * @param tempOutputFile
   * @return The name of the output file in proConfig.programJars, or null if no
   *         filename includes TMP.
   */
  private String replaceTmpProguardOutput(
      Configuration proConfig,
      File tempOutputFile) {
    final ClassPath jars = proConfig.programJars;
    String result = null;
    for (int index = 0; index < jars.size(); index++) {
      final ClassPathEntry jar = jars.get(index);
      if (jar.isOutput()) {
        result = jar.getName();
        jar.setFile(tempOutputFile);
        break;
      }
    }
    return result;
  }

  private void watchFuture(ProguardTask task, IProgressMonitor monitor)
      throws InterruptedException,
      ExecutionException,
      IOException {
    ListenableFuture<File> f = task.getBuildTask();
    monitor.beginTask(BUILDER_ID, 100);
    while (!isInterrupted() && !monitor.isCanceled()) {
      if (f.isDone()) {
        moveProguardOutputToFinalDestination(f.get(), task.getDestinationFile());
        return;
      }
      monitor.worked(1);
      Thread.sleep(500);
    }
    if (isInterrupted() || monitor.isCanceled()) {
      System.out.println("got cancel");
    }
  }

  @Override
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
      throws CoreException {
    Exception exception = null;
    try {
      String proguardSignature = computeProguardSignature();
      final ProguardTask task = runningProguardTask.get();
      if (task.signatureMatches(proguardSignature)) {
        watchFuture(task, monitor);
      } else {
        final File tmpfile = task.getTempFile();
        // If the signatures aren't the same, then
        // the work of the current builder is wasted.
        // Finish and then delete it. (There's no good
        // way to interrupt Proguard.)
        if (tmpfile != null) {
          task.getBuildTask().addListener(new Runnable() {
            @Override
            public void run() {
              tmpfile.delete();
            }
          }, MoreExecutors.sameThreadExecutor());
        }
        ProguardTask newTask = createProguardBuildTask(proguardSignature);
        runningProguardTask.set(newTask);
        watchFuture(newTask, monitor);
      }
    } catch (IOException e) {
      exception = e;
    } catch (ParseException e) {
      exception = e;
    } catch (InterruptedException e) {
      exception = e;
    } catch (ExecutionException e) {
      exception = e;
    }
    if (exception != null) {
      final Status status = new Status(IStatus.ERROR,
          BUILDER_ID,
          exception.getLocalizedMessage(),
          exception);
      throw new CoreException(status);
    }
    // TODO only refresh the output jar
    getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
    return null;
  }

  protected File executeProguard(Configuration proConfig, File tempOutputFile) {
    try {
      new ProGuard(proConfig).execute();
    } catch (IOException e) {
      tempOutputFile.delete();
      return null;
    }
    return tempOutputFile;
  }

  public static final String BUILDER_ID = "AndroidProguardScala.androidProguardScala";
  private final ExecutorService executorService = MoreExecutors.getExitingExecutorService((ThreadPoolExecutor) Executors.newFixedThreadPool(4));
  private final ListeningExecutorService listeningExecutorService = MoreExecutors.listeningDecorator(executorService);
  private final List<IMarker> markers = new ArrayList<IMarker>();
  final AtomicReference<ProguardTask> runningProguardTask = ProguardTask.emptyAtomicReferenceToTask();
}
