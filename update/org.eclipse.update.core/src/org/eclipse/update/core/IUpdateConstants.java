package org.eclipse.update.core;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
 
/**
 * Manages a list of static constants.
 * 
 * @since 2.0.2
 */
public interface IUpdateConstants {
	
		/**
	 * No matching rule specified 
	 * @since 2.0
	 */
	public static final int RULE_NONE = 0;

	/**
	 * Dependency can be satisfied only with plug-in version matching 
	 * exactly the specified version.
	 * @since 2.0
	 */
	public static final int RULE_PERFECT = 1;

	/**
	 * Dependency can be satisfied only with plug-in version that is 
	 * equivalent to the specified version (same major and minor version
	 * identifier, greater than or equal service identifier).
	 * @since 2.0
	 */
	public static final int RULE_EQUIVALENT = 2;

	/**
	 * Dependency can be satisfied only with plug-in version that is 
	 * compatible with the specified version (either is equivalent,
	 * or greater minor identifier (but same major identifier)).
	 * @since 2.0
	 */
	public static final int RULE_COMPATIBLE = 3;

	/**
	 * Dependency can be satisfied only with plug-in version that is 
	 * greater or equal to the specified version.
	 * @since 2.0
	 */
	public static final int RULE_GREATER_OR_EQUAL = 4;

	/**
	 * The search location for updates is defined by the root feature.
	 * @since 2.0.2
	 */
	public static final int SEARCH_ROOT = 1<<1;
	
	/**
	 * The search location for updates is defined by this feature.
	 * @since 2.0.2
	 */
	public static final int SEARCH_SELF = 1<<2;
	

}
