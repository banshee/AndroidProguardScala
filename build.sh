#!/bin/bash

buildDir=/tmp/apsbuilds

mkdir $buildDir

baseUrl=http://download.scala-ide.org/ecosystem
for eclipse in e37 e38; do
  for scala in scala29 scala210; do
    for releaseType in dev stable; do
      dirname=${eclipse}_${scala}_${releaseType}
      destination=$buildDir/$dirname
      mkdir -p $destination
      rsync -a * $destination
      finalUrl=$baseUrl/$eclipse/$scala/$releaseType/site
      ( cd $destination ; mvn -Pset-versions -Drepo.scala-ide=$finalUrl -Drepo.eclipse=http://download.eclipse.org/releases/indigo/ -Dtycho.style=maven --non-recursive exec:java )
    done
  done
done
