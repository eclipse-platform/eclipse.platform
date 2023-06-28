/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.console;


import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.console.ConsoleColorProvider;
import org.eclipse.debug.ui.console.IConsoleColorProvider;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

/**
 * Creates documents for processes as they are registered with a launch.
 * The singleton manager is accessible from the debug UI plugin.
 */
public class ProcessConsoleManager implements ILaunchListener {

	/**
	 * Creates console for given process
	 */
	private final class ConsoleCreation extends Job {
		private final ILaunch launch;
		private final IProcess process;

		ConsoleCreation(ILaunch launch, IProcess process) {
			super("Creating console for " + process.getLabel()); //$NON-NLS-1$
			this.launch = launch;
			this.process = process;
			setSystem(true);
			setUser(false);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (monitor.isCanceled() || getConsoleDocument(process) != null) {
				return Status.CANCEL_STATUS;
			}
			IConsoleColorProvider colorProvider = getColorProvider(process.getAttribute(IProcess.ATTR_PROCESS_TYPE));
			String encoding = launch.getAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING);
			ProcessConsole pc = new ProcessConsole(process, colorProvider, encoding);
			pc.setAttribute(IDebugUIConstants.ATTR_CONSOLE_PROCESS, process);

			// add new console to console manager.
			ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { pc });

