/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core;


import org.eclipse.core.runtime.IAdaptable;

/**
 * Plug-in dependency entry.
 * Describes a feature dependency on a particular plug-in. The dependency 
 * can specify a specific plug-in version and a matching rule for 
 * satisfying the dependency.
 * <p>
 * Clients may implement this interface. However, in most cases clients should 
 * directly instantiate or subclass the provided implementation of this 
 * interface.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.Import
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IImport extends IAdaptable, IUpdateConstants, IPlatformEnvironment {

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
	 * Returns the matching rule for the dependency identifier.
	 * 
	 * @return matching id rule
	 * @since 2.1 
	 */
	public int getIdRule();
	
	/**
	 * Returns the dependency kind
	 * 
	 * @see #KIND_PLUGIN
	 * @see #KIND_FEATURE
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
