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
package org.eclipse.team.internal.ui.synchronize.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionGroup;

/**
 * This action group provides radio buttons for each possible direction of synchronization information. The
 * modes created by this action group can be configured. The actions directly set the mode of an
 * {@link ISynchronizePageConfiguration}.
 * @since 3.0
 */
public class DirectionFilterActionGroup extends ActionGroup implements IPropertyChangeListener {

	// The list of created actions
	private List actions = new ArrayList(3);

	// The modes
	private DirectionFilterAction incomingMode;
	private DirectionFilterAction outgoingMode;
	private DirectionFilterAction bothMode;
	private DirectionFilterAction conflictsMode;

	private ISynchronizePageConfiguration configuration;

	/**
	 * An action filter for a specific mode.
	 */
	class DirectionFilterAction extends Action {
		private int modeId;

		public DirectionFilterAction(String prefix,String commandId, int modeId) {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			this.modeId = modeId;
			Utils.initAction(this, prefix);
		}
		@Override
		public void run() {
			if(isChecked()) {
				configuration.setMode(modeId);
			}
		}
		public int getModeId() {
			return modeId;
		}
	}

	/**
	 * Creates a direction filter group with the given supported modes. The
	 * possible values for modes are defined by the {@link ISynchronizePageConfiguration}
	 * interface.
	 *
	 * @param configuration the page configuration
	 */
	public DirectionFilterActionGroup(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
		createActions();
		configuration.addPropertyChangeListener(this);
		checkMode(configuration.getMode());
	}

	/**
	 * Sets up the sync modes and the actions for switching between them.
	 */
	private void createActions() {
		// Create the actions
		int supportedModes = configuration.getSupportedModes();
		if (supportedModes == 0) return;
		int currentMode = configuration.getMode();
		if ((supportedModes & currentMode) == 0) {
			currentMode = getSupportedMode(supportedModes);
			if (currentMode == 0) return;
			configuration.setMode(currentMode);
		}
		if((supportedModes & ISynchronizePageConfiguration.INCOMING_MODE) != 0) {
			incomingMode = new DirectionFilterAction("action.directionFilterIncoming.", "org.eclipse.team.ui.syncview.incomingFilter",  ISynchronizePageConfiguration.INCOMING_MODE); //$NON-NLS-1$ //$NON-NLS-2$
			actions.add(incomingMode);
			incomingMode.setChecked(currentMode == ISynchronizePageConfiguration.INCOMING_MODE);
		}

		if((supportedModes & ISynchronizePageConfiguration.OUTGOING_MODE) != 0) {
			outgoingMode = new DirectionFilterAction("action.directionFilterOutgoing.", "org.eclipse.team.ui.syncview.outgoingFilter",  ISynchronizePageConfiguration.OUTGOING_MODE); //$NON-NLS-1$ //$NON-NLS-2$
			actions.add(outgoingMode);
			outgoingMode.setChecked(currentMode == ISynchronizePageConfiguration.OUTGOING_MODE);
		}

		if((supportedModes & ISynchronizePageConfiguration.BOTH_MODE) != 0) {
			bothMode = new DirectionFilterAction("action.directionFilterBoth.", "org.eclipse.team.ui.syncview.bothFilter", ISynchronizePageConfiguration.BOTH_MODE); //$NON-NLS-1$ //$NON-NLS-2$
			actions.add(bothMode);
			bothMode.setChecked(currentMode == ISynchronizePageConfiguration.BOTH_MODE);
		}

		if((supportedModes & ISynchronizePageConfiguration.CONFLICTING_MODE) != 0) {
			conflictsMode = new DirectionFilterAction("action.directionFilterConflicts.", "org.eclipse.team.ui.syncview.conflictsFilter", ISynchronizePageConfiguration.CONFLICTING_MODE); //$NON-NLS-1$ //$NON-NLS-2$
			actions.add(conflictsMode);
			conflictsMode.setChecked(currentMode == ISynchronizePageConfiguration.CONFLICTING_MODE);
		}
	}

	/**
	 * @param supportedModes
	 * @return the support mode
	 */
	private int getSupportedMode(int supportedModes) {
		if((supportedModes & ISynchronizePageConfiguration.INCOMING_MODE) != 0) {
			return ISynchronizePageConfiguration.INCOMING_MODE;
		}
		if((supportedModes & ISynchronizePageConfiguration.OUTGOING_MODE) != 0) {
			return ISynchronizePageConfiguration.OUTGOING_MODE;
		}
		if((supportedModes & ISynchronizePageConfiguration.BOTH_MODE) != 0) {
			return ISynchronizePageConfiguration.BOTH_MODE;
		}
		if((supportedModes & ISynchronizePageConfiguration.CONFLICTING_MODE) != 0) {
			return ISynchronizePageConfiguration.CONFLICTING_MODE;
		}
		return 0;
	}

	public void fillActionBars(IActionBars actionBars, String group) {
		super.fillActionBars(actionBars);
		IToolBarManager toolBar = actionBars.getToolBarManager();
		for (Object action2 : actions) {
			DirectionFilterAction action = (DirectionFilterAction) action2;
			if(group != null) {
				toolBar.appendToGroup(group, action);
			} else {
				toolBar.add(action);
			}
		}
	}

	public void fillToolBar(String groupId, IToolBarManager toolBar) {
		for (Object action2 : actions) {
			DirectionFilterAction action = (DirectionFilterAction) action2;
				toolBar.appendToGroup(groupId, action);
		}
	}

	public void fillMenu(IContributionManager manager) {
		for (Object action2 : actions) {
			DirectionFilterAction action = (DirectionFilterAction) action2;
				manager.add(action);
		}
	}

	private void checkMode(int mode) {
		for (Object action2 : actions) {
			DirectionFilterAction action = (DirectionFilterAction)action2;
			if(action.getModeId() == mode) {
				action.setChecked(true);
			} else {
				action.setChecked(false);
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if(event.getProperty().equals(ISynchronizePageConfiguration.P_MODE)) {
			Integer mode = (Integer)event.getNewValue();
			checkMode(mode.intValue());
		}
	}

	@Override
	public void dispose() {
		super.dispose();
	}
}
