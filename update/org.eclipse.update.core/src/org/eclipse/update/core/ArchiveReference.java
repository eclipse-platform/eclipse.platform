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

import org.eclipse.update.core.model.ArchiveReferenceModel;

/**
 * Convenience implementation of a site archive.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.IArchiveReference
 * @see org.eclipse.update.core.model.ArchiveReferenceModel
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class ArchiveReference
	extends ArchiveReferenceModel
	implements IArchiveReference {

	/**
	 * Constructor for ArchiveReference
	 * @since 2.0
	 */
	public ArchiveReference() {
		super();
	}

	/**
	 * @see Object#toString()
	 * @since 2.0
	 */
	public String toString() {
		String result = "IArchiveReference: "; //$NON-NLS-1$
		result =
			result
				+ ((getPath() == null)
					? getURL().toExternalForm()
					: getPath() + " : " + getURL().toExternalForm()); //$NON-NLS-1$
		return result;
	}
}
