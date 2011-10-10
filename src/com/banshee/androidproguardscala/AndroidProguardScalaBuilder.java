package com.banshee.androidproguardscala;

import java.io.IOException;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import proguard.Configuration;
import proguard.ConfigurationParser;
import proguard.ParseException;
import proguard.ProGuard;

public class AndroidProguardScalaBuilder extends IncrementalProjectBuilder {
  public IPath proguardConfigFile() {
    IPath q = this.getProject().getLocation().append("proguard.config");
    return q;
  }

  @Override
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
      throws CoreException {
    this.forgetLastBuiltState();
    try {
      startProguard();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
    return null;
  }

  protected void startProguard() throws IOException, ParseException {
    IPath proguardConfigFile = proguardConfigFile();
    Configuration proConfig = proguardConfiguration(proguardConfigFile);
    new ProGuard(proConfig).execute();
  }

  public Configuration proguardConfiguration(IPath proguardConfigFile)
      throws IOException,
      ParseException {
    ConfigurationParser parser = new ConfigurationParser(proguardConfigFile.toFile());
    Configuration proguardConfiguration = new Configuration();
    parser.parse(proguardConfiguration);
    return proguardConfiguration;
  }

  public static final String BUILDER_ID = "AndroidProguardScala.androidProguardScala";
}
