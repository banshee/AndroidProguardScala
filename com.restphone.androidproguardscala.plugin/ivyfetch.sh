# java -jar C:/Users/james/eclipse/plugins/org.apache.ivy.eclipse.ant_2.3.0.cr2_20121105223351/ivy.jar -ivy ivy.xml  -settings ../../ivysettings.xml -retrieve "lib/[module]-[type]-[artifact]-[revision].[ext]" ; rm lib/*source* ; rm lib/*javadoc* ; ls -l lib
mkdir -p lib
rm lib/*jar
rm jarjar/*jar
java -jar /Users/james/backupEclipseIndigo/plugins/org.apache.ivy.eclipse.ant_2.2.0.final_20100923230623/ivy.jar -ivy ivy.xml  -settings ../../ivysettings.xml -retrieve "lib/[module]-[type]-[artifact]-[revision].[ext]"
rm lib/*source*
rm lib/*javadoc* 
cp lib/*jar jarjar
ls -l lib
