package org.eclipse.update.core;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.IAdaptable;

/**
 * Plug-in dependency entry.
 * Describes a feture dependency on a particular plug-in. The dependency 
 * can specify a specific plug-in version and a matching rule for 
 * satisfying the dependency.
 * <p>
 * Clients may implement this interface. However, in most cases clients should 
 * directly instantiate or subclass the provided implementation of this 
 * interface.
 * </p>
 * @see org.eclipse.update.core.Import
 * @since 2.0
 */
public interface IImport extends IAdaptable {

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
	 * The import relates to a plugin
	 * @since 2.0.2
	 */
	public static final int KIND_PLUGIN = 0;
	
	/**
	 * The import relates to a feature
	 * @since 2.0.2
	 */
	public static final int KIND_FEATURE = 0;

	/** 
	 * Returns an identifier of the dependent plug-in.
	 * 
	 * @return plug-in identifier
	 * @since 2.0 
	 */
	public VersionedIdentifier getVersionedIdentifier();

	/**
	 * Returns the matching rule for the dependency.
	 * 
	 * @return matching rule
	 * @since 2.0 
	 */
	public int getRule();
	
	/**
	 * Returns the dependency kind
	 * 
	 * @see KIND_PLUGIN
	 * @see KIND_FEATURE
	 * @return KIND_PLUGIN if the dependency relates to a plugin, 
	 * KIND_FEATURE if the dependency relates to a feature.
	 */
	public int getKind();
}