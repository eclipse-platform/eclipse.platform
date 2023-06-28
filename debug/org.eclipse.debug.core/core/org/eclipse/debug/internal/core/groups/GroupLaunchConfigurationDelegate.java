/*******************************************************************************
 *  Copyright (c) 2009, 2019 QNX Software Systems and others.
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
 *      Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 529651
 *******************************************************************************/
package org.eclipse.debug.internal.core.groups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.internal.core.DebugCoreMessages;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.core.groups.GroupLaunchElement.GroupElementPostLaunchAction;
import org.eclipse.debug.internal.core.groups.observer.ProcessObserver;
import org.eclipse.debug.internal.core.groups.observer.StreamObserver;
import org.eclipse.osgi.util.NLS;

/**
 * Group Launch delegate. Launches each configuration in the user selected mode
 *
 * @since 3.11
 */
public class GroupLaunchConfigurationDelegate extends LaunchConfigurationDelegate implements ILaunchConfigurationDelegate2 {
	public static final int CODE_GROUP_LAUNCH_START = 233;
	public static final int CODE_GROUP_LAUNCH_DONE = 234;

	private static final int CODE_BUILD_BEFORE_LAUNCH = 206;

	private static final String NAME_PROP = "name"; //$NON-NLS-1$
	private static final String ENABLED_PROP = "enabled"; //$NON-NLS-1$
	private static final String ADOPT_PROP = "adoptIfRunning"; //$NON-NLS-1$
	private static final String MODE_PROP = "mode"; //$NON-NLS-1$
	private static final String ACTION_PROP = "action"; //$NON-NLS-1$
	private static final String ACTION_PARAM_PROP = "actionParam"; //$NON-NLS-1$
	private static final String MULTI_LAUNCH_CONSTANTS_PREFIX = "org.eclipse.debug.core.launchGroup"; //$NON-NLS-1$

	private static final String DEBUG_CORE = "org.eclipse.debug.core"; //$NON-NLS-1$

	private static final Status UNSUPPORTED_MODE = new Status(IStatus.ERROR, DEBUG_CORE, 230, IInternalDebugCoreConstants.EMPTY_STRING, null);
	private static final Status GROUP_ELEMENT_STARTED = new Status(IStatus.OK, DEBUG_CORE, 231, IInternalDebugCoreConstants.EMPTY_STRING, null);
	private static final Status GROUP_CYCLE = new Status(IStatus.ERROR, DEBUG_CORE, 232, IInternalDebugCoreConstants.EMPTY_STRING, null);

	private static final Status GROUP_LAUNCH_START = new Status(IStatus.INFO, DEBUG_CORE, CODE_GROUP_LAUNCH_START, IInternalDebugCoreConstants.EMPTY_STRING, null);
	private static final Status GROUP_LAUNCH_DONE = new Status(IStatus.INFO, DEBUG_CORE, CODE_GROUP_LAUNCH_DONE, IInternalDebugCoreConstants.EMPTY_STRING, null);

	private static final Status BUILD_BEFORE_LAUNCH = new Status(IStatus.INFO, DEBUG_CORE, CODE_BUILD_BEFORE_LAUNCH, IInternalDebugCoreConstants.EMPTY_STRING, null);

