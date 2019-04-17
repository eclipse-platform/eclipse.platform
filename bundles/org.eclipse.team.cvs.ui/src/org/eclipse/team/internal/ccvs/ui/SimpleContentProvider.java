/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui;

 
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * A default content provider to prevent subclasses from
 * having to implement methods they don't need.
 */
public class SimpleContentProvider implements IStructuredContentProvider {

	/**
	 * SimpleContentProvider constructor.
	 */
	public SimpleContentProvider() {
		super();
	}
	
	@Override
	public void dispose() {
	}
	
	@Override
	public Object[] getElements(Object element) {
		return new Object[0];
	}
	
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
