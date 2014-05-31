#!/bin/sh

jar=`ls target | grep 'Graphene-assembly-\d\.\d\.\d\.jar'`
minjar=`echo $jar | sed 's/Graphene-assembly-\(.*\)\.jar/graphene-\1.min.jar/g'`
java -Xms1G -Xmx4G -jar ./proguard/lib/proguard.jar -injars target/$jar -outjars target/$minjar @config.pro
