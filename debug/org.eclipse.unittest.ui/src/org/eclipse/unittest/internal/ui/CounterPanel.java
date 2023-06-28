/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.unittest.internal.ui;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * A panel with counters for the number of Runs, Errors and Failures.
 */
public class CounterPanel extends Composite {
	protected Text fNumberOfErrors;
	protected Text fNumberOfFailures;
	protected Text fNumberOfRuns;
	protected Integer fTotal;
	protected int fIgnoredCount;
	protected int fAssumptionFailedCount;

	private final Image fErrorIcon = Images.createImage("ovr16/error_ovr.png"); //$NON-NLS-1$
	private final Image fFailureIcon = Images.createImage("ovr16/failed_ovr.png"); //$NON-NLS-1$

	/**
	 * Constructs a CounterPanel object
	 *
	 * @param parent a parent composite
	 */
	public CounterPanel(Composite parent) {
		super(parent, SWT.WRAP);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 9;
		gridLayout.makeColumnsEqualWidth = false;
		gridLayout.marginWidth = 0;
		setLayout(gridLayout);

		fNumberOfRuns = createLabel(Messages.CounterPanel_label_runs, null, " 0/0  "); //$NON-NLS-1$
		fNumberOfErrors = createLabel(Messages.CounterPanel_label_errors, fErrorIcon, " 0 "); //$NON-NLS-1$
		fNumberOfFailures = createLabel(Messages.CounterPanel_label_failures, fFailureIcon, " 0 "); //$NON-NLS-1$

		addDisposeListener(e -> disposeIcons());
	}

	private void disposeIcons() {
		fErrorIcon.dispose();
		fFailureIcon.dispose();
	}

	private Text createLabel(String name, Image image, String init) {
		Label label = new Label(this, SWT.NONE);
		if (image != null) {
			image.setBackground(label.getBackground());
			label.setImage(image);
		}
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		label = new Label(this, SWT.NONE);
		label.setText(name);
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		// label.setFont(JFaceResources.getBannerFont());

		Text value = new Text(this, SWT.READ_ONLY);
		value.setText(init);
		fixReadonlyTextBackground(value);
		value.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING));
		return value;
	}

	/**
	 * Resets the counters presented on the panel
	 */
	public void reset() {
		fTotal = null;
		setErrorValue(0);
		setFailureValue(0);
		setRunValue(0, 0, 0);
	}

	/**
	 * Sets the total count value
	 *
	 * @param value total count value, can be <code>null</code> if unknown yet
	 */
	public void setTotal(Integer value) {
		fTotal = value;
	}

	/**
	 * Sets the run counter values
	 *
	 * @param value                  a run counter value
	 * @param ignoredCount           an ignored tests counter value
	 * @param assumptionFailureCount a number of assumption failure counter value
	 */
	public void setRunValue(int value, int ignoredCount, int assumptionFailureCount) {
		String runString;
		String runStringTooltip;
		String totalString = fTotal == null ? "?" : fTotal.toString(); //$NON-NLS-1$
		if (ignoredCount == 0 && assumptionFailureCount == 0) {
			runString = MessageFormat.format(Messages.CounterPanel_runcount, Integer.toString(value), totalString);
			runStringTooltip = runString;
		} else if (ignoredCount != 0 && assumptionFailureCount == 0) {
			runString = MessageFormat.format(Messages.CounterPanel_runcount_skipped, Integer.toString(value),
					totalString, Integer.toString(ignoredCount));
			runStringTooltip = MessageFormat.format(Messages.CounterPanel_runcount_ignored, Integer.toString(value),
					totalString, Integer.toString(ignoredCount));
		} else if (ignoredCount == 0 && assumptionFailureCount != 0) {
			runString = MessageFormat.format(Messages.CounterPanel_runcount_skipped, Integer.toString(value),
					totalString, Integer.toString(assumptionFailureCount));
			runStringTooltip = MessageFormat.format(Messages.CounterPanel_runcount_assumptionsFailed,
					Integer.toString(value), totalString, Integer.toString(assumptionFailureCount));
		} else {
			runString = MessageFormat.format(Messages.CounterPanel_runcount_skipped, Integer.toString(value),
					totalString, Integer.toString(ignoredCount + assumptionFailureCount));
			runStringTooltip = MessageFormat.format(Messages.CounterPanel_runcount_ignored_assumptionsFailed,
					Integer.toString(value), totalString, Integer.toString(ignoredCount),
					Integer.toString(assumptionFailureCount));
		}
		fNumberOfRuns.setText(runString);
		fNumberOfRuns.setToolTipText(runStringTooltip);

		if (fIgnoredCount == 0 && ignoredCount > 0 || fIgnoredCount != 0 && ignoredCount == 0) {
			layout();
		} else if (fAssumptionFailedCount == 0 && assumptionFailureCount > 0
				|| fAssumptionFailedCount != 0 && assumptionFailureCount == 0) {
			layout();
		} else {
			fNumberOfRuns.redraw();
			redraw();
		}
		fIgnoredCount = ignoredCount;
		fAssumptionFailedCount = assumptionFailureCount;
	}

	/**
	 * Sets an error counter value
	 *
	 * @param value am error counter value
	 */
	public void setErrorValue(int value) {
		fNumberOfErrors.setText(Integer.toString(value));
		redraw();
	}

	/**
	 * Sets a failure counter value
	 *
	 * @param value a failure counter value
	 */
	public void setFailureValue(int value) {
		fNumberOfFailures.setText(Integer.toString(value));
		redraw();
	}

	/**
	 * Fixes https://bugs.eclipse.org/71765 by setting the background color to
	 * {@code SWT.COLOR_WIDGET_BACKGROUND}.
	 * <p>
	 * Should be applied to all SWT.READ_ONLY Texts in dialogs (or at least those
	 * which don't have an SWT.BORDER). Search regex:
	 * {@code new Text\([^,]+,[^\)]+SWT\.READ_ONLY}
	 *
	 * @param textField the text field
	 */
	public static void fixReadonlyTextBackground(Text textField) {
		textField.setBackground(textField.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
	}
}
