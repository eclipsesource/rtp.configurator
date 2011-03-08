#!/bin/sh
# Called once the tycho build has completed.
# Takes care of a few shortcomings in the current tycho build:

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
# Now change the permission:
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


