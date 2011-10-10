package com.banshee.androidproguardscala;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.util.concurrent.ListenableFuture;

public class ProguardTask {
  public static AtomicReference<ProguardTask> emptyAtomicReferenceToTask() {
    ProguardTask t = emptyTask();
    return new AtomicReference<ProguardTask>(t);
  }

  public static ProguardTask emptyTask() {
    return new ProguardTask(emptyTaskSignture, null, null, null);
  }

  public ProguardTask(
      String signature,
      ListenableFuture<File> proguardFuture,
      File tempFile,
      File destinationFile) {
    this.signature = signature;
    this.destinationFile = destinationFile;
    this.tempFile = tempFile;
    this.buildTask = proguardFuture;
  }

  public ListenableFuture<File> getBuildTask() {
    return buildTask;
  }

  public File getDestinationFile() {
    return destinationFile;
  }

  public File getTempFile() {
    return tempFile;
  }

  public boolean isEmpty() {
    return signature.equals(emptyTaskSignture);
  }

  public boolean signatureMatches(String rhs) {
    return false;
  }

  final private static String emptyTaskSignture = "empty";

  final private ListenableFuture<File> buildTask;
  final private File destinationFile;
  final private File tempFile;
  final String signature;
}
