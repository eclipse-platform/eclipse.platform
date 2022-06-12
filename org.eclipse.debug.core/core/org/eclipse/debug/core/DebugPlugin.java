/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Anton Kosyakov (Itemis AG) - Bug 438621 - [step filtering] Provide an extension point to enhance methods step filtering.
 *******************************************************************************/
package org.eclipse.debug.core;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDisconnect;
import org.eclipse.debug.core.model.IDropToFrame;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStep;
import org.eclipse.debug.core.model.IStepFilter;
import org.eclipse.debug.core.model.IStepFilters;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.core.model.ITerminate;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.debug.internal.core.BreakpointManager;
import org.eclipse.debug.internal.core.DebugCoreMessages;
import org.eclipse.debug.internal.core.DebugOptions;
import org.eclipse.debug.internal.core.ExpressionManager;
import org.eclipse.debug.internal.core.IConfigurationElementConstants;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.internal.core.LogicalStructureManager;
import org.eclipse.debug.internal.core.MemoryBlockManager;
import org.eclipse.debug.internal.core.Preferences;
import org.eclipse.debug.internal.core.StepFilterManager;
import org.eclipse.debug.internal.core.commands.CommandAdapterFactory;
import org.eclipse.debug.internal.core.groups.GroupMemberChangeListener;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupUtils;
import org.eclipse.osgi.service.environment.Constants;
import org.osgi.framework.BundleContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * There is one instance of the debug plug-in available from
 * <code>DebugPlugin.getDefault()</code>. The debug plug-in provides:
 * <ul>
 * <li>access to the breakpoint manager</li>
 * <li>access to the launch manager</li>
 * <li>access to the expression manager</li>
 * <li>access to the registered launcher extensions</li>
 * <li>access to the memory block manager</li>
 * <li>debug event notification</li>
 * <li>status handlers</li>
 * </ul>
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be sub-classed by clients.
 */
public class DebugPlugin extends Plugin {

