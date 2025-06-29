/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor.templates;

import org.eclipse.ant.internal.ui.editor.AntSourceViewerInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlCreatorExtension;
import org.eclipse.swt.widgets.Shell;

public class AntTemplateInformationControlCreator implements IInformationControlCreator, IInformationControlCreatorExtension {

	private AntSourceViewerInformationControl fControl;

	public AntTemplateInformationControlCreator() {
	}

	@Override
	public IInformationControl createInformationControl(Shell parent) {
		fControl = new AntSourceViewerInformationControl(parent);
		fControl.addDisposeListener(e -> fControl = null);
		return fControl;
	}

	@Override
	public boolean canReuse(IInformationControl control) {
		return fControl == control && fControl != null;
	}

	@Override
	public boolean canReplace(IInformationControlCreator creator) {
		return (creator != null && getClass() == creator.getClass());
	}
}
