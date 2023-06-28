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
package org.eclipse.debug.ui.launchview.internal.view;

import org.eclipse.debug.ui.launchview.internal.LaunchViewBundleInfo;
import org.eclipse.debug.ui.launchview.internal.model.LaunchObjectModel;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class LaunchViewLabelProvider extends BaseLabelProvider implements IStyledLabelProvider {

	private static final ImageDescriptor ICON_RUNNING = AbstractUIPlugin.imageDescriptorFromPlugin(LaunchViewBundleInfo.PLUGIN_ID, "icons/run_co.png"); //$NON-NLS-1$

	private final ImageRegistry perConfig = new ImageRegistry();

	@Override
	public StyledString getStyledText(Object element) {
		if (element instanceof LaunchObjectModel) {
			return ((LaunchObjectModel) element).getLabel();
		}

		return null;
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof LaunchObjectModel) {
			LaunchObjectModel obj = (LaunchObjectModel) element;
			if (obj.getObject() != null && obj.getObject().canTerminate()) {
				return getCachedRunningImage(obj);
			}

			return obj.getImage();
		}

		return null;
	}

	private Image getCachedRunningImage(LaunchObjectModel obj) {
		Image img = perConfig.get(obj.getObject().getId());
		if (img == null) {
			img = new DecorationOverlayIcon(obj.getImage(), ICON_RUNNING, IDecoration.TOP_LEFT).createImage();
			perConfig.put(obj.getObject().getId(), img);
		}
		return img;
	}

	@Override
	public void dispose() {
		perConfig.dispose();
		super.dispose();
	}

}
