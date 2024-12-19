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
 * Default implementation for {@link LaunchAttributeIdentity}
 *
 * @since 3.22
 */
public record LauchAttributeIdentityRecord(String id) implements LaunchAttributeIdentity {

	/**
	 * Convenience way to compose full qualified name for launch attribute
	 *
	 * @param qualifier usually corresponds to Bundle-Symbolic-Name
	 * @param key short key to name this very attribute in the scope of
	 *            qualifier
	 */
	public LauchAttributeIdentityRecord(String qualifier, String key) {
		this(qualifier + "." + key); //$NON-NLS-1$
	}

}
