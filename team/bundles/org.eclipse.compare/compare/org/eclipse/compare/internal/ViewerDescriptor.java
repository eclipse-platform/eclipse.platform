/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
import org.eclipse.compare.IViewerCreator;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;

/**
 * Creates <code>Viewer</code>s from an <code>IConfigurationElement</code>.
 */
public class ViewerDescriptor implements IViewerDescriptor {
	private final static String CLASS_ATTRIBUTE= "class"; //$NON-NLS-1$
	private final static String EXTENSIONS_ATTRIBUTE= "extensions"; //$NON-NLS-1$
	private final static String LINKED_EDITOR_ATTRIBUTE = "linkedEditor"; //$NON-NLS-1$
	private final static String LABEL_ATTRIBUTE = "label"; //$NON-NLS-1$

	private final IConfigurationElement fConfiguration;
	private IViewerCreator fViewerCreator;
	private Class<? extends Viewer> fViewerClass;

	public ViewerDescriptor(IConfigurationElement config) {
		fConfiguration= config;
	}

	@Override
	public Viewer createViewer(Viewer currentViewer, Composite parent, CompareConfiguration mp) {
		if (currentViewer != null && currentViewer.getClass() == fViewerClass) {
			//System.out.println("reused viewer: " + currentViewer.getClass().getName());
			return currentViewer;
		}

		if (fViewerCreator == null) {
			try {
				fViewerCreator= (IViewerCreator) fConfiguration.createExecutableExtension(CLASS_ATTRIBUTE);
			} catch (CoreException e) {
				CompareUIPlugin.log(e);
			}
		}

		if (fViewerCreator != null) {
			// If we are going to return a new viewer, we want to preemptively deregister
			// any handlers to avoid the logging of conflict warnings
			if (currentViewer != null) {
				CompareHandlerService[] compareHandlerService = Adapters.adapt(currentViewer, CompareHandlerService[].class);
				if (compareHandlerService != null) {
					for (CompareHandlerService s : compareHandlerService) {
						s.dispose();
					}
				}
			}
			Viewer viewer= fViewerCreator.createViewer(parent, mp);
			if (viewer != null)
				fViewerClass= viewer.getClass();
			return viewer;
		}

		return null;
	}

	public String getExtension() {
		return fConfiguration.getAttribute(EXTENSIONS_ATTRIBUTE);
	}

	String getLabel() {
		return fConfiguration.getAttribute(LABEL_ATTRIBUTE);
	}

	String getLinkedEditorId() {
		return fConfiguration.getAttribute(LINKED_EDITOR_ATTRIBUTE);
	}

	String getViewerClass() {
		return fConfiguration.getAttribute(CLASS_ATTRIBUTE);
	}

	@SuppressWarnings("nls")
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ViewerDescriptor [");
		if (fViewerClass != null) {
			sb.append("viewerClass=");
			sb.append(fViewerClass);
			sb.append(", ");
		}
		if (fViewerCreator != null) {
			sb.append("viewerCreator=");
			sb.append(fViewerCreator);
			sb.append(", ");
		}
		String viewerClass = getViewerClass();
		if (viewerClass != null) {
			sb.append("viewerClass=");
			sb.append(viewerClass);
			sb.append(", ");
		}
		if (fConfiguration != null) {
			sb.append("configuration=");
			sb.append(fConfiguration);
		}
		sb.append("]");
		return sb.toString();
	}

}
