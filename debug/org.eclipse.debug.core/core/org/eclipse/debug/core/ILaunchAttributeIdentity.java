/*******************************************************************************
 * Copyright (c) 2024, 2025 ArSysOp.
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
 * @since 3.23
 */
public interface ILaunchAttributeIdentity {

	/**
	 * String id of {@link ILaunchConfiguration} attribute for "low-level"
	 * operations
	 *
	 * @return id of launch attribute
	 */
	String id();

	/**
	 * creates an instance of {@link ILaunchAttributeIdentity}
	 *
	 * @param id an id of launch attribute
	 * @return created instance of {@link ILaunchAttributeIdentity}
	 */
	static ILaunchAttributeIdentity of(String id) {
		return new Record(id);
	}

	/**
	 * Default implementation for {@link ILaunchAttributeIdentity}
	 *
	 */
	record Record(String id) implements ILaunchAttributeIdentity {

	}

}
