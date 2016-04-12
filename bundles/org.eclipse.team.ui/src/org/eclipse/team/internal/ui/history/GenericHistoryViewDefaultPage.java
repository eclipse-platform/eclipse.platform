/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.internal.ui.history;

import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.ui.history.HistoryPage;

public class GenericHistoryViewDefaultPage extends HistoryPage {

	private Composite pgComp;

	@Override
	public void createControl(Composite parent) {
		pgComp = new Composite(parent, SWT.NULL);
		pgComp.setLayout(new FillLayout());
		pgComp.setBackground(JFaceColors.getBannerBackground(pgComp.getDisplay()));
	}

	@Override
	public Control getControl() {
		return pgComp;
	}

	@Override
	public void setFocus() {
		pgComp.setFocus();
	}

	@Override
	public boolean isValidInput(Object object) {
		return false;
	}

	@Override
	public void refresh() {
		//nothing to refresh
	}

	@Override
	public String getName() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public Object getInput() {
		return null;
	}

	@Override
	public boolean inputSet() {
		//no history to show
		return false;
	}

}
