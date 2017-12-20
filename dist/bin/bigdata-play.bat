@REM bigdata-play launcher script
@REM
@REM Environment:
@REM JAVA_HOME - location of a JDK home dir (optional if java on path)
@REM CFG_OPTS  - JVM options (optional)
@REM Configuration:
@REM BIGDATA_PLAY_config.txt found in the BIGDATA_PLAY_HOME.
@setlocal enabledelayedexpansion

@echo off

if "%BIGDATA_PLAY_HOME%"=="" set "BIGDATA_PLAY_HOME=%~dp0\\.."

set "APP_LIB_DIR=%BIGDATA_PLAY_HOME%\lib\"

rem Detect if we were double clicked, although theoretically A user could
rem manually run cmd /c
for %%x in (!cmdcmdline!) do if %%~x==/c set DOUBLECLICKED=1

rem FIRST we load the config file of extra options.
set "CFG_FILE=%BIGDATA_PLAY_HOME%\BIGDATA_PLAY_config.txt"
set CFG_OPTS=
if exist "%CFG_FILE%" (
  FOR /F "tokens=* eol=# usebackq delims=" %%i IN ("%CFG_FILE%") DO (
    set DO_NOT_REUSE_ME=%%i
    rem ZOMG (Part #2) WE use !! here to delay the expansion of
    rem CFG_OPTS, otherwise it remains "" for this loop.
    set CFG_OPTS=!CFG_OPTS! !DO_NOT_REUSE_ME!
  )
)

rem We use the value of the JAVACMD environment variable if defined
set _JAVACMD=%JAVACMD%

if "%_JAVACMD%"=="" (
  if not "%JAVA_HOME%"=="" (
    if exist "%JAVA_HOME%\bin\java.exe" set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
  )
)

if "%_JAVACMD%"=="" set _JAVACMD=java

rem Detect if this java is ok to use.
for /F %%j in ('"%_JAVACMD%" -version  2^>^&1') do (
  if %%~j==java set JAVAINSTALLED=1
  if %%~j==openjdk set JAVAINSTALLED=1
)

rem BAT has no logical or, so we do it OLD SCHOOL! Oppan Redmond Style
set JAVAOK=true
if not defined JAVAINSTALLED set JAVAOK=false

if "%JAVAOK%"=="false" (
  echo.
  echo A Java JDK is not installed or can't be found.
  if not "%JAVA_HOME%"=="" (
    echo JAVA_HOME = "%JAVA_HOME%"
  )
  echo.
  echo Please go to
  echo   http://www.oracle.com/technetwork/java/javase/downloads/index.html
  echo and download a valid Java JDK and install before running bigdata-play.
  echo.
  echo If you think this message is in error, please check
  echo your environment variables to see if "java.exe" and "javac.exe" are
  echo available via JAVA_HOME or PATH.
  echo.
  if defined DOUBLECLICKED pause
  exit /B 1
)


rem We use the value of the JAVA_OPTS environment variable if defined, rather than the config.
set _JAVA_OPTS=%JAVA_OPTS%
if "!_JAVA_OPTS!"=="" set _JAVA_OPTS=!CFG_OPTS!

rem We keep in _JAVA_PARAMS all -J-prefixed and -D-prefixed arguments
rem "-J" is stripped, "-D" is left as is, and everything is appended to JAVA_OPTS
set _JAVA_PARAMS=
set _APP_ARGS=

:param_loop
call set _PARAM1=%%1
set "_TEST_PARAM=%~1"

if ["!_PARAM1!"]==[""] goto param_afterloop


rem ignore arguments that do not start with '-'
if "%_TEST_PARAM:~0,1%"=="-" goto param_java_check
set _APP_ARGS=!_APP_ARGS! !_PARAM1!
shift
goto param_loop

:param_java_check
if "!_TEST_PARAM:~0,2!"=="-J" (
  rem strip -J prefix
  set _JAVA_PARAMS=!_JAVA_PARAMS! !_TEST_PARAM:~2!
  shift
  goto param_loop
)

if "!_TEST_PARAM:~0,2!"=="-D" (
  rem test if this was double-quoted property "-Dprop=42"
  for /F "delims== tokens=1,*" %%G in ("!_TEST_PARAM!") DO (
    if not ["%%H"] == [""] (
      set _JAVA_PARAMS=!_JAVA_PARAMS! !_PARAM1!
    ) else if [%2] neq [] (
      rem it was a normal property: -Dprop=42 or -Drop="42"
      call set _PARAM1=%%1=%%2
      set _JAVA_PARAMS=!_JAVA_PARAMS! !_PARAM1!
      shift
    )
  )
) else (
  if "!_TEST_PARAM!"=="-main" (
    call set CUSTOM_MAIN_CLASS=%%2
    shift
  ) else (
    set _APP_ARGS=!_APP_ARGS! !_PARAM1!
  )
)
shift
goto param_loop
:param_afterloop

set _JAVA_OPTS=!_JAVA_OPTS! !_JAVA_PARAMS!
:run

set "APP_CLASSPATH=%APP_LIB_DIR%\..\conf\;%APP_LIB_DIR%\bigdata-play.bigdata-play-1.0-SNAPSHOT-sans-externalized.jar;%APP_LIB_DIR%\org.scala-lang.scala-library-2.11.8.jar;%APP_LIB_DIR%\com.typesafe.play.twirl-api_2.11-1.3.3.jar;%APP_LIB_DIR%\org.scala-lang.modules.scala-xml_2.11-1.0.6.jar;%APP_LIB_DIR%\com.typesafe.play.play-server_2.11-2.6.2.jar;%APP_LIB_DIR%\com.typesafe.play.play_2.11-2.6.2.jar;%APP_LIB_DIR%\com.typesafe.play.build-link-2.6.2.jar;%APP_LIB_DIR%\com.typesafe.play.play-exceptions-2.6.2.jar;%APP_LIB_DIR%\com.typesafe.play.play-netty-utils-2.6.2.jar;%APP_LIB_DIR%\org.slf4j.slf4j-api-1.7.25.jar;%APP_LIB_DIR%\org.slf4j.jul-to-slf4j-1.7.25.jar;%APP_LIB_DIR%\org.slf4j.jcl-over-slf4j-1.7.25.jar;%APP_LIB_DIR%\com.typesafe.play.play-streams_2.11-2.6.2.jar;%APP_LIB_DIR%\org.reactivestreams.reactive-streams-1.0.0.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-stream_2.11-2.5.3.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-actor_2.11-2.5.3.jar;%APP_LIB_DIR%\com.typesafe.config-1.3.1.jar;%APP_LIB_DIR%\org.scala-lang.modules.scala-java8-compat_2.11-0.8.0.jar;%APP_LIB_DIR%\com.typesafe.ssl-config-core_2.11-0.2.1.jar;%APP_LIB_DIR%\org.scala-lang.modules.scala-parser-combinators_2.11-1.0.6.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-slf4j_2.11-2.5.3.jar;%APP_LIB_DIR%\com.fasterxml.jackson.core.jackson-core-2.8.9.jar;%APP_LIB_DIR%\com.fasterxml.jackson.core.jackson-annotations-2.8.9.jar;%APP_LIB_DIR%\com.fasterxml.jackson.core.jackson-databind-2.8.9.jar;%APP_LIB_DIR%\com.fasterxml.jackson.datatype.jackson-datatype-jdk8-2.8.9.jar;%APP_LIB_DIR%\com.fasterxml.jackson.datatype.jackson-datatype-jsr310-2.8.9.jar;%APP_LIB_DIR%\commons-codec.commons-codec-1.10.jar;%APP_LIB_DIR%\com.typesafe.play.play-json_2.11-2.6.2.jar;%APP_LIB_DIR%\com.typesafe.play.play-functional_2.11-2.6.2.jar;%APP_LIB_DIR%\org.scala-lang.scala-reflect-2.11.8.jar;%APP_LIB_DIR%\org.typelevel.macro-compat_2.11-1.1.1.jar;%APP_LIB_DIR%\joda-time.joda-time-2.9.9.jar;%APP_LIB_DIR%\com.google.guava.guava-22.0.jar;%APP_LIB_DIR%\com.google.code.findbugs.jsr305-1.3.9.jar;%APP_LIB_DIR%\com.google.errorprone.error_prone_annotations-2.0.18.jar;%APP_LIB_DIR%\com.google.j2objc.j2objc-annotations-1.1.jar;%APP_LIB_DIR%\org.codehaus.mojo.animal-sniffer-annotations-1.14.jar;%APP_LIB_DIR%\io.jsonwebtoken.jjwt-0.7.0.jar;%APP_LIB_DIR%\org.apache.commons.commons-lang3-3.6.jar;%APP_LIB_DIR%\javax.transaction.jta-1.1.jar;%APP_LIB_DIR%\javax.inject.javax.inject-1.jar;%APP_LIB_DIR%\com.typesafe.play.filters-helpers_2.11-2.6.2.jar;%APP_LIB_DIR%\com.typesafe.play.play-logback_2.11-2.6.2.jar;%APP_LIB_DIR%\ch.qos.logback.logback-classic-1.2.3.jar;%APP_LIB_DIR%\ch.qos.logback.logback-core-1.2.3.jar;%APP_LIB_DIR%\com.typesafe.play.play-akka-http-server_2.11-2.6.2.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-http-core_2.11-10.0.9.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-parsing_2.11-10.0.9.jar;%APP_LIB_DIR%\com.typesafe.play.play-guice_2.11-2.6.2.jar;%APP_LIB_DIR%\com.google.inject.guice-4.1.0.jar;%APP_LIB_DIR%\aopalliance.aopalliance-1.0.jar;%APP_LIB_DIR%\com.google.inject.extensions.guice-assistedinject-4.1.0.jar;%APP_LIB_DIR%\com.ftel.bigdata-core_2.11-0.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\log4j.log4j-1.2.17.jar;%APP_LIB_DIR%\org.elasticsearch.elasticsearch-spark-20_2.11-5.0.0-alpha5.jar;%APP_LIB_DIR%\org.apache.spark.spark-sql_2.11-2.0.0.jar;%APP_LIB_DIR%\com.univocity.univocity-parsers-2.1.1.jar;%APP_LIB_DIR%\org.apache.spark.spark-sketch_2.11-2.0.0.jar;%APP_LIB_DIR%\org.apache.spark.spark-tags_2.11-2.0.0.jar;%APP_LIB_DIR%\org.scalatest.scalatest_2.11-2.2.6.jar;%APP_LIB_DIR%\org.spark-project.spark.unused-1.0.0.jar;%APP_LIB_DIR%\org.apache.spark.spark-core_2.11-2.0.0.jar;%APP_LIB_DIR%\org.apache.avro.avro-mapred-1.7.7-hadoop2.jar;%APP_LIB_DIR%\org.apache.avro.avro-ipc-1.7.7-tests.jar;%APP_LIB_DIR%\org.apache.avro.avro-ipc-1.7.7.jar;%APP_LIB_DIR%\org.apache.avro.avro-1.7.7.jar;%APP_LIB_DIR%\org.codehaus.jackson.jackson-core-asl-1.9.13.jar;%APP_LIB_DIR%\org.codehaus.jackson.jackson-mapper-asl-1.9.13.jar;%APP_LIB_DIR%\org.xerial.snappy.snappy-java-1.1.2.4.jar;%APP_LIB_DIR%\org.apache.commons.commons-compress-1.4.1.jar;%APP_LIB_DIR%\org.tukaani.xz-1.0.jar;%APP_LIB_DIR%\com.twitter.chill_2.11-0.8.0.jar;%APP_LIB_DIR%\com.twitter.chill-java-0.8.0.jar;%APP_LIB_DIR%\com.esotericsoftware.kryo-shaded-3.0.3.jar;%APP_LIB_DIR%\com.esotericsoftware.minlog-1.3.0.jar;%APP_LIB_DIR%\org.objenesis.objenesis-2.1.jar;%APP_LIB_DIR%\org.apache.xbean.xbean-asm5-shaded-4.4.jar;%APP_LIB_DIR%\org.apache.hadoop.hadoop-client-2.2.0.jar;%APP_LIB_DIR%\org.apache.hadoop.hadoop-common-2.2.0.jar;%APP_LIB_DIR%\org.apache.hadoop.hadoop-annotations-2.2.0.jar;%APP_LIB_DIR%\commons-cli.commons-cli-1.2.jar;%APP_LIB_DIR%\org.apache.commons.commons-math-2.1.jar;%APP_LIB_DIR%\xmlenc.xmlenc-0.52.jar;%APP_LIB_DIR%\commons-httpclient.commons-httpclient-3.1.jar;%APP_LIB_DIR%\commons-io.commons-io-2.1.jar;%APP_LIB_DIR%\commons-net.commons-net-3.6.jar;%APP_LIB_DIR%\commons-lang.commons-lang-2.5.jar;%APP_LIB_DIR%\commons-configuration.commons-configuration-1.6.jar;%APP_LIB_DIR%\commons-collections.commons-collections-3.2.1.jar;%APP_LIB_DIR%\commons-digester.commons-digester-1.8.jar;%APP_LIB_DIR%\commons-beanutils.commons-beanutils-1.7.0.jar;%APP_LIB_DIR%\commons-beanutils.commons-beanutils-core-1.8.0.jar;%APP_LIB_DIR%\com.google.protobuf.protobuf-java-2.5.0.jar;%APP_LIB_DIR%\org.apache.hadoop.hadoop-auth-2.2.0.jar;%APP_LIB_DIR%\org.apache.hadoop.hadoop-hdfs-2.2.0.jar;%APP_LIB_DIR%\org.mortbay.jetty.jetty-util-6.1.26.jar;%APP_LIB_DIR%\org.apache.hadoop.hadoop-mapreduce-client-app-2.2.0.jar;%APP_LIB_DIR%\org.apache.hadoop.hadoop-mapreduce-client-common-2.2.0.jar;%APP_LIB_DIR%\org.apache.hadoop.hadoop-yarn-common-2.2.0.jar;%APP_LIB_DIR%\org.apache.hadoop.hadoop-yarn-api-2.2.0.jar;%APP_LIB_DIR%\org.slf4j.slf4j-log4j12-1.7.16.jar;%APP_LIB_DIR%\org.apache.hadoop.hadoop-yarn-client-2.2.0.jar;%APP_LIB_DIR%\org.apache.hadoop.hadoop-mapreduce-client-core-2.2.0.jar;%APP_LIB_DIR%\org.apache.hadoop.hadoop-yarn-server-common-2.2.0.jar;%APP_LIB_DIR%\org.apache.hadoop.hadoop-mapreduce-client-shuffle-2.2.0.jar;%APP_LIB_DIR%\org.apache.hadoop.hadoop-mapreduce-client-jobclient-2.2.0.jar;%APP_LIB_DIR%\org.apache.spark.spark-launcher_2.11-2.0.0.jar;%APP_LIB_DIR%\org.apache.spark.spark-network-common_2.11-2.0.0.jar;%APP_LIB_DIR%\org.apache.spark.spark-network-shuffle_2.11-2.0.0.jar;%APP_LIB_DIR%\org.fusesource.leveldbjni.leveldbjni-all-1.8.jar;%APP_LIB_DIR%\org.apache.spark.spark-unsafe_2.11-2.0.0.jar;%APP_LIB_DIR%\net.java.dev.jets3t.jets3t-0.7.1.jar;%APP_LIB_DIR%\org.apache.curator.curator-recipes-2.4.0.jar;%APP_LIB_DIR%\org.apache.curator.curator-framework-2.4.0.jar;%APP_LIB_DIR%\org.apache.curator.curator-client-2.4.0.jar;%APP_LIB_DIR%\org.apache.zookeeper.zookeeper-3.4.5.jar;%APP_LIB_DIR%\javax.servlet.javax.servlet-api-3.1.0.jar;%APP_LIB_DIR%\org.apache.commons.commons-math3-3.4.1.jar;%APP_LIB_DIR%\com.ning.compress-lzf-1.0.3.jar;%APP_LIB_DIR%\net.jpountz.lz4.lz4-1.3.0.jar;%APP_LIB_DIR%\org.roaringbitmap.RoaringBitmap-0.5.11.jar;%APP_LIB_DIR%\org.json4s.json4s-jackson_2.11-3.2.11.jar;%APP_LIB_DIR%\org.json4s.json4s-core_2.11-3.2.11.jar;%APP_LIB_DIR%\org.json4s.json4s-ast_2.11-3.2.11.jar;%APP_LIB_DIR%\org.scala-lang.scalap-2.11.8.jar;%APP_LIB_DIR%\org.scala-lang.scala-compiler-2.11.8.jar;%APP_LIB_DIR%\org.glassfish.jersey.core.jersey-client-2.22.2.jar;%APP_LIB_DIR%\javax.ws.rs.javax.ws.rs-api-2.0.1.jar;%APP_LIB_DIR%\org.glassfish.jersey.core.jersey-common-2.22.2.jar;%APP_LIB_DIR%\javax.annotation.javax.annotation-api-1.2.jar;%APP_LIB_DIR%\org.glassfish.jersey.bundles.repackaged.jersey-guava-2.22.2.jar;%APP_LIB_DIR%\org.glassfish.hk2.hk2-api-2.4.0-b34.jar;%APP_LIB_DIR%\org.glassfish.hk2.hk2-utils-2.4.0-b34.jar;%APP_LIB_DIR%\org.glassfish.hk2.external.aopalliance-repackaged-2.4.0-b34.jar;%APP_LIB_DIR%\org.glassfish.hk2.external.javax.inject-2.4.0-b34.jar;%APP_LIB_DIR%\org.glassfish.hk2.hk2-locator-2.4.0-b34.jar;%APP_LIB_DIR%\org.javassist.javassist-3.18.1-GA.jar;%APP_LIB_DIR%\org.glassfish.hk2.osgi-resource-locator-1.0.1.jar;%APP_LIB_DIR%\org.glassfish.jersey.core.jersey-server-2.22.2.jar;%APP_LIB_DIR%\org.glassfish.jersey.media.jersey-media-jaxb-2.22.2.jar;%APP_LIB_DIR%\javax.validation.validation-api-1.1.0.Final.jar;%APP_LIB_DIR%\org.glassfish.jersey.containers.jersey-container-servlet-2.22.2.jar;%APP_LIB_DIR%\org.glassfish.jersey.containers.jersey-container-servlet-core-2.22.2.jar;%APP_LIB_DIR%\org.apache.mesos.mesos-0.21.1-shaded-protobuf.jar;%APP_LIB_DIR%\com.clearspring.analytics.stream-2.7.0.jar;%APP_LIB_DIR%\io.dropwizard.metrics.metrics-core-3.1.2.jar;%APP_LIB_DIR%\io.dropwizard.metrics.metrics-jvm-3.1.2.jar;%APP_LIB_DIR%\io.dropwizard.metrics.metrics-json-3.1.2.jar;%APP_LIB_DIR%\io.dropwizard.metrics.metrics-graphite-3.1.2.jar;%APP_LIB_DIR%\org.apache.ivy.ivy-2.4.0.jar;%APP_LIB_DIR%\oro.oro-2.0.8.jar;%APP_LIB_DIR%\net.razorvine.pyrolite-4.9.jar;%APP_LIB_DIR%\net.sf.py4j.py4j-0.10.1.jar;%APP_LIB_DIR%\org.apache.spark.spark-catalyst_2.11-2.0.0.jar;%APP_LIB_DIR%\org.codehaus.janino.janino-2.7.8.jar;%APP_LIB_DIR%\org.codehaus.janino.commons-compiler-2.7.8.jar;%APP_LIB_DIR%\org.antlr.antlr4-runtime-4.5.3.jar;%APP_LIB_DIR%\org.apache.parquet.parquet-column-1.7.0.jar;%APP_LIB_DIR%\org.apache.parquet.parquet-common-1.7.0.jar;%APP_LIB_DIR%\org.apache.parquet.parquet-encoding-1.7.0.jar;%APP_LIB_DIR%\org.apache.parquet.parquet-generator-1.7.0.jar;%APP_LIB_DIR%\org.apache.parquet.parquet-hadoop-1.7.0.jar;%APP_LIB_DIR%\org.apache.parquet.parquet-format-2.3.0-incubating.jar;%APP_LIB_DIR%\org.apache.parquet.parquet-jackson-1.7.0.jar;%APP_LIB_DIR%\org.scalaj.scalaj-http_2.11-2.3.0.jar;%APP_LIB_DIR%\org.postgresql.postgresql-42.0.0.jar;%APP_LIB_DIR%\com.typesafe.slick.slick_2.11-3.2.0.jar;%APP_LIB_DIR%\com.typesafe.slick.slick-hikaricp_2.11-3.2.0.jar;%APP_LIB_DIR%\com.zaxxer.HikariCP-2.5.1.jar;%APP_LIB_DIR%\com.sksamuel.elastic4s.elastic4s-core_2.11-5.4.0.jar;%APP_LIB_DIR%\com.sksamuel.exts.exts_2.11-1.40.0.jar;%APP_LIB_DIR%\org.typelevel.cats_2.11-0.9.0.jar;%APP_LIB_DIR%\org.typelevel.cats-macros_2.11-0.9.0.jar;%APP_LIB_DIR%\com.github.mpilquist.simulacrum_2.11-0.10.0.jar;%APP_LIB_DIR%\org.typelevel.machinist_2.11-0.6.1.jar;%APP_LIB_DIR%\org.typelevel.cats-kernel_2.11-0.9.0.jar;%APP_LIB_DIR%\org.typelevel.cats-kernel-laws_2.11-0.9.0.jar;%APP_LIB_DIR%\org.scalacheck.scalacheck_2.11-1.13.4.jar;%APP_LIB_DIR%\org.scala-sbt.test-interface-1.0.jar;%APP_LIB_DIR%\org.typelevel.discipline_2.11-0.7.2.jar;%APP_LIB_DIR%\org.typelevel.catalysts-platform_2.11-0.0.5.jar;%APP_LIB_DIR%\org.typelevel.catalysts-macros_2.11-0.0.5.jar;%APP_LIB_DIR%\org.typelevel.cats-core_2.11-0.9.0.jar;%APP_LIB_DIR%\org.typelevel.cats-laws_2.11-0.9.0.jar;%APP_LIB_DIR%\org.typelevel.cats-free_2.11-0.9.0.jar;%APP_LIB_DIR%\org.typelevel.cats-jvm_2.11-0.9.0.jar;%APP_LIB_DIR%\org.elasticsearch.elasticsearch-5.4.0.jar;%APP_LIB_DIR%\org.apache.lucene.lucene-core-6.5.0.jar;%APP_LIB_DIR%\org.apache.lucene.lucene-analyzers-common-6.5.0.jar;%APP_LIB_DIR%\org.apache.lucene.lucene-backward-codecs-6.5.0.jar;%APP_LIB_DIR%\org.apache.lucene.lucene-grouping-6.5.0.jar;%APP_LIB_DIR%\org.apache.lucene.lucene-highlighter-6.5.0.jar;%APP_LIB_DIR%\org.apache.lucene.lucene-join-6.5.0.jar;%APP_LIB_DIR%\org.apache.lucene.lucene-memory-6.5.0.jar;%APP_LIB_DIR%\org.apache.lucene.lucene-misc-6.5.0.jar;%APP_LIB_DIR%\org.apache.lucene.lucene-queries-6.5.0.jar;%APP_LIB_DIR%\org.apache.lucene.lucene-queryparser-6.5.0.jar;%APP_LIB_DIR%\org.apache.lucene.lucene-sandbox-6.5.0.jar;%APP_LIB_DIR%\org.apache.lucene.lucene-spatial-6.5.0.jar;%APP_LIB_DIR%\org.apache.lucene.lucene-spatial-extras-6.5.0.jar;%APP_LIB_DIR%\org.apache.lucene.lucene-spatial3d-6.5.0.jar;%APP_LIB_DIR%\org.apache.lucene.lucene-suggest-6.5.0.jar;%APP_LIB_DIR%\org.elasticsearch.securesm-1.1.jar;%APP_LIB_DIR%\net.sf.jopt-simple.jopt-simple-5.0.2.jar;%APP_LIB_DIR%\com.carrotsearch.hppc-0.7.1.jar;%APP_LIB_DIR%\org.yaml.snakeyaml-1.15.jar;%APP_LIB_DIR%\com.fasterxml.jackson.dataformat.jackson-dataformat-smile-2.8.6.jar;%APP_LIB_DIR%\com.fasterxml.jackson.dataformat.jackson-dataformat-yaml-2.8.6.jar;%APP_LIB_DIR%\com.fasterxml.jackson.dataformat.jackson-dataformat-cbor-2.8.6.jar;%APP_LIB_DIR%\com.tdunning.t-digest-3.0.jar;%APP_LIB_DIR%\org.hdrhistogram.HdrHistogram-2.1.9.jar;%APP_LIB_DIR%\org.elasticsearch.jna-4.4.0.jar;%APP_LIB_DIR%\org.locationtech.spatial4j.spatial4j-0.6.jar;%APP_LIB_DIR%\com.vividsolutions.jts-1.13.jar;%APP_LIB_DIR%\com.sksamuel.elastic4s.elastic4s-tcp_2.11-5.4.0.jar;%APP_LIB_DIR%\io.netty.netty-all-4.1.7.Final.jar;%APP_LIB_DIR%\org.elasticsearch.client.transport-5.4.0.jar;%APP_LIB_DIR%\org.elasticsearch.plugin.transport-netty3-client-5.4.0.jar;%APP_LIB_DIR%\io.netty.netty-3.10.6.Final.jar;%APP_LIB_DIR%\org.elasticsearch.plugin.transport-netty4-client-5.4.0.jar;%APP_LIB_DIR%\io.netty.netty-buffer-4.1.9.Final.jar;%APP_LIB_DIR%\io.netty.netty-codec-4.1.9.Final.jar;%APP_LIB_DIR%\io.netty.netty-codec-http-4.1.9.Final.jar;%APP_LIB_DIR%\io.netty.netty-common-4.1.9.Final.jar;%APP_LIB_DIR%\io.netty.netty-handler-4.1.9.Final.jar;%APP_LIB_DIR%\io.netty.netty-resolver-4.1.9.Final.jar;%APP_LIB_DIR%\io.netty.netty-transport-4.1.9.Final.jar;%APP_LIB_DIR%\org.elasticsearch.plugin.reindex-client-5.4.0.jar;%APP_LIB_DIR%\org.elasticsearch.client.rest-5.4.0.jar;%APP_LIB_DIR%\org.apache.httpcomponents.httpclient-4.5.2.jar;%APP_LIB_DIR%\org.apache.httpcomponents.httpcore-4.4.5.jar;%APP_LIB_DIR%\org.apache.httpcomponents.httpasyncclient-4.1.2.jar;%APP_LIB_DIR%\org.apache.httpcomponents.httpcore-nio-4.4.5.jar;%APP_LIB_DIR%\commons-logging.commons-logging-1.1.3.jar;%APP_LIB_DIR%\org.elasticsearch.plugin.lang-mustache-client-5.4.0.jar;%APP_LIB_DIR%\com.github.spullara.mustache.java.compiler-0.9.3.jar;%APP_LIB_DIR%\org.elasticsearch.plugin.percolator-client-5.4.0.jar;%APP_LIB_DIR%\org.apache.logging.log4j.log4j-api-2.6.2.jar;%APP_LIB_DIR%\org.apache.logging.log4j.log4j-core-2.6.2.jar;%APP_LIB_DIR%\org.apache.logging.log4j.log4j-1.2-api-2.6.2.jar;%APP_LIB_DIR%\com.sksamuel.elastic4s.elastic4s-http_2.11-5.4.0.jar;%APP_LIB_DIR%\com.fasterxml.jackson.module.jackson-module-scala_2.11-2.8.8.jar;%APP_LIB_DIR%\com.fasterxml.jackson.module.jackson-module-paranamer-2.8.8.jar;%APP_LIB_DIR%\com.thoughtworks.paranamer.paranamer-2.8.jar;%APP_LIB_DIR%\com.optimaize.languagedetector.language-detector-0.6.jar;%APP_LIB_DIR%\net.arnx.jsonic-1.2.11.jar;%APP_LIB_DIR%\com.intellij.annotations-12.0.jar;%APP_LIB_DIR%\com.google.code.gson.gson-2.8.1.jar;%APP_LIB_DIR%\com.typesafe.play.play-slick_2.11-3.0.0.jar;%APP_LIB_DIR%\com.typesafe.play.play-jdbc-api_2.11-2.6.0.jar;%APP_LIB_DIR%\com.typesafe.play.play-slick-evolutions_2.11-3.0.0.jar;%APP_LIB_DIR%\com.typesafe.play.play-jdbc-evolutions_2.11-2.6.0.jar;%APP_LIB_DIR%\org.jsoup.jsoup-1.8.3.jar;%APP_LIB_DIR%\net.debasishg.redisclient_2.11-3.4.jar;%APP_LIB_DIR%\commons-pool.commons-pool-1.6.jar;%APP_LIB_DIR%\com.h2database.h2-1.4.194.jar;%APP_LIB_DIR%\bigdata-play.bigdata-play-1.0-SNAPSHOT-assets.jar"
set "APP_MAIN_CLASS=play.core.server.ProdServerStart"

if defined CUSTOM_MAIN_CLASS (
    set MAIN_CLASS=!CUSTOM_MAIN_CLASS!
) else (
    set MAIN_CLASS=!APP_MAIN_CLASS!
)

rem Call the application and pass all arguments unchanged.
"%_JAVACMD%" !_JAVA_OPTS! !BIGDATA_PLAY_OPTS! -cp "%APP_CLASSPATH%" %MAIN_CLASS% !_APP_ARGS!

@endlocal


:end

exit /B %ERRORLEVEL%
