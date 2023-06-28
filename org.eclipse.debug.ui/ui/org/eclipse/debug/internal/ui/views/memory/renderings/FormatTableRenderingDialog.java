/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *     QNX - Alena Laskavaia Bug 240330 - [Memory View] Items per row should not be limited to 16
 *     Freescale -  Teodor Madan  Bug 248486 -  [Memory View] Need a way to set default memory view format for a particular renderer
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory.renderings;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.memory.IPersistableDebugElement;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PageBook;

public class FormatTableRenderingDialog extends TrayDialog
	{

		// possible number of addressable units per column
		private int[] fColumnSizes = new int[] {1, 2, 4, 8, 16, 32, 64, 128};

		// possible number of addressable units per column
		private int[] fRowSizes = new int[] {1, 2, 4, 8, 16, 32, 64, 128};

		private Combo fColumnControl;
		private Combo fRowControl;

		private int fCurrentColIdx = -1;
		private int fCurrentRowIdx = -1;
		private Control fPreivewPage;
		private PageBook fPreviewPageBook;
		private Button fDefaultButton;
		private Text fDefaultColValue;
		private Text fDefaultRowValue;
		private AbstractBaseTableRendering fRendering;
		private int fColumnSize;
		private int fRowSize;
		private Label fMsgLabel;
		private boolean fDisableCancel = false;
		private String fMsg;


		public FormatTableRenderingDialog(AbstractBaseTableRendering rendering, Shell parentShell) {
			super(parentShell);
			setShellStyle(getShellStyle() | SWT.RESIZE);
			fRendering = rendering;
			fMsg = DebugUIMessages.FormatTableRenderingAction_1;
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			getShell().setText(DebugUIMessages.FormatTableRenderingAction_0);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IDebugUIConstants.PLUGIN_ID + ".FormatTableRenderingDialog_context"); //$NON-NLS-1$

			Composite composite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.numColumns = 3;
			layout.makeColumnsEqualWidth = false;
			composite.setLayout(layout);
			GridData data = new GridData();
			data.grabExcessHorizontalSpace = true;
			data.grabExcessVerticalSpace = true;
			data.horizontalAlignment = SWT.FILL;
			data.verticalAlignment = SWT.FILL;
			composite.setLayoutData(data);

			fMsgLabel = new Label(composite, SWT.NONE);
			fMsgLabel.setText(fMsg);

			data = new GridData();
			data.grabExcessHorizontalSpace = true;
			data.horizontalAlignment = SWT.BEGINNING;
			data.horizontalSpan = 3;
			fMsgLabel.setLayoutData(data);

			Label rowLabel = new Label(composite, SWT.NONE);
			rowLabel.setText(DebugUIMessages.FormatTableRenderingAction_2);
			fRowControl = new Combo(composite, SWT.READ_ONLY);
			for (int size : fRowSizes) {
				fRowControl.add(String.valueOf(size));
			}

			fRowControl.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					if (fCurrentRowIdx != fRowControl.getSelectionIndex())
					{
						fCurrentRowIdx = fRowControl.getSelectionIndex();
						refreshPreviewPage();
						updateButtons();
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}});

			data = new GridData();
			data.grabExcessHorizontalSpace = false;
			data.horizontalAlignment = SWT.BEGINNING;
			fRowControl.setLayoutData(data);

			Label unit = new Label(composite, SWT.NONE);
			unit.setText(DebugUIMessages.FormatTableRenderingAction_3);

			Label columnLabel = new Label(composite, SWT.NONE);
			columnLabel.setText(DebugUIMessages.FormatTableRenderingAction_4);
			fColumnControl = new Combo(composite, SWT.READ_ONLY);
			for (int size : fColumnSizes) {
				fColumnControl.add(String.valueOf(size));
			}

			fColumnControl.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					if (fCurrentColIdx != fColumnControl.getSelectionIndex())
					{
						fCurrentColIdx = fColumnControl.getSelectionIndex();
						refreshPreviewPage();
						updateButtons();
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}});

			data = new GridData();
			data.grabExcessHorizontalSpace = false;
			data.horizontalAlignment = SWT.BEGINNING;
			fColumnControl.setLayoutData(data);

			unit = new Label(composite, SWT.NONE);
			unit.setText(DebugUIMessages.FormatTableRenderingAction_5);

			populateDialog();

			fDefaultButton = new Button(composite, SWT.NONE);
			fDefaultButton.setText(DebugUIMessages.FormatTableRenderingAction_8);
			data = new GridData();
			data.grabExcessHorizontalSpace = true;
			data.grabExcessVerticalSpace = true;
			data.horizontalAlignment = SWT.END;
			data.horizontalSpan = 3;
			fDefaultButton.setLayoutData(data);

			fDefaultButton.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					saveDefaults();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {

				}});

			Group group = new Group(composite, SWT.NONE);
			group.setText(DebugUIMessages.FormatTableRenderingAction_7);
			group.setLayout(new GridLayout());
			data = new GridData();
			data.grabExcessHorizontalSpace = true;
			data.grabExcessVerticalSpace = true;
			data.horizontalAlignment = SWT.FILL;
			data.verticalAlignment = SWT.FILL;
			data.horizontalSpan = 3;
			group.setLayoutData(data);

			fPreviewPageBook = new PageBook(group, SWT.NONE);
			data = new GridData();
			data.grabExcessHorizontalSpace = true;
			data.grabExcessVerticalSpace = true;
			data.horizontalAlignment = SWT.FILL;
			data.verticalAlignment = SWT.FILL;
			fPreviewPageBook.setLayoutData(data);

			int rowSize = fRowSizes[fRowControl.getSelectionIndex()];
			int colSize = fColumnSizes[fColumnControl.getSelectionIndex()];

			fPreivewPage = createPreviewPage(fPreviewPageBook, rowSize, colSize);
			fPreviewPageBook.showPage(fPreivewPage);

			Label defaultRow = new Label(composite, SWT.NONE);
			defaultRow.setText(DebugUIMessages.FormatTableRenderingDialog_0);
			fDefaultRowValue = new Text(composite, SWT.READ_ONLY);

			int defRow = getDefaultRowSize();

			fDefaultRowValue.setText(String.valueOf(defRow));
			Label def = new Label(composite, SWT.NONE);
			def.setText(DebugUIMessages.FormatTableRenderingDialog_1);

			data = new GridData();
			data.grabExcessHorizontalSpace = true;
			data.grabExcessVerticalSpace = true;
			data.horizontalAlignment = SWT.BEGINNING;
			def.setLayoutData(data);


			Label defaultCol = new Label(composite, SWT.NONE);
			defaultCol.setText(DebugUIMessages.FormatTableRenderingDialog_2);
			fDefaultColValue = new Text(composite, SWT.READ_ONLY);

			int defCol = getDefaultColumnSize();

			fDefaultColValue.setText(String.valueOf(defCol));
			def = new Label(composite, SWT.NONE);
			def.setText(DebugUIMessages.FormatTableRenderingDialog_3);

			data = new GridData();
			data.grabExcessHorizontalSpace = true;
			data.grabExcessVerticalSpace = true;
			data.horizontalAlignment = SWT.BEGINNING;
			def.setLayoutData(data);

			Button restoreButton = new Button(composite, SWT.NONE);
			restoreButton.setText(DebugUIMessages.FormatTableRenderingAction_6);
			restoreButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					restoreDefaults();
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}});
			data = new GridData();
			data.grabExcessHorizontalSpace = true;
			data.horizontalAlignment = SWT.END;
			data.horizontalSpan = 3;
			data.verticalAlignment = SWT.CENTER;
			restoreButton.setLayoutData(data);

			return composite;
		}

		@Override
		protected void okPressed() {
			int idx = fColumnControl.getSelectionIndex();
			fColumnSize = fColumnSizes[idx];
			fRowSize = fRowSizes[fRowControl.getSelectionIndex()];
			super.okPressed();
		}

		private void populateDialog()
		{
			int currentColSize = fRendering.getAddressableUnitPerColumn();
			int currentRowSize = fRendering.getAddressableUnitPerLine();
			setCurrentRowColSizes(currentRowSize, currentColSize);
		}

		private int populateControl(int currentSize, int[] searchArray, Combo control) {
			int idx = 0;
			for (int i=0 ;i<searchArray.length; i++)
			{
				if (searchArray[i] == currentSize)
				{
					idx = i;
					break;
				}
			}
			control.select(idx);
			return idx;
		}

		private Control createPreviewPage(Composite parent, int rowSize, int colSize)
		{
			if (!isValid(rowSize, colSize))
			{
				Label label = new Label(parent, SWT.NONE);
				StringBuilder errorMsg = new StringBuilder();
				errorMsg.append(DebugUIMessages.FormatTableRenderingAction_9);
				errorMsg.append("\n"); //$NON-NLS-1$
				errorMsg.append(DebugUIMessages.FormatTableRenderingAction_11);

				if (colSize > rowSize)
				{
					errorMsg.append("\n"); //$NON-NLS-1$
					errorMsg.append(DebugUIMessages.FormatTableRenderingAction_13);
				}

				label.setText(errorMsg.toString());

				return label;
			}

			Table table = new Table(parent, SWT.BORDER);
			table.setHeaderVisible(true);

			int numCol = rowSize/colSize;

			TableColumn addressCol = new TableColumn(table, SWT.NONE);

			TableColumn[] columns = new TableColumn[numCol];
			for (int i=0; i<columns.length; i++)
			{
				columns[i] = new TableColumn(table, SWT.NONE);
			}

			StringBuilder buf = new StringBuilder();
			for (int j=0; j<colSize; j++)
			{
				buf.append("X"); //$NON-NLS-1$
			}

			for (int i = 0; i < 4; i++) {
				TableItem tableItem = new TableItem(table, SWT.NONE);

				String[] text = new String[numCol + 1];
				text[0] = DebugUIMessages.FormatTableRenderingAction_15;
				for (int j=1; j<text.length; j++)
				{
					text[j] = buf.toString();
				}

				tableItem.setText(text);
			}

			addressCol.pack();
			for (TableColumn column : columns) {
				column.pack();
			}


			return table;
		}

		private boolean isValid(int rowSize, int colSize)
		{
			if (rowSize % colSize != 0)
				return false;

			if (colSize > rowSize)
				return false;

			return true;
		}

		private void refreshPreviewPage() {
			fPreivewPage.dispose();

			int rowSize = fRowSizes[fRowControl.getSelectionIndex()];
			int colSize = fColumnSizes[fColumnControl.getSelectionIndex()];
			fPreivewPage = createPreviewPage(fPreviewPageBook, rowSize, colSize);
			fPreviewPageBook.showPage(fPreivewPage);
		}

		private void updateButtons() {
			int rowSize = fRowSizes[fRowControl.getSelectionIndex()];
			int colSize = fColumnSizes[fColumnControl.getSelectionIndex()];
			Button button = getButton(IDialogConstants.OK_ID);
			if (!isValid(rowSize, colSize))
			{
				button.setEnabled(false);
				fDefaultButton.setEnabled(false);


			}
			else
			{
				button.setEnabled(true);
				fDefaultButton.setEnabled(true);
			}
		}

		private String getRowPrefId(String modelId) {
			String rowPrefId = IDebugPreferenceConstants.PREF_ROW_SIZE + ":" + modelId; //$NON-NLS-1$
			return rowPrefId;
		}

		private String getColumnPrefId(String modelId) {
			String colPrefId = IDebugPreferenceConstants.PREF_COLUMN_SIZE + ":" + modelId; //$NON-NLS-1$
			return colPrefId;
		}

		private int getDefaultRowSize()
		{
			int size = -1;
			IPersistableDebugElement elmt = fRendering.getMemoryBlock().getAdapter(IPersistableDebugElement.class);
			if (elmt != null)
			{
				if (elmt.supportsProperty(fRendering, IDebugPreferenceConstants.PREF_ROW_SIZE_BY_MODEL))
					return getDefaultFromPersistableElement(IDebugPreferenceConstants.PREF_ROW_SIZE_BY_MODEL);
			}

			size = getDefaultRowSize(fRendering.getMemoryBlock().getModelIdentifier());

			return size;
		}

		private int getDefaultColumnSize()
		{
			int size = -1;
			IPersistableDebugElement elmt = fRendering.getMemoryBlock().getAdapter(IPersistableDebugElement.class);
			if (elmt != null)
			{
				if (elmt.supportsProperty(fRendering, IDebugPreferenceConstants.PREF_COL_SIZE_BY_MODEL))
					return getDefaultFromPersistableElement(IDebugPreferenceConstants.PREF_COL_SIZE_BY_MODEL);
			}

			size = getDefaultColumnSize(fRendering.getMemoryBlock().getModelIdentifier());
			return size;
		}

		private int getDefaultRowSize(String modelId)
		{
			int row = DebugUITools.getPreferenceStore().getInt(getRowPrefId(modelId));
			if (row == 0)
			{
				DebugUITools.getPreferenceStore().setValue(getRowPrefId(modelId), IDebugPreferenceConstants.PREF_ROW_SIZE_DEFAULT);
				row = DebugUITools.getPreferenceStore().getInt(getRowPrefId(modelId));
			}

			return row;

		}

		private int getDefaultColumnSize(String modelId)
		{
			int col = DebugUITools.getPreferenceStore().getInt(getColumnPrefId(modelId));
			if (col == 0)
			{
				// if not yet defined, initialize with default
				DebugUITools.getPreferenceStore().setValue(getColumnPrefId(modelId), IDebugPreferenceConstants.PREF_COLUMN_SIZE_DEFAULT);
				col = DebugUITools.getPreferenceStore().getInt(getColumnPrefId(modelId));
			}
			return col;
		}

		private void saveDefaults() {
			int columnSize = fColumnSizes[fColumnControl.getSelectionIndex()];
			int rowSize = fRowSizes[fRowControl.getSelectionIndex()];

			IPersistableDebugElement elmt = fRendering.getMemoryBlock().getAdapter(IPersistableDebugElement.class);

			if (elmt != null && elmt.supportsProperty(fRendering, IDebugPreferenceConstants.PREF_ROW_SIZE_BY_MODEL)
				&& elmt.supportsProperty(fRendering, IDebugPreferenceConstants.PREF_COL_SIZE_BY_MODEL))
			{
				try {
					elmt.setProperty(fRendering, IDebugPreferenceConstants.PREF_ROW_SIZE_BY_MODEL, Integer.valueOf(rowSize));
					elmt.setProperty(fRendering, IDebugPreferenceConstants.PREF_COL_SIZE_BY_MODEL, Integer.valueOf(columnSize));
				} catch (CoreException e) {
					DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(), DebugUIMessages.FormatTableRenderingDialog_4, DebugUIMessages.FormatTableRenderingDialog_5, e);
				}
			}
			else
			{
				// save preference
				// find model id
				String modelId = fRendering.getMemoryBlock().getModelIdentifier();

				// constrcut preference id
				String rowPrefId = getRowPrefId(modelId);
				String colPrefId = getColumnPrefId(modelId);

				// save setting
				IPreferenceStore prefStore = DebugUITools.getPreferenceStore();
				prefStore.setValue(rowPrefId, rowSize);
				prefStore.setValue(colPrefId, columnSize);
			}

			fDefaultColValue.setText(String.valueOf(getDefaultColumnSize()));
			fDefaultRowValue.setText(String.valueOf(getDefaultRowSize()));

			fDefaultRowValue.getParent().layout();
		}

		private void restoreDefaults() {

			// Determine the default values by using the following search order:
			// IPersistableDebugElement, workspace preference, plugin defaults
			// issue: 248486
			int defaultRowSize = getDefaultRowSize();
			int defaultColSize = getDefaultColumnSize();

			populateControl(defaultRowSize, fRowSizes, fRowControl);
			populateControl(defaultColSize, fColumnSizes, fColumnControl);

			fCurrentRowIdx = fRowControl.getSelectionIndex();
			fCurrentColIdx = fColumnControl.getSelectionIndex();

			refreshPreviewPage();
			updateButtons();
		}

		public int getColumnSize() {
			return fColumnSize;
		}

		public int getRowSize() {
			return fRowSize;
		}

		public void setCurrentRowColSizes(int row, int col)
		{
			fCurrentColIdx = populateControl(col, fColumnSizes, fColumnControl);
			fCurrentRowIdx = populateControl(row, fRowSizes, fRowControl);
		}

		@Override
		protected Control createButtonBar(Composite parent) {
			Control ret =  super.createButtonBar(parent);
			if (fDisableCancel)
				getButton(IDialogConstants.CANCEL_ID).setEnabled(false);
			updateButtons();
			return ret;
		}

		public void openError(String msg)
		{
			fDisableCancel = true;
			fMsg = msg;
			open();
		}

		private int getDefaultFromPersistableElement(String propertyId) {
			int defaultValue = -1;
			IPersistableDebugElement elmt = fRendering.getMemoryBlock().getAdapter(IPersistableDebugElement.class);
			if (elmt != null)
			{
				try {
					Object valueMB = elmt.getProperty(this, propertyId);
					if (valueMB != null && !(valueMB instanceof Integer))
					{
						IStatus status = DebugUIPlugin.newErrorStatus("Model returned invalid type on " + propertyId, null); //$NON-NLS-1$
						DebugUIPlugin.log(status);
					}

					if (valueMB != null)
					{
						Integer value = (Integer)valueMB;
						defaultValue = value.intValue();
					}
				} catch (CoreException e) {
					DebugUIPlugin.log(e);
				}
			}
			return defaultValue;
		}
	}
