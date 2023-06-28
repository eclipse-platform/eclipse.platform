/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.stringsubstitution;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.variables.IStringVariable;
import org.eclipse.debug.ui.stringsubstitution.IArgumentSelector;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;

/**
 * Selects a resource argument for a string substitution variable
 */
public class ResourceSelector implements IArgumentSelector {

	@Override
	public String selectArgument(IStringVariable variable, Shell shell) {
		ResourceListSelectionDialog dialog = new ResourceListSelectionDialog(shell, ResourcesPlugin.getWorkspace().getRoot(), IResource.FILE | IResource.FOLDER | IResource.PROJECT);
		dialog.setTitle(StringSubstitutionMessages.ResourceSelector_0);
		if (dialog.open() == Window.OK) {
			Object[] objects = dialog.getResult();
			if (objects.length == 1) {
				return ((IResource)objects[0]).getFullPath().toString();
			}
		}
		return null;
	}

}
