/*******************************************************************************
 * Copyright (c) 2026 Sergii Zolotarov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sergii Zolotarov - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions;

import java.util.List;
import java.util.Locale;

import org.eclipse.debug.internal.ui.actions.LaunchAsChoices.Choice;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

/**
 * Shows the launch choices with a filter field. Enter launches the selected
 * choice. Up and Down select another choice, also while the filter field has
 * the focus.
 */
public final class LaunchAsDialog extends Dialog {

	private final String title;
	private final String buttonLabel;
	private final List<Choice> choices;
	private final Choice initial;
	private Text filterText;
	private TableViewer viewer;
	private Choice result;

	/**
	 * @param buttonLabel the text of the OK button, for example "Run"
	 * @param initial     the choice to select first, or null for the first choice
	 */
	public LaunchAsDialog(Shell parent, String title, String buttonLabel, List<Choice> choices, Choice initial) {
		super(parent);
		this.title = title;
		this.buttonLabel = buttonLabel;
		this.choices = choices;
		this.initial = initial;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(title);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		LocalResourceManager images = new LocalResourceManager(JFaceResources.getResources(), area);

		filterText = new Text(area, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
		filterText.setMessage(ActionMessages.LaunchAsDialog_filter);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(filterText);

		viewer = new TableViewer(area, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Choice) element).label();
			}

			@Override
			public Image getImage(Object element) {
				Choice choice = (Choice) element;
				return choice.image() != null ? images.create(choice.image()) : null;
			}
		});
		viewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer v, Object parentElement, Object element) {
				String filter = filterText.getText().trim().toLowerCase(Locale.ROOT);
				return filter.isEmpty() || ((Choice) element).label().toLowerCase(Locale.ROOT).contains(filter);
			}
		});
		viewer.setInput(choices);
		viewer.addDoubleClickListener(event -> okPressed());
		Table table = viewer.getTable();
		GridDataFactory.fillDefaults().grab(true, true).hint(convertWidthInCharsToPixels(60),
				table.getItemHeight() * Math.min(Math.max(choices.size(), 3), 15)).applyTo(table);

		filterText.addModifyListener(event -> {
			viewer.refresh();
			if (selected() == null && table.getItemCount() > 0) {
				viewer.setSelection(new StructuredSelection(table.getItem(0).getData()), true);
			}
			updateOkButton();
		});
		filterText.addListener(SWT.KeyDown, event -> {
			if (event.keyCode == SWT.ARROW_DOWN || event.keyCode == SWT.ARROW_UP) {
				int count = table.getItemCount();
				if (count > 0) {
					int index = table.getSelectionIndex() + (event.keyCode == SWT.ARROW_DOWN ? 1 : -1);
					index = Math.max(0, Math.min(count - 1, index));
					viewer.setSelection(new StructuredSelection(table.getItem(index).getData()), true);
				}
				event.doit = false;
			}
		});
		viewer.addSelectionChangedListener(event -> updateOkButton());

		Choice first = initial != null && choices.contains(initial) ? initial : choices.isEmpty() ? null : choices.get(0);
		if (first != null) {
			viewer.setSelection(new StructuredSelection(first), true);
		}
		return area;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setText(buttonLabel);
		updateOkButton();
	}

	@Override
	public void create() {
		super.create();
		filterText.setFocus();
	}

	private void updateOkButton() {
		if (getButton(IDialogConstants.OK_ID) != null) {
			getButton(IDialogConstants.OK_ID).setEnabled(selected() != null);
		}
	}

	private Choice selected() {
		return ((IStructuredSelection) viewer.getSelection()).getFirstElement() instanceof Choice choice ? choice
				: null;
	}

	@Override
	protected void okPressed() {
		result = selected();
		if (result != null) {
			super.okPressed();
		}
	}

	/** @return the selected choice after OK, or null */
	public Choice getResult() {
		return result;
	}

	/** For the tests: the filter field */
	public Text getFilterText() {
		return filterText;
	}

	/** For the tests: the list */
	public TableViewer getViewer() {
		return viewer;
	}
}
