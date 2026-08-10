/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime.bundlegroups;

import java.util.ArrayList;
import java.util.List;

/**
 * The contents of a feature.xml file, as far as a bundle group needs them.
 */
class FeatureManifest {

	String id;
	String version;
	String pluginIdentifier;
	String application;
	boolean primary;
	String description;
	String licenseURL;
	final List<String> pluginIds = new ArrayList<>();
}
