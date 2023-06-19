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

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetRegistryReader;
import org.eclipse.ui.internal.provisional.cheatsheets.TaskEditor;
import org.osgi.framework.Bundle;

public class TaskEditorManager {

	private static TaskEditorManager instance;

	private TaskEditorManager() {
	}

	public static TaskEditorManager getInstance() {
		if (instance == null) {
			instance = new TaskEditorManager();
		}
		return instance;
	}

	public TaskEditor getEditor(String editorKind) {
		CheatSheetRegistryReader.TaskEditorNode editorInfo =
			CheatSheetRegistryReader.getInstance().findTaskEditor(editorKind);
		if (editorInfo != null) {
			TaskEditor editorInstance = null;
			Class<?> extClass = null;
			String className = editorInfo.getClassName();
			try {
				Bundle bundle = Platform.getBundle(editorInfo.getPluginId());
				extClass = bundle.loadClass(className);
			} catch (Exception e) {
				String message = NLS.bind(Messages.ERROR_LOADING_CLASS, (new Object[] {className}));
				CheatSheetPlugin.getPlugin().getLog().error(message, e);
			}
			try {
				if (extClass != null) {
					Constructor<?> c = extClass.getConstructor();
					editorInstance = (TaskEditor) c.newInstance();
				}
			} catch (Exception e) {
				String message = NLS.bind(Messages.ERROR_CREATING_CLASS, (new Object[] {className}));
				CheatSheetPlugin.getPlugin().getLog().error(message, e);
			}

			return editorInstance;
		}

		return null;
	}

	public ImageDescriptor getImageDescriptor(String editorKind) {
		CheatSheetRegistryReader.TaskEditorNode editorInfo =
			CheatSheetRegistryReader.getInstance().findTaskEditor(editorKind);
		if (editorInfo != null) {
			Bundle bundle = Platform.getBundle(editorInfo.getPluginId());
			URL url = FileLocator.find(bundle, IPath.fromOSString(editorInfo.getIconPath()), null);
			if (url != null) {
				try {
					url = FileLocator.resolve(url);
					return ImageDescriptor.createFromURL(url);
				} catch (IOException e) {
					return null;
				}
			}
		}
		return null;
	}

}
