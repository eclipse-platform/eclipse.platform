/*******************************************************************************
 * Copyright (c) 2017, 2019 SSI Schaefer IT Solutions GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.debug.ui.launchview.internal.model;

import org.eclipse.debug.ui.launchview.internal.LaunchViewBundleInfo;
import org.eclipse.debug.ui.launchview.internal.LaunchViewMessages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class LaunchObjectFavoriteContainerModel extends LaunchObjectContainerModel {

	private static final Image FAV_ICON = AbstractUIPlugin.imageDescriptorFromPlugin(LaunchViewBundleInfo.PLUGIN_ID, "icons/favorite_star.png") //$NON-NLS-1$
			.createImage();

	public LaunchObjectFavoriteContainerModel() {
		super(LaunchViewMessages.LaunchObjectFavoriteContainerModel_Favorites, FAV_ICON);
	}

}