	@Override
	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
		return new GroupLaunch(configuration, mode);
	}

	@Override
	public void launch(ILaunchConfiguration groupConfig, String mode, final ILaunch groupLaunch, IProgressMonitor monitor) throws CoreException {
		final GroupLaunch group = (GroupLaunch) groupLaunch;

		IStatusHandler groupStateHandler = DebugPlugin.getDefault().getStatusHandler(GROUP_LAUNCH_START);
		groupStateHandler.handleStatus(GROUP_LAUNCH_START, group);

		try {
			SubMonitor progress = SubMonitor.convert(monitor, NLS.bind(DebugCoreMessages.GroupLaunchConfigurationDelegate_Launching, groupConfig.getName()), 1000);

			List<GroupLaunchElement> launches = createLaunchElements(groupConfig);
			for (int i = 0; i < launches.size(); ++i) {
				GroupLaunchElement le = launches.get(i);

				if (!le.enabled) {
					continue;
				}

				// find launch; if not found, skip (error?)
				final ILaunchConfiguration conf = findLaunchConfiguration(le.name);
				if (conf == null) {
					continue;
				}

				// determine mode for each launch
				final String localMode;
				if (!le.mode.equals(GroupLaunchElement.MODE_INHERIT)) {
					localMode = le.mode;
				} else {
					localMode = mode;
				}
				if (!conf.supportsMode(localMode)) {
					IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(UNSUPPORTED_MODE);
					handler.handleStatus(UNSUPPORTED_MODE, new String[] {
							conf.getName(), localMode });
					continue;
				}

				if (groupConfig.getName().equals(conf.getName())) {
					// loop detected. report as appropriate and die.
					IStatusHandler cycleHandler = DebugPlugin.getDefault().getStatusHandler(GROUP_CYCLE);
					cycleHandler.handleStatus(GROUP_CYCLE, conf.getName());
				} else if (!launchChild(progress.newChild(1000 / launches.size()), group, le, conf, localMode, (i == launches.size() - 1))) {
					break;
				}

				// in case the group has been terminated while waiting in the
				// post launch action.
				if (group.isTerminated()) {
					break;
				}
			}


			if (!group.hasChildren()) {
				DebugPlugin.getDefault().getLaunchManager().removeLaunch(group);
			}
		} finally {
			// safety net - launching is finished also in case of a problem.
			group.markLaunched();
			groupStateHandler.handleStatus(GROUP_LAUNCH_DONE, group);
			monitor.done();
		}
	}

	private boolean launchChild(SubMonitor monitor, final GroupLaunch group, GroupLaunchElement le, final ILaunchConfiguration child, final String localMode, boolean lastConfig) throws CoreException {
		final Set<ILaunch> running = le.adoptIfRunning ? findRunningLaunch(le.name) : Collections.emptySet();
		ILaunch subLaunch = running.stream().findFirst().orElse(null);
		boolean launched = false;
		if (subLaunch == null) {
			boolean build = true;// see DebugUIPreferenceInitializer
			IStatusHandler buildHandler = DebugPlugin.getDefault().getStatusHandler(BUILD_BEFORE_LAUNCH);
			try {
				Object resolution = buildHandler.handleStatus(BUILD_BEFORE_LAUNCH, child);
				if (resolution instanceof Boolean) {
					build = ((Boolean) resolution).booleanValue();
				}
			} catch (Exception e) {
				// ignore and use default
			}
			subLaunch = child.launch(localMode, monitor, build);
			launched = true;
		}

		group.addSubLaunch(subLaunch);

		// Now that we added the launch in our list, we have already
		// received the real launchChanged event, and did not know
		// it was part of our list
		// So, fake another event now.
		group.launchChanged(subLaunch);

		if (launched) {
			// give handler a chance to perform additional actions after
			// launching each of the members.
			IStatusHandler postLaunchHandler = DebugPlugin.getDefault().getStatusHandler(GROUP_ELEMENT_STARTED);
			postLaunchHandler.handleStatus(GROUP_ELEMENT_STARTED, new ILaunch[] {
					group, subLaunch });
		}

		// if this is the last child, mark the group as "launching finished", so
		// that from now on the last terminating child will also terminate the
		// group.
		if (lastConfig) {
			group.markLaunched();
		}

		// in case we adopted the launch, and did not launch outselves, don't
		// execute the post launch action!
		if (launched) {
			return postLaunchAction(subLaunch, le, monitor);
		} else {
			return true;
		}
	}

	private boolean postLaunchAction(ILaunch subLaunch, GroupLaunchElement le, IProgressMonitor monitor) {
		switch (le.action) {
			case NONE:
				return true;
			case WAIT_FOR_TERMINATION:
				monitor.subTask(NLS.bind(DebugCoreMessages.GroupLaunchConfigurationDelegate_Waiting_for_termination, subLaunch.getLaunchConfiguration().getName()));
				while (!subLaunch.isTerminated() && !monitor.isCanceled()) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						break;
					}
				}
				monitor.subTask(""); //$NON-NLS-1$
				break;
			case DELAY:
				Integer waitSecs = (Integer) le.actionParam;
				if (waitSecs != null) {
					monitor.subTask(NLS.bind(DebugCoreMessages.GroupLaunchConfigurationDelegate_Delaying, waitSecs.toString()));
					try {
						Thread.sleep(waitSecs * 1000); // param is milliseconds
					} catch (InterruptedException e) {
						// ok
					}
				}
				break;

			case OUTPUT_REGEXP:
				String regexp = (String) le.actionParam;
				if (regexp != null) {
					monitor.subTask(NLS.bind(DebugCoreMessages.GroupLaunchConfigurationDelegate_waiting, regexp, subLaunch.getLaunchConfiguration().getName()));
					if (!waitForOutputMatching(subLaunch, monitor, regexp)) {
						return false;
					}
				}

				break;

			default:
				assert false : "new post launch action type is missing logic"; //$NON-NLS-1$
		}

		return true;
	}

	// blocks until a specific string is in the log output
	private boolean waitForOutputMatching(ILaunch launch, IProgressMonitor m, String regexp) {
		int processCount = launch.getProcesses().length;
		final ExecutorService executor = Executors.newCachedThreadPool();
		final CountDownLatch countDownLatch = new CountDownLatch(processCount);
		Future<Integer> process = null;
		for (IProcess p : launch.getProcesses()) {
			process = executor.submit(new ProcessObserver(m, p, countDownLatch));
			executor.submit(new StreamObserver(m, p, regexp, countDownLatch));
		}
		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			// should not happen at all!
		}
		executor.shutdown();
		// process terminated before condition
		if (process == null || process.isDone()) {
			return false;
		}
		// condition matched
		return true;
	}

	@Override
	protected void buildProjects(IProject[] projects, IProgressMonitor monitor) throws CoreException {
		// do nothing, project can be rebuild for each launch individually
	}

	@Override
	public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		// not build for this one
		return false;
	}

	protected static ILaunchConfiguration findLaunchConfiguration(String name) throws CoreException {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfiguration[] launchConfigurations = launchManager.getLaunchConfigurations();
		for (ILaunchConfiguration config : launchConfigurations) {
			if (config.getName().equals(name)) {
				return config;
			}
		}
		return null;
	}

	protected static Set<ILaunch> findRunningLaunch(String name) {
		Set<ILaunch> result = new HashSet<>();
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		for (ILaunch l : launchManager.getLaunches()) {
			if (l.isTerminated()) {
				continue;
			}
			if (l.getLaunchConfiguration().getName().equals(name)) {
				result.add(l);
			}
		}
		return result;
	}

	/**
	 * (Re-)reads all {@link GroupLaunchElement}s from a
	 * {@link ILaunchConfiguration}s attributes.
	 *
	 * @param configuration the launch configuration; this must be of the type
	 *            handled by this delegate
	 * @return all {@link GroupLaunchElement}s found in the given
	 *         {@link ILaunchConfiguration}s attributes. Never
	 *         <code>null</code>.
	 */
	public static List<GroupLaunchElement> createLaunchElements(ILaunchConfiguration configuration) {
		List<GroupLaunchElement> result = new ArrayList<>();
		try {
			Map<String, Object> attrs = configuration.getAttributes();
			for (Map.Entry<String, Object> entry : attrs.entrySet()) {
				String attr = entry.getKey();
				try {
					if (attr.startsWith(MULTI_LAUNCH_CONSTANTS_PREFIX)) {
						String prop = attr.substring(MULTI_LAUNCH_CONSTANTS_PREFIX.length() + 1);
						int k = prop.indexOf('.');
						String num = prop.substring(0, k);
						int index = Integer.parseInt(num);
						String name = prop.substring(k + 1);
						if (name.equals(NAME_PROP)) {
							GroupLaunchElement el = new GroupLaunchElement();
							el.index = index;
							el.name = (String) entry.getValue();

							Object actionParam = null;
							String actionStr = (String) attrs.get(getProp(index, ACTION_PROP));

							GroupElementPostLaunchAction action;
							try {
								action = GroupElementPostLaunchAction.valueOf(actionStr);
							} catch (Exception e) {
								action = GroupElementPostLaunchAction.NONE;
							}
							if (action == GroupElementPostLaunchAction.DELAY) {
								try {
									actionParam = Integer.parseInt((String) attrs.get(getProp(index, ACTION_PARAM_PROP)));
								} catch (NumberFormatException exc) {
									DebugPlugin.log(exc);
								}
							}
							if (action == GroupElementPostLaunchAction.OUTPUT_REGEXP) {
								actionParam = attrs.get(getProp(index, ACTION_PARAM_PROP));
							}
							el.action = action;
							el.actionParam = actionParam;
							if (attrs.containsKey(getProp(index, ADOPT_PROP))) {
								el.adoptIfRunning = (Boolean) attrs.get(getProp(index, ADOPT_PROP));
							}
							el.mode = (String) attrs.get(getProp(index, MODE_PROP));
							el.enabled = (Boolean) attrs.get(getProp(index, ENABLED_PROP));
							try {
								el.data = findLaunchConfiguration(el.name);
							} catch (Exception e) {
								el.data = null;
							}
							while (index >= result.size()) {
								result.add(null);
							}
							result.set(index, el);

						}
					}
				} catch (Exception e) {
					DebugPlugin.log(e);
				}
			}
		} catch (CoreException e) {
			DebugPlugin.log(e);
		}
		return result;
	}

	public static void storeLaunchElements(ILaunchConfigurationWorkingCopy configuration, List<GroupLaunchElement> input) {
		int i = 0;
		removeLaunchElements(configuration);
		for (GroupLaunchElement el : input) {
			if (el == null) {
				continue;
			}
			configuration.setAttribute(getProp(i, NAME_PROP), el.name);
			configuration.setAttribute(getProp(i, ACTION_PROP), el.action.toString());
			configuration.setAttribute(getProp(i, ADOPT_PROP), el.adoptIfRunning);
			// note: the saving of the action param will need to be enhanced if
			// ever an action type is introduced that uses something that can't
			// be reconstructed from its toString()
			configuration.setAttribute(getProp(i, ACTION_PARAM_PROP), el.actionParam != null ? el.actionParam.toString() : null);
			configuration.setAttribute(getProp(i, MODE_PROP), el.mode);
			configuration.setAttribute(getProp(i, ENABLED_PROP), el.enabled);
			i++;
		}
	}

	public static void removeLaunchElements(ILaunchConfigurationWorkingCopy configuration) {
		try {
			for (String attr : configuration.getAttributes().keySet()) {
				try {
					if (attr.startsWith(MULTI_LAUNCH_CONSTANTS_PREFIX)) {
						configuration.removeAttribute(attr);
					}
				} catch (Exception e) {
					DebugPlugin.log(e);
				}
			}
		} catch (CoreException e) {
			DebugPlugin.log(e);
		}
	}

	public static String getProp(int index, String string) {
		return MULTI_LAUNCH_CONSTANTS_PREFIX + "." + index + "." + string; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
