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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.Document;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.ICompareInput;


public class SimpleTextViewer extends AbstractViewer {
		
	private SourceViewer fSourceViewer;
	private ICompareInput fInput;
	
	
	SimpleTextViewer(Composite parent) {
		fSourceViewer= new SourceViewer(parent, null, SWT.H_SCROLL | SWT.V_SCROLL);
		fSourceViewer.setEditable(false);
	}
		
	public Control getControl() {
		return fSourceViewer.getTextWidget();
	}
	
	public void setInput(Object input) {
		if (input instanceof IStreamContentAccessor) {
			fSourceViewer.setDocument(new Document(getString(input)));
		} else if (input instanceof ICompareInput) {
			fInput= (ICompareInput) input;
			ITypedElement left= fInput.getLeft();
			fSourceViewer.setDocument(new Document(getString(left)));
		}
	}
	
	public Object getInput() {
		return fInput;
	}
	
	private String getString(Object input) {
		
		if (input instanceof IStreamContentAccessor) {
			try {
				return Utilities.readString((IStreamContentAccessor) input);
			} catch (CoreException ex) {
				// NeedWork
				CompareUIPlugin.log(ex);
			}
		}
		return ""; //$NON-NLS-1$
	}
}
