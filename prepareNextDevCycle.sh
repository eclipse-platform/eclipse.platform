#!/bin/bash -xe

#*******************************************************************************
# Copyright (c) 2025, 2025 Hannes Wellmann and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Hannes Wellmann - initial API and implementation
#*******************************************************************************

# This script is called by the pipeline for preparing the next development cycle (this file's name is crucial!)
# and applies the changes required individually for Eclipse-Platform.
# The calling pipeline also defines environment variables usable in this script.

pushd 'platform'
brandingPlugins=('org.eclipse.platform' 'org.eclipse.sdk')
for plugin  in "${brandingPlugins[@]}"; do
	
	if [ ! -f "${plugin}/splash.png" ] || [ ! -f "${plugin}/eclipse_lg.png" ] || [ ! -f "${plugin}/eclipse_lg@2x.png" ]; then
		echo "Expected target Splash-screen or branding images are missing for plugin ${plugin}"
		# Probably the naming or file format of any of these files has changed and this script wasn't adapted.
		exit 1
	fi

	# Update splash-screen
	cp -f "org.eclipse.platform/futureSplashScreens/splash_${NEXT_RELEASE_NAME}.png" "${plugin}/splash.png"
	if [ -f 'org.eclipse.platform/futureSplashScreens/eclipse_lg.png' ]; then
		# About-dialog image is usally the same for multiple releases
		cp -f org.eclipse.platform/futureSplashScreens/eclipse_lg*.png ${plugin}
	fi
done
rm "org.eclipse.platform/futureSplashScreens/splash_${NEXT_RELEASE_NAME}.png"
rm org.eclipse.platform/futureSplashScreens/eclipse_lg*.png
popd

git commit --all --message "Splash Screen for ${NEXT_RELEASE_VERSION} (${NEXT_RELEASE_NAME})"


# Enforce qualifier update in 'org.eclipse.help.webapp'
sed -i '2,$ d' 'ua/org.eclipse.help.webapp/forceQualifierUpdate.txt'
echo "Qualifier update for ${NEXT_RELEASE_VERSION} stream" >> 'ua/org.eclipse.help.webapp/forceQualifierUpdate.txt'

git commit --all --message "Qualifier update of eclipse.help.webapp for ${NEXT_RELEASE_VERSION}"

