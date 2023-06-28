/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.debug.examples.core.midi.launcher.ClockControl;
import org.eclipse.debug.examples.core.midi.launcher.TempoControl;
import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.debug.ui.IDetailPaneFactory;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Creates detail panes for sequencer controls.
 *
 * @since 1.0
 */
public class ControlDetailPaneFactory implements IDetailPaneFactory {

	/**
	 * Identifier for the tempo slider detail pane
	 */
	public static final String ID_TEMPO_SLIDER = "TEMPO_SLIDER"; //$NON-NLS-1$

	/**
	 * Identifier for the clock slider detail pane
	 */
	public static final String ID_CLOCK_SLIDER = "CLOCK_SLIDER"; //$NON-NLS-1$

	@Override
	public IDetailPane createDetailPane(String paneID) {
		if (ID_TEMPO_SLIDER.equals(paneID)) {
			return new TempoSliderDetailPane();
		}
		if (ID_CLOCK_SLIDER.equals(paneID)) {
			return new ClockSliderDetailPane();
		}
		return null;
	}

	@Override
	public String getDefaultDetailPane(IStructuredSelection selection) {
		if (selection.size() == 1) {
			Object element = selection.getFirstElement();
			if (element instanceof TempoControl) {
				return ID_TEMPO_SLIDER;
			}
			if (element instanceof ClockControl) {
				return ID_CLOCK_SLIDER;
			}
		}
		return null;
	}

	@Override
	public String getDetailPaneDescription(String paneID) {
		if (ID_TEMPO_SLIDER.equals(paneID)) {
			return "Tempo Slider"; //$NON-NLS-1$
		}
		if (ID_CLOCK_SLIDER.equals(paneID)) {
			return "Clock Slider"; //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public String getDetailPaneName(String paneID) {
		if (ID_TEMPO_SLIDER.equals(paneID)) {
			return "Tempo Slider"; //$NON-NLS-1$
		}
		if (ID_CLOCK_SLIDER.equals(paneID)) {
			return "Clock Slider"; //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public Set<String> getDetailPaneTypes(IStructuredSelection selection) {
		Set<String> set = new HashSet<>();
		if (selection.size() == 1) {
			Object element = selection.getFirstElement();
			if (element instanceof TempoControl) {
				set.add(ID_TEMPO_SLIDER);
			}
			if (element instanceof ClockControl) {
				set.add(ID_CLOCK_SLIDER);
			}
		}
		return set;
	}

}
