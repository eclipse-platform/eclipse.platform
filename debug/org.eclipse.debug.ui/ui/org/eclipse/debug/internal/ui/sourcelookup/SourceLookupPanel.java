/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
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
 *     QNX Software Systems - Mikhail Khodjaiants - Bug 89748
 *******************************************************************************/
package org.eclipse.debug.internal.ui.sourcelookup;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.DefaultSourceContainer;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.WorkingSetSourceContainer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

/**
 * The panel that contains the list of source containers.
 *
 * @since 3.0
 */
public class SourceLookupPanel extends AbstractLaunchConfigurationTab implements IPropertyChangeListener {
	//the configuration being edited
	protected ILaunchConfiguration fConfig;
	//the viewer displaying the containers
	protected SourceContainerViewer fPathViewer;
	//the duplicates checkbox
	protected Button fDuplicatesButton;
	//the source actions - up, down, add, remove, restore
	protected List<SourceContainerAction> fActions = new ArrayList<>(6);
	//the director that will be used by the tab to manage/store the containers
	protected ISourceLookupDirector fLocator;

	protected AddContainerAction fAddAction;
	protected EditContainerAction fEditAction;
	protected RestoreDefaultAction fRestoreDefaultAction;

	/**
	 * Creates and returns the source lookup control.
	 *
	 * @param parent the parent widget of this control
	 */
	@Override
	public void createControl(Composite parent) {
		Font font = parent.getFont();
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);
		SWTFactory.createLabel(comp, SourceLookupUIMessages.sourceTab_lookupLabel, 2);

		fPathViewer = new SourceContainerViewer(comp, this);
		fPathViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		fPathViewer.getControl().setFont(font);

		Composite pathButtonComp = SWTFactory.createComposite(comp, comp.getFont(), 1, 1, GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL, 0, 0);

		SWTFactory.createVerticalSpacer(comp, 2);

