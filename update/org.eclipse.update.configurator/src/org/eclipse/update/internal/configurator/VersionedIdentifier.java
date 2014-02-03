/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import org.osgi.framework.Version;

public class VersionedIdentifier {
	private String identifier;
	private Version version;

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

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return (identifier + "_" + getVersion()).hashCode(); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return identifier + "_" + getVersion(); //$NON-NLS-1$
	}
}