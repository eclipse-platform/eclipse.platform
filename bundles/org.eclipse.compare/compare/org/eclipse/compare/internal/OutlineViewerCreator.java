/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
package org.eclipse.compare.internal;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
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

	private ListenerList<IPropertyChangeListener> listeners = new ListenerList<>(ListenerList.IDENTITY);

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
		final PropertyChangeEvent event = new PropertyChangeEvent(this, PROP_INPUT, oldInput, newInput);
		for (final IPropertyChangeListener listener : listeners) {
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					listener.propertyChange(event);
				}
				@Override
				public void handleException(Throwable exception) {
					// Logged by SafeRunner
				}
			});
		}
	}

	public abstract Object getInput();

}
