package com.restphone.androidproguardscala

import java.io.File
import java.io.ObjectStreamException

import scala.Array.canBuildFrom

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.IResourceDelta
import org.eclipse.core.resources.IWorkspaceRoot
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.FileLocator
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Path
import org.eclipse.core.runtime.Platform
import org.eclipse.core.runtime.Status
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.JavaCore

import com.google.common.io.Files
import com.restphone.jartender.CacheSystem
import com.restphone.jartender.FileFailureValidation.FailureValidation
import com.restphone.jartender.FileFailureValidation.FailureWithoutException
import com.restphone.jartender.FileFailureValidation.convertExceptionToValidation
import com.restphone.jartender.FileFailureValidation.stdExceptions
import com.restphone.jartender.FileFailureValidation.validatedFile
import com.restphone.jartender.JartenderCacheParameters
import com.restphone.jartender.RichFile
import com.restphone.jartender.SerializableUtilities

import scalaz.Scalaz._

class AndroidProguardScalaBuilder extends IncrementalProjectBuilder {
  import RichPath._

  def buildPatternMatch[T]( fn: T => Boolean ) = new Object {
    def unapply[U <% T]( x: U ) = if ( fn( x ) ) some( x ) else none
  }

  override def build( kind: Int, args: java.util.Map[String, String], monitor: IProgressMonitor ): Array[IProject] = {
    val buildRequired = {
      val pathIsBuildArtifact = buildPatternMatch[IPath]( _.lastSegment.startsWith( "proguard_" ) )
      def buildArtifactsRequireRebuild( xs: Stream[IPath] ): Boolean = {
        xs match {
          case pathIsBuildArtifact( h ) #:: Stream.Empty => false
          case pathIsBuildArtifact( h ) #:: t => buildArtifactsRequireRebuild( t )
          case _ => true
        }
      }

      val affected_paths = getDelta( getProject ) match {
        case x: IResourceDelta => x.getAffectedChildren map { _.getFullPath }
        case null => Array.empty[IPath]
      }

      buildArtifactsRequireRebuild( affected_paths.toStream )
    }

    if ( buildRequired ) {
      val proguardDefaults = {
        val pathToDefaultsFile = pluginDirectory.get / "proguard_cache_conf" / "proguard_defaults.conf"
        RichFile.slurp( pathToDefaultsFile.toFile )
      }

      Seq( cacheDir, confDir, libDirectory ) foreach RichPath.ensureDirExists

      val projectdata = new JavaProjectData( javaProject )

      val proguardProcessedConfFile = confDir / "proguard_postprocessed.conf"
      val proguardAdditionsFile = confDir / "proguard_additions.conf"
      val cachedJar = cacheDir / "scala-library.CKSUM.jar"
      val outputJar = rootDirectoryOfProject / "libs" / AndroidProguardScalaBuilder.minifiedScalaLibraryName
      val existingOutputFolders = projectdata.outputDirectories

      implicit def convertIPathToString( p: IPath ): String = p.toString

      val jartenderParams = JartenderCacheParameters(
        cacheDir = cacheDir,
        classFiles = existingOutputFolders.toList,
        inputJars = projectdata.inputJars map { _.fullPath },
        libraryJars = projectdata.libraryJars map { _.fullPath } )

      val proguardCacheParameters = new ProguardCacheParameters(
        jartenderConfiguration = jartenderParams,
        proguardAdditionsFile = proguardAdditionsFile,
        proguardProcessedConfFile = proguardProcessedConfFile )

      val cacheFile = new File( cacheDir / "cache.data" )

      def readCacheSystemFromFile( f: File ) = {
        val deserializeExceptions = classOf[ClassNotFoundException] :: classOf[ObjectStreamException] :: classOf[ClassCastException] :: stdExceptions
        implicit val safeDeserialize = convertExceptionToValidation( deserializeExceptions )_

        def bytesToCacheSystem( bytes: Array[Byte] ): FailureValidation[CacheSystem] =
          for {
            x <- safeDeserialize( "deserialize CacheSystem", none ) {
              val obj: Option[CacheSystem] = SerializableUtilities.byteArrayToObject( bytes )
              obj match {
                case Some( v ) => v.success
                case None => FailureWithoutException( "failed to get a cache system object from bytes" ).failureNel
              }
            }
          } yield x

        for {
          existingFile <- validatedFile( f, "reading cache file" )
          bytesFromCacheFile = Files.toByteArray( cacheFile )
          result <- bytesToCacheSystem( bytesFromCacheFile )
        } yield result
      }

      val result = for {
        cacheSystem <- readCacheSystemFromFile( cacheFile ) ||| ( new CacheSystem ).success
        runner = new ProguardRunner( cacheSystem )
        shrinker = runner.createShrinker( proguardCacheParameters )
        cacheEntry <- cacheSystem.execute( shrinker )
        outputPath = cacheEntry.c.jarfilepath
        outputJarFile <- cacheSystem.installOutputJar( new File( outputPath ), new File( outputJar.toString ) )
      } yield {
        val bytesForCache = SerializableUtilities.convertToByteArray( cacheSystem )
        val lnx = bytesForCache.length
        Files.write( bytesForCache, cacheFile )
        cacheEntry
      }

      logMsg( "cache run: " + result )

      Iterable( outputJar, confDir, cacheDir ) foreach tellEclipsePathNeedsToBeRefreshed
    }

    Array.empty
  }

