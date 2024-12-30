/*******************************************************************************
 * Copyright (c) 2024 ArSysOp.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alexander Fedorov (ArSysOp) - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.core;

/**
 * Identifies an attribute in {@link ILaunchConfiguration}
 *
 * @since 3.22
 */
public interface LaunchAttributeIdentity {

	/**
	 * String id of {@link ILaunchConfiguration} attribute for "low-level"
	 * operations
	 *
	 * @return id of attribute
	 */
	String id();

}
