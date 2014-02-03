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

import java.net.*;

import org.eclipse.core.runtime.*;

/**
 * URL entry is an annotated URL object. It allows descriptive text to be
 * associated with a URL. When used as description object, the annotation
 * typically corresponds to short descriptive text, with the URL reference
 * pointing to full browsable description.
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
 * @see org.eclipse.update.core.URLEntry
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IURLEntry extends IAdaptable {

	public static final int UPDATE_SITE = 0;
	public static final int WEB_SITE = 1;	

	/** 
	 * Returns the URL annotation or <code>null</code> if none
	 * 
	 * @return url annotation or <code>null</code> if none
	 * @since 2.0 
	 */
	public String getAnnotation();

	/**
	 * Returns the actual URL.
	 * 
	 * @return url.
	 * @since 2.0 
	 */
	public URL getURL();
	
	/**
	 * Returns the type of the URLEntry
	 * 
	 * @see #UPDATE_SITE
	 * @see #WEB_SITE
	 * @return type
	 * @since 2.0 
	 */
	public int getType();	
}