	/**
	 * Unique identifier constant (value <code>"org.eclipse.debug.core"</code>)
	 * for the Debug Core plug-in.
	 */
	private static final String PI_DEBUG_CORE = "org.eclipse.debug.core"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"launchConfigurationTypes"</code>)
	 * for the launch configuration types extension point.
	 *
	 * @since 2.0
	 */
	public static final String EXTENSION_POINT_LAUNCH_CONFIGURATION_TYPES= "launchConfigurationTypes"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"launchConfigurationComparators"</code>)
	 * for the launch configuration comparators extension point.
	 *
	 * @since 2.0
	 */
	public static final String EXTENSION_POINT_LAUNCH_CONFIGURATION_COMPARATORS= "launchConfigurationComparators"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"breakpoints"</code>) for the
	 * breakpoints extension point.
	 *
	 * @since 2.0
	 */
	public static final String EXTENSION_POINT_BREAKPOINTS= "breakpoints";	 //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"statusHandlers"</code>) for the
	 * status handlers extension point.
	 *
	 * @since 2.0
	 */
	public static final String EXTENSION_POINT_STATUS_HANDLERS= "statusHandlers";	 //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"sourceLocators"</code>) for the
	 * source locators extension point.
	 *
	 * @since 2.0
	 */
	public static final String EXTENSION_POINT_SOURCE_LOCATORS= "sourceLocators";	 //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"launchModes"</code>) for the
	 * source modes extension point.
	 *
	 * @since 3.0
	 */
	public static final String EXTENSION_POINT_LAUNCH_MODES= "launchModes";	 //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"launchDelegates"</code>) for the
	 * launch delegates extension point.
	 *
	 * @since 3.0
	 */
	public static final String EXTENSION_POINT_LAUNCH_DELEGATES= "launchDelegates";	 //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"processFactories"</code>) for the
	 * process factories extension point.
	 *
	 * @since 3.0
	 */
	public static final String EXTENSION_POINT_PROCESS_FACTORIES = "processFactories"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"logicalStructureTypes"</code>) for the
	 * logical structure types extension point.
	 *
	 * @since 3.0
	 */
	public static final String EXTENSION_POINT_LOGICAL_STRUCTURE_TYPES = "logicalStructureTypes"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"logicalStructureProviders"</code>) for the
	 * logical structure types extension point.
	 *
	 * @since 3.1
	 */
	public static final String EXTENSION_POINT_LOGICAL_STRUCTURE_PROVIDERS = "logicalStructureProviders"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"sourceContainerTypes"</code>) for the
	 * source container types extension point.
	 *
	 * @since 3.0
	 */
	public static final String EXTENSION_POINT_SOURCE_CONTAINER_TYPES = "sourceContainerTypes";	 //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"sourcePathComputers"</code>) for the
	 * source path computers extension point.
	 *
	 * @since 3.0
	 */
	public static final String EXTENSION_POINT_SOURCE_PATH_COMPUTERS = "sourcePathComputers"; //$NON-NLS-1$

	/**
	 * Simple identifier constant for the launch options extension point
	 *
	 * @since 3.3
	 */
	public static final String EXTENSION_POINT_LAUNCH_OPTIONS = "launchOptions"; //$NON-NLS-1$

	/**
	 * Simple identifier constant for the breakpoint import participant extension point
	 *
	 * @since 3.5
	 */
	public static final String EXTENSION_POINT_BREAKPOINT_IMPORT_PARTICIPANTS = "breakpointImportParticipants"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"stepFilters"</code>) for the
	 * step filters extension point.
	 *
	 * @since 3.10
	 */
	public static final String EXTENSION_POINT_STEP_FILTERS = "stepFilters"; //$NON-NLS-1$

	/**
	 * Status code indicating an unexpected error.
	 *
	 * @since 3.4
	 */
	public static final int ERROR = 125;

	/**
	 * Status code indicating an unexpected internal error.  Internal errors
	 * should never be displayed to the user in dialogs or status text.
	 * Internal error messages are not translated.
	 */
	public static final int INTERNAL_ERROR = 120;

	/**
	 * Status code indicating that the Eclipse runtime does not support
	 * launching a program with a working directory. This feature is only
	 * available if Eclipse is run on a 1.3 runtime or higher.
	 * <p>
	 * A status handler may be registered for this error condition,
	 * and should return a <code>Boolean</code> indicating whether the program
	 * should be re-launched with the default working directory.
	 * </p>
	 */
	public static final int ERR_WORKING_DIRECTORY_NOT_SUPPORTED = 115;

	/**
	 * The launch configuration attribute that designates the process factory ID
	 * for the process factory to be used when creating a new process as a result of launching
	 * the launch configuration.
	 * @since 3.0
	 */
	public static final String ATTR_PROCESS_FACTORY_ID = "process_factory_id"; //$NON-NLS-1$

	/**
	 * The launch attribute that designates whether or not it's associated
	 * launch should capture output. Value is a string representing a boolean -
	 * <code>true</code> or <code>false</code>. When unspecified, the default
	 * value is considered <code>true</code>.
	 *
	 * @since 3.1
	 */
	public static final String ATTR_CAPTURE_OUTPUT = PI_DEBUG_CORE + ".capture_output"; //$NON-NLS-1$

	/**
	 * The launch attribute that stores the time stamp of when a launch configuration was
	 * launched. Value is {@link Long#toString(long)} of {@link System#currentTimeMillis()}.
	 *
	 * @since 3.6
	 */
	public static final String ATTR_LAUNCH_TIMESTAMP = PI_DEBUG_CORE + ".launch.timestamp";  //$NON-NLS-1$

	/**
	 * The launch attribute that stores the time stamp of when a launch configuration was
	 * launched. Value is {@link Long#toString(long)} of {@link System#currentTimeMillis()}.
	 *
	 * @since 3.15
	 */
	public static final String ATTR_TERMINATE_TIMESTAMP = PI_DEBUG_CORE + ".terminate.timestamp"; //$NON-NLS-1$

	/**
	 * This launch attribute designates the encoding to be used by the console
	 * associated with the launch.
	 * <p>
	 * For release 3.3, the system encoding is used when unspecified. Since 3.4,
	 * the inherited encoding is used when unspecified. See {@link ILaunchManager} for a
	 * description in <code>getEncoding(ILaunchConfiguration)</code>.
	 * </p>
	 * <p>
	 * Value of this constant is the same as the value of the old
	 * <code>IDebugUIConstants.ATTR_CONSOLE_ENCODING</code> constant for backward
	 * compatibility.
	 * </p>
	 * @since 3.3
	 */
	public static final String ATTR_CONSOLE_ENCODING = "org.eclipse.debug.ui.ATTR_CONSOLE_ENCODING"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute - a boolean value indicating whether a
	 * configuration should be launched with merged error and standard output.
	 * Merging output can ensure the process output appears in console in same
	 * order as the process produce it. On the other hand the error output can
	 * not be colored different from standard output anymore. Default value is
	 * <code>false</code>.
	 *
	 * @since 3.14
	 */
	public static final String ATTR_MERGE_OUTPUT = PI_DEBUG_CORE + ".ATTR_MERGE_OUTPUT"; //$NON-NLS-1$

	/**
	 * Boolean preference key (value
	 * <code>org.eclipse.debug.core.PREF_DELETE_CONFIGS_ON_PROJECT_DELETE</code>)
	 * that controls whether to delete associated configurations when a project
	 * is deleted. Default value is <code>false</code>.
	 *
	 * @since 3.7
	 */
	public static final String PREF_DELETE_CONFIGS_ON_PROJECT_DELETE = PI_DEBUG_CORE + ".PREF_DELETE_CONFIGS_ON_PROJECT_DELETE"; //$NON-NLS-1$

	/**
	 * Deleted breakpoint marker attribute (value
	 * <code>"org.eclipse.debug.core.breakpointIsDeleted"</code>). The attribute is a
	 * <code>boolean</code> corresponding to the deleted state of a breakpoint.
	 *
	 * @see org.eclipse.core.resources.IMarker#getAttribute(String, boolean)
	 * @since 3.7
	 */
	public static final String ATTR_BREAKPOINT_IS_DELETED= PI_DEBUG_CORE + ".breakpointIsDeleted"; //$NON-NLS-1$

	/**
	 * Attribute key for the environment used when an {@link IProcess} was run
	 * @see IProcess
	 * @since 3.8
	 */
	public static final String ATTR_ENVIRONMENT = PI_DEBUG_CORE + ".ATTR_ENVIRONMENT"; //$NON-NLS-1$

	/**
	 * Attribute key for the path of the working directory for an {@link IProcess}
	 *
	 * @see IProcess
	 * @since 3.8
	 */
	public static final String ATTR_WORKING_DIRECTORY = PI_DEBUG_CORE + ".ATTR_WORKING_DIRECTORY"; //$NON-NLS-1$

	/**
	 * Attribute key for path of the executable that launched an {@link IProcess}
	 *
	 * @see IProcess
	 * @since 3.8
	 */
	public static final String ATTR_PATH = PI_DEBUG_CORE + ".ATTR_PATH"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute that designates whether or not the
	 * descendants of the {@link IProcess} associated to a launch of this
	 * configuration should be terminated if the main-process is terminated. The
	 * descendants (also called child- or sub-processes) of a operating system
	 * process are the processes started by that process.
	 *
	 * Value is a string representing a boolean - <code>true</code> or
	 * <code>false</code>. When unspecified, the default value is considered
	 * <code>true</code>.
	 *
	 * @since 3.18
	 */
	public static final String ATTR_TERMINATE_DESCENDANTS = PI_DEBUG_CORE + ".TERMINATE_DESCENDANTS"; //$NON-NLS-1$

	/**
	 * The singleton debug plug-in instance.
	 */
	private static DebugPlugin fgDebugPlugin= null;

	/**
	 * The singleton breakpoint manager.
	 */
	private BreakpointManager fBreakpointManager;

	/**
	 * The singleton expression manager.
	 */
	private ExpressionManager fExpressionManager;

	/**
	 * The singleton launch manager.
	 */
	private LaunchManager fLaunchManager;

	/**
	 * The singleton memory block manager.
	 * @since 3.1
	 */
	private MemoryBlockManager fMemoryBlockManager;

	/**
	 * The collection of debug event listeners.
	 */
	private ListenerList<IDebugEventSetListener> fEventListeners = new ListenerList<>();

	/**
	 * Event filters, or <code>null</code> if none.
	 */
	private ListenerList<IDebugEventFilter> fEventFilters = new ListenerList<>();

	/**
	 * Whether this plug-in is in the process of shutting
	 * down.
	 */
	private boolean fShuttingDown= false;

	/**
	 * Table of status handlers. Keys are {plug-in identifier, status code}
	 * pairs, and values are associated <code>IConfigurationElement</code>s.
	 */
	private HashMap<StatusHandlerKey, IConfigurationElement> fStatusHandlers = null;

	/**
	 * Map of process factories. Keys are process factory IDs
	 * and values are associated <code>IConfigurationElement</code>s.
	 * @since 3.0
	 */
	private HashMap<String, IConfigurationElement> fProcessFactories = null;

	/**
	 * Mode constants for the event notifier
	 */
	private static final int NOTIFY_FILTERS = 0;
	private static final int NOTIFY_EVENTS = 1;

	/**
	 * Queue of debug events to fire to listeners and asynchronous runnables to execute
	 * in the order received.
	 *
	 * @since 3.1
	 */
	private List<Object> fEventQueue = new ArrayList<>();

	/**
	 * Job to fire events to listeners.
	 * @since 3.1
	 */
	private EventDispatchJob fEventDispatchJob = new EventDispatchJob();

	/**
	 * Event dispatch job. Processes event queue of debug events and runnables.
	 *
	 * @since 3.1
	 */
	class EventDispatchJob extends Job {

		EventNotifier fNotifier = new EventNotifier();
		AsynchRunner fRunner = new AsynchRunner();

		/**
		 * Creates a new event dispatch job.
		 */
		public EventDispatchJob() {
			super(DebugCoreMessages.DebugPlugin_1);
			setPriority(Job.INTERACTIVE);
			setSystem(true);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {

			while (!fEventQueue.isEmpty()) {
				Object next = null;
				synchronized (fEventQueue) {
					if (!fEventQueue.isEmpty()) {
						next = fEventQueue.remove(0);
					}
				}
				if (next instanceof Runnable) {
					fRunner.async((Runnable) next);
				} else if (next != null) {
					fNotifier.dispatch((DebugEvent[]) next);
				}
			}
			return Status.OK_STATUS;
		}

		@Override
		public boolean shouldRun() {
			return shouldSchedule();
		}

		@Override
		public boolean shouldSchedule() {
			return !(isShuttingDown() || fEventListeners.isEmpty());
		}

	}

	/**
	 * Returns the singleton instance of the debug plug-in.
	 *
	 * @return the debug plug-in
	 */
	public static DebugPlugin getDefault() {
		return fgDebugPlugin;
	}

	/**
	 * Sets the singleton instance of the debug plug-in.
	 *
	 * @param plugin the debug plug-in, or <code>null</code>
	 *  when shutting down
	 */
	private static void setDefault(DebugPlugin plugin) {
		fgDebugPlugin = plugin;
	}

	/**
	 * Convenience method which returns the unique identifier of this plug-in.
	 *
	 * @return debug plug-in identifier
	 */
	public static String getUniqueIdentifier() {
		return PI_DEBUG_CORE;
	}

	/**
	 * Constructs the debug plug-in.
	 * <p>
	 * An instance of this plug-in runtime class is automatically created
	 * when the facilities provided by this plug-in are required.
	 * <b>Clients must never explicitly instantiate a plug-in runtime class.</b>
	 * </p>
	 */
	public DebugPlugin() {
		super();
		setDefault(this);
	}

	/**
	 * Adds the given listener to the collection of registered debug
	 * event listeners. Has no effect if an identical listener is already
	 * registered.
	 *
	 * @param listener the listener to add
	 * @since 2.0
	 */
	public void addDebugEventListener(IDebugEventSetListener listener) {
		fEventListeners.add(listener);
	}

	/**
	 * Notifies all registered debug event set listeners of the given
	 * debug events. Events which are filtered by a registered debug event
	 * filter are not fired.
	 *
	 * @param events array of debug events to fire
	 * @see IDebugEventFilter
	 * @see IDebugEventSetListener
	 * @since 2.0
	 */
	public void fireDebugEventSet(DebugEvent[] events) {
		if (isShuttingDown() || events == null || fEventListeners.isEmpty()) {
			return;
		}
		synchronized (fEventQueue) {
			fEventQueue.add(events);
		}
		fEventDispatchJob.schedule();
	}

	/**
	 * Asynchronously executes the given runnable in a separate
	 * thread, after debug event dispatch has completed. If debug
	 * events are not currently being dispatched, the runnable is
	 * scheduled to run in a separate thread immediately.
	 *
	 * @param r runnable to execute asynchronously
	 * @since 2.1
	 */
	public void asyncExec(Runnable r) {
		synchronized (fEventQueue) {
			fEventQueue.add(r);
		}
		fEventDispatchJob.schedule();
	}

	/**
	 * Returns the breakpoint manager.
	 *
	 * @return the breakpoint manager
	 * @see IBreakpointManager
	 */
	public synchronized IBreakpointManager getBreakpointManager() {
		if (fBreakpointManager == null) {
			fBreakpointManager = new BreakpointManager();
		}
		return fBreakpointManager;
	}

	/**
	 * Returns the launch manager.
	 *
	 * @return the launch manager
	 * @see ILaunchManager
	 */
	public synchronized ILaunchManager getLaunchManager() {
		if (fLaunchManager == null) {
			fLaunchManager = new LaunchManager();
			fLaunchManager.getAllLaunchConfigurations();
			// monitor launch configuration renames for launch groups
			fLaunchManager.addLaunchConfigurationListener(new GroupMemberChangeListener());
		}
		return fLaunchManager;
	}

	/**
	 * Returns the memory block manager.
	 * @return the memory block manager.
	 * @see IMemoryBlockManager
	 * @since 3.1
	 */
	public synchronized IMemoryBlockManager getMemoryBlockManager(){
		if (fMemoryBlockManager == null) {
			fMemoryBlockManager = new MemoryBlockManager();
		}
		return fMemoryBlockManager;
	}

	/**
	 * Returns the status handler registered for the given
	 * status, or <code>null</code> if none.
	 *
	 * @param status status for which a status handler has been requested
	 * @return the status handler registered for the given
	 *  status, or <code>null</code> if none
	 * @since 2.0
	 */
	public IStatusHandler getStatusHandler(IStatus status) {
		boolean enabled = Platform.getPreferencesService().getBoolean(DebugPlugin.getUniqueIdentifier(), IInternalDebugCoreConstants.PREF_ENABLE_STATUS_HANDLERS, true, null);
		if (!enabled) {
			return null;
		}
		StatusHandlerKey key = new StatusHandlerKey(status.getPlugin(), status.getCode());
		if (fStatusHandlers == null) {
			initializeStatusHandlers();
		}
		IConfigurationElement config = fStatusHandlers.get(key);
		if (config != null) {
			try {
				Object handler = config.createExecutableExtension(IConfigurationElementConstants.CLASS);
				if (handler instanceof IStatusHandler) {
					return (IStatusHandler)handler;
				}
				invalidStatusHandler(null, MessageFormat.format("Registered status handler {0} does not implement required interface IStatusHandler.", new Object[] { config.getDeclaringExtension().getUniqueIdentifier() })); //$NON-NLS-1$
			} catch (CoreException e) {
				log(e);
			}
		}
		return null;
	}

	/**
	 * Returns the expression manager.
	 *
	 * @return the expression manager
	 * @see IExpressionManager
	 * @since 2.0
	 */
	public synchronized IExpressionManager getExpressionManager() {
		if (fExpressionManager == null) {
			fExpressionManager = new ExpressionManager();
		}
		return fExpressionManager;
	}

	/**
	 * Removes the given listener from the collection of registered debug
	 * event listeners. Has no effect if an identical listener is not already
	 * registered.
	 *
	 * @param listener the listener to remove
	 * @since 2.0
	 */
	public void removeDebugEventListener(IDebugEventSetListener listener) {
		fEventListeners.remove(listener);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			setShuttingDown(true);

			if (fLaunchManager != null) {
				fLaunchManager.shutdown();
			}
			if (fBreakpointManager != null) {
				fBreakpointManager.shutdown();
			}
			if (fMemoryBlockManager != null)  {
				fMemoryBlockManager.shutdown();
			}

			fEventListeners.clear();
			fEventFilters.clear();

			SourceLookupUtils.shutdown();
			Preferences.savePreferences(DebugPlugin.getUniqueIdentifier());
			ResourcesPlugin.getWorkspace().removeSaveParticipant(getUniqueIdentifier());
		} finally {
			super.stop(context);
			setDefault(null);
		}
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		new DebugOptions(context);
		ResourcesPlugin.getWorkspace().addSaveParticipant(getUniqueIdentifier(),
				new ISaveParticipant() {
					@Override
					public void saving(ISaveContext saveContext) throws CoreException {
						if (fExpressionManager != null) {
							fExpressionManager.storeWatchExpressions();
						}
						Preferences.savePreferences(DebugPlugin.getUniqueIdentifier());
					}
					@Override
					public void rollback(ISaveContext saveContext) {}
					@Override
					public void prepareToSave(ISaveContext saveContext) throws CoreException {}
					@Override
					public void doneSaving(ISaveContext saveContext) {}
				});
		//command adapters
		IAdapterManager manager= Platform.getAdapterManager();
		CommandAdapterFactory actionFactory = new CommandAdapterFactory();
		manager.registerAdapters(actionFactory, IDisconnect.class);
		manager.registerAdapters(actionFactory, IDropToFrame.class);
		manager.registerAdapters(actionFactory, IStep.class);
		manager.registerAdapters(actionFactory, IStepFilters.class);
		manager.registerAdapters(actionFactory, ISuspendResume.class);
		manager.registerAdapters(actionFactory, ITerminate.class);
		manager.registerAdapters(actionFactory, ILaunch.class);
		manager.registerAdapters(actionFactory, IProcess.class);
		manager.registerAdapters(actionFactory, IDebugElement.class);
	}

	/**
	 * Creates and returns a new process representing the given
	 * <code>java.lang.Process</code>. A streams proxy is created
	 * for the I/O streams in the system process. The process
	 * is added to the given launch.
	 * <p>
	 * If the launch configuration associated with the given launch
	 * specifies a process factory, it will be used to instantiate
	 * the new process.
	 * </p>
	 * @param launch the launch the process is contained in
	 * @param process the system process to wrap
	 * @param label the label assigned to the process
	 * @return the process
	 * @see IProcess
	 * @see IProcessFactory
	 */
	public static IProcess newProcess(ILaunch launch, Process process, String label) {
		return newProcess(launch, process, label, null);
	}

	/**
	 * Creates and returns a new process representing the given
	 * <code>java.lang.Process</code>. A streams proxy is created
	 * for the I/O streams in the system process. The process
	 * is added to the given launch, and the process is initialized
	 * with the given attribute map.
	 * <p>
	 * If the launch configuration associated with the given launch
	 * specifies a process factory, it will be used to instantiate
	 * the new process.
	 * </p>
	 * @param launch the launch the process is contained in
	 * @param process the system process to wrap
	 * @param label the label assigned to the process
	 * @param attributes initial values for the attribute map
	 * @return the process <code>null</code> can be returned if errors occur dealing with the process factory
	 * designated to create the process.
	 * @see IProcess
	 * @see IProcessFactory
	 * @since 2.1
	 */
	public static IProcess newProcess(ILaunch launch, Process process, String label, Map<String, String> attributes) {
		ILaunchConfiguration config= launch.getLaunchConfiguration();
		String processFactoryID= null;
		if (config != null) {
			try {
				processFactoryID= config.getAttribute(ATTR_PROCESS_FACTORY_ID, (String)null);
			} catch (CoreException e) {
			}
		}
		if (processFactoryID != null) {
			DebugPlugin plugin= DebugPlugin.getDefault();
			if (plugin.fProcessFactories == null) {
				plugin.initializeProcessFactories();
			}
			IConfigurationElement element= plugin.fProcessFactories.get(processFactoryID);
			if (element == null) {
				return null;
			}
			IProcessFactory processFactory= null;
			try {
				processFactory = (IProcessFactory)element.createExecutableExtension(IConfigurationElementConstants.CLASS);
			} catch (CoreException exception) {
				log(exception);
				return null;
			}
			return processFactory.newProcess(launch, process, label, attributes);
		}
		return new RuntimeProcess(launch, process, label, attributes);
	}

	/**
	 * Returns any logical structure types that have been contributed for the given
	 * value.
	 *
	 * @param value the value for which logical structure types have been requested
	 * @return logical structure types that have been contributed for the given
	 * value, possibly an empty collection
	 *
	 * @since 3.0
	 */
	public static ILogicalStructureType[] getLogicalStructureTypes(IValue value) {
		return LogicalStructureManager.getDefault().getLogicalStructureTypes(value);
	}

	/**
	 * Returns the default logical structure type among the given combination of
	 * logical structure types, or <code>null</code> if none. When the given combination
	 * of logical structure type is applicable for a value, the default logical structure
	 * type is used to display a value.
	 *
	 * @param types a combination of structures applicable to a value
	 * @return the default structure that should be used to display the value
	 * or <code>null</code> if none
	 *
	 * @since 3.1
	 */
	public static ILogicalStructureType getDefaultStructureType(ILogicalStructureType[] types) {
		return LogicalStructureManager.getDefault().getSelectedStructureType(types);
	}

	/**
	 * Sets the default logical structure type among the given combination of logical structure
	 * types. The logical structure types provided should all be applicable to a single
	 * value. Specifying <code>null</code> indicates there is no default logical structure
	 * for the given combination of types.
	 *
	 * @param types a combination of logical structure types applicable to a value
	 * @param def the default logical structure among the given combination of types
	 * or <code>null</code> if none
	 *
	 * @since 3.1
	 */
	public static void setDefaultStructureType(ILogicalStructureType[] types, ILogicalStructureType def) {
		LogicalStructureManager.getDefault().setEnabledType(types, def);
	}

	/**
	 * Convenience method that performs a runtime exec on the given command line
	 * in the context of the specified working directory, and returns the
	 * resulting process. If the current runtime does not support the
	 * specification of a working directory, the status handler for error code
	 * <code>ERR_WORKING_DIRECTORY_NOT_SUPPORTED</code> is queried to see if the
	 * exec should be re-executed without specifying a working directory.
	 *
	 * @param cmdLine the command line
	 * @param workingDirectory the working directory, or <code>null</code>
	 * @return the resulting process or <code>null</code> if the exec is
	 *  canceled
	 * @exception CoreException if the exec fails
	 * @see Runtime
	 *
	 * @since 2.1
	 */
	public static Process exec(String[] cmdLine, File workingDirectory) throws CoreException {
		return exec(cmdLine, workingDirectory, null);
	}

	/**
	 * Convenience method that performs a runtime exec on the given command line
	 * in the context of the specified working directory, and returns the
	 * resulting process. If the current runtime does not support the
	 * specification of a working directory, the status handler for error code
	 * <code>ERR_WORKING_DIRECTORY_NOT_SUPPORTED</code> is queried to see if the
	 * exec should be re-executed without specifying a working directory.
	 *
	 * @param cmdLine the command line
	 * @param workingDirectory the working directory, or <code>null</code>
	 * @param envp the environment variables set in the process, or <code>null</code>
	 * @return the resulting process or <code>null</code> if the exec is
	 *  canceled
	 * @exception CoreException if the exec fails
	 * @see Runtime
	 *
	 * @since 3.0
	 */
	public static Process exec(String[] cmdLine, File workingDirectory, String[] envp) throws CoreException {
		return exec(cmdLine, workingDirectory, envp, false);
	}

	/**
	 * Convenience method that performs a runtime exec on the given command line
	 * in the context of the specified working directory, and returns the
	 * resulting process. If the current runtime does not support the
	 * specification of a working directory, the status handler for error code
	 * <code>ERR_WORKING_DIRECTORY_NOT_SUPPORTED</code> is queried to see if the
	 * exec should be re-executed without specifying a working directory.
	 *
	 * @param cmdLine the command line
	 * @param workingDirectory the working directory, or <code>null</code>
	 * @param envp the environment variables set in the process, or
	 *            <code>null</code>
	 * @param mergeOutput if <code>true</code> the error stream will be merged
	 *            with standard output stream and both can be read through the
	 *            same output stream
	 * @return the resulting process or <code>null</code> if the exec is
	 *         canceled
	 * @exception CoreException if the exec fails
	 * @see Runtime
	 *
	 * @since 3.14
	 */
	public static Process exec(String[] cmdLine, File workingDirectory, String[] envp, boolean mergeOutput) throws CoreException {
		Process p = null;
		try {
			// starting with and without merged output could be done with the
			// same process builder approach but since the handling of
			// environment variables is slightly different between
			// ProcessBuilder and Runtime.exec only the new option uses process
			// builder to not break existing caller of this method
			if (mergeOutput) {
				ProcessBuilder pb = new ProcessBuilder(cmdLine);
				pb.directory(workingDirectory);
				pb.redirectErrorStream(mergeOutput);
				if (envp != null) {
					Map<String, String> env = pb.environment();
					env.clear();
					for (String e : envp) {
						int index = e.indexOf('=');
						if (index != -1) {
							env.put(e.substring(0, index), e.substring(index + 1));
						}
					}
				}
				p = pb.start();
			} else if (workingDirectory == null) {
				p = Runtime.getRuntime().exec(cmdLine, envp);
			} else {
				p = Runtime.getRuntime().exec(cmdLine, envp, workingDirectory);
			}
		} catch (IOException e) {
			Status status = new Status(IStatus.ERROR, getUniqueIdentifier(), ERROR, DebugCoreMessages.DebugPlugin_0, e);
			throw new CoreException(status);
		} catch (NoSuchMethodError e) {
			//attempting launches on 1.2.* - no ability to set working directory
			IStatus status = new Status(IStatus.ERROR, getUniqueIdentifier(), ERR_WORKING_DIRECTORY_NOT_SUPPORTED, DebugCoreMessages.DebugPlugin_Eclipse_runtime_does_not_support_working_directory_2, e);
			IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(status);

			if (handler != null) {
				Object result = handler.handleStatus(status, null);
				if (result instanceof Boolean && ((Boolean) result).booleanValue()) {
					p = exec(cmdLine, null);
				}
			}
		}
		return p;
	}

	/**
	 * Returns whether this plug-in is in the process of
	 * being shutdown.
	 *
	 * @return whether this plug-in is in the process of
	 *  being shutdown
	 */
	private boolean isShuttingDown() {
		return fShuttingDown;
	}

	/**
	 * Sets whether this plug-in is in the process of
	 * being shutdown.
	 *
	 * @param value whether this plug-in is in the process of
	 *  being shutdown
	 */
	private void setShuttingDown(boolean value) {
		fShuttingDown = value;
	}


	/**
	 * Adds the given debug event filter to the registered
	 * event filters. Has no effect if an identical filter
	 * is already registered.
	 *
	 * @param filter debug event filter
	 * @since 2.0
	 */
	public void addDebugEventFilter(IDebugEventFilter filter) {
		fEventFilters.add(filter);
	}

	/**
	 * Removes the given debug event filter from the registered
	 * event filters. Has no effect if an identical filter
	 * is not already registered.
	 *
	 * @param filter debug event filter
	 * @since 2.0
	 */
	public void removeDebugEventFilter(IDebugEventFilter filter) {
		fEventFilters.remove(filter);
	}

	/**
	 * Logs the given message if in debug mode.
	 *
	 * @param message the message to log
	 * @since 2.0
	 */
	public static void logDebugMessage(String message) {
		if (getDefault().isDebugging()) {
			// this message is intentionally not externalized, as an exception may
			// be due to the resource bundle itself
			log(new Status(IStatus.ERROR, getUniqueIdentifier(), ERROR, MessageFormat.format(DebugCoreMessages.DebugPlugin_2, new Object[] { message }), null));
		}
	}

	/**
	 * Logs the given message with this plug-in's log and the given
	 * throwable or <code>null</code> if none.
	 * @param message the message to log
	 * @param throwable the exception that occurred or <code>null</code> if none
	 */
	public static void logMessage(String message, Throwable throwable) {
		log(new Status(IStatus.ERROR, getUniqueIdentifier(), ERROR, message, throwable));
	}

	/**
	 * Logs the specified status with this plug-in's log.
	 *
	 * @param status status to log
	 * @since 2.0
	 */
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	/**
	 * Logs the specified throwable with this plug-in's log.
	 *
	 * @param t throwable to log
	 * @since 2.0
	 */
	public static void log(Throwable t) {
		IStatus status= new Status(IStatus.ERROR, getUniqueIdentifier(), ERROR, DebugCoreMessages.DebugPlugin_3, t);
		log(status);
	}

	/**
	 * Register status handlers.
	 *
	 */
	private void initializeStatusHandlers() {
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.PI_DEBUG_CORE, EXTENSION_POINT_STATUS_HANDLERS);
		IConfigurationElement[] infos= extensionPoint.getConfigurationElements();
		fStatusHandlers = new HashMap<>(infos.length);
		for (IConfigurationElement configurationElement : infos) {
			String id = configurationElement.getAttribute("plugin"); //$NON-NLS-1$
			String code = configurationElement.getAttribute("code"); //$NON-NLS-1$

			if (id != null && code != null) {
				try {
					StatusHandlerKey key = new StatusHandlerKey(id, Integer.parseInt(code));
					fStatusHandlers.put(key, configurationElement);
				} catch (NumberFormatException e) {
					// invalid status handler
					invalidStatusHandler(e, configurationElement.getAttribute("id")); //$NON-NLS-1$
				}
			} else {
				// invalid status handler
				invalidStatusHandler(null, configurationElement.getAttribute("id")); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Register process factories.
	 *
	 */
	private void initializeProcessFactories() {
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.PI_DEBUG_CORE, EXTENSION_POINT_PROCESS_FACTORIES);
		IConfigurationElement[] infos= extensionPoint.getConfigurationElements();
		fProcessFactories = new HashMap<>(infos.length);
		for (IConfigurationElement configurationElement : infos) {
			String id = configurationElement.getAttribute("id"); //$NON-NLS-1$
			String clss = configurationElement.getAttribute("class"); //$NON-NLS-1$
			if (id != null && clss != null) {
				fProcessFactories.put(id, configurationElement);
			} else {
				// invalid process factory
				String badDefiner = configurationElement.getContributor().getName();
				log(new Status(IStatus.ERROR, DebugPlugin.PI_DEBUG_CORE, ERROR, MessageFormat.format(DebugCoreMessages.DebugPlugin_4, new Object[] {
					badDefiner, id }), null));
			}
		}
	}

	private void invalidStatusHandler(Exception e, String id) {
		log(new Status(IStatus.ERROR, DebugPlugin.PI_DEBUG_CORE, ERROR, MessageFormat.format(DebugCoreMessages.DebugPlugin_5, new Object[] { id }), e));
	}

	/**
	 * Key for status handler extensions - a plug-in identifier/code pair
	 */
	class StatusHandlerKey {

		String fPluginId;
		int fCode;

		StatusHandlerKey(String pluginId, int code) {
			fPluginId = pluginId;
			fCode = code;
		}

		@Override
		public int hashCode() {
			return fPluginId.hashCode() + fCode;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof StatusHandlerKey) {
				StatusHandlerKey s = (StatusHandlerKey)obj;
				return fCode == s.fCode && fPluginId.equals(s.fPluginId);
			}
			return false;
		}
	}

	/**
	 * Executes runnables after event dispatch is complete.
	 *
	 * @since 3.0
	 */
	class AsynchRunner implements ISafeRunnable {

		private Runnable fRunnable = null;

		void async(Runnable runnable) {
			fRunnable = runnable;
			SafeRunner.run(this);
			fRunnable = null;

		}

		@Override
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, getUniqueIdentifier(), ERROR, DebugCoreMessages.DebugPlugin_6, exception);
			log(status);
		}

		@Override
		public void run() throws Exception {
			fRunnable.run();
		}

	}

	/**
	 * Filters and dispatches events in a safe runnable to handle any
	 * exceptions.
	 */
	class EventNotifier implements ISafeRunnable {

		private DebugEvent[] fEvents;
		private IDebugEventSetListener fListener;
		private IDebugEventFilter fFilter;
		private int fMode;

		@Override
		public void handleException(Throwable exception) {
			switch (fMode) {
				case NOTIFY_FILTERS:
					IStatus status = new Status(IStatus.ERROR, getUniqueIdentifier(), ERROR, DebugCoreMessages.DebugPlugin_7, exception);
					log(status);
					break;
				case NOTIFY_EVENTS:
					status = new Status(IStatus.ERROR, getUniqueIdentifier(), ERROR, DebugCoreMessages.DebugPlugin_8, exception);
					log(status);
					break;
				default:
					break;
			}
		}

		@Override
		public void run() throws Exception {
			switch (fMode) {
				case NOTIFY_FILTERS:
					fEvents = fFilter.filterDebugEvents(fEvents);
					break;
				case NOTIFY_EVENTS:
					fListener.handleDebugEvents(fEvents);
					break;
				default:
					break;
			}
		}

		/**
		 * Filter and dispatch the given events. If an exception occurs in one
		 * listener, events are still fired to subsequent listeners.
		 *
		 * @param events debug events
		 */
		void dispatch(DebugEvent[] events) {
			fEvents = events;
			if (!fEventFilters.isEmpty()) {
				fMode = NOTIFY_FILTERS;
				for (IDebugEventFilter iDebugEventFilter : fEventFilters) {
					fFilter = iDebugEventFilter;
					SafeRunner.run(this);
					if (fEvents == null || fEvents.length == 0) {
						return;
					}
				}
			}

			fMode = NOTIFY_EVENTS;
			if (DebugOptions.DEBUG_EVENTS) {
				for (DebugEvent event : fEvents) {
					DebugOptions.trace(event.toString());
				}
			}
			for (IDebugEventSetListener iDebugEventSetListener : fEventListeners) {
				fListener = iDebugEventSetListener;
				SafeRunner.run(this);
			}
			fEvents = null;
			fFilter = null;
			fListener = null;
		}

	}

	/**
	 * Creates and returns a new XML document.
	 *
	 * @return a new XML document
	 * @throws CoreException if unable to create a new document
	 * @since 3.0
	 */
	public static Document newDocument()throws CoreException {
		try {
			return LaunchManager.getDocument();
		} catch (ParserConfigurationException e) {
			abort("Unable to create new XML document.", e);  //$NON-NLS-1$
		}
		return null;
	}

	/**
	 * Serializes the given XML document into a string.
	 *
	 * @param document XML document to serialize
	 * @return a string representing the given document
	 * @throws CoreException if unable to serialize the document
	 * @since 3.0
	 */
	public static String serializeDocument(Document document) throws CoreException {
		try {
			return LaunchManager.serializeDocument(document);
		} catch (TransformerException e) {
			abort("Unable to serialize XML document.", e);  //$NON-NLS-1$
		} catch (IOException e) {
			abort("Unable to serialize XML document.",e);  //$NON-NLS-1$
		}
		return null;
	}

	/**
	 * Parses the given string representing an XML document, returning its
	 * root element.
	 *
	 * @param document XML document as a string
	 * @return the document's root element
	 * @throws CoreException if unable to parse the document
	 * @since 3.0
	 */
	public static Element parseDocument(String document) throws CoreException {
		Element root = null;
		InputStream stream = null;
		try{
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			parser.setErrorHandler(new DefaultHandler());
			stream = new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8));
			root = parser.parse(stream).getDocumentElement();
		} catch (ParserConfigurationException e) {
			abort("Unable to parse XML document.", e);  //$NON-NLS-1$
		} catch (FactoryConfigurationError e) {
			abort("Unable to parse XML document.", e);  //$NON-NLS-1$
		} catch (SAXException e) {
			abort("Unable to parse XML document.", e);  //$NON-NLS-1$
		} catch (IOException e) {
			abort("Unable to parse XML document.", e);  //$NON-NLS-1$
		} finally {
			try{
				if (stream != null) {
					stream.close();
				}
			} catch(IOException e) {
				abort("Unable to parse XML document.", e);  //$NON-NLS-1$
			}
		}
		return root;
	}

	/**
	 * Throws an exception with the given message and underlying exception.
	 *
	 * @param message error message
	 * @param exception underlying exception, or <code>null</code>
	 * @throws CoreException if a problem is encountered
	 */
	private static void abort(String message, Throwable exception) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, message, exception);
		throw new CoreException(status);
	}

	private static String[] parseArgumentsWindows(String args, boolean split) {
		// see http://msdn.microsoft.com/en-us/library/a1y7w461.aspx
		List<String> result = new ArrayList<>();

		final int DEFAULT= 0;
		final int ARG= 1;
		final int IN_DOUBLE_QUOTE= 2;

		int state= DEFAULT;
		int backslashes= 0;
		StringBuilder buf= new StringBuilder();
		int len= args.length();
		for (int i= 0; i < len; i++) {
			char ch= args.charAt(i);
			if (ch == '\\') {
				backslashes++;
				continue;
			} else if (backslashes != 0) {
				if (ch == '"') {
					for (; backslashes >= 2; backslashes-= 2) {
						buf.append('\\');
						if (split) {
							buf.append('\\');
						}
					}
					if (backslashes == 1) {
						if (state == DEFAULT) {
							state= ARG;
						}
						if (split) {
							buf.append('\\');
						}
						buf.append('"');
						backslashes= 0;
						continue;
					} // else fall through to switch
				} else {
					// false alarm, treat passed backslashes literally...
					if (state == DEFAULT) {
						state= ARG;
					}
					for (; backslashes > 0; backslashes--) {
						buf.append('\\');
					}
					// fall through to switch
				}
			}
			if (Character.isWhitespace(ch)) {
				if (state == DEFAULT) {
					// skip
					continue;
				} else if (state == ARG) {
					state= DEFAULT;
					result.add(buf.toString());
					buf.setLength(0);
					continue;
				}
			}
			switch (state) {
				case DEFAULT:
				case ARG:
					if (ch == '"') {
						state= IN_DOUBLE_QUOTE;
						if (split) {
							buf.append(ch);
						}
					} else {
						state= ARG;
						buf.append(ch);
					}
					break;

				case IN_DOUBLE_QUOTE:
					if (ch == '"') {
						if (i + 1 < len && args.charAt(i + 1) == '"') {
							/* Undocumented feature in Windows:
							 * Two consecutive double quotes inside a double-quoted argument are interpreted as
							 * a single double quote.
							 */
							buf.append('"');
							i++;
							if (split) {
								buf.append(ch);
							}
						} else if (buf.length() == 0) {
							// empty string on Windows platform. Account for bug in constructor of JDK's java.lang.ProcessImpl.
							result.add("\"\""); //$NON-NLS-1$
							state= DEFAULT;
						} else {
							state= ARG;
							if (split) {
								buf.append(ch);
							}
						}
					} else {
						buf.append(ch);
					}
					break;

				default:
					throw new IllegalStateException();
			}
		}
		if (buf.length() > 0 || state != DEFAULT) {
			result.add(buf.toString());
		}

		return result.toArray(new String[result.size()]);
	}

	private static String[] parseArgumentsImpl(String args, boolean split) {
		// man sh, see topic QUOTING
		List<String> result = new ArrayList<>();

		final int DEFAULT= 0;
		final int ARG= 1;
		final int IN_DOUBLE_QUOTE= 2;
		final int IN_SINGLE_QUOTE= 3;

		int state= DEFAULT;
		StringBuilder buf= new StringBuilder();
		int len= args.length();
		for (int i= 0; i < len; i++) {
			char ch= args.charAt(i);
			if (Character.isWhitespace(ch)) {
				if (state == DEFAULT) {
					// skip
					continue;
				} else if (state == ARG) {
					state= DEFAULT;
					result.add(buf.toString());
					buf.setLength(0);
					continue;
				}
			}
			switch (state) {
				case DEFAULT:
				case ARG:
					if (ch == '"') {
						if (split) {
							buf.append(ch);
						}
						state= IN_DOUBLE_QUOTE;
					} else if (ch == '\'') {
						if (split) {
							buf.append(ch);
						}
						state= IN_SINGLE_QUOTE;
					} else if (ch == '\\' && i + 1 < len) {
						if (split) {
							buf.append(ch);
						}
						state= ARG;
						ch= args.charAt(++i);
						buf.append(ch);
					} else {
						state= ARG;
						buf.append(ch);
					}
					break;

				case IN_DOUBLE_QUOTE:
					if (ch == '"') {
						if (split) {
							buf.append(ch);
						}
						state= ARG;
					} else if (ch == '\\' && i + 1 < len &&
							(args.charAt(i + 1) == '\\' || args.charAt(i + 1) == '"')) {
						if (split) {
							buf.append(ch);
						}
						ch= args.charAt(++i);
						buf.append(ch);
					} else {
						buf.append(ch);
					}
					break;

				case IN_SINGLE_QUOTE:
					if (ch == '\'') {
						if (split) {
							buf.append(ch);
						}
						state= ARG;
					} else {
						buf.append(ch);
					}
					break;

				default:
					throw new IllegalStateException();
			}
		}
		if (buf.length() > 0 || state != DEFAULT) {
			result.add(buf.toString());
		}

		return result.toArray(new String[result.size()]);
	}

	/**
	 * Parses the given command line into separate arguments that can be passed
	 * to <code>DebugPlugin.exec(String[], File)</code>. Embedded quotes and
	 * backslashes are interpreted, i.e. the resulting arguments are in the form
	 * that will be passed to an invoked process.
	 * <p>
	 * The reverse operation is {@link #renderArguments(String[], int[])}.
	 * </p>
	 *
	 * @param args command line arguments as a single string
	 * @return individual arguments
	 * @see #renderArguments(String[], int[])
	 * @since 3.1
	 */
	public static String[] parseArguments(String args) {
		if (args == null) {
			return new String[0];
		}

		if (Constants.OS_WIN32.equals(Platform.getOS())) {
			return parseArgumentsWindows(args, false);
		}

		return parseArgumentsImpl(args, false);
	}

	/**
	 * Splits the given command line into separate arguments that can be
	 * concatenated with a space as joiner. Embedded quotes and backslashes are
	 * kept as is (i.e. not interpreted).
	 * <p>
	 * Use this method to avoid e.g. losing quotes around an argument like
	 * <code>"${env_var:A}"</code>, which may later be substituted by a string
	 * that contains spaces.
	 * </p>
	 *
	 * @param args command line arguments as a single string
	 * @return individual arguments in original form
	 * @since 3.10
	 */
	public static String[] splitArguments(String args) {
		if (args == null) {
			return new String[0];
		}

		if (Constants.OS_WIN32.equals(Platform.getOS())) {
			return parseArgumentsWindows(args, true);
		}

		return parseArgumentsImpl(args, true);
	}

	/**
	 * Renders the given array of argument strings into a single command line.
	 * <p>
	 * If an argument contains whitespace, it it quoted. Contained quotes or
	 * backslashes will be escaped.
	 * </p>
	 * <p>
	 * If <code>segments</code> is not <code>null</code>, the array is filled
	 * with the offsets of the start positions of arguments 1 to
	 * <code>arguments.length - 1</code>, as rendered in the resulting string.
	 * </p>
	 *
	 * @param arguments the command line arguments
	 * @param segments an array of size <code>arguments.length - 1</code> or
	 *            <code>null</code>
	 * @return the command line
	 * @see #parseArguments(String)
	 * @since 3.8
	 */
	public static String renderArguments(String[] arguments, int[] segments) {
		boolean isWin32= Platform.getOS().equals(Constants.OS_WIN32);
		StringBuilder buf = new StringBuilder();
		int count = arguments.length;
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				buf.append(' ');
			}

			boolean containsSpace = false;
			char[] characters = arguments[i].toCharArray();
			for (char ch : characters) {
				if (ch == ' ' || ch == '\t') {
					containsSpace = true;
					buf.append('"');
					break;
				}
			}

			int backslashes = 0;
			for (int j = 0; j < characters.length; j++) {
				char ch = characters[j];
				if (ch == '"') {
					if (isWin32) {
						if (j == 0 && characters.length == 2 && characters[1] == '"') {
							// empty string on windows platform, see bug 130767. Bug in constructor of JDK's java.lang.ProcessImpl.
							buf.append("\"\""); //$NON-NLS-1$
							break;
						}
						if (backslashes > 0) {
							// Feature in Windows: need to double-escape backslashes in front of double quote.
							for (; backslashes > 0; backslashes--) {
								buf.append('\\');
							}
						}
					}
					buf.append('\\');
				} else if (ch == '\\') {
					if (isWin32) {
						backslashes++;
					} else {
						buf.append('\\');
					}
				} else if (isWin32) {
					backslashes = 0;
				}
				buf.append(ch);
			}
			if (containsSpace) {
				buf.append('"');
			} else if (characters.length == 0) {
				buf.append("\"\""); //$NON-NLS-1$
			}

			if (segments != null && i < count - 1) {
				segments[i] = buf.length() + 1;
			}
		}
		return buf.toString();
	}

	/**
	 * Sets whether step filters should be applied to step commands. This
	 * setting is a global option applied to all registered debug targets.
	 *
	 * @param useStepFilters whether step filters should be applied to step
	 *  commands
	 * @since 3.3
	 * @see org.eclipse.debug.core.model.IStepFilters
	 */
	public static void setUseStepFilters(boolean useStepFilters) {
		getStepFilterManager().setUseStepFilters(useStepFilters);
	}

	/**
	 * Returns whether step filters are applied to step commands.
	 *
	 * @return whether step filters are applied to step commands
	 * @since 3.3
	 * @see org.eclipse.debug.core.model.IStepFilters
	 * @see org.eclipse.debug.core.commands.IStepFiltersHandler
	 */
	public static boolean isUseStepFilters() {
		return getStepFilterManager().isUseStepFilters();
	}

	/**
	 * Returns any step filters that have been contributed for the given model
	 * identifier.
	 *
	 * @param modelIdentifier the model identifier
	 * @return step filters that have been contributed for the given model
	 *         identifier, possibly an empty collection
	 * @since 3.10
	 * @see org.eclipse.debug.core.model.IStepFilter
	 */
	public static IStepFilter[] getStepFilters(String modelIdentifier) {
		return getStepFilterManager().getStepFilters(modelIdentifier);
	}

	/**
	 * Returns the step filter manager.
	 *
	 * @return step filter manager
	 */
	private static StepFilterManager getStepFilterManager() {
		return ((LaunchManager)getDefault().getLaunchManager()).getStepFilterManager();
	}

	/**
	 * Returns an adapter of the specified type for the given object or <code>null</code>
	 * if none. The object itself is returned if it is an instance of the specified type.
	 * If the object is adaptable and does not subclass <code>PlatformObject</code>, and
	 * does not provide the specified adapter directly, the platform's adapter manager
	 * is consulted for an adapter.
	 *
	 * @param element element to retrieve adapter for
	 * @param type adapter type
	 * @return adapter or <code>null</code>
	 * @since 3.4
	 */
	public static Object getAdapter(Object element, Class<?> type) {
		Object adapter = null;
		if (element != null) {
			if (type.isInstance(element)) {
				return element;
			} else {
				if (element instanceof IAdaptable) {
					adapter = ((IAdaptable)element).getAdapter(type);
				}
				// for objects that don't subclass PlatformObject, check the platform's adapter manager
				if (adapter == null && !(element instanceof PlatformObject)) {
					adapter = Platform.getAdapterManager().getAdapter(element, type);
				}
				// force load the adapter in case it really is available
				if (adapter == null) {
					adapter = Platform.getAdapterManager().loadAdapter(element, type.getName());
				}
			}
		}
		return adapter;
	}

}


