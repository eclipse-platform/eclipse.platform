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

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IViewerCreator;
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
	private final static String LABEL_ATTRIBUTE = "label"; //$NON-NLS-1$

	private IConfigurationElement fConfiguration;
	private IViewerCreator fViewerCreator;
	private Class fViewerClass;

	public ViewerDescriptor(IConfigurationElement config) {
		fConfiguration= config;
	}

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
				CompareHandlerService[] compareHandlerService = (CompareHandlerService[]) Utilities.getAdapter(currentViewer, CompareHandlerService[].class);
				if (compareHandlerService != null) {
					for (int i = 0; i < compareHandlerService.length; i++) {
						compareHandlerService[i].dispose();
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
}
