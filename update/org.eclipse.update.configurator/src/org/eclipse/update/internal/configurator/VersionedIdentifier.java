/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import org.osgi.framework.Version;

public class VersionedIdentifier {
	private final String identifier;
	private final Version version;

	public VersionedIdentifier(String id, String version) {
		this.identifier = id;
		this.version = Version.parseVersion(version);
	}

	public Version getVersion() {
		return version;
	}

	public String getIdentifier() {
		return identifier;
	}

	private boolean equalIdentifiers(VersionedIdentifier id) {
		if (id == null)
			return identifier == null;
		return id.identifier.equals(identifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof VersionedIdentifier))
			return false;

		VersionedIdentifier other = (VersionedIdentifier) obj;
		if (!equalIdentifiers(other))
			return false;
		return version.equals(other.getVersion());
	}

	@Override
	public int hashCode() {
		return (identifier + "_" + getVersion()).hashCode(); //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return identifier + "_" + getVersion(); //$NON-NLS-1$
	}
}