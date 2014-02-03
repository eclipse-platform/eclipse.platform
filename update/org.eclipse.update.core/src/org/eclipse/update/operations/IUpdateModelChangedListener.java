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
package org.eclipse.update.operations;

/**
 * Listener for update model changes. 
 * Usually, when features are installed, configured, etc.
 * a GUI may need to update its state, so it will have to register
 * with the OperationsManager for update events.
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
public interface IUpdateModelChangedListener {
	/**
	 * Called after a feature/site/etc. is added
	 * @param parent parent object
	 * @param children added children
	 */
	public void objectsAdded(Object parent, Object [] children);
	/**
	 * Called after a feature/site/etc. is removed.
	 * @param parent parent object
	 * @param children removed children
	 */
	public void objectsRemoved(Object parent, Object [] children);
	/**
	 * Called when there are changes to a site/feature/etc.
	 * @param object object that changed
	 * @param property object property that changed
	 */
	public void objectChanged(Object object, String property);
}
