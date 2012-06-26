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
 
import java.net.*;

import org.eclipse.core.runtime.*;

/**
 * Site content provider.
 * A site content provider is an abstraction of each site internal 
 * organization. It allows the site content to be accessed in
 * a standard way regardless of the internal organization. All concrete site
 * implementations need to implement a site content provider.
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
 * @see org.eclipse.update.core.SiteContentProvider
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface ISiteContentProvider {
	
	/**
	 * Returns the URL of this site
	 * 
	 * @return site URL
	 * @since 2.0
	 */	
	public URL getURL();
			
	
	/**
	 * Returns a URL for the identified archive 
	 * 
	 * @param id archive identifier
	 * @return archive URL, or <code>null</code>.
	 * @exception CoreException 
	 * @since 2.0 
	 */
	public URL getArchiveReference(String id)  throws CoreException;

	/**
	 * Returns the site for this provider
	 * 
	 * @return provider site
	 * @since 2.0
	 */
	public ISite getSite();	
	
	/**
	 * Sets the site for this provider.
	 * In general, this method should only be called as part of
	 * site creation. Once set, the site should not be reset.
	 * 
	 * @param site provider site
	 * @since 2.0
	 */
	public void setSite(ISite site);
}


