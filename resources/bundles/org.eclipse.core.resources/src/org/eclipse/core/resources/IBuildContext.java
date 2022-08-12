/*******************************************************************************
 * Copyright (c) 2010, 2011 Broadcom Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Broadcom Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.resources;

/**
 * Stores information about the context in which a builder was called.
 *
 * <p>
 * This can be interrogated by a builder to determine what's been built
 * before, and what's being built after it, for this particular build
 * invocation.
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 *
 * @since 3.7
 */
public interface IBuildContext {

	/**
	 * Gets a array of build configurations that were built before this build configuration,
	 * as part of the current top-level build invocation.
	 *
	 * @return an array of all referenced build configurations that have been built
	 * in the current build; never null.
	 */
	IBuildConfiguration[] getAllReferencedBuildConfigs();

	/**
	 * Gets a array of build configurations that will be built after this build configuration,
	 * as part of the current top-level build invocation.
	 * <p>
	 * If the array is empty, this configuration is the last in the build chain.
	 * </p>
	 *
	 * @return an array of all referencing build configurations that will be built
	 * in the current build; never null.
	 */
	IBuildConfiguration[] getAllReferencingBuildConfigs();

	/**
	 * Returns the full array of configurations that were requested to be built
	 * by the API user.  These configurations may be anywhere in the build
	 * order (depending on how the build graph has been flattened).
 	 * <p>
 	 * This array won't include any build configurations being built by virtue
 	 * of being referenced from a requested build configuration.
	 * </p>
	 * May return the empty array if this is a top-level workspace build.
	 *
	 * @return an array of configurations that were requested to be built.
	 */
	IBuildConfiguration[] getRequestedConfigs();
}
