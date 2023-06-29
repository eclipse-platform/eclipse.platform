/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
package org.eclipse.debug.internal.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;

/**
 * Contains constants to be used internally in all debug components
 *
 * @since 3.4
 */
public interface IInternalDebugCoreConstants {

	/**
	 * Represents the empty string
	 */
	String EMPTY_STRING = ""; //$NON-NLS-1$

	/**
	 * Boolean preference controlling whether status handler extensions
	 * are enabled. Default value is <code>true</code>. When disabled
	 * any call to {@link DebugPlugin#getStatusHandler(IStatus)} will return <code>null</code>.
	 *
	 * @since 3.4.2
	 */
	String PREF_ENABLE_STATUS_HANDLERS = DebugPlugin.getUniqueIdentifier() + ".PREF_ENABLE_STATUS_HANDLERS"; //$NON-NLS-1$

	/**
	 * Persistence of breakpoint manager enabled state.
	 *
	 * @since 3.6
	 */
	String PREF_BREAKPOINT_MANAGER_ENABLED_STATE =  DebugPlugin.getUniqueIdentifier() + ".PREF_BREAKPOINT_MANAGER_ENABLED_STATE"; //$NON-NLS-1$

}
