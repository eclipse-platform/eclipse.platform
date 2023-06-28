/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
package org.eclipse.debug.examples.ui.midi.detailpanes;

import org.eclipse.debug.examples.core.midi.launcher.ClockControl;
import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * A slider to control the clock position.
 *
 * @since 1.0
 */
public class ClockSliderDetailPane implements IDetailPane {

	private Slider fSlider;
	private ClockControl fControl;

	@Override
	public Control createControl(Composite parent) {
		fSlider = new Slider(parent, SWT.HORIZONTAL);
		fSlider.setMinimum(0);
		fSlider.setMaximum(1000);
		fSlider.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				int selection = fSlider.getSelection();
				if (fControl != null) {
					fControl.setValue(Integer.toString(selection));
				}
			}
		});
		return fSlider;
	}

	@Override
	public void display(IStructuredSelection selection) {
		fControl = null;
		if (selection == null || selection.isEmpty()) {
			fSlider.setEnabled(false);
		} else {
			fSlider.setEnabled(true);
			fControl = (ClockControl) selection.getFirstElement();
			int max = (int)fControl.getSequencer().getMicrosecondLength() / 1000000;
			long micro = fControl.getSequencer().getMicrosecondPosition();
			int seconds = (int) micro / 1000000;
			fSlider.setMaximum(max);
			fSlider.setSelection(seconds);
		}
	}

	@Override
	public void dispose() {
	}

	@Override
	public String getDescription() {
		return "Location (seconds)"; //$NON-NLS-1$
	}

	@Override
	public String getID() {
		return ControlDetailPaneFactory.ID_CLOCK_SLIDER;
	}

	@Override
	public String getName() {
		return "Clock Slider (seconds)"; //$NON-NLS-1$
	}

	@Override
	public void init(IWorkbenchPartSite partSite) {
	}

	@Override
	public boolean setFocus() {
		fSlider.setFocus();
		return true;
	}

}
