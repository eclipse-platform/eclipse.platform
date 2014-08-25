/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@gmail.com> - Bug 442475
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;


public abstract class AbstractViewer extends Viewer {

	public void setInput(Object input) {
		// empty default implementation
	}
	
	public Object getInput() {
		return null;
	}
	
	public ISelection getSelection() {
		return StructuredSelection.EMPTY;
	}
	
	public void setSelection(ISelection s, boolean reveal) {
		// empty default implementation
	}
	
	public void refresh() {
		// empty default implementation
	}
}
