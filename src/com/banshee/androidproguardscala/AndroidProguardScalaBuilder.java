package com.banshee.androidproguardscala;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import proguard.ParseException;
import proguard.ProGuard;

public class AndroidProguardScalaBuilder extends IncrementalProjectBuilder {
	public IPath proguardConfigFile() {
		IPath q = this.getProject().getLocation().append("proguard.config"); // XXX
		// This
		// string
		// should
		// be
		// configurable
		return q;
	}

	@SuppressWarnings("unchecked")
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		this.forgetLastBuiltState(); // Proguard has its own internal system for
		// this
		startProguardWrapped(monitor);
		getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		return null;
	}

	class APSOutputStream extends ByteArrayOutputStream {
		public AndroidProguardScalaBuilder androidScalaBuilder;
		public IProgressMonitor monitor;

		public APSOutputStream(AndroidProguardScalaBuilder s, IProgressMonitor m) {
			androidScalaBuilder = s;
			monitor = m;
			monitor.beginTask("proguard", IProgressMonitor.UNKNOWN);
		}

		public synchronized void write(byte b[], int off, int len) {
			super.write(b, off, len);
			monitor.subTask("bytes of output from proguard: "
					+ Integer.toString(this.count) + " ");
			monitor.worked(1);
			if (androidScalaBuilder.isInterrupted()) {
				throw new OperationCanceledException();
			}
		}
	}

	protected void startProguard(IProgressMonitor monitor) throws IOException,
			ParseException {
		IPath proguardConfigFile = proguardConfigFile();
		proguard.Configuration proConfig = proguardConfiguration(proguardConfigFile);
		APSOutputStream apsOut = new APSOutputStream(this, monitor);
		PrintStream originalSystemOut = System.out;
		try {
			System.setOut(new PrintStream(apsOut));
			new ProGuard(proConfig).execute();
		} finally {
			System.setOut(originalSystemOut);
		}
	}

	public proguard.Configuration proguardConfiguration(IPath proguardConfigFile)
			throws IOException, ParseException {
		proguard.ConfigurationParser parser = new proguard.ConfigurationParser(
				proguardConfigFile.toFile());
		proguard.Configuration proguardConfiguration = new proguard.Configuration();
		parser.parse(proguardConfiguration);
		return proguardConfiguration;
	}

	protected void startProguardWrapped(IProgressMonitor monitor) {
		try {
			startProguard(monitor);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	public static final String BUILDER_ID = "AndroidProguardScala.androidProguardScala";
}
