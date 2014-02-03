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
package org.eclipse.update.search;

/**
 * This interface is used for update site adapter used 
 * for specific query searches. It adds a mapping ID 
 * that can be used when mapping file is specified.
 * If a matching mapping is found for this ID, 
 * the replacement URL found in the mapping file will be
 * used instead of this adapter.
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
public interface IQueryUpdateSiteAdapter extends IUpdateSiteAdapter {
/**
 * Returns an ID that can be used for matching against the information in the address mapping file.
 * @return a mapping Id to compare against the address mapping file.
 */
	public String getMappingId();

}
