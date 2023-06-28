/*******************************************************************************
 *  Copyright (c) 2009, 2018 QNX Software Systems and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      QNX Software Systems - initial API and implementation
 *      Freescale Semiconductor
 *      SSI Schaefer
 *******************************************************************************/
package org.eclipse.debug.internal.core.groups;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.core.DebugCoreMessages;

/**
 * A specialization of launch to track sub-launches life-cycle, also terminates
 * itself when all sub-launches are terminated
 *
 * @since 3.11
 */
public class GroupLaunch extends Launch implements ILaunchesListener2 {

	/**
	 * Whether this process has been terminated
	 */
	private boolean fTerminated;

	/**
	 * Keeps track of whether launching has been finished
	 */
	private boolean fLaunched = false;

	/**
	 * A map of all our sub-launches and the current processes that belong to
	 * each one.
	 */
	private Map<ILaunch, IProcess[]> subLaunches = new HashMap<>();

	public GroupLaunch(ILaunchConfiguration launchConfiguration, String mode) {
		super(launchConfiguration, mode, null);
		getLaunchManager().addLaunchListener((ILaunchesListener2) this);
	}

	public void markLaunched() {
		fLaunched = true;
	}

	/**
	 * Associate the launch
	 *
	 * @param subLaunch
	 */
	public void addSubLaunch(ILaunch subLaunch) {
		synchronized (subLaunches) {
			subLaunches.put(subLaunch, new IProcess[] {});
		}
	}

	private boolean isChild(ILaunch launch) {
		synchronized (subLaunches) {
			for (ILaunch subLaunch : subLaunches.keySet()) {
				if (subLaunch == launch) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Override default behavior by querying all sub-launches to see if they are
	 * terminated
	 *
	 * @see org.eclipse.debug.core.Launch#isTerminated()
	 */
	@Override
	public boolean isTerminated() {
		if (fTerminated) {
			return true;
		}

		synchronized (subLaunches) {
			if (subLaunches.isEmpty()) {
				return fLaunched; // in case we're done launching and there is
									// nobody -> terminated
			}

			for (ILaunch launch : subLaunches.keySet()) {
				if (!launch.isTerminated()) {
					return false;
				}
			}
		}
		return fLaunched; // we're done only if we're already done launching.
							// this is required for the WAIT_FOR_TERMINATION
							// mode.
	}

	/**
	 * Override default behavior by querying all sub-launches if they can be
	 * terminated
	 *
	 * @see org.eclipse.debug.core.Launch#canTerminate()
	 */
	@Override
	public boolean canTerminate() {
		synchronized (subLaunches) {
			if (subLaunches.isEmpty()) {
				return false;
			}

			for (ILaunch launch : subLaunches.keySet()) {
				if (launch.canTerminate()) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Override default behavior by terminating all sub-launches
	 *
	 * @see org.eclipse.debug.core.Launch#terminate()
	 */
	@Override
	public void terminate() throws DebugException {
		MultiStatus status = new MultiStatus(DebugPlugin.getUniqueIdentifier(), DebugException.REQUEST_FAILED, DebugCoreMessages.Launch_terminate_failed, null);

		// somebody want's to explicitly kill the whole group, which should
		// immediately terminate and stop launching. So allow termination of the
		// group when children disappear even if launching has not finished yet.
		markLaunched();

		synchronized (subLaunches) {
			for (ILaunch launch : subLaunches.keySet()) {
				if (launch.canTerminate()) {
					try {
						launch.terminate();
					} catch (DebugException e) {
						status.merge(e.getStatus());
					}
				}
			}
		}

		if (status.isOK()) {
			return;
		}

		IStatus[] children = status.getChildren();
		if (children.length == 1) {
			throw new DebugException(children[0]);
		}

		throw new DebugException(status);
	}

	/**
	 * Handle terminated sub-launch
	 *
	 * @param launch
	 */
	private void launchTerminated(ILaunch launch) {
		if (this == launch) {
			return;
		}

		synchronized (subLaunches) {
			// Remove sub launch, keeping the processes of the terminated launch
			// to show the association and to keep the console content
			// accessible
			if (subLaunches.remove(launch) != null) {
				// terminate ourselves if this is the last sub launch
				if (subLaunches.isEmpty() && fLaunched) {
					fTerminated = true;
					fireTerminate();
				}
			}
		}
	}

	@Override
	public void launchChanged(ILaunch launch) {
		if (this == launch) {
			return;
		}

		synchronized (subLaunches) {
			// add/remove processes
			if (isChild(launch)) {
				// Remove old processes
				IProcess[] oldProcesses = subLaunches.get(launch);
				IProcess[] newProcesses = launch.getProcesses();

				// avoid notifications when processes have not changed.
				if (!Arrays.equals(oldProcesses, newProcesses)) {
					for (IProcess oldProcess : oldProcesses) {
						removeProcess(oldProcess);
					}

					// Add new processes
					for (IProcess newProcess : newProcesses) {
						addProcess(newProcess);
					}

					// Replace the processes of the changed launch
					subLaunches.put(launch, newProcesses);
				}
			}
		}
	}

	@Override
	public void launchRemoved(ILaunch launch) {
		if (this == launch) {
			super.launchRemoved(launch);
			// Remove the processes we got from the sub-launches from this
			// launch
			IProcess[] processes = getProcesses();
			for (IProcess process : processes) {
				removeProcess(process);
			}

			getLaunchManager().removeLaunchListener((ILaunchesListener2) this);
		}
	}

	@Override
	public void launchesTerminated(ILaunch[] launches) {
		for (ILaunch launch : launches) {
			launchTerminated(launch);
		}
	}

	@Override
	public void launchesAdded(ILaunch[] launches) {
		for (ILaunch launch : launches) {
			launchAdded(launch);
		}
	}

	@Override
	public void launchesChanged(ILaunch[] launches) {
		for (ILaunch launch : launches) {
			launchChanged(launch);
		}
	}

	@Override
	public void launchesRemoved(ILaunch[] launches) {
		for (ILaunch launch : launches) {
			launchRemoved(launch);
		}
	}
}