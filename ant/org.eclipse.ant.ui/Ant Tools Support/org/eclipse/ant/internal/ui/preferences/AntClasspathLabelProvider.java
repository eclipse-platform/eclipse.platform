/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.ant.internal.ui.preferences;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Label provider for classpath elements
 */
public class AntClasspathLabelProvider implements ILabelProvider {

	private final AntClasspathBlock fBlock;

	public AntClasspathLabelProvider(AntClasspathBlock block) {
		fBlock = block;
	}

	private Image getFolderImage() {
		return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
	}

	private Image getJarImage() {
		return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_JAR);
	}

	public Image getClasspathImage() {
		return AntUIImages.getImage(IAntUIConstants.IMG_TAB_CLASSPATH);
	}

	@Override
	public Image getImage(Object element) {
		String file;
		if (element instanceof ClasspathEntry entry) {
			if (entry.isEclipseRuntimeRequired()) {
				return AntUIImages.getImage(IAntUIConstants.IMG_ANT_ECLIPSE_RUNTIME_OBJECT);
			}
			file = entry.toString();
			if (file.endsWith("/")) { //$NON-NLS-1$
				return getFolderImage();
			}
			return getJarImage();
		}

		return getClasspathImage();
	}

	@Override
	public String getText(Object element) {
		if (element instanceof IAntClasspathEntry entry) {
			StringBuilder label = new StringBuilder(entry.getLabel());
			if (element instanceof GlobalClasspathEntries) {
				if (((GlobalClasspathEntries) element).getType() == ClasspathModel.ANT_HOME) {
					AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
					String defaultAntHome = prefs.getDefaultAntHome();
					String currentAntHome = fBlock.getAntHome();
					label.append(" ("); //$NON-NLS-1$
					if (defaultAntHome == null || defaultAntHome.equals(currentAntHome)) {
						label.append(AntPreferencesMessages.AntClasspathLabelProvider_0);
					} else {
						label.append(fBlock.getAntHome());
					}
					label.append(')');
				}
			}
			return label.toString();
		}
		return element.toString();
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		// do nothing
	}

	@Override
	public void dispose() {
		// do nothing
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// do nothing
	}
}
