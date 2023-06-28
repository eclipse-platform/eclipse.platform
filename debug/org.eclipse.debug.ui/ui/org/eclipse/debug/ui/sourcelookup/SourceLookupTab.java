/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others.
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
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.ui.sourcelookup;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupPanel;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupUIMessages;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * A launch configuration tab that displays and edits the source
 * lookup path for a launch configuration. This tab works with the
 * debug platform source lookup facilities - a source lookup director
 * with associated participants and source containers.
 * <p>
 * Clients may call {@link #setHelpContextId(String)} on this tab prior to control
 * creation to alter the default context help associated with this tab.
 * </p>
 * <p>
 * This tab may be instantiated.
 * </p>
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */

public class SourceLookupTab extends AbstractLaunchConfigurationTab {
	//the panel displaying the containers
	private SourceLookupPanel fSourceLookupPanel;

	/**
	 * Constructs a new tab with default context help.
	 */
	public SourceLookupTab() {
		super();
		setHelpContextId(IDebugHelpContextIds.SOURCELOOKUP_TAB);
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), getHelpContextId());
		GridLayout topLayout = new GridLayout();
		topLayout.marginWidth = 0;
		topLayout.marginHeight = 0;
		topLayout.numColumns = 1;
		comp.setLayout(topLayout);
		comp.setFont(parent.getFont());

		fSourceLookupPanel = new SourceLookupPanel();
		fSourceLookupPanel.setLaunchConfigurationDialog(
				getLaunchConfigurationDialog());
		fSourceLookupPanel.createControl(comp);
		GridData gd = (GridData) fSourceLookupPanel.getControl().getLayoutData();
		gd.heightHint = 200;
		gd.widthHint = 250;
		Dialog.applyDialogFont(comp);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {

	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		fSourceLookupPanel.initializeFrom(configuration);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		fSourceLookupPanel.performApply(configuration);
	}

	@Override
	public String getId() {
		return "org.eclipse.debug.ui.sourceLookupTab"; //$NON-NLS-1$
	}

	@Override
	public String getName() {
		return SourceLookupUIMessages.sourceTab_tabTitle;
	}

	@Override
	public Image getImage() {
		return DebugPluginImages.getImage(IInternalDebugUIConstants.IMG_SRC_LOOKUP_TAB);
	}

	@Override
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		fSourceLookupPanel.activated(workingCopy);
	}

	@Override
	public void dispose() {
		if (fSourceLookupPanel != null) {
			if (fSourceLookupPanel.getDirector() != null) {
				fSourceLookupPanel.getDirector().dispose();
			}
			fSourceLookupPanel.dispose();
		}
		fSourceLookupPanel = null;
		super.dispose();
	}

	@Override
	public String getErrorMessage() {
		if (fSourceLookupPanel != null) {
			return fSourceLookupPanel.getErrorMessage();
		}
		return super.getErrorMessage();
	}

	@Override
	public String getMessage() {
		if (fSourceLookupPanel != null) {
			return fSourceLookupPanel.getMessage();
		}
		return super.getMessage();
	}

	/**
	 * @since 3.13
	 */
	@Override
	protected void initializeAttributes() {
		super.initializeAttributes();
		getAttributesLabelsForPrototype().put(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, SourceLookupUIMessages.sourceTab_AttributeLabel_SourceLocatorMemento);
		getAttributesLabelsForPrototype().put(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, SourceLookupUIMessages.sourceTab_AttributeLabel_SourceLocatorID);
	}
}
