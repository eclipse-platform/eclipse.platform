/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare;

/**
 * A <code>ICompareNavigator</code> is used to navigate through the individual
 * differences of a <code>CompareEditorInput</code> or another type of Compare container.
 * <p>
 * You can retrieve an object implementing the <code>ICompareNavigator</code> from a
 * <code>CompareEditorInput</code> by calling <code>getAdapter(ICompareNavigator)</code>
 * on the <code>CompareEditorInput</code>.
 * </p>
 * <p>
 * Although it is legal for clients to implement this interface, it is better
 * to subclass {@link CompareNavigator}.
 * 
 * @since 3.0
 */
public interface ICompareNavigator {
	
	/**
	 * Starting from the current selection <code>selectChange</code> selects and reveals the next (previous) change.
	 * If the end (or beginning) is reached, the method returns <code>true</code>.
	 * 
	 * @param next if <code>true</code> the next change is selected, otherwise the previous change
	 * @return returns <code>true</code> if end (beginning) is reached, <code>false</code> otherwise
	 */
	public boolean selectChange(boolean next);

}
