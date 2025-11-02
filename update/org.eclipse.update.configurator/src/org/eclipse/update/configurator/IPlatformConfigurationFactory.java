/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.configurator;

import java.io.IOException;
import java.net.URL;

/**
 * Factory for creating platform configurations.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
@Deprecated(forRemoval = true, since = "2025-06 (removal in 2027-06 or later)")
public interface IPlatformConfigurationFactory {
	/**
	 * Returns the current platform configuration.
	 *
	 * @return platform configuration used in current instance of platform
	 */
	@Deprecated
	public IPlatformConfiguration getCurrentPlatformConfiguration();
	/**
	 * Returns a platform configuration object, optionally initialized with previously saved
	 * configuration information.
	 *
	 * @param url location of previously save configuration information. If <code>null</code>
	 * is specified, an empty configuration object is returned
	 * @return platform configuration used in current instance of platform
	 */
	@Deprecated
	public IPlatformConfiguration getPlatformConfiguration(URL url) throws IOException;

	/**
	 * Returns a platform configuration object, optionally initialized with previously saved
	 * configuration information.
	 *
	 * @param url location of previously save configuration information. If <code>null</code>
	 * is specified, an empty configuration object is returned
	 * @param loc location of the platform installation.  Used to resolve entries in the saved
	 * location
	 * @return platform configuration used in current instance of platform
	 */
	@Deprecated
	public IPlatformConfiguration getPlatformConfiguration(URL url, URL loc) throws IOException;
}
