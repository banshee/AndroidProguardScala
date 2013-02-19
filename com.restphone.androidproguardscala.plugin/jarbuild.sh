# This is hardcoded to a single machine; that's obviouly bad.

jarjar=lib/jarjar-jar-jarjar-1.3.jar

# Run all classfiles in bin directory through jarjar to move scala.* to com.restphone.scala.*
tmpjar1=/tmp/jar1.jar
tmpjar2=/tmp/jar2.jar
( cd bin ; jar cf $tmpjar1 `find com -name \*.class` )
java -jar $jarjar process jarjarrule.move_scala_to_com_restphone $tmpjar1 $tmpjar2
( cd bin ; rm -rf com org ; jar xf $tmpjar2 )

# Run all lib files through jarjar
mkdir -p jarjar
for i in lib/*
do
  java -jar $jarjar process jarjarrule.move_scala_to_com_restphone $i jarjar/`basename $i`
done

#cleanup build artifacts
rm -rf /tmp/plug?
