/*******************************************************************************
 * Copyright (c) 2012 Mentor Graphics Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mohamed Hussein (Mentor Graphics) - initial API and implementation (Bug 386673)
 *******************************************************************************/
package org.eclipse.debug.ui;

/**
 * Optional enhancements for {@link ILaunchConfigurationTab}.
 * @since 3.9
 */
public interface ILaunchConfigurationTab2 extends ILaunchConfigurationTab {

	/**
	 * Returns a warning message to be displayed to the user
	 * or <code>null</code> if none is present.
	 * @return Returns a warning message to be displayed to the user
	 * or <code>null</code> if none is present.
	 */
	String getWarningMessage();
}