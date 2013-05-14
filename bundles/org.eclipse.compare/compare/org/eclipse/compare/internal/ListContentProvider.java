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

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/** 
 * A specialized content provider to show a list of editor parts.
 */ 
public class ListContentProvider implements IStructuredContentProvider {
	List fContents;	

	public ListContentProvider() {
		// nothing to do
	}
	
	public Object[] getElements(Object input) {
		if (fContents != null && fContents == input)
			return fContents.toArray();
		return new Object[0];
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof List) 
			fContents= (List)newInput;
		else
			fContents= null;
		// we use a fixed set.
	}

	public void dispose() {
		// empty default implementation
	}
	
	public boolean isDeleted(Object o) {
		return fContents != null && !fContents.contains(o);
	}
}
