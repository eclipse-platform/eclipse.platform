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
package org.eclipse.platform.internal;

import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.cheatsheets.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.intro.*;
import org.eclipse.ui.intro.config.*;

public final class CheatSheetStandbyContent implements IStandbyContentPart {

	private static String MEMENTO_CHEATSHEET_ID_ATT = "cheatsheetId"; //$NON-NLS-1$

	//private IIntroPart introPart;
	private ICheatSheetViewer viewer;
	private Composite container;
	private String input;

	@Override
	public void init(IIntroPart introPart, IMemento memento) {
		//this.introPart = introPart;
		// try to restore last state.
		input = getCachedInput(memento);
	}

	@Override
	public void createPartControl(Composite parent, FormToolkit toolkit) {
		container = toolkit.createComposite(parent);
		FillLayout layout = new FillLayout();
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);

		viewer = CheatSheetViewerFactory.createCheatSheetView();
		viewer.createPartControl(container);
	}

	@Override
	public Control getControl() {
		return container;
	}

	@Override
	public void setInput(Object input) {
		// if the new input is null, use cacched input from momento.
		if (input != null)
			this.input = (String) input;
		viewer.setInput(this.input);
	}

	@Override
	public void setFocus() {
		viewer.setFocus();
	}

	@Override
	public void dispose() {

	}

	@Override
	public void saveState(IMemento memento) {
		String currentCheatSheetId = viewer.getCheatSheetID();
		if (currentCheatSheetId != null)
			memento.putString(MEMENTO_CHEATSHEET_ID_ATT, currentCheatSheetId);
	}

	/**
	 * Tries to create the last content part viewed, based on content part id..
	 */
	private String getCachedInput(IMemento memento) {
		if (memento == null)
			return null;
		return memento.getString(MEMENTO_CHEATSHEET_ID_ATT);

	}

}
