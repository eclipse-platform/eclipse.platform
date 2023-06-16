/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Mickael Istria (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.quickaccess;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.ui.quickaccess.QuickAccessElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class LaunchQuickAccessElement extends QuickAccessElement {

	private static final String MODE_RUN = "run"; //$NON-NLS-1$
	private static final String MODE_DEBUG = "debug"; //$NON-NLS-1$
	private static final String MODE_PROFILE = "profile"; //$NON-NLS-1$
	private static final ImageRegistry REGISTRY = DebugPluginImages.getImageRegistry();

	private ILaunchConfiguration launch;
	private ILaunchMode launchMode;

	static {
		registerImage(MODE_RUN);
		registerImage(MODE_DEBUG);
		registerImage(MODE_PROFILE);
	}

	public LaunchQuickAccessElement(ILaunchConfiguration launch, ILaunchMode launchMode) {
		this.launch = launch;
		this.launchMode = launchMode;
	}

	private static void registerImage(String key) {
		Bundle bundle = FrameworkUtil.getBundle(DebugPluginImages.class);
		String path = "$nl$/icons/full/ovr16/quickaccess_" + key + ".png"; //$NON-NLS-1$ //$NON-NLS-2$
		if (bundle == null) {
			REGISTRY.put(key, ImageDescriptor.getMissingImageDescriptor());
		} else {
			REGISTRY.put(key,
					ImageDescriptor.createFromURLSupplier(true,
							() -> FileLocator.find(bundle, IPath.fromOSString(path), null)));
		}
	}

	@Override
	public String getLabel() {
		return launch.getName();
	}

	@Override
	public String getMatchLabel() {
		return getLabel() + ' ' + launchMode.getLabel();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		try {
			ImageDescriptor baseImage = DebugPluginImages.getImageDescriptor(launch.getType().getIdentifier());
			ImageDescriptor overlay = REGISTRY.getDescriptor(launchMode.getIdentifier());
			if (overlay != null) {
				return new DecorationOverlayIcon(baseImage, overlay, IDecoration.BOTTOM_RIGHT);
			} else {
				return baseImage;
			}
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
			return null;
		}
	}

	@Override
	public String getId() {
		return launch.getName() + '/' + launchMode.getIdentifier();
	}

	@Override
	public void execute() {
		DebugUITools.launch(launch, launchMode.getIdentifier());
	}

}
