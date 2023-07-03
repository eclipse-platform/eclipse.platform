/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.ui.cheatsheets;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.cheatsheets.actions.CheatSheetHelpMenuAction;

/**
 * Factory for the cheat sheet's public extensions.
 * <p>
 * This allows the extensions to be made available for use by RCP applications
 * without exposing their concrete implementation classes.
 * </p>
 *
 * @since 3.1
 */

public class CheatSheetExtensionFactory implements IExecutableExtensionFactory,
		IExecutableExtension {
	/**
	 * Factory ID for the Help menu cheat sheet action.
	 */
	public static final String HELP_MENU_ACTION = "helpMenuAction"; //$NON-NLS-1$

	private IConfigurationElement config;

	private String id;

	private String propertyName;

	/**
	 * The default constructor.
	 */
	public CheatSheetExtensionFactory() {
		// do nothing
	}

	@Override
	public Object create() throws CoreException {
		if (HELP_MENU_ACTION.equals(id))
			return configure(new CheatSheetHelpMenuAction());
		throw new CoreException(new Status(IStatus.ERROR,
				"org.eclipse.ui.cheatsheets", //$NON-NLS-1$
				0, "Unknown id in data argument for " + getClass(), null)); //$NON-NLS-1$
	}

	@Override
	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		if (data instanceof String)
			id = (String) data;
		else
			throw new CoreException(new Status(IStatus.ERROR,
					PlatformUI.PLUGIN_ID, 0,
					"Data argument must be a String for " + getClass(), null)); //$NON-NLS-1$
		this.config = config;
		this.propertyName = propertyName;
	}

	private Object configure(Object obj) throws CoreException {
		if (obj instanceof IExecutableExtension) {
			((IExecutableExtension) obj).setInitializationData(config,
					propertyName, null);
		}
		return obj;
	}
}