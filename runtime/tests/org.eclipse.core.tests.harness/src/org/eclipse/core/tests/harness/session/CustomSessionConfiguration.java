/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.harness.session;

import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.core.tests.harness.session.customization.SessionCustomization;
import org.osgi.framework.Bundle;

/**
 * A session customization to use a custom session configuration, i.e., a custom
 * "config.ini" file with a defined set of bundles to be used.
 */
public interface CustomSessionConfiguration extends SessionCustomization {

	/**
	 * {@return the path of the used configuration directory}
	 */
	public Path getConfigurationDirectory() throws IOException;

	/**
	 * Sets the given configuration directory. If not called, a temporary folder is
	 * used as the configuration directory.
	 *
	 * @param configurationDirectory the path of the directory to place the
	 *                               configuration in, must not be {@code null}
	 *
	 * @return this
	 */
	public CustomSessionConfiguration setConfigurationDirectory(Path configurationDirectory);

	/**
	 * Adds the bundle containing the given class to the session configuration.
	 *
	 * @param classFromBundle a class from the bundle to add, must not be
	 *                        {@code null}
	 *
	 * @return this
	 */
	public CustomSessionConfiguration addBundle(Class<?> classFromBundle);

	/**
	 * Adds the bundle to the session configuration.
	 *
	 * @param bundle the bundle to add, must not be {@code null}
	 *
	 * @return this
	 */
	public CustomSessionConfiguration addBundle(Bundle bundle);

	/**
	 * Activates the "osgi.configuration.cascaded" option for this configuration.
	 *
	 * @return this
	 */
	public CustomSessionConfiguration setCascaded();

	/**
	 * Marks the configuration area as read only. This will be effective from the
	 * second session on, since the the first session requires a writable
	 * configuration area for initial setup.
	 *
	 * @return this
	 */
	public CustomSessionConfiguration setReadOnly();

	/**
	 * Sets the given config value for the application configuration via the ini. If
	 * the value is null, the key will be removed from the ini.
	 *
	 * @param key   the key to define
	 * @param value the value to set to the key or {@code null} to remove the key
	 *
	 * @return this
	 */
	public CustomSessionConfiguration setConfigIniValue(String key, String value);

	/**
	 * Sets the given system property for all subsequently executed sessions. If the
	 * value is null, the property will not be set in subsequent runs anymore.
	 *
	 * @param key   the system property key
	 * @param value the value to set for the key or {@code null} to remove the key
	 *
	 * @return this
	 */
	public CustomSessionConfiguration setSystemProperty(String key, String value);

}
