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

import org.eclipse.core.runtime.preferences.PreferenceMetadata;

/**
 *
 * The definition of {@link ILaunchConfiguration} attribute convenience to:
 * <ul>
 * <li>{@link ILaunchConfiguration#getAttribute(String, String)} and similar
 * operations</li>
 * <li>{@link ILaunchConfigurationWorkingCopy#setAttribute(String, String)} and
 * similar operations</li>
 * <li>Connecting {@link ILaunchConfiguration} attributes with preferences</li>
 * <li>Representing {@link ILaunchConfiguration} attributes in UI</li>
 * </ul>
 *
 * @see LaunchAttributeRead
 * @see LaunchAttributeWrite
 *
 * @since 3.22
 */
public interface LaunchAttributeDefined<V> {

	/**
	 *
	 * @return identity for defined attribute
	 */
	LaunchAttributeIdentity identity();

	/**
	 *
	 * @return preference metadata for defined attribute
	 */
	PreferenceMetadata<V> metadata();

}
