#!/bin/bash

buildDir=/tmp/apsbuilds

mkdir -p $buildDir

~/src/s3cmd/s3cmd del --recursive s3://androidproguardscala/UpdateSiteForAndroidProguardScalaEcosystem/

baseUrl=http://download.scala-ide.org/ecosystem
for eclipse in e37 e38; do
  for scala in scala210; do
  # for scala in scala29 scala210; do
    for releaseType in dev stable; do
        dirname=${releaseType}-${scala}-${eclipse}
        destination=$buildDir/$dirname
        mkdir -p $destination
        rsync --delete --delete-excluded --exclude target -a * $destination
        finalUrl=$baseUrl/$eclipse/${nextOrCurrent}$scala/$releaseType/site
        if [ "$eclipse" = "e37" ] ; then
          eclipseUrl=http://download.eclipse.org/releases/indigo/
        else
          eclipseUrl=http://download.eclipse.org/releases/juno/
        fi
        eclipseUrlArg="-Drepo.eclipse=$eclipseUrl"
        scalaIdeUrl="-Drepo.scala-ide=$finalUrl"
        ( cd $destination ; mvn -Pset-versions $scalaIdeUrl $eclipseUrlArg -Dtycho.style=maven --non-recursive exec:java ; echo "cd com.restphone.androidproguardscala.parent ; mvn $scalaIdeUrl $eclipseUrlArg install" > compile.sh ; sh compile.sh )
        echo set up in $destination $scalaIdeUrl $eclipseUrlArg
        ~/src/s3cmd/s3cmd -P sync $destination/com.restphone.androidproguardscala.updatesite/target/repository/ s3://androidproguardscala/UpdateSiteForAndroidProguardScalaEcosystem/$dirname/
        ~/src/s3cmd/s3cmd -P sync $destination/com.restphone.androidproguardscala.updatesite/target/com.restphone.androidproguardscala.updatesite.zip s3://androidproguardscala/UpdateSiteForAndroidProguardScalaEcosystem/$dirname/
    done
  done
done
