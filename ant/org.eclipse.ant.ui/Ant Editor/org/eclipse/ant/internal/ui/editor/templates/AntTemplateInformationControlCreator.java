/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor.templates;

import org.eclipse.ant.internal.ui.editor.AntSourceViewerInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlCreatorExtension;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Shell;


public class AntTemplateInformationControlCreator implements IInformationControlCreator, IInformationControlCreatorExtension {
	
	private AntSourceViewerInformationControl fControl;
	
	public AntTemplateInformationControlCreator()  {
	}
	
	/*
	 * @see org.eclipse.jface.text.IInformationControlCreator#createInformationControl(org.eclipse.swt.widgets.Shell)
	 */
	public IInformationControl createInformationControl(Shell parent) {
		fControl= new AntSourceViewerInformationControl(parent);
		fControl.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				fControl= null;
			}
		});
		return fControl;
	}

	/*
	 * @see org.eclipse.jface.text.IInformationControlCreatorExtension#canReuse(org.eclipse.jface.text.IInformationControl)
	 */
	public boolean canReuse(IInformationControl control) {
		return fControl == control && fControl != null;
	}

	/*
	 * @see org.eclipse.jface.text.IInformationControlCreatorExtension#canReplace(org.eclipse.jface.text.IInformationControlCreator)
	 */
	public boolean canReplace(IInformationControlCreator creator) {
		return (creator != null && getClass() == creator.getClass());
	}
}
