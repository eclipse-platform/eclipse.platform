/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare;

import org.eclipse.core.runtime.Adapters;

/**
 * Supports cross-pane navigation through the differences of a compare container.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @see INavigatable
 * @since 3.3
 */
public abstract class CompareNavigator implements ICompareNavigator {
	@Override
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
			if (i + 1 < navigators.length && navigators[i + 1] != null
					&& navigators[i + 1].getInput() != downStreamInput) {
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
	 * Returns the {@link INavigatable} for the given object if the object
	 * adapts to {@link INavigatable}.
	 *
	 * @param object the object
	 * @return the {@link INavigatable} for the given object or {@code null}
	 */
	protected final INavigatable getNavigator(Object object) {
		if (object == null)
			return null;
		return Adapters.adapt(object, INavigatable.class);
	}

	/**
	 * Returns whether a call to {@link ICompareNavigator#selectChange(boolean)}
	 * with the same parameter would succeed.
	 *
	 * @param next if <code>true</code> the next change is selected, otherwise
	 *     the previous change
	 * @return whether a call to {@link ICompareNavigator#selectChange(boolean)}
	 *     with the same parameter would succeed.
	 * @since 3.3
	 */
	public boolean hasChange(boolean next) {
		INavigatable[] navigators= getNavigatables();
		Object downStreamInput = null;
		for (int i = navigators.length; --i >= 0;) {
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
