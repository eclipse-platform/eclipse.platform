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

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.custom.SashForm;

/**
 * The Splitter adds support for nesting to a SashForm.
 * <P>
 * If Splitters are nested directly:
 * <UL>
 * <LI>changing the visibility of a child may propagate upward to the parent Splitter if the child
 * is the last child to become invisible or the first to become visible.</LI>
 * <LI>maximizing a child makes it as large as the topmost enclosing Splitter</LI>
 * </UL>
 * 
 * @since 2.1
 */
public class Splitter extends SashForm {
	
	private static final String VISIBILITY= "org.eclipse.compare.internal.visibility"; //$NON-NLS-1$
	
	/**
	 * Constructs a new instance of this class given its parent
	 * and a style value describing its behavior and appearance.
	 * <p>
	 * The style value is either one of the style constants defined in
	 * class <code>SWT</code> which is applicable to instances of this
	 * class, or must be built by <em>bitwise OR</em>'ing together
	 * (that is, using the <code>int</code> "|" operator) two or more
	 * of those <code>SWT</code> style constants. The class description
	 * lists the style constants that are applicable to the class.
	 * Style bits are also inherited from superclasses.
	 * </p>
	 *
	 * @param parent a widget which will be the parent of the new instance (cannot be null)
	 * @param style the style of widget to construct
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
	 * </ul>
	 * @exception org.eclipse.swt.SWTException <ul>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
	 * </ul>
	 */
	public Splitter(Composite parent, int style) {
		super(parent, style);
	}
		
	/**
	 * Sets the visibility of the given child in this Splitter. If this change
	 * affects the visibility state of the whole Splitter, and if the Splitter
	 * is directly nested in one or more Splitters, this method recursively
	 * propagates the new state upward.
	 *
	 * @param child the child control for which the visibility is changed
	 * @param visible the new visibility state
	 */
	public void setVisible(Control child, boolean visible) {
		
		boolean wasEmpty= isEmpty();
				
		child.setVisible(visible);
		child.setData(VISIBILITY, new Boolean(visible));
		
		if (wasEmpty != isEmpty()) {
			// recursively walk up
			Composite parent= getParent();
			if (parent instanceof Splitter) {
				Splitter sp= (Splitter) parent;
				sp.setVisible(this, visible);
				sp.layout();
			}
		} else {
			layout();
		}
	}

	/* (non-Javadoc)
	 * Recursively calls setMaximizedControl for all direct parents that are
	 * itself Splitters.
	 */
	public void setMaximizedControl(Control control) {
		if (control == null || control == getMaximizedControl())
			super.setMaximizedControl(null);
		else
			super.setMaximizedControl(control);

		// recursively walk upward
		Composite parent= getParent();
		if (parent instanceof Splitter)
			((Splitter) parent).setMaximizedControl(this);
		else
			layout(true);
	}

	/* (non-Javadoc)
	 * Returns true if Splitter has no children or if all children are invisible.
	 */
	private boolean isEmpty() {
		Control[] controls= getChildren();
		for (int i= 0; i < controls.length; i++)
			if (isVisible(controls[i]))
				return false;
		return true;
	}
	
	/* (non-Javadoc)
	 * Returns the visibility state of the given child control. If the
	 * control is a Sash, this method always returns false.
	 */
	private boolean isVisible(Control child) {
		if (child instanceof Sash)
			return false;
		Object data= child.getData(VISIBILITY);
		if (data instanceof Boolean)
			return ((Boolean)data).booleanValue();
		return true;
	}
}
