#!/bin/bash
################################################################################
# Copyright (c) 2011 EclipseSource Inc. and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
# 
# Contributors:
#     hmalphettes - initial API and implementation
################################################################################
# This scripts generates a command-line to launch equinox.
# It uses the arguments defined in the *.ini file

# set path to eclipse folder. If local folder, use '.'; otherwise, use /path/to/eclipse/
eclipsehome=`dirname $0`;
cd $eclipsehome
eclipsehome=`pwd`

iniLookupFolder=$eclipsehome
# get path to equinox jar inside $eclipsehome folder
ini=$(find $eclipsehome -mindepth 1 -maxdepth 1 -name "*.ini" | sort | tail -1);
if [ ! -f "$ini" ]; then
#maybe a mac
appFolder=$(find $eclipsehome -mindepth 1 -maxdepth 1 -type d -name "*.app" | sort | tail -1);
  iniLookupFolder="$appFolder/Contents/MacOS"
  if [ -d "$iniLookupFolder" ]; then
    ini=$(find $iniLookupFolder -mindepth 1 -maxdepth 1 -type f -name "*.ini" | sort | tail -1);
  fi
fi
if [ -f "$ini" ]; then
  args=`cat $ini | tr '\n' ' ' | awk -F'-startup ' '{print $2}'`
  ini_str=`cat $ini | tr '\n' ' '`
else
  #this only works for a standalone (aka "roaming") install
  args=$(find $eclipsehome -name "org.eclipse.equinox.launcher_*.jar" | sort | tail -1);
fi
if [ ! -f "$args" ]; then
  #was returned as path relative to iniLookupFolder
  args="$iniLookupFolder/$args"
fi

if [ -z "$JAVA_OPTS" ]; then
#    JAVA_OPTS="-XX:MaxPermSize=384m -Xms96m -Xmx784m"
  #PermGen
  XXMaxPermSize=`sed -n '/--launcher\.XXMaxPermSize/{n;p;}' cloud.ini`
  if [ -n "$XXMaxPermSize" ]; then
    XXMaxPermSize=" $XXMaxPermSize"
  fi
  #vmargs
  JAVA_OPTS=`sed '1,/-vmargs/d' cloud.ini | tr '\n' ' '`$XXMaxPermSize
  if [ -z "$JAVA_OPTS" ]; then
    JAVA_OPTS="-XX:MaxPermSize=384m -Xms96m -Xmx784m"
  fi
fi
#use -install unless it was already specified in the ini file:
installArg=
if echo $* | grep -Eq ' -install'
then
    #echo "-install already defined in the ini file"
    installArg=""
else
    installArg=" -install $eclipsehome"
fi

#use -configuration unless it was already specified in the ini file:
configurationArg=
if echo $* | grep -Eq ' -configuration'
then
    #echo "-install already defined in the ini file"
    configurationArg=""
else
    tmp_config_area=`mktemp -d /tmp/cloudConfigArea.XXXXXX`
    configurationArg=" -configuration $tmp_config_area"
fi

logback=
if echo $* | grep -Eq ' -Dlogback.configurationFile='
then
    #logback conf specified on cmd line.
    logback=""
else
    logback=" -Dlogback.configurationFile=$eclipsehome/etc/logback.xml "
fi

#Read the console argument. It could be a flag.
console=`awk '{if ($1 ~ /-console/){print $1}}' < $ini | head -1`
if [ ! -z "$console" ]; then
  consoleArg=`sed -n '/-console/{n;p;}' $ini`
  first=`echo $consoleArg | cut -c1-1`
  echo "$consoleArg -> $first"
  if [ "$first" = "-" ]; then
    console=" -console $consoleArg"
  else
    console=" -console"
  fi
fi

cmd="java $JAVA_OPTS -jar $args$installArg$configurationArg$logback$console $*"
echo "Staring Equinox with $cmd"
$cmd