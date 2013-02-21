# from https://github.com/scalatest/scalatest-eclipse-plugin/blob/master/ecosystem-build.sh
# 
#!/bin/bash


####################
#
# $1 - build profile
# $2 - ecosystem location
# $3 - scala version 
# $4 - ecosystem id
# $5 - eclipse ecosystem
#
####################
function build() {
  cd ${ROOT_DIR}
  mvn -Pset-versions -P$1 -Drepo.scala-ide=$2 -Dscala.version=$3 -Drepo.eclipse=$5 -Dtycho.style=maven --non-recursive exec:java

  mvn -Pset-versions -P$1 -Drepo.scala-ide=$2 -Dscala.version=$3 -Drepo.eclipse=$5 clean package

  rm -rf ${TARGET_DIR}/$4
  mkdir -p ${TARGET_DIR}

  cp -r ${ROOT_DIR}/org.scala-ide.sdt.scalatest.update-site/target/site/ ${TARGET_DIR}/$4
}

###################

# root dir (containing this script)
ROOT_DIR=$(dirname $0)
cd ${ROOT_DIR}
ROOT_DIR=${PWD}

TARGET_DIR=~/tmp/scalatest-build-ecosystem

# scala-ide/build-tools/maven-tool/merge-site/ location
MERGE_TOOL_DIR=~/git/build-tools/maven-tool/merge-site

###################

set -x

rm -rf ${TARGET_DIR}

#build scala-ide-master-scala-2.9 http://download.scala-ide.org/ecosystem/e37/scala29/dev/site/ 2.9.3-scala-ide-m2 e37-scala29-dev http://download.eclipse.org/releases/indigo/
#build scala-ide-master-scala-2.9 http://download.scala-ide.org/ecosystem/e38/scala29/dev/site/ 2.9.3-scala-ide-m2 e38-scala29-dev http://download.eclipse.org/releases/juno/
#build scala-ide-master-scala-trunk http://download.scala-ide.org/ecosystem/e37/scala29/dev/site/ 2.10.0-RC1 e37-scala210-dev http://download.eclipse.org/releases/indigo/
#build scala-ide-master-scala-trunk http://download.scala-ide.org/ecosystem/e38/scala29/dev/site/ 2.10.0-RC1 e38-scala210-dev http://download.eclipse.org/releases/juno/
# build scala-ide-2.1-m3 http://download.scala-ide.org/ecosystem/next/e37/scala29/dev/base/ 2.9.3-RC2 e37-scala29-m3 http://download.eclipse.org/releases/indigo/
# build scala-ide-2.1-m3 http://download.scala-ide.org/ecosystem/next/e38/scala29/dev/base/ 2.9.3-RC2 e38-scala29-m3 http://download.eclipse.org/releases/juno/
build scala-ide-2.1-m3-2_10 http://download.scala-ide.org/sdk/next/e37/scala210/dev/base/ 2.10.0 e37-scala210-m3 http://download.eclipse.org/releases/indigo/
# build scala-ide-2.1-m3-2_10 http://download.scala-ide.org/sdk/next/e38/scala210/dev/base/ 2.10.0 e38-scala210-m3 http://download.eclipse.org/releases/juno/
# build scala-ide-2.0.2 http://download.scala-ide.org/sdk/e37/scala29/stable/site/ 2.9.2 e37-scala29-2.0.2 http://download.eclipse.org/releases/indigo/

cd ${MERGE_TOOL_DIR}
#mvn -Drepo.dest=${TARGET_DIR}/combined -Drepo.source=file://${TARGET_DIR}/e37-scala29-dev package
#mvn -Drepo.dest=${TARGET_DIR}/combined -Drepo.source=file://${TARGET_DIR}/e38-scala29-dev package
#mvn -Drepo.dest=${TARGET_DIR}/combined -Drepo.source=file://${TARGET_DIR}/e37-scala210-dev package
#mvn -Drepo.dest=${TARGET_DIR}/combined -Drepo.source=file://${TARGET_DIR}/e38-scala210-dev package
# mvn -Drepo.dest=${TARGET_DIR}/combined -Drepo.source=file://${TARGET_DIR}/e37-scala29-m3 package
# mvn -Drepo.dest=${TARGET_DIR}/combined -Drepo.source=file://${TARGET_DIR}/e38-scala29-m3 package
# mvn -Drepo.dest=${TARGET_DIR}/combined -Drepo.source=file://${TARGET_DIR}/e37-scala210-m3 package
# mvn -Drepo.dest=${TARGET_DIR}/combined -Drepo.source=file://${TARGET_DIR}/e38-scala210-m3 package
# mvn -Drepo.dest=${TARGET_DIR}/combined -Drepo.source=file://${TARGET_DIR}/e37-scala29-2.0.2 package
