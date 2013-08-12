project_dir=~/workspace/AndroidProguardScala
rm -rf ~/.m2/repository/com/restphone/ ${project_dir}/AndroidProguardScala/com.restphone.androidproguardscala.*/lib/*
( cd ${project_dir}/com.restphone.androidproguardscala.parent && MAVEN_OPTS="-Xmx5120m -XX:MaxPermSize=1208M" mvn clean install )
