#!/bin/sh
#
# Start-up script for the GUI of ProGuard -- free class file shrinker,
# optimizer, obfuscator, and preverifier for Java bytecode.
#
# Note: when passing file names containing spaces to this script,
#       you'll have to add escaped quotes around them, e.g.
#       "\"/My Directory/My File.txt\""

PROGUARD_HOME=`dirname "$0"`
PROGUARD_HOME=`dirname "$PROGUARD_HOME"`

java -jar $PROGUARD_HOME/lib/proguardgui.jar "$@"
