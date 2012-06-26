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
package org.eclipse.update.search;

import java.net.*;

/**
 * This interface wraps an update site URL and adds 
 * a presentation label. It is used to encapsulate sites that need
 * to be visited during the update search.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 3.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IUpdateSiteAdapter {
	/**
	 * Returns the presentation string that can be used
	 * for this site.
	 * @return the update site label
	 */
	public String getLabel();
	/**
	 * Returns the URL of the update site.
	 * @return the URL of the update site.
	 */
	public URL getURL();
}
