/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.*;

/**
 * Supports cross-pane navigation through the differences contained in a {@link CompareEditorInput}
 * or a similar type of compare container.
 * @see INavigatable
 */
public class CompareEditorInputNavigator extends CompareNavigator {

	// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
	private boolean fNextFirstTime= true;
	private Object[] fPanes;
	
	/**
	 * Create a navigator for navigating the given panes
	 * @param panes the panes to navigate.
	 */
	public CompareEditorInputNavigator(Object[] panes) {
		fPanes= panes;
	}
	
	/**
	 * Return the set of panes that this navigator is navigating.
	 * The {@link INavigatable} is obtain from each pane using the
	 * adaptable mechanism.
	 * @return the set of panes that this navigator is navigating
	 */
	public Object[] getPanes() {
		return fPanes;
	}
	
	protected INavigatable[] getNavigatables() {
		List result = new ArrayList();
		Object[] panes = getPanes();
		for (int i = 0; i < panes.length; i++) {
			Object pane = panes[i];
			INavigatable navigator= getNavigator(pane);
			if (navigator != null)
				result.add(navigator);
		}
		return (INavigatable[]) result.toArray(new INavigatable[result.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.ICompareNavigator#selectChange(boolean)
	 */
	public boolean selectChange(boolean next) {
		// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
		if (next && fNextFirstTime && mustOpen()) {
			fNextFirstTime= false;
			if (openElement())
				return false;
		}
		return super.selectChange(next);
	}
	
	/*
	 * Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
	 */
	private boolean mustOpen() {
		Object[] panes = getPanes();
		if (panes == null || panes.length == 0)
			return false;
		for (int i= 1; i < panes.length; i++) {
			Object pane= panes[i];
			INavigatable nav = getNavigator(pane);
			if (nav != null && nav.getInput() != null)
				return false;
		}
		return true;
	}
	
	/*
	 * Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
	 */
	private boolean openElement() {
		Object[] panes = getPanes();
		if (panes == null || panes.length == 0)
			return false;
		INavigatable nav = getNavigator(panes[0]);
		if (nav != null) {
			if (!nav.openSelectedChange())
				// selected change not opened, open first instead
				nav.selectChange(INavigatable.FIRST_CHANGE);
			return true;
		}
		return false;
	}
}