			// If a launch is removed the associated console is removed too. It can happen
			// that the launch is removed even before the console could be created.
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=546710#c13
			ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
			if (!launchManager.isRegistered(launch)) {
				removeLaunch(launch);
			}
			return Status.OK_STATUS;
		}

		@Override
		public boolean belongsTo(Object family) {
			return process == family || ProcessConsoleManager.class == family;
		}

		@Override
		public boolean shouldSchedule() {
			Job[] jobs = Job.getJobManager().find(process);
			for (Job job : jobs) {
				if (job instanceof ConsoleCreation) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * Console document content provider extensions, keyed by extension id
	 */
	private Map<String, IConfigurationElement> fColorProviders;

	/**
	 * The default color provider. Used if no color provider is contributed
	 * for the given process type.
	 */
	private IConsoleColorProvider fDefaultColorProvider;

	/**
	 * Console line trackers; keyed by process type to list of trackers (1:N)
	 */
	private Map<String, List<IConfigurationElement>> fLineTrackers;

	/**
	 * Map of processes for a launch to compute removed processes
	 */
	private Map<ILaunch, IProcess[]> fProcesses;

	/**
	 * Lock for fLineTrackers
	 */
	private Object fLineTrackersLock = new Object();
	/**
	 * @see ILaunchListener#launchRemoved(ILaunch)
	 */
	@Override
	public void launchRemoved(ILaunch launch) {
		removeLaunch(launch);
	}

	protected void removeLaunch(ILaunch launch) {
		for (IProcess process : launch.getProcesses()) {
			removeProcess(process);
		}
		if (fProcesses != null) {
			fProcesses.remove(launch);
		}
	}

	/**
	 * Removes the console and document associated with the given process.
	 *
	 * @param iProcess process to clean up
	 */
	private void removeProcess(IProcess iProcess) {
		IConsole console = getConsole(iProcess);

		if (console != null) {
			IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
			manager.removeConsoles(new IConsole[]{console});
		}
	}

	/**
	 * Returns the console for the given process, or <code>null</code> if none.
	 *
	 * @param process
	 * @return the console for the given process, or <code>null</code> if none
	 */
	public IConsole getConsole(IProcess process) {
		IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
		for (IConsole console : manager.getConsoles()) {
			if (console instanceof ProcessConsole) {
				ProcessConsole pc = (ProcessConsole)console;
				if (pc.getProcess().equals(process)) {
					return pc;
				}
			}
		}
		return null;
	}

	/**
	 * @see ILaunchListener#launchAdded(ILaunch)
	 */
	@Override
	public void launchAdded(ILaunch launch) {
		launchChanged(launch);
	}

	/**
	 * @see ILaunchListener#launchChanged(ILaunch)
	 */
	@Override
	public void launchChanged(final ILaunch launch) {
		IProcess[] processes= launch.getProcesses();
		for (IProcess process : processes) {
			if (process.getStreamsProxy() == null) {
				continue;
			}
			if (getConsoleDocument(process) == null) {
				// create a new console in a separated thread, see bug 355011.
				Job job = new ConsoleCreation(launch, process);
				job.schedule();
			}
		}
		List<IProcess> removed = getRemovedProcesses(launch);
		if (removed != null) {
			for (IProcess p : removed) {
				removeProcess(p);
			}
		}
	}

	/**
	 * Returns the document for the process, or <code>null</code>
	 * if none.
	 */
	public IDocument getConsoleDocument(IProcess process) {
		ProcessConsole console = (ProcessConsole) getConsole(process);
		return (console != null ? console.getDocument() : null);
	}

	/**
	 * Called by the debug ui plug-in on startup.
	 * The console document manager starts listening for
	 * launches to be registered and initializes if any launches
	 * already exist.
	 */
	public void startup() {
		ILaunchManager launchManager= DebugPlugin.getDefault().getLaunchManager();
		launchManager.addLaunchListener(this);

		//set up the docs for launches already registered
		for (ILaunch launch : launchManager.getLaunches()) {
			launchAdded(launch);
		}
	}

	/**
	 * Called by the debug ui plug-in on shutdown.
	 * The console document manager de-registers as a
	 * launch listener and kills all existing console documents.
	 */
	public void shutdown() {
		Job.getJobManager().cancel(ProcessConsoleManager.class);
		ILaunchManager launchManager= DebugPlugin.getDefault().getLaunchManager();
		for (ILaunch launch : launchManager.getLaunches()) {
			removeLaunch(launch);
		}
		launchManager.removeLaunchListener(this);
		if (fProcesses != null) {
			fProcesses.clear();
		}
	}

	/**
	 * Returns a new console document color provider extension for the given
	 * process type, or <code>null</code> if none.
	 *
	 * @param type corresponds to <code>IProcess.ATTR_PROCESS_TYPE</code>
	 * @return IConsoleColorProvider
	 */
	public IConsoleColorProvider getColorProvider(String type) {
		if (fColorProviders == null) {
			fColorProviders = new HashMap<>();
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugUIPlugin.getUniqueIdentifier(), IDebugUIConstants.EXTENSION_POINT_CONSOLE_COLOR_PROVIDERS);
			for (IConfigurationElement extension : extensionPoint.getConfigurationElements()) {
				fColorProviders.put(extension.getAttribute("processType"), extension); //$NON-NLS-1$
			}
		}
		IConfigurationElement extension = fColorProviders.get(type);
		if (extension != null) {
			try {
				Object colorProvider = extension.createExecutableExtension("class"); //$NON-NLS-1$
				if (colorProvider instanceof IConsoleColorProvider) {
					return (IConsoleColorProvider)colorProvider;
				}
				DebugUIPlugin.logErrorMessage(MessageFormat.format(
						"Extension {0} must specify an instanceof IConsoleColorProvider for class attribute.", //$NON-NLS-1$
						new Object[] { extension.getDeclaringExtension().getUniqueIdentifier() }));
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
			}
		}
		//no color provider found of specified type, return default color provider.
		if (fDefaultColorProvider == null) {
			fDefaultColorProvider = new ConsoleColorProvider();
		}
		return fDefaultColorProvider;
	}

	/**
	 * Returns the Line Trackers for a given process type.
	 * @param process The process for which line trackers are required.
	 * @return An array of line trackers which match the given process type.
	 */
	public IConsoleLineTracker[] getLineTrackers(IProcess process) {
		String type = process.getAttribute(IProcess.ATTR_PROCESS_TYPE);

		if (fLineTrackers == null) {
			synchronized (fLineTrackersLock) { // can't use fLineTrackers as lock as it is null here
				fLineTrackers = new HashMap<>();
				IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(DebugUIPlugin.getUniqueIdentifier(), IDebugUIConstants.EXTENSION_POINT_CONSOLE_LINE_TRACKERS);
				for (IConfigurationElement extension : extensionPoint.getConfigurationElements()) {
					String processType = extension.getAttribute("processType"); //$NON-NLS-1$
					List<IConfigurationElement> list = fLineTrackers.get(processType);
					if (list == null) {
						list = new ArrayList<>();
						fLineTrackers.put(processType, list);
					}
					list.add(extension);
				}
			}
		}

		ArrayList<IConsoleLineTracker> trackers = new ArrayList<>();
		if (type != null) {
			List<IConfigurationElement> lineTrackerExtensions;
			synchronized (fLineTrackers) {// need to synchronize as the update to list might be still happening
				lineTrackerExtensions = fLineTrackers.get(type);
			}
			if(lineTrackerExtensions != null) {
				for (IConfigurationElement element : lineTrackerExtensions) {
					try {
						trackers.add((IConsoleLineTracker) element.createExecutableExtension("class")); //$NON-NLS-1$
					} catch (CoreException e) {
						DebugUIPlugin.log(e);
					}
				}
			}
		}
		return trackers.toArray(new IConsoleLineTracker[0]);
	}

	/**
	 * Returns the processes that have been removed from the given
	 * launch, or <code>null</code> if none.
	 *
	 * @param launch launch that has changed
	 * @return removed processes or <code>null</code>
	 */
	private List<IProcess> getRemovedProcesses(ILaunch launch) {
		List<IProcess> removed = null;
		if (fProcesses == null) {
			fProcesses = new HashMap<>();
		}
		IProcess[] old = fProcesses.get(launch);
		IProcess[] curr = launch.getProcesses();
		if (old != null) {
			for (IProcess process : old) {
				if (!contains(curr, process)) {
					if (removed == null) {
						removed = new ArrayList<>();
					}
					removed.add(process);
				}
			}
		}
		// update cache with current processes
		fProcesses.put(launch, curr);
		return removed;
	}

	/**
	 * Returns whether the given object is contained in the list.
	 *
	 * @param list list to search
	 * @param object object to search for
	 * @return whether the given object is contained in the list
	 */
	private boolean contains(Object[] list, Object object) {
		for (Object object2 : list) {
			if (object2.equals(object)) {
				return true;
			}
		}
		return false;
	}
}
