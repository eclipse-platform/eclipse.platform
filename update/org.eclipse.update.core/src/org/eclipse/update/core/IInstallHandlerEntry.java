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

import java.net.URL;

import org.eclipse.core.runtime.IAdaptable;

/**
 * Install handler entry.
 * Associates an optional custom install handler with the feature.
 * Install handlers must implement the IInstallHandler interface.
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
 * @see org.eclipse.update.core.InstallHandlerEntry
 * @see org.eclipse.update.core.IInstallHandler
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IInstallHandlerEntry extends IAdaptable {

	/**
	 * Returns optional URL used for browser-triggered installation handling.
	 * 
	 * @return url
	 * @since 2.0 
	 */
	public URL getURL();

	/**
	 * Returns optional name of a library containing the install
	 * handler classes. If specified, the referenced library
	 * must be contained in the feature archive.
	 * 
	 * @return install handler library name
	 * @since 2.0 
	 */
	public String getLibrary();

	/**
	 * Returns install handler name.
	 * It is interpreted depending on the value of the library
	 * specification. If library is not specified, the name
	 * is intepreted as an identifier of a "global" install
	 * handler registered in the <code>org.eclipse.update.core.installHandlers</code> 
	 * extension point. If library is specified, the name is interpreted
	 * as a fully qualified name of a class contained in the
	 * library. In both cases, the resulting class must
	 * implement IInstallHandler. The class is dynamically loaded and
	 * called at specific points during feature processing.
	 * The handler has visibility to the API classes from the update plug-in,
	 * and plug-ins required by the update plugin. 
	 * 
	 * @see IInstallHandler
	 * @return handler name
	 * @since 2.0 
	 */
	public String getHandlerName();

}
