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
package org.eclipse.core.tests.harness.session.customization;

import java.nio.file.Path;
import org.eclipse.core.tests.harness.session.CustomSessionConfiguration;
import org.eclipse.core.tests.session.Setup;
import org.osgi.framework.Bundle;

public class CustomSessionConfigurationDummy implements CustomSessionConfiguration {

	private Path configurationDirectory;

	public CustomSessionConfigurationDummy() {
	}

	@Override
	public CustomSessionConfiguration setCascaded() {
		return this;
	}

	@Override
	public CustomSessionConfiguration setReadOnly() {
		return this;
	}

	@Override
	public Path getConfigurationDirectory() {
		return configurationDirectory;
	}

	@Override
	public CustomSessionConfiguration setConfigurationDirectory(Path configurationDirectory) {
		this.configurationDirectory = configurationDirectory;
		return this;
	}

	@Override
	public void prepareSession(Setup setup) {
		// Do nothing
	}

	@Override
	public void cleanupSession(Setup setup) {
		// Do nothing
	}

	@Override
	public CustomSessionConfiguration addBundle(Class<?> classFromBundle) {
		return this;
	}

	@Override
	public CustomSessionConfiguration addBundle(Bundle bundle) {
		return this;
	}

	@Override
	public CustomSessionConfiguration setConfigIniValue(String key, String value) {
		return this;
	}

	@Override
	public CustomSessionConfiguration setSystemProperty(String key, String value) {
		return this;
	}

}
