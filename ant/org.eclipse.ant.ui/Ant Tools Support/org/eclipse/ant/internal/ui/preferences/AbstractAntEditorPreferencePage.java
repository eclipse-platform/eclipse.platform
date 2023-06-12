/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.preferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public abstract class AbstractAntEditorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private OverlayPreferenceStore fOverlayStore;
	protected List<IStatus> fStatusList;
	private boolean fInitialized = false;

	private Map<Button, String> fCheckBoxes = new HashMap<>();
	private SelectionListener fCheckBoxListener = new SelectionListener() {
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// do nothing
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			Button button = (Button) e.widget;
			fOverlayStore.setValue(fCheckBoxes.get(button), button.getSelection());
		}
	};

	private Map<Text, String> fTextFields = new HashMap<>();
	private ModifyListener fTextFieldListener = e -> {
		if (fInitialized) {
			Text text = (Text) e.widget;
			fOverlayStore.setValue(fTextFields.get(text), text.getText());
		}
	};

	private Map<Text, String[]> fNumberFields = new HashMap<>();
	private ModifyListener fNumberFieldListener = e -> {
		if (fInitialized) {
			numberFieldChanged((Text) e.widget);
		}
	};

	public AbstractAntEditorPreferencePage() {
		super();
		setPreferenceStore(AntUIPlugin.getDefault().getPreferenceStore());
		fOverlayStore = createOverlayStore();
	}

	protected abstract OverlayPreferenceStore createOverlayStore();

	@Override
	public void init(IWorkbench workbench) {
		// do nothing
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), getHelpContextId());
	}

	abstract protected String getHelpContextId();

	protected void initializeFields() {
		Map<Button, String> checkBoxes = getCheckBoxes();
		Map<Text, String> textFields = getTextFields();
		for (Button b : checkBoxes.keySet()) {
			String key = checkBoxes.get(b);
			b.setSelection(getOverlayStore().getBoolean(key));
		}
		for (Text t : textFields.keySet()) {
			String key = textFields.get(t);
			t.setText(getOverlayStore().getString(key));
		}
		fInitialized = true;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean performOk() {
		getOverlayStore().propagate();
		AntUIPlugin.getDefault().savePluginPreferences();
		return true;
	}

	protected OverlayPreferenceStore getOverlayStore() {
		return fOverlayStore;
	}

	protected OverlayPreferenceStore setOverlayStore() {
		return fOverlayStore;
	}

	protected Map<Button, String> getCheckBoxes() {
		return fCheckBoxes;
	}

	protected Map<Text, String> getTextFields() {
		return fTextFields;
	}

	protected Map<Text, String[]> getNumberFields() {
		return fNumberFields;
	}

	@Override
	protected void performDefaults() {
		getOverlayStore().loadDefaults();
		initializeFields();
		handleDefaults();
		super.performDefaults();
	}

	protected abstract void handleDefaults();

	@Override
	public void dispose() {
		if (getOverlayStore() != null) {
			getOverlayStore().stop();
			fOverlayStore = null;
		}
		super.dispose();
	}

	protected Button addCheckBox(Composite parent, String labelText, String key, int indentation) {
		Button checkBox = new Button(parent, SWT.CHECK);
		checkBox.setText(labelText);
		checkBox.setFont(parent.getFont());

		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent = indentation;
		gd.horizontalSpan = 2;
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(fCheckBoxListener);

		getCheckBoxes().put(checkBox, key);

		return checkBox;
	}

	protected Text addTextField(Composite composite, String labelText, String key, int textLimit, int indentation, String[] errorMessages) {
		Font font = composite.getFont();

		Label label = new Label(composite, SWT.NONE);
		label.setText(labelText);
		label.setFont(font);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent = indentation;
		label.setLayoutData(gd);

		Text textControl = new Text(composite, SWT.BORDER | SWT.SINGLE);
		textControl.setFont(font);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.widthHint = convertWidthInCharsToPixels(textLimit + 1);
		textControl.setLayoutData(gd);
		textControl.setTextLimit(textLimit);
		getTextFields().put(textControl, key);
		if (errorMessages != null) {
			getNumberFields().put(textControl, errorMessages);
			textControl.addModifyListener(fNumberFieldListener);
		} else {
			textControl.addModifyListener(fTextFieldListener);
		}

		return textControl;
	}

	private void numberFieldChanged(Text textControl) {
		String number = textControl.getText();
		IStatus status = validatePositiveNumber(number, getNumberFields().get(textControl));
		if (!status.matches(IStatus.ERROR)) {
			getOverlayStore().setValue(getTextFields().get(textControl), number);
		}
		updateStatus(status);
	}

	private IStatus validatePositiveNumber(String number, String[] errorMessages) {
		StatusInfo status = new StatusInfo();
		if (number.length() == 0) {
			status.setError(errorMessages[0]);
		} else {
			try {
				int value = Integer.parseInt(number);
				if (value < 0)
					status.setError(MessageFormat.format(errorMessages[1], new Object[] { number }));
			}
			catch (NumberFormatException e) {
				status.setError(MessageFormat.format(errorMessages[1], new Object[] { number }));
			}
		}
		return status;
	}

	protected void updateStatus(IStatus status) {
		if (!status.matches(IStatus.ERROR)) {
			for (Text text : getNumberFields().keySet()) {
				IStatus s = validatePositiveNumber(text.getText(), getNumberFields().get(text));
				status = s.getSeverity() > status.getSeverity() ? s : status;
			}
		}

		List<IStatus> statusList = getStatusList();
		if (statusList != null) {
			List<IStatus> temp = new ArrayList<>(statusList.size() + 1);
			temp.add(status);
			temp.addAll(statusList);
			status = getMostSevere(temp);
		}
		setValid(!status.matches(IStatus.ERROR));
		applyToStatusLine(this, status);
	}

	protected List<IStatus> getStatusList() {
		return fStatusList;
	}

	/**
	 * Finds the most severe status from an array of status. An error is more severe than a warning, and a warning is more severe than OK.
	 */
	private IStatus getMostSevere(List<IStatus> statusList) {
		IStatus max = null;
		for (IStatus curr : statusList) {
			if (curr.matches(IStatus.ERROR)) {
				return curr;
			}
			if (max == null || curr.getSeverity() > max.getSeverity()) {
				max = curr;
			}
		}
		return max;
	}

	/*
	 * Applies the status to the status line of a dialog page.
	 */
	private void applyToStatusLine(DialogPage page, IStatus status) {
		String message = status.getMessage();
		switch (status.getSeverity()) {
			case IStatus.OK:
				page.setMessage(message, IMessageProvider.NONE);
				page.setErrorMessage(null);
				break;
			case IStatus.WARNING:
				page.setMessage(message, IMessageProvider.WARNING);
				page.setErrorMessage(null);
				break;
			case IStatus.INFO:
				page.setMessage(message, IMessageProvider.INFORMATION);
				page.setErrorMessage(null);
				break;
			default:
				if (message.length() == 0) {
					message = null;
				}
				page.setMessage(null);
				page.setErrorMessage(message);
				break;
		}
	}

	/**
	 * Returns an array of size 2: - first element is of type <code>Label</code> - second element is of type <code>Text</code> Use
	 * <code>getLabelControl</code> and <code>getTextControl</code> to get the 2 controls.
	 */
	protected Control[] addLabelledTextField(Composite composite, String label, String key, int textLimit, int indentation, String[] errorMessages) {
		Label labelControl = new Label(composite, SWT.NONE);
		labelControl.setText(label);
		labelControl.setFont(composite.getFont());
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent = indentation;
		labelControl.setLayoutData(gd);

		Text textControl = new Text(composite, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		if (textLimit > -1) {
			gd.widthHint = convertWidthInCharsToPixels(textLimit + 1);
			textControl.setTextLimit(textLimit);
		} else {
			gd.widthHint = convertWidthInCharsToPixels(50);
		}
		textControl.setLayoutData(gd);
		textControl.setFont(composite.getFont());
		fTextFields.put(textControl, key);
		if (errorMessages != null) {
			fNumberFields.put(textControl, errorMessages);
			textControl.addModifyListener(fNumberFieldListener);
		} else {
			textControl.addModifyListener(fTextFieldListener);
		}

		return new Control[] { labelControl, textControl };
	}

	protected String loadPreviewContentFromFile(String filename) {
		String line;
		String separator = System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuilder buffer = new StringBuilder(512);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filename)))) {
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
				buffer.append(separator);
			}
		}
		catch (IOException io) {
			AntUIPlugin.log(io);
		}
		return buffer.toString();
	}

	protected Label getLabelControl(Control[] labelledTextField) {
		return (Label) labelledTextField[0];
	}

	protected Text getTextControl(Control[] labelledTextField) {
		return (Text) labelledTextField[1];
	}
}
