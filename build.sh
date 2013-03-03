#!/bin/bash

buildDir=/tmp/apsbuilds

mkdir -p $buildDir

baseUrl=http://download.scala-ide.org/ecosystem
for eclipse in e37 ; do
# for eclipse in e37 e38; do
  for scala in scala210; do
  # for scala in scala29 scala210; do
    for releaseType in dev; do
    # for releaseType in dev stable; do
      dirname=${eclipse}_${scala}_${releaseType}
      destination=$buildDir/$dirname
      mkdir -p $destination
      rsync -a * $destination
      finalUrl=$baseUrl/$eclipse/$scala/$releaseType/site
      eclipseUrl=http://download.eclipse.org/releases/indigo/
      eclipseUrlArg="-Drepo.eclipse=$eclipseUrl"
      scalaIdeUrl="-Drepo.scala-ide=$finalUrl"
      ( cd $destination ; mvn -Pset-versions $scalaIdeUrl $eclipseUrlArg -Dtycho.style=maven --non-recursive exec:java ; echo "mvn $scalaIdeUrl $eclipseUrlArg install" > compile.sh ; )
    done
  done
done
