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

pushd 'platform/org.eclipse.platform'

if [ ! -f splash.png ] || [ ! -f eclipse_lg.png ] || [ ! -f eclipse_lg@2x.png ]; then
	echo 'Expected target Splash-screen or branding images are missing'
	# Probably the naming or file format of any of these files has changed and this script wasn't adapted.
	exit 1
fi

mv -f futureSplashScreens/splash_${NEXT_RELEASE_NAME}.png splash.png
if [ -f 'futureSplashScreens/eclipse_lg.png' ]; then
	# About-dialog image is usally the same for multiple releases
	mv -f futureSplashScreens/eclipse_lg.png eclipse_lg.png
	mv -f futureSplashScreens/eclipse_lg@2x.png eclipse_lg@2x.png
fi
popd

git commit --all --message "Splash Screen for ${NEXT_RELEASE_VERSION} (${NEXT_RELEASE_NAME})"
