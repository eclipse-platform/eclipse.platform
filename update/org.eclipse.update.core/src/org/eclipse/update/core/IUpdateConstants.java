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

 
/**
 * Manages a list of static constants.
 * 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 2.0.2
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
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
	 * Dependency can be satisfied only if the required identifier
	 * is a prefix of the specified identifier.
	 * @since 2.1
	 */
	public static final int RULE_PREFIX = 1;

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
