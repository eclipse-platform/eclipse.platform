package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.core.Policy;
import org.eclipse.update.internal.core.UpdateManagerUtils;

/**
 * Feature Identifier. This is a utility class combining an versioned identifier and a name
 * <p>
 * Clients may instantiate; not intended to be subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.VersionedIdentifier
 * @since 2.0
 */
public class FeatureIdentifier {
	private VersionedIdentifier id;
	private String name;

	/**
	 * Construct a feature identifier from a versioned identifier and a string 
	 * The string is the representation of the name
	 * 
	 * @param id versioned identifier
	 * @param name string representation of the feature
	 * @since 2.0.1
	 */
	public FeatureIdentifier(VersionedIdentifier id, String name) {
		this.id = id;
		this.name= name;
	}

	/**
	 * Returns the identifier
	 * 
	 * @return identifier
	 * @since 2.0.1
	 */
	public VersionedIdentifier getVersionedIdentifier() {
		return id;
	}

	/**
	 * Returns a string representation of the feature identifier.
	 * The resulting string is the name if it exists or 
	 * the textual representation of the VersionedIdentifier
	 * 
	 * @return string representation of feature identifier.
	 * @since 2.0.1
	 */
	public String getName() {
		if (name!=null) return name;
		return id.toString();
	}

	/**
	 * Compares two feature identifiers for equality
	 * 
	 * @param obj other versioned identifier to compare to
	 * @return <code>true</code> if the two objects are equal, 
	 * <code>false</code> otherwise
	 * @since 2.0.1
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof FeatureIdentifier))
			return false;
		FeatureIdentifier vid = (FeatureIdentifier) obj;
		return id.equals(vid.getVersionedIdentifier());
	}

	/**
	 * Returns a computed hashcode for the versioned identifier.
	 * 
	 * @return hash code
	 * @since 2.0.1
	 */
	public int hashCode() {
		return id.toString().hashCode();
	}
}