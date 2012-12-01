java -jar /Users/james/backupEclipseIndigo/plugins/org.apache.ivy.eclipse.ant_2.2.0.final_20100923230623/ivy.jar -ivy ivy.xml  -settings ../ivysettings.xml -retrieve "lib/[module]-[type]-[artifact]-[revision].[ext]" ; rm lib/*source* ; rm lib/*javadoc* ; ls -l lib
java -jar C:/Users/james/eclipse/plugins/org.apache.ivy.eclipse.ant_2.3.0.cr2_20121105223351/ivy.jar -ivy ivy.xml  -settings ../ivysettings.xml -retrieve "lib/[module]-[type]-[artifact]-[revision].[ext]" ; rm lib/*source* ; rm lib/*javadoc* ; ls -l lib

# Run all classfiles in bin directory through jarjar to move scala.* to com.restphone.scala.*
tmpjar1=/tmp/jar1.jar
tmpjar2=/tmp/jar2.jar
( cd bin ; jar cf $tmpjar1 `find com -name /*.class` )
java -jar ~/lib/jarjar.jar process jarjarrule.move_scala_to_com_restphone $tmpjar1 $tmpjar2
( cd bin ; jar xf $tmpjar2 )

# Run all lib files through jarjar
mkdir -p jarjar
for i in lib/*
do
  java -jar ~/lib/jarjar.jar process jarjarrule.move_scala_to_com_restphone $i jarjar/`basename $i`
done

#cleanup build artifacts
rm -rf /tmp/plug?
