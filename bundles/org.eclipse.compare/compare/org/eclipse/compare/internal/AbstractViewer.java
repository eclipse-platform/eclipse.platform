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
package org.eclipse.compare.internal;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;


public abstract class AbstractViewer extends Viewer {

	public void setInput(Object input) {
		// empty default implementation
	}
	
	public Object getInput() {
		return null;
	}
	
	public ISelection getSelection() {
		return null;
	}
	
	public void setSelection(ISelection s, boolean reveal) {
		// empty default implementation
	}
	
	public void refresh() {
		// empty default implementation
	}
}
