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
# Called once the tycho build has completed.
# Takes care of a few shortcomings in the current tycho build.

#Change to the packages directory starting from the directory of this script:
cd `dirname $0`/..
PACKAGES_FOLDER=`pwd`

echo "Running post-tycho-release.sh from $PACKAGES_FOLDER"
cd $PACKAGES_FOLDER

# Look for the linux and macos products,
# Look for the *.sh files there and change their permission to executable.
# tar.gz those products.
# For example:
# org.eclipse.rtp.package.products/target/products/org.eclipse.rtp.package.basic/linux/gtk/x86/rt-basic-incubation-0.1.0.v20110308-1242-N
# is such a folder where the manips must take place.
BUILT_PRODUCTS="$PACKAGES_FOLDER/org.eclipse.rtp.package.products/target/products"


#We choose to be mention by name each one of the product
# It is tedious and a bit redundant but we want to make sure that the expected folders
# are where they are supposed to be.
# If that is not the case let's fail the build quickly.
RT_BASIC_LINUX32_PRODUCT="$BUILT_PRODUCTS/org.eclipse.rtp.package.basic/linux/gtk/x86"
[ ! -d "$RT_BASIC_LINUX32_PRODUCT" ] && { echo "ERROR: unable to locate a built product $RT_BASIC_LINUX32_PRODUCT is not a folder"; exit 42; }
# Reads the name of the top level folder.
RT_BASIC_FOLDER_NAME=`find $RT_BASIC_LINUX32_PRODUCT -maxdepth 1 -mindepth 1 -type d -exec basename {} \;`
#get the version number from the folder name. it looks like this:
# rt-basic-incubation-0.1.0.v20110308-1653-N
BUILD_VERSION=$(echo "$RT_BASIC_FOLDER_NAME" | sed 's/^rt-basic-incubation-//')

# Now change the permission: NOT useful anymore with TYCHO-566 partially fixed.
find $RT_BASIC_LINUX32_PRODUCT/$RT_BASIC_FOLDER_NAME -maxdepth 1 -name *.sh -exec chmod +x {} \;

#Remove the executable launcher specific to linux32
RT_BASIC_EXEC=$RT_BASIC_LINUX32_PRODUCT/$RT_BASIC_FOLDER_NAME/eclipse
[ -f "$RT_BASIC_EXEC" ] && rm $RT_BASIC_EXEC || echo "INFO: no native launcher to delete $RT_BASIC_EXEC"
# Now tar.gz the whole thing
cd $RT_BASIC_LINUX32_PRODUCT
tar cvzf $RT_BASIC_FOLDER_NAME.tar.gz $RT_BASIC_FOLDER_NAME/
#Also zip the product. It will look better than the zip produced by tycho that contains the native launcher.
zip -r $RT_BASIC_FOLDER_NAME.zip $RT_BASIC_FOLDER_NAME/
mv $RT_BASIC_FOLDER_NAME.zip $BUILT_PRODUCTS/../
mv $RT_BASIC_FOLDER_NAME.tar.gz $BUILT_PRODUCTS/../
cd $PACKAGES_FOLDER

#Same for web:
RT_WEB_LINUX32_PRODUCT="$BUILT_PRODUCTS/org.eclipse.rtp.package.web/linux/gtk/x86"
[ ! -d "$RT_WEB_LINUX32_PRODUCT" ] && { echo "ERROR: unable to locate a built product $RT_WEB_LINUX32_PRODUCT is not a folder"; exit 42; }
RT_WEB_FOLDER_NAME=`find $RT_WEB_LINUX32_PRODUCT -maxdepth 1 -mindepth 1 -type d -exec basename {} \;`
find $RT_WEB_LINUX32_PRODUCT/$RT_WEB_FOLDER_NAME -maxdepth 1 -name *.sh -exec chmod +x {} \;
RT_WEB_EXEC=$RT_WEB_LINUX32_PRODUCT/$RT_WEB_FOLDER_NAME/eclipse
[ -f "$RT_WEB_EXEC" ] && rm $RT_WEB_EXEC || echo "INFO: no native launcher to delete $RT_WEB_EXEC"
cd $RT_WEB_LINUX32_PRODUCT
tar cvzf $RT_WEB_FOLDER_NAME.tar.gz $RT_WEB_FOLDER_NAME/
zip -r $RT_WEB_FOLDER_NAME.zip $RT_WEB_FOLDER_NAME/
mv $RT_WEB_FOLDER_NAME.zip $BUILT_PRODUCTS/../
mv $RT_WEB_FOLDER_NAME.tar.gz $BUILT_PRODUCTS/../
cd $PACKAGES_FOLDER

#If we have many more products let's consider something more generic.

# Move one linux tar.gz and one linux zip archive of each product to
# a location on download.eclipse.org where they can be downloaded.
# Move the generated p2 repository to a location on download.eclipse.org
# where they can be consumed.
DOWNLOAD_FOLDER=/home/data/httpd/download.eclipse.org/rtp/incubation

  DOWNLOAD_PRODUCTS_FOLDER="$DOWNLOAD_FOLDER/$BUILD_VERSION"

# The p2 repository is already taken care of by the build.
# Although we should definitly take catre of mataining a symbolic link to the latest or update
# a composite repository and may delete the old builds.
# check that the build identifier is defined and well known.
BUILD_IDENTIFIER=`echo "$BUILD_VERSION" | sed 's/^.*\(.\)$/\1/'`
if [ "$BUILD_IDENTIFIER" == "N" ]; then
  DOWNLOAD_P2_FOLDER="$DOWNLOAD_FOLDER/updates/3.7-N-builds"
elif [ "$BUILD_IDENTIFIER" == "I" ]; then
  DOWNLOAD_P2_FOLDER="$DOWNLOAD_FOLDER/updates/3.7-I-builds"
elif [ "$BUILD_IDENTIFIER" == "S" ]; then
  DOWNLOAD_P2_FOLDER="$DOWNLOAD_FOLDER/updates/3.7milestones"
elif [ "$BUILD_IDENTIFIER" == "R" ]; then
  DOWNLOAD_P2_FOLDER="$DOWNLOAD_FOLDER/updates/3.7"
else
  echo "Unknown build identifier: the last character in the version $BUILD_VERSION is not 'N', 'I', 'S' or 'R'"
fi



if [ -d "$DOWNLOAD_FOLDER" ]; then

  echo "Deploying the p2 repository in $DOWNLOAD_PRODUCTS_FOLDER"
  mv $BUILT_PRODUCTS/repository $BUILT_PRODUCTS/$BUILD_VERSION
  mv $BUILT_PRODUCTS/$BUILD_VERSION $DOWNLOAD_P2_FOLDER

  echo "Deploying the archived products in $DOWNLOAD_PRODUCTS_FOLDER"
  mkdir $DOWNLOAD_PRODUCTS_FOLDER
  mv $BUILT_PRODUCTS/../$RT_BASIC_FOLDER_NAME.zip $DOWNLOAD_PRODUCTS_FOLDER
  mv $BUILT_PRODUCTS/../$RT_BASIC_FOLDER_NAME.tar.gz $DOWNLOAD_PRODUCTS_FOLDER
  mv $BUILT_PRODUCTS/../$RT_WEB_FOLDER_NAME.zip /home$DOWNLOAD_PRODUCTS_FOLDER
  mv $BUILT_PRODUCTS/../$RT_WEB_FOLDER_NAME.tar.gz $DOWNLOAD_PRODUCTS_FOLDER

else
  echo "We are not on the download machine; not deploying in $DOWNLOAD_P2_FOLDER"
fi