  def tellEclipsePathNeedsToBeRefreshed( p: IPath ) = {
    getProject.getFile( p ).refreshLocal( IResource.DEPTH_INFINITE, null )
  }

  // We can't create the cacheController until the plugin is started.  That happens
  // after object initialization, so it's a lazy.
  lazy val cacheController = ProguardCacheController.buildCacheController( pluginDirectory.toString )

  override def clean( monitor: IProgressMonitor ): Unit = cacheController.clean_cache( cacheDir.toString )

  def rootDirectoryOfProject = convertResourceToFilesystemLocation( getProject )
  def cacheDir = rootDirectoryOfProject / "proguard_cache"
  def confDir = rootDirectoryOfProject / "proguard_cache_conf"
  def libDirectory = rootDirectoryOfProject / "libs"
  //  def scalaProject = scala.tools.eclipse.ScalaProject(getProject)

  def isCpeLibrary( x: IClasspathEntry ) = x.getEntryKind == IClasspathEntry.CPE_LIBRARY
  def isMinifiedLibraryName( s: String ) = s == AndroidProguardScalaBuilder.minifiedScalaLibraryName

  def convertResourceToFilesystemLocation( resource: IResource ) = new Path( resource.getLocationURI.getPath )

  def logger() = new ProvidesLogging {
    def logMsg( msg: String ) = AndroidProguardScalaBuilder.this.logMsg( msg )
    def logError( msg: String ) = AndroidProguardScalaBuilder.this.logMsg( msg, IStatus.ERROR )
  }

  private val lastSegmentIsString = ( s: String ) => ( p: IPath ) => p.lastSegment.equals( s )
  val lastSegmentIsScalaLibrary = lastSegmentIsString( "scala-library.jar" )
  val lastSegmentIsAndroidLibrary = lastSegmentIsString( "android.jar" )
  val fileExists = ( p: IPath ) => p.toFile.exists

  lazy val javaProject = JavaCore.create( getProject )

  def objToString[T]( x: T ) = x.toString

  def getWorkspaceRoot: IWorkspaceRoot = ResourcesPlugin.getWorkspace.getRoot
  def rootDirectoryOfWorkspace: IPath = getWorkspaceRoot.getLocation

  val platformBundle = Platform.getBundle( "com.restphone.androidproguardscala" );

  // This seems like a hack, but it's apparently the right thing to do to find the plugin
  // directory.
  def pluginDirectory =
    for {
      u <- NotNull( FileLocator.find( platformBundle, new Path( "/" ), null ), "cannot find directory for bundle" )
      filenameUrl <- NotNull( FileLocator.resolve( u ), "FileLocator.resolve must not return null" )
      f = new Path( filenameUrl.getFile )
    } yield f

  def logMsg( msg: String, status: Integer = IStatus.INFO ) = {
    val log = Platform.getLog( platformBundle );
    val s = new Status( status, pluginId, msg )
    log.log( s )
  }

  val pluginId = "com.restphone.androidproguardscala"
}

object AndroidProguardScalaBuilder {
  val BUILDER_ID = "com.restphone.androidproguardscala.Builder";
  val minifiedScalaLibraryName = "scala_library.min.jar"
}

class RichPath( p: IPath ) {
  def /( that: String ) = p.append( that )
}

object RichPath {
  implicit def toRichPath( p: IPath ): RichPath = new RichPath( p )
  def ensureDirExists( p: IPath ) = RichFile.ensureDirExists( p.toFile )
}
