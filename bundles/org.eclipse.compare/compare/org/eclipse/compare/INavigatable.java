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

import org.eclipse.swt.widgets.Widget;

/**
 * Interface that allow clients to navigate through the changes shown in a compare pane.
 * <p>
 * This interface may be implemented by clients.
 * </p>
 * @since 3.3
 * @see ICompareNavigator
 */
public interface INavigatable {
	
	/**
	 * Property key that can be used to associate an instance of this interface with 
	 * an SWT widget using {@link Widget#setData(String, Object)}. 
	 */
	static final String NAVIGATOR_PROPERTY= "org.eclipse.compare.internal.Navigator"; //$NON-NLS-1$
	
	/**
	 * Change flag used to navigate to the next change.
	 * @see #selectChange(int)
	 */
	static final int NEXT_CHANGE= 1;
	
	/**
	 * Change flag used to navigate to the previous change.
	 * @see #selectChange(int)
	 */
	static final int PREVIOUS_CHANGE= 2;
	
	/**
	 * Change flag used to navigate to the first change.
	 * @see #selectChange(int)
	 */
	static final int FIRST_CHANGE= 3;
	
	/**
	 * Change flag used to navigate to the last change.
	 * @see #selectChange(int)
	 */
	static final int LAST_CHANGE= 4;
	
	/**
	 * Return the input of the compare pane being navigated or <code>null</code>
	 * if the pane does not have an input. 
	 * @return the input of the compare pane being navigated or <code>null</code>
	 */
	Object getInput();
	
	/**
	 * Starting from the current selection <code>selectChange</code> selects and reveals the specified change.
	 * If the end (or beginning) is reached, the method returns <code>true</code>.
	 * 
	 * @param changeFlag the change to be selected. One of <code>NEXT_CHANGE</code>, <code>PREVIOUS_CHANGE</code>,
	 * <code>FIRST_CHANGE</code> or <code>LAST_CHANGE</code>.
	 * @return returns <code>true</code> if end (beginning) is reached, <code>false</code> otherwise
	 */
	boolean selectChange(int changeFlag);
	
	/**
	 * Return whether a call to {@link #selectChange(int)} with the same parameter
	 * would succeed.
	 * @param changeFlag the change to be selected. One of <code>NEXT_CHANGE</code> or <code>PREVIOUS_CHANGE</code>
	 * @return whether a call to {@link #selectChange(int)} with the same parameter
	 * would succeed.
	 */
	boolean hasChange(int changeFlag);
	
	/**
	 * Request that the currently selected change be opened. Return <code>true</code>
	 * if the request resulted in the change being opened and <code>false</code> if the
	 * currently selected change could not be opened.
	 * @return whether the selected change was opened.
	 */
	boolean openSelectedChange();

}