		fDuplicatesButton = SWTFactory.createCheckButton(comp, SourceLookupUIMessages.sourceTab_searchDuplicateLabel, null, false, 2);
		fDuplicatesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		});

		fAddAction = new AddContainerAction();
		Button button = SWTFactory.createPushButton(pathButtonComp, fAddAction.getText(), null);
		fAddAction.setButton(button);
		addAction(fAddAction);

		fEditAction = new EditContainerAction();
		button = SWTFactory.createPushButton(pathButtonComp, fEditAction.getText(), null);
		fEditAction.setButton(button);
		addAction(fEditAction);

		SourceContainerAction action = new RemoveAction();
		button = SWTFactory.createPushButton(pathButtonComp, action.getText(), null);
		action.setButton(button);
		addAction(action);

		action = new UpAction();
		button = SWTFactory.createPushButton(pathButtonComp, action.getText(), null);
		action.setButton(button);
		addAction(action);

		action = new DownAction();
		button = SWTFactory.createPushButton(pathButtonComp, action.getText(), null);
		action.setButton(button);
		addAction(action);

		fRestoreDefaultAction = new RestoreDefaultAction();
		button = SWTFactory.createPushButton(pathButtonComp, fRestoreDefaultAction.getText(), null);
		fRestoreDefaultAction.setButton(button);
		addAction(fRestoreDefaultAction);

		retargetActions(fPathViewer);

		//listen to changes user made to the working sets, if a working set is being removed
		//check current list to validate working sets
		IWorkingSetManager workingSetMgr = PlatformUI.getWorkbench().getWorkingSetManager();
		workingSetMgr.addPropertyChangeListener(this);
		/*Dialog.applyDialogFont(comp);*/
		setControl(comp);
	}

	/**
	 * Adds the given action to the action collection in this tab
	 */
	protected void addAction(SourceContainerAction action) {
		fActions.add(action);
	}

	/**
	 * Re-targets actions to the given viewer
	 */
	protected void retargetActions(SourceContainerViewer viewer) {
		for (SourceContainerAction action : fActions) {
			action.setViewer(viewer);
		}
	}

	/**
	 * Initializes this control based on the settings in the given
	 * launch configuration.
	 */
	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		if (fLocator != null) {
			fLocator.dispose();
			fLocator = null;
		}
		setErrorMessage(null);
		setMessage(null);
		String memento = null;
		String type = null;
		try{
			memento = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, (String)null);
			type = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, (String)null);
			if (type == null) {
				type = configuration.getType().getSourceLocatorId();
			}
		} catch(CoreException e){
			setErrorMessage(e.getMessage());
			return;
		}

		if(type == null) {
			setErrorMessage(SourceLookupUIMessages.sourceLookupPanel_2);
			return;
		}

		boolean migration = false;
		try {
			ISourceLocator locator = getLaunchManager().newSourceLocator(type);
			if(!(locator instanceof AbstractSourceLookupDirector)) {
				// migrate to the new source lookup infrastructure
				memento = null; // don't use old memento
				type = configuration.getType().getSourceLocatorId();
				if(type == null) {
					setErrorMessage(SourceLookupUIMessages.sourceLookupPanel_2);
					return;
				}
				locator = getLaunchManager().newSourceLocator(type);
				if (!(locator instanceof AbstractSourceLookupDirector)) {
					setErrorMessage(SourceLookupUIMessages.sourceLookupPanel_2);
					return;
				}
				migration = true;
			}
			fLocator = (AbstractSourceLookupDirector)locator;
			if (memento == null) {
				fLocator.initializeDefaults(configuration);
			} else {
				fLocator.initializeFromMemento(memento, configuration);
			}
		} catch (CoreException e) {
			setErrorMessage(e.getMessage());
			return;
		}
		initializeFrom(fLocator);
		if (migration && configuration.isWorkingCopy()) {
			// ensure perform apply actual updates the config
			setDirty(true);
			performApply((ILaunchConfigurationWorkingCopy)configuration);
		}
	}

	/**
	 * Initializes this control based on the settings in the given
	 * AbstractSourceLookupDirector
	 */
	public void initializeFrom(ISourceLookupDirector locator) {
		if(fConfig == null) {
			fConfig = locator.getLaunchConfiguration();
		}
		fPathViewer.setEntries(locator.getSourceContainers());
		fDuplicatesButton.setSelection(locator.isFindDuplicates());
		fLocator = locator;
		fAddAction.setSourceLookupDirector(locator);
		fEditAction.setSourceLookupDirector(locator);
		fRestoreDefaultAction.setSourceLookupDirector(locator);
		setDirty(false);
	}

	/**
	 * Saves the containers and duplicate policy into the given working copy of the configuration.
	 * Saving the configuration will result in a change event, which will be picked up by the director
	 * and used to refresh its internal list.
	 *
	 * @param workingCopy the working copy of the configuration that these values should be stored in, may be null.
	 * 	If null, will be written into a working copy of the configuration referenced by the director.
	 */
	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		if (isDirty()) {
			if (fLocator == null) {
				configuration.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, (String)null);
				configuration.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, (String)null);
				return;
			}
			ILaunchConfigurationWorkingCopy workingCopy = configuration;
			if(configuration == null) {
				try {
					ILaunchConfiguration config = fLocator.getLaunchConfiguration();
					if(config != null) {
						workingCopy = config.getWorkingCopy();
					}
				}
				catch(CoreException e) {
					DebugUIPlugin.log(e);
					setErrorMessage(SourceLookupUIMessages.sourceLookupPanel_1);
					return;
				}
			}
			if(workingCopy == null) {
				DebugUIPlugin.logErrorMessage("Error occurred - a working copy could not be acquired, therefore source lookup path changes will not be applied.");  //$NON-NLS-1$
				return;
			}
			//set new values in director so memento returned is correct
			fLocator.setSourceContainers(fPathViewer.getEntries());
			fLocator.setFindDuplicates(fDuplicatesButton.getSelection());

			//writing to the file will cause a change event and the listeners will be updated
			try {
				if (isDefault()) {
					workingCopy.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, (String)null);
					workingCopy.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, (String)null);
				} else {
					workingCopy.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, fLocator.getMemento());
					workingCopy.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, fLocator.getId());
				}
			}
			catch(CoreException e) {
				DebugUIPlugin.log(e);
				setErrorMessage(SourceLookupUIMessages.sourceLookupPanel_1);
			}

		}
	}

	/**
	 * determines of the current source lookup path is the default path
	 * @param configuration
	 * @return
	 */
	protected boolean isDefault() {
		ISourceContainer[] current = getEntries();
		return !fDuplicatesButton.getSelection() && current.length == 1 && current[0] instanceof DefaultSourceContainer;
	}

	/**
	 * Returns the entries visible in the viewer
	 */
	public ISourceContainer[] getEntries() {
		return fPathViewer.getEntries();
	}

	@Override
	public void setDirty(boolean dirty) {
		super.setDirty(dirty);

	}

	@Override
	public String getName() {
		return SourceLookupUIMessages.sourceTab_tabTitle;
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {}

	@Override
	protected void updateLaunchConfigurationDialog() {
		if (getLaunchConfigurationDialog() != null) {
			super.updateLaunchConfigurationDialog();
		}
	}

	/**
	 * This is called whenever a working set has been removed. Loops through the original list
	 * of working sets the user stores in the configuration. If the one being removed is in the
	 * list, remove it from the list
	 * @param affectedWorkingSet - the working set being removed
	 */
	private void validateWorkingSetSourceContainers(IWorkingSet affectedWorkingSet) {
		List<?> sourceContainers = (List<?>) fPathViewer.getInput();
		if (sourceContainers != null) {
			for (int i = 0; i < sourceContainers.size(); i++) {
				if (sourceContainers.get(i) instanceof WorkingSetSourceContainer) {
					WorkingSetSourceContainer wsSrcContainer = (WorkingSetSourceContainer) sourceContainers.get(i);
					if (wsSrcContainer.getName().equals(affectedWorkingSet.getName())) {
						sourceContainers.remove(i);
					}
				}
			}
		}
	}

	/**
	 * Listen to working set changes
	 * @param event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		//if the PropertyChangeEvent has changeId CHANGE_WORKING_SET_REMOVE,
		//validate the list to make sure all working sets are valid
		//if the PropertyChangeEvent has changeId CHANGE_WORKING_SET_NAME_CHANGE,
		//do nothing because the event only has newValue, since oldValue is not provided
		//there is no way to identify which working set does the newValue corresponds to
		if (event.getProperty().equals(IWorkingSetManager.CHANGE_WORKING_SET_REMOVE)) {
			validateWorkingSetSourceContainers((IWorkingSet) event.getOldValue());
		}
	}


	@Override
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		initializeFrom(workingCopy);
	}

	/**
	 * Returns the source lookup director associated with this
	 * panel, or <code>null</code> if none.
	 *
	 * @return the source lookup director associated with this
	 * panel, or <code>null</code> if none
	 */
	public ISourceLookupDirector getDirector() {
		return fLocator;
	}

	@Override
	public void dispose() {
		super.dispose();
		//listen to changes user made to the working sets, if a working set is being removed
		//check current list to validate working sets
		IWorkingSetManager workingSetMgr = PlatformUI.getWorkbench().getWorkingSetManager();
		workingSetMgr.removePropertyChangeListener(this);
	}
}
