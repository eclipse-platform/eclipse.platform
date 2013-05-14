/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;

/**
 * Class which allows content merge viewer to provide a structure viewer that can be used in the outline 
 * view.
 */
public abstract class OutlineViewerCreator {

	/**
	 * Property constant that identifies the input of the outline view.
	 */
	public static final String PROP_INPUT = "org.eclipse.compare.OutlineInput"; //$NON-NLS-1$
	
	private ListenerList listeners = new ListenerList(ListenerList.IDENTITY);
	
	/**
	 * Method called by the editor to create a structure viewer for the current content merge viewer.
	 * @param oldViewer the current viewer that is being used to show the structure
	 * @param input the input
	 * @param parent the parent composite
	 * @param configuration the compare configuration
	 * @return a viewer to be placed in the outline viewer or <code>null</code>
	 */
	public abstract Viewer findStructureViewer(Viewer oldViewer, ICompareInput input,
			Composite parent, CompareConfiguration configuration);
	
	public abstract boolean hasViewerFor(Object input);
	
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		listeners.add(listener);
	}

	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		listeners.remove(listener);
	}
	
	public void fireInputChange(Object oldInput, Object newInput) {
		Object[] list = listeners.getListeners();
		final PropertyChangeEvent event = new PropertyChangeEvent(this, PROP_INPUT, oldInput, newInput);
		for (int i = 0; i < list.length; i++) {
			final IPropertyChangeListener listener = (IPropertyChangeListener)list[i];
			SafeRunner.run(new ISafeRunnable() {
				public void run() throws Exception {
					listener.propertyChange(event);
				}
				public void handleException(Throwable exception) {
					// Logged by SafeRunner
				}
			});
		}
	}

	public abstract Object getInput();

}
