mkdir -p tmp
rm -f tmp/ProjectUtil*
cp bin/com/restphone/androidproguardscala/ProjectUtilities* tmp
( cd bin ; jar cf /tmp/jar1.jar `find com -name \*.class` )
rm -f /tmp/jarScalaReplaced.jar
rm -f /tmp/jarfinal.jar
java -jar ~/lib/jarjar.jar process lib/r1 /tmp/jar1.jar /tmp/jarScalaReplaced.jar
java -jar ~/lib/jarjar.jar process lib/r2 /tmp/jarScalaReplaced.jar /tmp/jarfinal.jar
( cd bin ; jar xf /tmp/jarfinal.jar )
mv tmp/ProjectUt* bin/com/restphone/androidproguardscala/
rm -rf /tmp/plug?
