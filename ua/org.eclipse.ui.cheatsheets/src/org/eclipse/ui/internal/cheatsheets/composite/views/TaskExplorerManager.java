/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Christoph Läubrich - Bug 552773 - Simplify logging in platform code base
 *******************************************************************************/

package org.eclipse.ui.internal.cheatsheets.composite.views;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetRegistryReader;
import org.eclipse.ui.internal.provisional.cheatsheets.TaskExplorer;
import org.osgi.framework.Bundle;

public class TaskExplorerManager {
private static TaskExplorerManager instance;

	private Map<String, Image> images;

	private TaskExplorerManager() {

	}

	public static TaskExplorerManager getInstance() {
		if (instance == null) {
			instance = new TaskExplorerManager();
		}
		return instance;
	}

	public TaskExplorer getExplorer(String explorerKind) {
		CheatSheetRegistryReader.TaskExplorerNode explorerInfo =
			CheatSheetRegistryReader.getInstance().findTaskExplorer(explorerKind);
		if (explorerInfo != null) {
			TaskExplorer explorerInstance = null;
			Class<?> extClass = null;
			String className = explorerInfo.getClassName();
			try {
				Bundle bundle = Platform.getBundle(explorerInfo.getPluginId());
				extClass = bundle.loadClass(className);
			} catch (Exception e) {
				String message = NLS.bind(Messages.ERROR_LOADING_CLASS, (new Object[] {className}));
				CheatSheetPlugin.getPlugin().getLog().error(message, e);
			}
			try {
				if (extClass != null) {
					Constructor<?> c = extClass.getConstructor();
					explorerInstance = (TaskExplorer) c.newInstance();
				}
			} catch (Exception e) {
				String message = NLS.bind(Messages.ERROR_CREATING_CLASS, (new Object[] {className}));
				CheatSheetPlugin.getPlugin().getLog().error(message, e);
			}

			return explorerInstance;
		}

		return null;
	}

	private ImageDescriptor getImageDescriptor(String explorerKind) {
		CheatSheetRegistryReader.TaskExplorerNode explorerInfo =
			CheatSheetRegistryReader.getInstance().findTaskExplorer(explorerKind);
		if (explorerInfo == null) {
			return null;
		}
		String iconPath = explorerInfo.getIconPath();
		if (iconPath == null) {
			return null;
		}
		Bundle bundle = Platform.getBundle(explorerInfo.getPluginId());
		URL url = FileLocator.find(bundle, IPath.fromOSString(iconPath), null);
		try {
			url = FileLocator.resolve(url);
			return ImageDescriptor.createFromURL(url);
		} catch (IOException e) {
			return null;
		}
	}

	private Map<String, Image> getImages() {
		if (images == null) {
			initImages();
		}
		return images;
	}


	private void initImages() {
		if (images == null) {
			images = new HashMap<>();
			String[] ids = CheatSheetRegistryReader.getInstance().getExplorerIds();
			for (String id : ids) {
				ImageDescriptor descriptor = getImageDescriptor(id);
				if (descriptor != null) {
					images.put(id, descriptor.createImage());
				}
			}
		}
	}

	public String getName(String explorerKind) {
		CheatSheetRegistryReader.TaskExplorerNode explorerInfo =
			CheatSheetRegistryReader.getInstance().findTaskExplorer(explorerKind);
		if (explorerInfo != null) {
			return explorerInfo.getName();
		}
		return null;
	}

	public Image getImage(String id) {
		return getImages().get(id);
	}

}
