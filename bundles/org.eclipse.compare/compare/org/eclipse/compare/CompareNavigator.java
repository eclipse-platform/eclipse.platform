/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare;

import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.runtime.IAdaptable;

/**
 * Supports cross-pane navigation through the differences of a compare container.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @see INavigatable
 * @since 3.3
 */
public abstract class CompareNavigator implements ICompareNavigator {
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.ICompareNavigator#selectChange(boolean)
	 */
	public boolean selectChange(boolean next) {
		// find most down stream CompareViewerPane
		INavigatable[] navigators= getNavigatables();
		Object downStreamInput = null;						
		for (int i = navigators.length - 1; i >=0; i--) {
			INavigatable navigatable = navigators[i];
			if (navigatable.getInput() == downStreamInput) {
				// Skip to up stream pane if it has the same input
				continue;
			}
			if (navigatable.selectChange(next ? INavigatable.NEXT_CHANGE : INavigatable.PREVIOUS_CHANGE)) {
				// at end of this navigator
				downStreamInput = navigatable.getInput();
				continue;
			}
			// not at end
			if (i + 1 < navigators.length && navigators[i+1] != null && navigators[i+1].getInput() != downStreamInput) {
				// The navigation has invoked a change in a downstream pane.
				// Set the selected change depending on the direction we are navigating
				navigators[i+1].selectChange(next ? INavigatable.FIRST_CHANGE : INavigatable.LAST_CHANGE);
			}
			return false;
		}
		
		return true;
	}
	
	protected abstract INavigatable[] getNavigatables();
	
	/**
	 * Return the {@link INavigatable} for the given object.
	 * If the object implements {@link INavigatable}, then
	 * the object is returned. Otherwise, if the object
	 * implements {@link IAdaptable}, the object is
	 * adapted to {@link INavigatable}.
	 * @param object the object
	 * @return the {@link INavigatable} for the given object or <code>null</code>
	 */
	protected final INavigatable getNavigator(Object object) {
		if (object == null)
			return null;
		Object data= Utilities.getAdapter(object, INavigatable.class);
		if (data instanceof INavigatable)
			return (INavigatable) data;
		return null;
	}

	/**
	 * Return whether a call to {@link ICompareNavigator#selectChange(boolean)} with the same parameter
	 * would succeed.
	 * @param next if <code>true</code> the next change is selected, otherwise the previous change
	 * @return whether a call to {@link ICompareNavigator#selectChange(boolean)} with the same parameter
	 * would succeed.
	 * @since 3.3
	 */
	public boolean hasChange(boolean next) {
		INavigatable[] navigators= getNavigatables();
		Object downStreamInput = null;						
		for (int i = navigators.length - 1; i >=0; i--) {
			INavigatable navigatable = navigators[i];
			if (navigatable.getInput() == downStreamInput) {
				// Skip to up stream pane if it has the same input
				continue;
			}
			if (navigatable.hasChange(next ? INavigatable.NEXT_CHANGE : INavigatable.PREVIOUS_CHANGE)) {
				return true;
			}
			// at end of this navigator
			downStreamInput = navigatable.getInput();
		}
		return false;
	}	
}
