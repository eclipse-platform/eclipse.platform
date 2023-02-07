/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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

package org.eclipse.help.ui.internal.views;

/**
 * Creates padding above the bookmarks view
 */

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class BookmarkHeaderPart extends AbstractFormPart implements IHelpPart  {

	private Composite container;
	private String id;

	public BookmarkHeaderPart(Composite parent, FormToolkit toolkit) {
		container = toolkit.createComposite(parent);
		Composite inner = toolkit.createComposite(container);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		container.setLayout(layout);
		GridData data = new GridData();
		data.heightHint = 2;
		inner.setLayoutData(data);
	}

	@Override
	public void init(ReusableHelpPart parent, String id, IMemento memento) {
		this.id = id;
	}

	@Override
	public void saveState(IMemento memento) {
	}

	@Override
	public Control getControl() {
		return container;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setVisible(boolean visible) {
		container.setVisible(visible);
	}

	@Override
	public boolean hasFocusControl(Control control) {
		return false;
	}

	@Override
	public boolean fillContextMenu(IMenuManager manager) {
		return false;
	}

	@Override
	public IAction getGlobalAction(String id) {
		return null;
	}

	@Override
	public void stop() {

	}

	@Override
	public void toggleRoleFilter() {

	}

	@Override
	public void refilter() {

	}

}
