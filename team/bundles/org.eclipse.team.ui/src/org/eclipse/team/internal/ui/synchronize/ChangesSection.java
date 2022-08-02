/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.forms.HyperlinkGroup;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;

/**
 * Section shown in a participant page to show the changes for this participant. This
 * includes a diff viewer for browsing the changes.
 *
 * @since 3.2
 */
public class ChangesSection extends Composite {

	private AbstractSynchronizePage page;
	private ISynchronizePageConfiguration configuration;
	private FormToolkit forms;

	/**
	 * Page book either shows the diff tree viewer if there are changes or
	 * shows a message to the user if there are no changes that would be
	 * shown in the tree.
	 */
	private PageBook changesSectionContainer;

	/**
	 * Diff tree viewer that shows synchronization changes. This is created
	 * by the participant.
	 */
	private Viewer changesViewer;

	/**
	 * Create a changes section on the following page.
	 *
	 * @param parent the parent control
	 * @param page the page showing this section
	 * @param configuration the configuration for the synchronize page
	 */
	public ChangesSection(Composite parent, AbstractSynchronizePage page, ISynchronizePageConfiguration configuration) {
		super(parent, SWT.NONE);
		this.page = page;
		this.configuration = configuration;

		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		setLayoutData(data);

		forms = new FormToolkit(parent.getDisplay());
		forms.setBackground(getListBackgroundColor());
		HyperlinkGroup group = forms.getHyperlinkGroup();
		group.setBackground(getListBackgroundColor());

		changesSectionContainer = new PageBook(this, SWT.NONE);
		data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		changesSectionContainer.setLayoutData(data);
	}

	protected Color getListBackgroundColor() {
		return getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
	}

	public PageBook getContainer() {
		return changesSectionContainer;
	}

	protected ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}

	protected FormToolkit getForms() {
		return forms;
	}

	protected AbstractSynchronizePage getPage() {
		return page;
	}

	@Override
	public void dispose() {
		super.dispose();
		forms.dispose();
	}

	public void setViewer(Viewer viewer) {
		this.changesViewer = viewer;
		initializeChangesViewer();
	}

	protected void initializeChangesViewer() {
		TeamUIPlugin.getStandardDisplay().asyncExec(() -> {
			Control control = ChangesSection.this.getChangesViewer().getControl();
			if (!getContainer().isDisposed() && !control.isDisposed()) {
				getContainer().showPage(control);
			}
		});
	}

	public Viewer getChangesViewer() {
		return changesViewer;
	}

	protected boolean isThreeWay() {
		return ISynchronizePageConfiguration.THREE_WAY.equals(getConfiguration().getComparisonType());
	}
}
