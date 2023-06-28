/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.unittest.internal.ui.history;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.transform.TransformerFactoryConfigurationError;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.internal.model.TestRunSession;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * A History item selection dialog
 */
public class HistoryDialog extends SelectionDialog {

	private static final Comparator<HistoryItem> COMPARING_START_DATE = Comparator.comparing(HistoryItem::getStartDate)
			.reversed();
	private Set<TestRunSession> fCurrentlyVisible;
	private Button fRemoveButton;
	private Button fExportButton;
	private TableViewer fTable;

	/**
	 * Constructs a history item selection dialog object
	 *
	 * @param shell           a shell object
	 * @param visibleSessions a set of visible {@link TestRunSession} objects
	 */
	public HistoryDialog(Shell shell, Set<TestRunSession> visibleSessions) {
		super(shell);
		fCurrentlyVisible = visibleSessions;
		setResult(Collections.emptyList());
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(Messages.HistoryDialog_title);
		getShell().setText(Messages.HistoryDialog_title);
		Composite res = new Composite(parent, SWT.NONE);
		res.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		res.setLayout(new GridLayout(2, false));
		this.fTable = createTable(res);
		fTable.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createButtons(res);
		return fTable.getControl();
	}

	private void createButtons(Composite res) {
		Composite buttons = new Composite(res, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, false, false));
		RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
		rowLayout.fill = true;
		buttons.setLayout(rowLayout);
		fRemoveButton = new Button(buttons, SWT.PUSH);
		fRemoveButton.setText(Messages.HistoryDialog_remove);
		fRemoveButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			for (Object selected : getResult()) {
				History.INSTANCE.remove((HistoryItem) selected);
			}
			fTable.refresh();
		}));
		Button importButton = new Button(buttons, SWT.PUSH);
		importButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			FileDialog fileDialog = new FileDialog(getShell());
			fileDialog.setFilterExtensions(new String[] { "*.xml" }); //$NON-NLS-1$
			fileDialog.setText(Messages.HistoryDialog_selectImport);
			String path = fileDialog.open();
			if (path == null) {
				return;
			}
			Path sourcePath = Path.of(path);
			Path targetPath = Path.of(History.INSTANCE.getDirectory().getAbsolutePath(),
					sourcePath.getFileName().toString());
			try {
				Files.copy(sourcePath, targetPath);
				History.INSTANCE.add(new HistoryItem(targetPath.toFile()));
			} catch (IOException e1) {
				UnitTestPlugin.log(e1);
			}
			fTable.refresh();
		}));
		importButton.setText(Messages.HistoryDialog_import);
		fExportButton = new Button(buttons, SWT.PUSH);
		fExportButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
			directoryDialog.setText(Messages.HistoryDialog_selectExport);
			String path = directoryDialog.open();
			if (path == null) {
				return;
			}
			File directory = new File(path);
			for (Object object : getResult()) {
				HistoryItem historyItem = (HistoryItem) object;
				try {
					historyItem.storeSessionToFile(new File(directory, historyItem.getFile().getName()));
				} catch (TransformerFactoryConfigurationError | CoreException e1) {
					UnitTestPlugin.log(e1);
				}
			}
			fTable.refresh();
		}));
		fExportButton.setText(Messages.HistoryDialog_export);
		updateButtons();
	}

	private TableViewer createTable(Composite parent) {
		TableViewer table = new TableViewer(parent);
		table.setContentProvider(new ArrayContentProvider());
		table.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				HistoryItem item1 = (HistoryItem) e1;
				HistoryItem item2 = (HistoryItem) e2;
				return COMPARING_START_DATE.compare(item1, item2);
			}
		});
		int fontSize = table.getTable().getFont().getFontData()[0].getHeight();
		table.getTable().setHeaderVisible(true);
		TableViewerColumn visibleColumn = new TableViewerColumn(table, SWT.DEFAULT);
		visibleColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((HistoryItem) element).getCurrentTestRunSession().filter(fCurrentlyVisible::contains)
						.map(any -> "👁️").orElse(""); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		visibleColumn.getColumn().setWidth(2 * fontSize);
		TableViewerColumn nameColumn = new TableViewerColumn(table, SWT.DEFAULT);
		nameColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((HistoryItem) element).getName();
			}
		});
		nameColumn.getColumn().setWidth(20 * fontSize);
		nameColumn.getColumn().setText(Messages.HistoryDialog_name);
		TableViewerColumn dateColumn = new TableViewerColumn(table, SWT.DEFAULT);
		dateColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Instant startDate = ((HistoryItem) element).getStartDate();
				return startDate != null
						? startDate.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.RFC_1123_DATE_TIME)
						: ""; //$NON-NLS-1$
			}
		});
		dateColumn.getColumn().setWidth(25 * fontSize);
		dateColumn.getColumn().setText(Messages.HistoryDialog_date);
		TableViewerColumn progressColumn = new TableViewerColumn(table, SWT.DEFAULT);
		progressColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((HistoryItem) element).getCurrentTestRunSession().filter(TestRunSession::isRunning)
						.map(any -> "🏃").orElse(""); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		progressColumn.getColumn().setWidth(2 * fontSize);
		progressColumn.getColumn().setText(Messages.HistoryDialog_progress);
		TableViewerColumn successColumn = new TableViewerColumn(table, SWT.DEFAULT);
		successColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				int failures = ((HistoryItem) element).getFailureCount();
				if (failures == 0) {
					return "✅"; //$NON-NLS-1$
				}
				return "❌ " + failures + Messages.HistoryDialog_failures; //$NON-NLS-1$
			}
		});
		successColumn.getColumn().setWidth(15 * fontSize);
		successColumn.getColumn().setText(Messages.HistoryDialog_result);
		TableViewerColumn sizeColumn = new TableViewerColumn(table, SWT.DEFAULT);
		sizeColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Long size = ((HistoryItem) element).getSizeOnDisk();
				if (size != null) {
					return size.toString() + " B"; //$NON-NLS-1$
				}
				return Character.toString('?');
			}
		});
		sizeColumn.getColumn().setText(Messages.HistoryDialog_size);
		sizeColumn.getColumn().setWidth(10 * fontSize);
		table.setInput(History.INSTANCE.getHistory());
		table.setSelection(new StructuredSelection(getInitialElementSelections().toArray()));
		table.addSelectionChangedListener(
				event -> setSelectionResult(((IStructuredSelection) event.getSelection()).toArray()));
		return table;
	}

	@Override
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		return super.createButton(parent, id, id == IDialogConstants.OK_ID ? IDialogConstants.OPEN_LABEL : label,
				defaultButton);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		updateButtons();
	}

	@Override
	protected void setSelectionResult(Object[] newResult) {
		super.setSelectionResult(newResult);
		updateButtons();
	}

	private void updateButtons() {
		Object[] selection = getResult();
		boolean singleItemSelection = selection.length == 1;
		Stream.of(getButton(IDialogConstants.OK_ID), fRemoveButton, fExportButton) //
				.filter(Objects::nonNull) //
				.forEach(button -> button.setEnabled(singleItemSelection));
		if (singleItemSelection) {
			HistoryItem item = (HistoryItem) selection[0];
			boolean isRunning = item.getCurrentTestRunSession().filter(TestRunSession::isRunning).isPresent();
			fRemoveButton.setEnabled(!isRunning);
			fExportButton.setEnabled(!isRunning);
		}

	}
}
