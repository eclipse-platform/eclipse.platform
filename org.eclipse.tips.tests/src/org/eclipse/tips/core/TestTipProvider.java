/*******************************************************************************
 * Copyright (c) 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.core;

import java.net.URL;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tips.ui.internal.util.ImageUtil;

public class TestTipProvider extends TipProvider {

	private static TipImage image;
	private final ResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());

	@Override
	public String getDescription() {
		return "Test Tip Provider";
	}

	@Override
	public String getID() {
		return getClass().getName();
	}

	@Override
	public TipImage getImage() {
		if (image == null) {
			URL url = Platform.getBundle("org.eclipse.tips.examples").getEntry("icons/48/c++.png");
			Image pluginImage = (Image) resourceManager.get(ImageDescriptor.createFromURL(url));
			String base64 = ImageUtil.decodeFromImage(pluginImage, SWT.IMAGE_PNG);
			image = new TipImage(base64);
		}
		return image;
	}

	@Override
	public IStatus loadNewTips(IProgressMonitor monitor) {
		setTips(Collections.emptyList());
		return Status.OK_STATUS;
	}

	@Override
	public void dispose() {
		resourceManager.dispose();
	}
}