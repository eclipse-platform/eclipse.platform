/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers.model;

import org.eclipse.debug.internal.core.commands.Request;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.ui.IMemento;

/**
 * @since 3.3
 */
abstract class MementoUpdate extends Request implements IViewerUpdate {

	private IPresentationContext fContext;
	private Object fElement;
	private TreePath fElementPath;
	private IMemento fMemento;
	protected TreeModelContentProvider fProvider;
	protected Object fViewerInput;

	/**
	 * Constructs a viewer state request.
	 * @param provider the content provider to use for the update
	 * @param viewerInput the current input
	 * @param elementPath the path of the element to update
	 * @param element the element to update
	 * @param memento Memento to update
	 * @param context the presentation context
	 *
	 */
	public MementoUpdate(TreeModelContentProvider provider, Object viewerInput, IPresentationContext context, Object element, TreePath elementPath, IMemento memento) {
		fContext = context;
		fElement = element;
		fElementPath = elementPath;
		fMemento = memento;
		fProvider = provider;
		fViewerInput = viewerInput;
	}

	@Override
	public IPresentationContext getPresentationContext() {
		return fContext;
	}

	@Override
	public Object getElement() {
		return fElement;
	}

	@Override
	public TreePath getElementPath() {
		return fElementPath;
	}

	public IMemento getMemento() {
		return fMemento;
	}

	public TreeModelContentProvider getContentProvider() {
		return fProvider;
	}

	public Object getElement(TreePath path) {
		return fProvider.getElement(path);
	}

	@Override
	public Object getViewerInput() {
		return fViewerInput;
	}



}
