/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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
package org.eclipse.debug.internal.examples.mixedmode;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 *
 */
public class DoNothingMainTab extends AbstractLaunchConfigurationTab implements ILaunchConfigurationListener {

	protected Button fButton = null;
	protected Set<String> fOptions = null;

	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_BOTH);
		fButton = SWTFactory.createCheckButton(comp, Messages.DoNothingMainTab_0, null, false, 1);
		fButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}

		});
		setControl(comp);
		getLaunchManager().addLaunchConfigurationListener(this);
	}

	@Override
	public String getName() {
		return Messages.DoNothingMainTab_1;
	}

	/**
	 * @return the set of modes this tab works with
	 */
	protected Set<String> getModes() {
		Set<String> set = new HashSet<>();
		set.add("profile"); //$NON-NLS-1$
		return set;
	}

	@Override
	public void dispose() {
		getLaunchManager().removeLaunchConfigurationListener(this);
		super.dispose();
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			Set<String> modes = configuration.getModes();
			if (modes != null) {
				modes.add(getLaunchConfigurationDialog().getMode());
				fButton.setSelection(getModes().containsAll(modes));
			} else {
				fButton.setSelection(false);
			}
		} catch (CoreException ce) {
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		if (fButton.getSelection()) {
			configuration.addModes(getModes());
		} else {
			configuration.removeModes(getModes());
		}
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
	}

	@Override
	public String getId() {
		return "org.eclipse.debug.examples.mixedmode.main.tab"; //$NON-NLS-1$
	}

	@Override
	public void launchConfigurationChanged(ILaunchConfiguration configuration) {
		try {
			Set<String> modes = configuration.getModes();
			modes.add(getLaunchConfigurationDialog().getMode());
			if (!fButton.isDisposed()) {
				fButton.getDisplay().syncExec(() -> {
					boolean sel = fButton.getSelection();
					if (!sel & modes.containsAll(getModes())) {
						fButton.setSelection(true);
					} else if (sel & !modes.containsAll(getModes())) {
						fButton.setSelection(false);
					}
				});
			}
		} catch (CoreException ce) {
			DebugUIPlugin.log(ce);
		}
	}

	/**
	 * @param configuration
	 */
	@Override
	public void launchConfigurationAdded(ILaunchConfiguration configuration) {
	}

	/**
	 * @param configuration
	 */
	@Override
	public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
	}
}
