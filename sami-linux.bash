#!/bin/bash

JAVA="java"
JARFLAGS="-Djava.library.path=../../lib/crw/worldwind/native64:/usr/lib/jni/ -Djava.util.logging.config.file=../../logging.properties"
CLASSPATH="../../build/classes:../../lib/swing-layout-1.0.4.jar:../../lib/commons-math3-3.0.jar:../../lib/jung/jung-algorithms-2.0.jar:../../lib/jung/jung-api-2.0.jar:../../lib/jung/jung-graph-impl-2.0.jar:../../lib/jung/jung-visualization-2.0.jar:../../lib/jung/collections-generic-4.01.jar:../../lib/perc/PercjPlanningCommon.jar:../../lib/perc/percjutils.jar:../../lib/crw/crwlib/crwlib_client.jar:../../lib/crw/crwlib/crwlib_core.jar:../../lib/crw/crwlib/robotutils.jar:../../lib/crw/worldwind/gdal.jar:../../lib/crw/worldwind/gluegen-rt.jar:../../lib/crw/worldwind/jogl.jar:../../lib/crw/worldwind/worldwind.jar:../../lib/crw/worldwind/worldwindx.jar:../../lib/crw/commons-logging-1.1.1.jar:../../../sami-crw/dist/sami-crw.jar"
LOGPATH="../logs"
MAINCLASS="sami.ui.MissionMonitor"

cd "$(dirname "$0")"
$JAVA $JARFLAGS -cp $CLASSPATH $MAINCLASS > $LOGPATH/sami.log 2>&1

exit

