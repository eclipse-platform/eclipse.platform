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
public interface IImport extends IAdaptable, IUpdateConstants {

	/**
	 * The import relates to a plugin
	 * @since 2.0.2
	 */
	public static final int KIND_PLUGIN = 0;
	
	/**
	 * The import relates to a feature
	 * @since 2.0.2
	 */
	public static final int KIND_FEATURE = 1;

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
	
	/**
	 * Returns the patch mode. If the import is in patch mode,
	 * the referenced feature is considered a patch target,
	 * and the feature that owns the import is patch carrier.
	 * Patch carrier and patched feature are linked in a
	 * distinct way: if a patched feature is disabled,
	 * all the patches are disabled with it.
	 * @return true if the element represents a patch
	 * reference, false otherwise.
	 */
	public boolean isPatch();
}