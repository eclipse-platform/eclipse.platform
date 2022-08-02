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
package org.eclipse.team.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * This class provides facilities to allow common widget groupings to be shared
 * by multiple dialogs or wizards.
 */
public abstract class DialogArea {
	private FontMetrics fontMetrics;
	private List<IPropertyChangeListener> listeners;

	/**
	 * Create a dialog area
	 */
	protected DialogArea() {
		this.listeners = new ArrayList<>();
	}

	/**
	 * Listener for property change events. The only event of interest is for
	 * property SELECTED_WORKING_SET which contains the selected working set or
	 * <code>null</code> if none is selected.
	 *
	 * @param listener
	 */
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	/**
	 * Remove the provided listener from the receiver.
	 *
	 * @param listener
	 */
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		listeners.remove(listener);
	}

	protected void firePropertyChangeChange(String property, Object oldValue, Object newValue) {
		PropertyChangeEvent event = new PropertyChangeEvent(this, property, oldValue, newValue);
		for (IPropertyChangeListener listener : listeners) {
			listener.propertyChange(event);
		}
	}

	/**
	 * Code copied from <code>org.eclipse.jface.dialogs.Dialog</code> to obtain
	 * a FontMetrics.
	 *
	 * @param control a control from which to obtain the current font
	 *
	 * @see org.eclipse.jface.dialogs.Dialog
	 */
	protected void initializeDialogUnits(Control control) {
		// Compute and store a font metric
		GC gc = new GC(control);
		gc.setFont(control.getFont());
		fontMetrics = gc.getFontMetrics();
		gc.dispose();
	}

	/**
	 * Create the area using the given parent as the containing composite
	 * @param parent
	 */
	public abstract void createArea(Composite parent);

	protected Button createCheckbox(Composite parent, String label, int span) {
		Button button = new Button(parent, SWT.CHECK | SWT.LEFT);
		button.setText(label);
		button.setFont(parent.getFont());
		GridData data = new GridData();
		data.horizontalSpan = span;
		button.setLayoutData(data);
		return button;
	}

	protected Button createButton(Composite parent, String label, int style) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);
		// we need to explicitly set the font to the parent's font for dialogs
		button.setFont(parent.getFont());
		GridData data = new GridData(style);
		data.heightHint = Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.BUTTON_HEIGHT);
		int widthHint = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		button.setLayoutData(data);
		return button;
	}

	protected Button createRadioButton(Composite parent, String label, int span) {
		Button button = new Button(parent, SWT.RADIO);
		button.setText(label);
		GridData data = new GridData();
		data.horizontalSpan = span;
		button.setLayoutData(data);
		return button;
	}
	protected Label createWrappingLabel(Composite parent, String text, int horizontalSpan) {
		Label label = new Label(parent, SWT.LEFT | SWT.WRAP);
		label.setText(text);
		label.setFont(parent.getFont());
		GridData data = new GridData();
		data.horizontalSpan = horizontalSpan;
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.widthHint= 0;
		label.setLayoutData(data);
		return label;
	}
	protected Label createLabel(Composite parent, String text, int horizontalSpan) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = horizontalSpan;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}
	/**
	 * Creates composite control and sets the default layout data.
	 * @param parent  the parent of the new composite
	 * @param numColumns  the number of columns for the new composite
	 * @param grab specify whether the composite should grab for excessive space in both directions.
	 * @return the newly-created coposite
	 */
	protected Composite createComposite(Composite parent, int numColumns, boolean grab) {
		final Composite composite = new Composite(parent, SWT.NULL);
		final Font font = parent.getFont();
		composite.setFont(font);

		composite.setLayout(new GridLayout(numColumns, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, grab, grab));

		return composite;
	}

	/**
	 * Creates composite control and sets the default layout data.
	 *
	 * @param parent  the parent of the new composite
	 * @param numColumns  the number of columns for the new composite
	 * @return the newly-created coposite
	 */
	protected Composite createGrabbingComposite(Composite parent, int numColumns) {
		Composite composite = new Composite(parent, SWT.NULL);
		Font font = parent.getFont();
		composite.setFont(font);

		// GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);

		// GridData
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		composite.setLayoutData(data);
		return composite;
	}

	protected int convertVerticalDLUsToPixels(int dlus) {
		return Dialog.convertVerticalDLUsToPixels(fontMetrics, dlus);
	}

	protected int convertHorizontalDLUsToPixels(int dlus) {
		return Dialog.convertHorizontalDLUsToPixels(fontMetrics, dlus);
	}
}
