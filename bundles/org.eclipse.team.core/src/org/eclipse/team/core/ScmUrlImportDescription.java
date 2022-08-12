/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core;

import java.net.URI;
import java.util.HashMap;

/**
 * Describes how a bundle import will be executed. A bundle importer delegate
 * creates bundle import descriptions when it validates bundle manifests for
 * importing. The result, a set of bundle import descriptions is then passed to
 * TeamUI, which basing on the info from the descriptions instantiate and
 * initialize IScmUrlImportWizardPage pages. The pages can be used to alter the
 * default import configuration e.g. for bundles stored in a CVS repository the
 * user may want to check out HEAD rather than a specific version.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class has been added as part of a work in
 * progress. There is no guarantee that this API will work or that it will
 * remain the same. Please do not use this API without consulting with the Team
 * team.
 *
 * @since 3.6
 */
public class ScmUrlImportDescription {
	private String url;
	private String project;
	private HashMap<String, Object> properties;

	public ScmUrlImportDescription(String url, String project) {
		this.url = url;
		this.project = project;
	}

	/**
	 * @return project name
	 */
	public String getProject() {
		return project;
	}

	/**
	 * SCM URL
	 *
	 * @return a string representation of the SCM URL
	 */
	public String getUrl() {
		return url;
	}

	public URI getUri() {
		return URI.create(url.replaceAll("\"", "")); //$NON-NLS-1$//$NON-NLS-2$
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Sets or removes a client property.
	 *
	 * @param key
	 *            property key
	 * @param value
	 *            property value or <code>null</code> to remove the property
	 */
	public synchronized void setProperty(String key, Object value) {
		if (properties == null) {
			properties = new HashMap<>();
		}
		if (value == null) {
			properties.remove(key);
		} else {
			properties.put(key, value);
		}

	}

	/**
	 * Returns the specified client property, or <code>null</code> if none.
	 *
	 * @param key
	 *            property key
	 * @return property value or <code>null</code>
	 */
	public synchronized Object getProperty(String key) {
		if (properties == null) {
			return null;
		}
		return properties.get(key);
	}
}
