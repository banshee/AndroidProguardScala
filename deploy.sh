project_dir=~/workspace/AndroidProguardScala

~/src/s3cmd/s3cmd del --recursive s3://androidproguardscala/UpdateSiteForAndroidProguardScala/
bin_dir=${project_dir}/com.restphone.androidproguardscala.updatesite/target/repository/
ls -lh $bin_dir
~/src/s3cmd/s3cmd -P sync ${bin_dir} s3://androidproguardscala/UpdateSiteForAndroidProguardScala/
