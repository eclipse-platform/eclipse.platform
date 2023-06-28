/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.views.memory.renderings;

import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.PlatformUI;

public class TableRenderingPreferencePage extends PreferencePage implements
	IPropertyChangeListener, SelectionListener{

	private Button fAuto;
	private Button fManual;
	private IntegerFieldEditor fPreBufferSize;
	private IntegerFieldEditor fPostBufferSize;
	private IntegerFieldEditor fPageSize;
	private Group fGroup;
	private Composite fComposite;


	public TableRenderingPreferencePage(String title) {
		super(title);
	}

	@Override
	protected Control createContents(Composite parent) {

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IDebugUIConstants.PLUGIN_ID + ".table_renderings_preference_page_context"); //$NON-NLS-1$

		fComposite = new Composite(parent, SWT.NONE);
		fComposite.setLayout(new GridLayout());
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		fComposite.setLayoutData(data);

		GridData hspanData = new GridData(SWT.FILL, SWT.FILL, true, false);
		hspanData.horizontalSpan = 2;

		SWTFactory.createWrapLabel(fComposite, DebugUIMessages.TableRenderingPreferencePage_10, 2, 300);

		fAuto = new Button(fComposite, SWT.RADIO);
		fAuto.setText(DebugUIMessages.TableRenderingPreferencePage_0);
		fAuto.setLayoutData(hspanData);

		fGroup = new Group(fComposite, SWT.NONE);
		fGroup.setText(DebugUIMessages.TableRenderingPreferencePage_5);
		GridData groupData = new GridData(SWT.FILL, SWT.FILL, true, false);
		groupData.horizontalSpan = 2;
		fGroup.setLayoutData(groupData);
		fPreBufferSize = new IntegerFieldEditor(IDebugPreferenceConstants.PREF_TABLE_RENDERING_PRE_BUFFER_SIZE, DebugUIMessages.TableRenderingPreferencePage_6, fGroup);
		fPreBufferSize.setPreferenceStore(getPreferenceStore());
		fPreBufferSize.load();
		fPostBufferSize = new IntegerFieldEditor(IDebugPreferenceConstants.PREF_TABLE_RENDERING_POST_BUFFER_SIZE, DebugUIMessages.TableRenderingPreferencePage_7, fGroup);
		fPostBufferSize.setPreferenceStore(getPreferenceStore());
		fPostBufferSize.load();

		fManual = new Button(fComposite, SWT.RADIO);
		fManual.setText(DebugUIMessages.TableRenderingPreferencePage_8);
		fManual.setLayoutData(hspanData);

		fPageSize = new IntegerFieldEditor(IDebugPreferenceConstants.PREF_TABLE_RENDERING_PAGE_SIZE, DebugUIMessages.TableRenderingPreferencePage_2, fComposite);
		fPageSize.setPreferenceStore(getPreferenceStore());
		fPageSize.load();

		fPreBufferSize.setPropertyChangeListener(this);
		fPostBufferSize.setPropertyChangeListener(this);
		fPageSize.setPropertyChangeListener(this);

		fAuto.addSelectionListener(this);
		fManual.addSelectionListener(this);

		loadLoadingModeFromPreference();
		updateTextEditorsEnablement();

		return fComposite;
	}

	/**
	 *
	 */
	private void loadLoadingModeFromPreference() {
		boolean isAuto = getPreferenceStore().getBoolean(IDebugPreferenceConstants.PREF_DYNAMIC_LOAD_MEM);
		fAuto.setSelection(isAuto);
		fManual.setSelection(!isAuto);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(FieldEditor.VALUE))
		{
			if (event.getSource().equals(fPageSize) ||
				event.getSource().equals(fPostBufferSize) ||
				event.getSource().equals(fPreBufferSize))
			{
				validatePageSize();
			}
		}

	}

	private void validatePageSize() {
		boolean autoLoad = fAuto.getSelection();
		try {
			int bufferSize = fPageSize.getIntValue();
			int preBuffer = fPreBufferSize.getIntValue();
			int postBuffer = fPostBufferSize.getIntValue();
			if (!autoLoad && bufferSize < 1)
			{
				setValid(false);
				setErrorMessage(DebugUIMessages.TableRenderingPreferencePage_3);
			}
			else if (autoLoad)
			{
				// For auto load mode, we must have have > 1 buffer size
				// otherwise, the rendering cannot be loaded dynamically

				if (preBuffer < 1 || postBuffer < 1)
				{
					setValid(false);
					setErrorMessage(DebugUIMessages.TableRenderingPreferencePage_9);
				}
				else
				{
					setValid(true);
					setErrorMessage(null);
				}
			}
			else
			{
				setValid(true);
				setErrorMessage(null);

			}
		} catch (NumberFormatException e) {
				setValid(false);
				setErrorMessage(DebugUIMessages.TableRenderingPreferencePage_4);
		}
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return DebugUIPlugin.getDefault().getPreferenceStore();
	}

	@Override
	public void dispose() {
		fAuto.removeSelectionListener(this);
		fManual.removeSelectionListener(this);
		fPageSize.setPropertyChangeListener(null);
		fPreBufferSize.setPropertyChangeListener(null);
		fPostBufferSize.setPropertyChangeListener(null);
		super.dispose();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		// do nothing
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		updateTextEditorsEnablement();
	}

	@Override
	public boolean performOk() {
		boolean auto = fAuto.getSelection();
		boolean currentValue = getPreferenceStore().getBoolean(IDebugPreferenceConstants.PREF_DYNAMIC_LOAD_MEM);
		if (auto != currentValue)
			getPreferenceStore().setValue(IDebugPreferenceConstants.PREF_DYNAMIC_LOAD_MEM, auto);

		fPageSize.store();
		fPreBufferSize.store();
		fPostBufferSize.store();
		return super.performOk();
	}

	@Override
	protected void performDefaults() {

		boolean auto = getPreferenceStore().getDefaultBoolean(IDebugPreferenceConstants.PREF_DYNAMIC_LOAD_MEM);
		fAuto.setSelection(auto);
		fManual.setSelection(!auto);
		updateTextEditorsEnablement();

		fPageSize.loadDefault();
		fPreBufferSize.loadDefault();
		fPostBufferSize.loadDefault();
		super.performDefaults();
	}

	/**
	 *
	 */
	private void updateTextEditorsEnablement() {
		boolean auto = fAuto.getSelection();
		fPreBufferSize.setEnabled(auto, fGroup);
		fPostBufferSize.setEnabled(auto, fGroup);
		fPageSize.setEnabled(!auto, fComposite);
	}
}
