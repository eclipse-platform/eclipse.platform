package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

/**
 * This is a utility class representing the options of a nested feature.
 * Feature will include other features. This class will represent the options of the inclusion.
 * <p>
 * Clients may instantiate; not intended to be subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.VersionedIdentifier
 * @since 2.0
 */
public class IncludedFeatureOptions {
	private boolean isOptional;
	private String name;

	/**
	 * Construct a feature options from a string and a boolean
	 * The string is the representation of the name.
	 * The boolean is the representation of the optionality of the nested feature.
	 * 
	 * @param name string representation of the feature
	 * @param isOptional <code>true</code> if the feature is optional, <code>false</code> otherwise.
	 * @since 2.0.1
	 */
	public IncludedFeatureOptions(String name, boolean isOptional) {
		this.isOptional = isOptional;
		this.name = name;
	}

	/**
	 * Returns the isOptional
	 * 
	 * @return isOptional
	 * @since 2.0.1
	 */
	public boolean isOptional() {
		return isOptional;
	}

	/**
	 * Returns a string representation of the feature identifier.
	 * 
	 * @return string representation of feature identifier or <code>null</code>.
	 * @since 2.0.1
	 */
	public String getName() {
		return name;
	}

}