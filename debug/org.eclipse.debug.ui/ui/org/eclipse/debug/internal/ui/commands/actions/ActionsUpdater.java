/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.commands.actions;


/**
 * Collects votes from handler update requests.
 *
 * @since 3.3
 *
 */
public class ActionsUpdater {

	private IEnabledTarget[] fActions;
	private int fNumVoters;
	private int fNumOfVotes = 0;
	private boolean fDone = false;
	private boolean fEnabled = true;

	public ActionsUpdater(IEnabledTarget[] actions, int numVoters) {
		fActions = actions;
		fNumVoters = numVoters;
	}

	public synchronized void setEnabled(boolean result) {
		fNumOfVotes++;
		if (fEnabled) {
			fEnabled = result;
		}
		done();
	}

	private synchronized void done() {
		if (!fDone) {
			if (!fEnabled || fNumOfVotes == fNumVoters) {
				fDone = true;
				for (IEnabledTarget fAction : fActions) {
					fAction.setEnabled(fEnabled);
				}
			}
		}
	}

}
