/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core;

import org.eclipse.core.runtime.*;

/**
 * Plug-in entry defines a packaging reference from a feature to a plug-in.
 * It indicates that the referenced plug-in is to be considered as
 * part of the feature. Note, that this does not necessarily indicate
 * that the plug-in files are packaged together with any other
 * feature files. The actual packaging details are determined by the
 * feature content provider for the feature.
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
 * @see org.eclipse.update.core.PluginEntry
 * @see org.eclipse.update.core.FeatureContentProvider
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IPlatformEnvironment extends IAdaptable {


	/**
	 * Returns optional operating system specification.
	 * A comma-separated list of os designators defined by the platform.
	 * Indicates this entry should only be installed on one of the specified
	 * os systems. If this attribute is not specified, or is <code>*</code>, the
	 * entry can be installed on all systems (portable implementation). If the

	 * This information is used as a hint by the installation and update
	 * support.
	 *
	 * @return the operating system specification, or <code>null</code>.
	 * @since 2.0 
	 */
	public String getOS();

	/**
	 * Returns optional system architecture specification. 
	 * A comma-separated list of arch designators defined by the platform.
	 * Indicates this entry should only be installed on one of the specified
	 * systems. If this attribute is not specified, or is <code>*</code>, the
	 * entry can be installed on all systems (portable implementation).
	 * 
	 * This information is used as a hint by the installation and update
	 * support.
	 * 
	 * @return system architecture specification, or <code>null</code>.
	 * @since 2.0 
	 */
	public String getWS();

	/**
	 * Returns optional system architecture specification. 
	 * A comma-separated list of arch designators defined by the platform.
	 * Indicates this entry should only be installed on one of the specified
	 * systems. If this attribute is not specified, or is <code>*</code>, the
	 * entry can be installed on all systems (portable implementation).
	 * 
	 * This information is used as a hint by the installation and update
	 * support.
	 * 
	 * @return system architecture specification, or <code>null</code>.
	 * @since 2.0 
	 */
	public String getOSArch();

	/**
	 * Returns optional locale specification. 
	 * A comma-separated list of locale designators defined by Java.
	 * Indicates this entry should only be installed on a system running
	 * with a compatible locale (using Java locale-matching rules).
	 * If this attribute is not specified, or is <code>*</code>, the entry can
	 * be installed on all systems (language-neutral implementation).
	 * 
	 * This information is used as a hint by the installation and update
	 *  support.
	 * 
	 * @return the locale specification, or <code>null</code>.
	 * @since 2.0 
	 */
	public String getNL();


}
