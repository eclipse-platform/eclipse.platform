/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Sebastian Davids - bug 50567 Eclipse native environment support on Win98
 *     Pawel Piech - Bug 82001: When shutting down the IDE, the debugger should first
 *     attempt to disconnect debug targets before terminating them
 *     Alena Laskavaia - Bug 259281
 *     Marc Khouzam - Bug 313143: Preferred Launch Delegate not recovered from preferences
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.internal.core;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.core.ILaunchesListener;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IDisconnect;
import org.eclipse.debug.core.model.IPersistableSourceLocator;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;
import org.eclipse.debug.internal.core.sourcelookup.SourceContainerType;
import org.eclipse.debug.internal.core.sourcelookup.SourcePathComputer;
import org.eclipse.osgi.service.environment.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Manages launch configurations, launch configuration types, and registered launches.
 *
 * @see ILaunchManager
 */
public class LaunchManager extends PlatformObject implements ILaunchManager, IResourceChangeListener {

	/**
	 * Preferred launch delegate preference name.
	 * <p>
	 * Prior to 3.5 this preferred launch delegates for all launch
	 * configuration types were serialized into a single XML string
	 * and stored in this preference.
	 * </p>
	 * <p>
	 * Since 3.5, the preferred launch delegates are stored in a separate
	 * preference for each launch configuration type.  The name of this
	 * preference is composed of the prefix, followed by a slash, followed by
	 * the launch configuration type id.  The values contain a set of launch
	 * delegates, delimited by a semicolon, and each delegate entry contains
	 * the delegate ID, followed by a comma, followed by comma-delimited
	 * launch modes.
	 *
	 * @since 3.3
	 */
	protected static final String PREF_PREFERRED_DELEGATES = DebugPlugin.getUniqueIdentifier() + ".PREFERRED_DELEGATES"; //$NON-NLS-1$

	/**
	 * Constant to define debug.ui for the status codes
	 *
	 * @since 3.2
	 */
	private static final String DEBUG_UI = "org.eclipse.debug.ui"; //$NON-NLS-1$

	/**
	 * Listing of unsupported launch configuration names for the Win 32 platform
	 * @since 3.5
	 */
	static final String[] UNSUPPORTED_WIN32_CONFIG_NAMES = new String[] {"aux", "clock$", "com1", "com2", "com3", "com4", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		"com5", "com6", "com7", "com8", "com9", "con", "lpt1", "lpt2", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "nul", "prn"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$

	/**
	 * Disallowed characters for launch configuration names
	 * '@' and '&' are disallowed because they corrupt menu items.
	 *
	 * @since 3.5
	 */
	static final char[] DISALLOWED_CONFIG_NAME_CHARS = new char[] { '@', '&','\\', '/', ':', '*', '?', '"', '<', '>', '|', '\0' };

	/**
	 * Status code for which a UI prompter is registered.
	 *
	 * @since 3.2
	 */
	protected static final IStatus promptStatus = new Status(IStatus.INFO, DEBUG_UI, 200, IInternalDebugCoreConstants.EMPTY_STRING, null);

	/**
	 * Step filter manager
	 */
	private StepFilterManager fStepFilterManager = null;

	/**
	 * Notifies a launch config listener in a safe runnable to handle
	 * exceptions.
	 */
	class ConfigurationNotifier implements ISafeRunnable {

		private ILaunchConfigurationListener fListener;
		private int fType;
		private ILaunchConfiguration fConfiguration;

		@Override
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, "An exception occurred during launch configuration change notification.", exception);  //$NON-NLS-1$
			DebugPlugin.log(status);
		}

		/**
		 * Notifies the given listener of the add/change/remove
		 *
		 * @param configuration the configuration that has changed
		 * @param update the type of change
		 */
		public void notify(ILaunchConfiguration configuration, int update) {
			fConfiguration = configuration;
			fType = update;
			for (ILaunchConfigurationListener iLaunchConfigurationListener : fLaunchConfigurationListeners) {
				fListener = iLaunchConfigurationListener;
				SafeRunner.run(this);
			}
			fConfiguration = null;
			fListener = null;
		}

		@Override
		public void run() throws Exception {
			switch (fType) {
				case ADDED:
					fListener.launchConfigurationAdded(fConfiguration);
					break;
				case REMOVED:
					fListener.launchConfigurationRemoved(fConfiguration);
					break;
				case CHANGED:
					fListener.launchConfigurationChanged(fConfiguration);
					break;
				default:
					break;
			}
		}
	}

	/**
	 * Notifies a launch listener (multiple launches) in a safe runnable to
	 * handle exceptions.
	 */
	class LaunchesNotifier implements ISafeRunnable {

		private ILaunchesListener fListener;
		private int fType;
		private ILaunch[] fNotifierLaunches;
		private ILaunch[] fRegistered;

		@Override
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, "An exception occurred during launch change notification.", exception);  //$NON-NLS-1$
			DebugPlugin.log(status);
		}

		/**
		 * Notifies the given listener of the adds/changes/removes
		 *
		 * @param launches the launches that changed
		 * @param update the type of change
		 */
		public void notify(ILaunch[] launches, int update) {
			fNotifierLaunches = launches;
			fType = update;
			fRegistered = null;
			for (ILaunchesListener iLaunchesListener : fLaunchesListeners) {
				fListener = iLaunchesListener;
				SafeRunner.run(this);
			}
			fNotifierLaunches = null;
			fRegistered = null;
			fListener = null;
		}

		@Override
		public void run() throws Exception {
			switch (fType) {
				case ADDED:
					fListener.launchesAdded(fNotifierLaunches);
					break;
				case REMOVED:
					fListener.launchesRemoved(fNotifierLaunches);
					break;
				case CHANGED:
				case TERMINATE:
					if (fRegistered == null) {
						List<ILaunch> registered = null;
						for (int j = 0; j < fNotifierLaunches.length; j++) {
							if (isRegistered(fNotifierLaunches[j])) {
								if (registered != null) {
									registered.add(fNotifierLaunches[j]);
								}
							} else if (registered == null) {
								registered = new ArrayList<>(fNotifierLaunches.length);
								for (int k = 0; k < j; k++) {
									registered.add(fNotifierLaunches[k]);
								}
							}
						}
						if (registered == null) {
							fRegistered = fNotifierLaunches;
						} else {
							fRegistered = registered.toArray(new ILaunch[registered.size()]);
						}
					}
					if (fRegistered.length > 0) {
						if (fType == CHANGED) {
							fListener.launchesChanged(fRegistered);
						}
						if (fType == TERMINATE && fListener instanceof ILaunchesListener2) {
							((ILaunchesListener2)fListener).launchesTerminated(fRegistered);
						}
					}
					break;
				default:
					break;
			}
		}
	}

	/**
	 * Visitor for handling a resource begin deleted, and the need to check mapped configurations
	 * for auto-deletion
	 * @since 3.4
	 */
	class MappedResourceVisitor implements IResourceDeltaVisitor {

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			if (0 != (delta.getFlags() & IResourceDelta.OPEN)) {
				return false;
			}
			if(delta.getKind() == IResourceDelta.REMOVED && delta.getFlags() != IResourceDelta.MOVED_TO) {
				ArrayList<ILaunchConfiguration> configs = collectAssociatedLaunches(delta.getResource());
				for (ILaunchConfiguration config : configs) {
					try {
						config.delete();
					} catch (CoreException e) {
						DebugPlugin.log(e.getStatus());
					}
				}
				return false;
			}
			return true;
		}
	}

	/**
	 * Visitor for handling resource deltas.
	 */
	class LaunchManagerVisitor implements IResourceDeltaVisitor {

		@Override
		public boolean visit(IResourceDelta delta) {
			if (delta == null) {
				return false;
			}
			if (0 != (delta.getFlags() & IResourceDelta.OPEN)) {
				if (delta.getResource() instanceof IProject) {
					IProject project = (IProject)delta.getResource();
					if (project.isOpen()) {
						LaunchManager.this.projectOpened(project);
					} else {
						LaunchManager.this.projectClosed(project);
					}
				}
				return false;
			}
			IResource resource = delta.getResource();
			if (resource instanceof IFile) {
				IFile file = (IFile)resource;
				if (ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION.equals(file.getFileExtension()) || ILaunchConfiguration.LAUNCH_CONFIGURATION_PROTOTYPE_FILE_EXTENSION.equals(file.getFileExtension())) {
					ILaunchConfiguration handle = new LaunchConfiguration(file);
					switch (delta.getKind()) {
						case IResourceDelta.ADDED :
							LaunchManager.this.launchConfigurationAdded(handle);
							break;
						case IResourceDelta.REMOVED :
							LaunchManager.this.launchConfigurationDeleted(handle);
							break;
						case IResourceDelta.CHANGED :
							LaunchManager.this.launchConfigurationChanged(handle);
							break;
						default:
							break;
					}
				}
				return false;
			}
			return true;
		}
	}

	/**
	 * Notifies a launch listener (single launch) in a safe runnable to handle
	 * exceptions.
	 */
	class LaunchNotifier implements ISafeRunnable {

		private ILaunchListener fListener;
		private int fType;
		private ILaunch fLaunch;

		@Override
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, "An exception occurred during launch change notification.", exception);  //$NON-NLS-1$
			DebugPlugin.log(status);
		}

		/**
		 * Notifies listeners of the add/change/remove
		 *
		 * @param launch the launch that has changed
		 * @param update the type of change
		 */
		public void notify(ILaunch launch, int update) {
			fLaunch = launch;
			fType = update;
			for (ILaunchListener iLaunchListener : fListeners) {
				fListener = iLaunchListener;
				SafeRunner.run(this);
			}
			fLaunch = null;
			fListener = null;
		}

		@Override
		public void run() throws Exception {
			switch (fType) {
				case ADDED:
					fListener.launchAdded(fLaunch);
					break;
				case REMOVED:
					fListener.launchRemoved(fLaunch);
					break;
				case CHANGED:
					if (isRegistered(fLaunch)) {
						fListener.launchChanged(fLaunch);
					}
					break;
				default:
					break;
			}
		}
	}

	/**
	 * Collects files whose extension matches the launch configuration file
	 * extension.
	 */
	static class ResourceProxyVisitor implements IResourceProxyVisitor {

		private final List<IResource> fList;

		protected ResourceProxyVisitor(List<IResource> list) {
			fList= list;
		}

		@Override
		public boolean visit(IResourceProxy proxy) {
			if (proxy.getType() == IResource.FILE) {
				if (ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION.equalsIgnoreCase(proxy.requestFullPath().getFileExtension()) || ILaunchConfiguration.LAUNCH_CONFIGURATION_PROTOTYPE_FILE_EXTENSION.equalsIgnoreCase(proxy.requestFullPath().getFileExtension())) {
					fList.add(proxy.requestResource());
				}
				return false;
			}
			return true;
		}
	}

	/**
	 * Internal class used to hold information about a preferred delegate
	 *
	 * @since 3.3
	 */
	static class PreferredDelegate {
		private ILaunchDelegate fDelegate = null;
		private String fTypeid = null;
		private Set<String> fModes = null;

		public PreferredDelegate(ILaunchDelegate delegate, String typeid, Set<String> modes) {
			fDelegate = delegate;
			fTypeid = typeid;
			fModes = modes;
		}

		public String getTypeId() {
			return fTypeid;
		}

		public Set<String> getModes() {
			return fModes;
		}

		public ILaunchDelegate getDelegate() {
			return fDelegate;
		}
	}

	/**
	 * Types of notifications
	 */
	public static final int ADDED = 0;
	public static final int REMOVED= 1;
	public static final int CHANGED= 2;
	public static final int TERMINATE= 3;

	/**
	 * The collection of native environment variables on the user's system. Cached
	 * after being computed once as the environment cannot change.
	 */
	private static HashMap<String, String> fgNativeEnv;
	private static Map<String, String> fgNativeEnvCasePreserved;

	/**
	 * Path to the local directory where local launch configurations
	 * are stored with the workspace.
	 */
	public static final IPath LOCAL_LAUNCH_CONFIGURATION_CONTAINER_PATH =
		DebugPlugin.getDefault().getStateLocation().append(".launches"); //$NON-NLS-1$

	/**
	 * Returns a Document that can be used to build a DOM tree
	 * @return the Document
	 * @throws ParserConfigurationException if an exception occurs creating the document builder
	 * @since 3.0
	 */
	public static Document getDocument() throws ParserConfigurationException {
		@SuppressWarnings("restriction")
		DocumentBuilder docBuilder = org.eclipse.core.internal.runtime.XmlProcessorFactory.createDocumentBuilderWithErrorOnDOCTYPE();
		return docBuilder.newDocument();
	}

	/**
	 * Serializes a XML document into a string - encoded in UTF8 format, with
	 * platform line separators.
	 *
	 * @param doc document to serialize
	 * @return the document as a string
	 * @throws TransformerException if an unrecoverable error occurs during the
	 *             serialization
	 * @throws IOException if I/O error occurs
	 */
	public static String serializeDocument(Document doc) throws TransformerException, IOException {
		return serializeDocument(doc, System.lineSeparator());
	}

	/**
	 * Serializes a XML document into a string - encoded in UTF8 format, with
	 * specified line separator.
	 *
	 * @param doc document to serialize
	 * @param lineDelimiter the new line separator to use
	 * @return the document as a string
	 * @throws TransformerException if an unrecoverable error occurs during the
	 *             serialization
	 * @throws IOException if I/O error occurs
	 */
	public static String serializeDocument(Document doc, String lineDelimiter) throws TransformerException, IOException {
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		@SuppressWarnings("restriction")
		TransformerFactory factory = org.eclipse.core.internal.runtime.XmlProcessorFactory.createTransformerFactoryWithErrorOnDOCTYPE();
		Transformer transformer = factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
		DOMSource source = new DOMSource(doc);
		StreamResult outputTarget = new StreamResult(s);
		transformer.transform(source, outputTarget);
		return s.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), lineDelimiter);
	}

	/**
	 * Collection of defined launch configuration type
	 * extensions.
	 */
	private List<ILaunchConfigurationType> fLaunchConfigurationTypes = null;

	/**
	 * Launch configuration cache. Keys are <code>LaunchConfiguration</code>,
	 * values are <code>LaunchConfigurationInfo</code>.
	 */
	private final Map<ILaunchConfiguration, LaunchConfigurationInfo> fLaunchConfigurations = new HashMap<>(10);

	/**
	 * A cache of launch configuration names currently in the workspace.
	 */
	private volatile String[] fSortedConfigNames = null;

	/**
	 * Collection of all launch configurations in the workspace.
	 * <code>List</code> of <code>ILaunchConfiguration</code>.
	 */
	private List<ILaunchConfiguration> fLaunchConfigurationIndex = null;

	/**
	 * Launch configuration comparator extensions,
	 * keyed by attribute name.
	 */
	private Map<String, LaunchConfigurationComparator> fComparators = null;

	/**
	 * Registered launch modes, or <code>null</code> if not initialized.
	 * Keys are mode identifiers, values are <code>ILaunchMode</code>s.
	 */
	private Map<String, ILaunchMode> fLaunchModes = null;

	/**
	 * A map of LaunchDelegate objects stored by id of delegate, or launch config type
	 */
	private HashMap<String, LaunchDelegate> fLaunchDelegates = null;

	/**
	 * Initial startup cache of preferred delegate so that the debug preferences are only parsed once
	 *
	 * @since 3.3
	 */
	private Set<PreferredDelegate> fPreferredDelegates = null;

	/**
	 * Collection of launches
	 */
	private final List<ILaunch> fLaunches = new ArrayList<>(10);
	/**
	 * Set of launches for efficient 'isRegistered()' check TODO remove this -
	 * Launches don't implement hashCode() or equals() - so its no more
	 * efficient than walking the other collection
	 */
	private final Set<ILaunch> fLaunchSet = new HashSet<>(10);

	/**
	 * Collection of listeners
	 */
	private ListenerList<ILaunchListener> fListeners = new ListenerList<>();

	/**
	 * Collection of "plural" listeners.
	 * @since 2.1
	 */
	private ListenerList<ILaunchesListener> fLaunchesListeners = new ListenerList<>();

	/**
	 * Visitor used to process resource deltas,
	 * to update launch configuration index.
	 */
	private LaunchManagerVisitor fgVisitor;

	/**
	 * Visitor used to process a deleted resource,
	 * to remove mapped launch configurations in the event
	 * auto-removal of launch configurations is enabled
	 *
	 * @since 3.4
	 */
	private MappedResourceVisitor fgMRVisitor;

	/**
	 * Whether this manager is listening for resource change events
	 */
	private boolean fListening = false;

	/**
	 * Launch configuration listeners
	 */
	private ListenerList<ILaunchConfigurationListener> fLaunchConfigurationListeners = new ListenerList<>();

	/**
	 * Table of source locator extensions. Keys
	 * are identifiers, and values are associated
	 * configuration elements.
	 */
	private Map<String, IConfigurationElement> fSourceLocators = null;

	/**
	 * The handles of launch configurations being moved, or <code>null</code>
	 */
	private ILaunchConfiguration fFrom;

	private ILaunchConfiguration fTo;

	/**
	 * Map of source container type extensions. Keys are extension ids
	 * and values are associated configuration elements.
	 */
	private Map<String, ISourceContainerType> sourceContainerTypes;

	/**
	 * Map of source path computer extensions. Keys are extension ids
	 * and values are associated configuration elements.
	 */
	private Map<String, ISourcePathComputer> sourcePathComputers;

	/**
	 * TODO, we can probably remove this too
	 */
	private Set<String> fActiveModes;

	@Override
	public void addLaunch(ILaunch launch) {
		if (internalAddLaunch(launch)) {
			fireUpdate(launch, ADDED);
			fireUpdate(new ILaunch[] {launch}, ADDED);
		}
	}

	@Override
	public void addLaunchConfigurationListener(ILaunchConfigurationListener listener) {
		fLaunchConfigurationListeners.add(listener);
	}

	@Override
	public void addLaunches(ILaunch[] launches) {
		List<ILaunch> added = new ArrayList<>(launches.length);
		for (ILaunch launch : launches) {
			if (internalAddLaunch(launch)) {
				added.add(launch);
			}
		}
		if (!added.isEmpty()) {
			ILaunch[] addedLaunches = added.toArray(new ILaunch[added.size()]);
			fireUpdate(addedLaunches, ADDED);
			for (int i = 0; i < addedLaunches.length; i++) {
				fireUpdate(launches[i], ADDED);
			}
		}
	}

	@Override
	public void addLaunchListener(ILaunchesListener listener) {
		fLaunchesListeners.add(listener);
	}

	@Override
	public void addLaunchListener(ILaunchListener listener) {
		fListeners.add(listener);
	}

	/**
	 * Clears all launch configurations (if any have been accessed)
	 */
	private void clearAllLaunchConfigurations() {
		if (fLaunchConfigurationTypes != null) {
			fLaunchConfigurationTypes.clear();
		}
		if (fLaunchConfigurationIndex != null) {
			fLaunchConfigurationIndex.clear();
		}
	}

	@Override
	public String getEncoding(ILaunchConfiguration configuration) throws CoreException {
		boolean forceSystemEncoding = configuration.getAttribute(DebugPlugin.ATTR_FORCE_SYSTEM_CONSOLE_ENCODING, false);
		if (forceSystemEncoding) {
			return Platform.getSystemCharset().name();
		}
		String encoding = configuration.getAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING, (String)null);
		if(encoding == null) {
			IResource[] resources = configuration.getMappedResources();
			if(resources != null && resources.length > 0) {
				IResource res = resources[0];
				if(res instanceof IFile) {
					return ((IFile)res).getCharset();
				}
				else if(res instanceof IContainer) {
					return ((IContainer)res).getDefaultCharset();
				}
			}
			else {
				return ResourcesPlugin.getEncoding();
			}
		}
		return encoding;
	}

	/**
	 * The launch config name cache is cleared when a config is added, deleted or changed.
	 */
	protected synchronized void clearConfigNameCache() {
		fSortedConfigNames = null;
	}

	/**
	 * Return an instance of DebugException containing the specified message and Throwable.
	 * @param message the message for the new {@link DebugException}
	 * @param throwable the underlying {@link Exception}
	 * @return the new {@link DebugException}
	 */
	protected DebugException createDebugException(String message, Throwable throwable) {
		return new DebugException(
					new Status(
					 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
					 DebugException.REQUEST_FAILED, message, throwable
					)
				);
	}

	/**
	 * Return a LaunchConfigurationInfo object initialized from XML contained in
	 * the specified stream.  Simply pass out any exceptions encountered so that
	 * caller can deal with them.  This is important since caller may need access to the
	 * actual exception.
	 *
	 * @param stream the {@link InputStream} to read from
	 * @return the new {@link LaunchConfigurationInfo}
	 * @throws CoreException if a problem is encountered
	 * @throws ParserConfigurationException if the stream fails to parse
	 * @throws IOException if there is a problem handling the given stream or writing the new info file
	 * @throws SAXException if there is a SAX parse exception
	 */
	protected LaunchConfigurationInfo createInfoFromXML(InputStream stream) throws CoreException,
																			 ParserConfigurationException,
																			 IOException,
																			 SAXException {
		return createInfoFromXML(stream, false);
	}

	/**
	 * Return a LaunchConfigurationInfo object initialized from XML contained in
	 * the specified stream. Simply pass out any exceptions encountered so that
	 * caller can deal with them. This is important since caller may need access
	 * to the actual exception.
	 *
	 * @param stream the {@link InputStream} to read from
	 * @param isPrototype if the XML corresponds to a prototype
	 * @return the new {@link LaunchConfigurationInfo}
	 * @throws CoreException if a problem is encountered
	 * @throws ParserConfigurationException if the stream fails to parse
	 * @throws IOException if there is a problem handling the given stream or
	 *             writing the new info file
	 * @throws SAXException if there is a SAX parse exception
	 *
	 * @since 3.12
	 */
	protected LaunchConfigurationInfo createInfoFromXML(InputStream stream, boolean isPrototype) throws CoreException, ParserConfigurationException, IOException, SAXException {
		Element root = null;
		@SuppressWarnings("restriction")
		DocumentBuilder parser = org.eclipse.core.internal.runtime.XmlProcessorFactory.createDocumentBuilderWithErrorOnDOCTYPE();
		parser.setErrorHandler(new DefaultHandler());
		root = parser.parse(new InputSource(stream)).getDocumentElement();
		LaunchConfigurationInfo info = new LaunchConfigurationInfo();
		info.initializeFromXML(root, isPrototype);
		return info;
	}

	/**
	 * Finds and returns all launch configurations in the given
	 * container (and sub-containers)
	 *
	 * @param container the container to search
	 * @return all launch configurations in the given container
	 */
	protected List<ILaunchConfiguration> findLaunchConfigurations(IContainer container) {
		if (container instanceof IProject && !((IProject)container).isOpen()) {
			return Collections.emptyList();
		}
		List<IResource> list = new ArrayList<>(10);
		ResourceProxyVisitor visitor= new ResourceProxyVisitor(list);
		try {
			container.accept(visitor, IResource.NONE);
		} catch (CoreException ce) {
			//Closed project...should not be possible with previous check
		}
		List<ILaunchConfiguration> configs = new ArrayList<>(list.size());
		for (IResource resource : list) {
			ILaunchConfiguration config = getLaunchConfiguration((IFile) resource);
			if(config != null && config.exists()) {
				configs.add(config);
			}
		}
		return configs;
	}

	/**
	 * Searches for the {@link ILaunchConfiguration} with the given name
	 * @param name the name to search for
	 * @return the {@link ILaunchConfiguration} with the given name or <code>null</code>
	 * @since 3.8
	 */
	public ILaunchConfiguration findLaunchConfiguration(String name) {
		if(name != null) {
			for (ILaunchConfiguration config : getLaunchConfigurations()) {
				if(name.equals(config.getName())) {
					return config;
				}
			}
		}
		return null;
	}

	/**
	 * Finds and returns all local launch configurations.
	 *
	 * @return all local launch configurations
	 */
	protected List<ILaunchConfiguration> findLocalLaunchConfigurations() {
		IPath containerPath = LOCAL_LAUNCH_CONFIGURATION_CONTAINER_PATH;
		final File directory = containerPath.toFile();
		if (directory.isDirectory()) {
			List<ILaunchConfiguration> configs = new ArrayList<>();
			FilenameFilter configFilter = (dir, name) -> dir.equals(directory) &&
					name.endsWith(ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION);
			File[] configFiles = directory.listFiles(configFilter);
			if (configFiles != null && configFiles.length > 0) {
				LaunchConfiguration config = null;
				for (File configFile : configFiles) {
					config = new LaunchConfiguration(LaunchConfiguration.getSimpleName(configFile.getName()), null, false);
					configs.add(config);
				}
			}
			FilenameFilter prototypeFilter = (dir, name) -> dir.equals(directory) && name.endsWith(ILaunchConfiguration.LAUNCH_CONFIGURATION_PROTOTYPE_FILE_EXTENSION);
			File[] prototypeFiles = directory.listFiles(prototypeFilter);
			if (prototypeFiles != null && prototypeFiles.length > 0) {
				LaunchConfiguration config = null;
				for (File prototypeFile : prototypeFiles) {
					config = new LaunchConfiguration(LaunchConfiguration.getSimpleName(prototypeFile.getName()), null, true);
					configs.add(config);
				}
			}
			return configs;
		}
		return Collections.emptyList();
	}

	/**
	 * Fires notification to (single) listeners that a launch has been
	 * added/changed/removed.
	 *
	 * @param launch launch that has changed
	 * @param update type of change
	 */
	public void fireUpdate(ILaunch launch, int update) {
		new LaunchNotifier().notify(launch, update);
	}

	/**
	 * Fires notification to (plural) listeners that a launch has been
	 * added/changed/removed.
	 *
	 * @param launches launches that have changed
	 * @param update type of change
	 */
	public void fireUpdate(ILaunch[] launches, int update) {
		new LaunchesNotifier().notify(launches, update);
	}

	@Override
	public String generateUniqueLaunchConfigurationNameFrom(String baseName) {
		int index = 1;
		int length = baseName.length();
		int copyIndex = baseName.lastIndexOf(" ("); //$NON-NLS-1$
		String base = baseName;
		if (copyIndex > -1 && length > copyIndex + 2 && baseName.charAt(length - 1) == ')') {
			String trailer = baseName.substring(copyIndex + 2, length - 1);
			if (isNumber(trailer)) {
				try {
					index = Integer.parseInt(trailer);
					base = baseName.substring(0, copyIndex);
				}
				catch (NumberFormatException nfe) {}
			}
		}
		String newName = base;
		while (isExistingLaunchConfigurationName(newName)) {
			newName = MessageFormat.format(DebugCoreMessages.LaunchManager_31, new Object[] {
					base, Integer.toString(index) });
			index++;
		}
		return newName;
	}

	/**
	 * Return a String that can be used as the name of a launch configuration.  The name
	 * is guaranteed to be unique (no existing or temporary launch configurations will have this name).
	 * The name that is returned uses the <code>basename</code> as a starting point.  If
	 * there is no existing launch configuration with this name, then <code>basename</code>
	 * is returned.  Otherwise, the value returned consists of the specified base plus
	 * some suffix that guarantees uniqueness. Passing <code>null</code> as the set of reserved names will cause this
	 * method to return <code>generateUniqueLaunchConfigurationNameFrom(String baseName)</code>.
	 *
	 * By specifying a set of reserved names, you can further constrain the name that will be generated
	 * by this method. For example you can give a base name of 'test' and a reserved set of [test(1), test(2)],
	 * which will result in a name of 'test(3)' being returned iff a configuration with the name 'test' already exists.
	 *
	 * @return launch configuration name
	 * @param basename the String that the returned name must begin with
	 * @param reservednames a set of strings that is further used to constrain what names can be generated
	 * @since 3.3
	 */
	public String generateUniqueLaunchConfigurationNameFrom(String basename, Set<String> reservednames) {
		if(reservednames == null) {
			return generateUniqueLaunchConfigurationNameFrom(basename);
		}
		int index = 1;
		int length= basename.length();
		String base = basename;
		int copyIndex = base.lastIndexOf(" ("); //$NON-NLS-1$
		if (copyIndex > -1 && length > copyIndex + 2 && base.charAt(length - 1) == ')') {
			String trailer = base.substring(copyIndex + 2, length -1);
			if (isNumber(trailer)) {
				try {
					index = Integer.parseInt(trailer);
					base = base.substring(0, copyIndex);
				}
				catch (NumberFormatException nfe) {}
			}
		}
		String newname = base;
		StringBuilder buffer = null;
		while (isExistingLaunchConfigurationName(newname) || reservednames.contains(newname)) {
			buffer = new StringBuilder(base);
			buffer.append(" ("); //$NON-NLS-1$
			buffer.append(String.valueOf(index));
			index++;
			buffer.append(')');
			newname = buffer.toString();
		}
		return newname;
	}

	/**
	 * Returns a collection of all launch configuration handles in
	 * the workspace. This collection is initialized lazily.
	 *
	 * @return all launch configuration handles
	 */
	public synchronized List<ILaunchConfiguration> getAllLaunchConfigurations() {
		if (fLaunchConfigurationIndex == null) {
			try {
				fLaunchConfigurationIndex = new ArrayList<>(20);
				List<ILaunchConfiguration> configs = findLocalLaunchConfigurations();
				verifyConfigurations(configs, fLaunchConfigurationIndex);
				configs = findLaunchConfigurations(ResourcesPlugin.getWorkspace().getRoot());
				verifyConfigurations(configs, fLaunchConfigurationIndex);
			} finally {
				hookResourceChangeListener();
			}
		}
		return fLaunchConfigurationIndex;
	}

	/**
	 * Return a sorted array of the names of all <code>ILaunchConfiguration</code>s in
	 * the workspace.  These are cached, and cache is cleared when a new config is added,
	 * deleted or changed.
	 * @return the sorted array of {@link ILaunchConfiguration} names
	 */
	protected synchronized String[] getAllSortedConfigNames() {
		if (fSortedConfigNames == null) {
			List<ILaunchConfiguration> collection = getAllLaunchConfigurations();
			ILaunchConfiguration[] configs = collection.toArray(new ILaunchConfiguration[collection.size()]);
			fSortedConfigNames = new String[configs.length];
			for (int i = 0; i < configs.length; i++) {
				fSortedConfigNames[i] = configs[i].getName();
			}
			Arrays.sort(fSortedConfigNames);
		}
		return fSortedConfigNames;
	}

	/**
	 * Returns the comparator registered for the given attribute, or
	 * <code>null</code> if none.
	 *
	 * @param attributeName attribute for which a comparator is required
	 * @return comparator, or <code>null</code> if none
	 */
	protected Comparator<Object> getComparator(String attributeName) {
		Map<String, LaunchConfigurationComparator> map = getComparators();
		 return map.get(attributeName);
	}

	/**
	 * Returns comparators, loading if required
	 * @return the complete map of {@link ILaunchConfiguration} {@link Comparator}s
	 */
	protected Map<String, LaunchConfigurationComparator> getComparators() {
		initializeComparators();
		return fComparators;
	}

	/**
	 * Returns the launch configurations specified by the given
	 * XML document.
	 *
	 * @param root XML document
	 * @return list of launch configurations
	 * @throws CoreException if a problem is encountered
	 */
	protected List<ILaunchConfiguration> getConfigsFromXML(Element root) throws CoreException {
		DebugException invalidFormat =
			new DebugException(
				new Status(
				 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
				 DebugException.REQUEST_FAILED, DebugCoreMessages.LaunchManager_Invalid_launch_configuration_index__18, null
				)
			);

		if (!root.getNodeName().equalsIgnoreCase("launchConfigurations")) { //$NON-NLS-1$
			throw invalidFormat;
		}

		// read each launch configuration
		List<ILaunchConfiguration> configs = new ArrayList<>(4);
		NodeList list = root.getChildNodes();
		int length = list.getLength();
		Node node = null;
		Element entry = null;
		String memento = null;
		for (int i = 0; i < length; ++i) {
			node = list.item(i);
			short type = node.getNodeType();
			if (type == Node.ELEMENT_NODE) {
				entry = (Element) node;
				if (!entry.getNodeName().equals("launchConfiguration")) { //$NON-NLS-1$
					throw invalidFormat;
				}
				memento = entry.getAttribute("memento"); //$NON-NLS-1$
				if (memento == null) {
					throw invalidFormat;
				}
				configs.add(getLaunchConfiguration(memento));
			}
		}
		return configs;
	}

	protected ConfigurationNotifier getConfigurationNotifier() {
		return new ConfigurationNotifier();
	}

	@Override
	public IDebugTarget[] getDebugTargets() {
		synchronized (fLaunches) {
			List<IDebugTarget> allTargets = new ArrayList<>(fLaunches.size());
			IDebugTarget[] targets = null;
			for (ILaunch launch : fLaunches) {
				targets = launch.getDebugTargets();
				Collections.addAll(allTargets, targets);
			}
			return allTargets.toArray(new IDebugTarget[allTargets.size()]);
		}
	}

	/**
	 * Returns the resource delta visitor for the launch manager.
	 *
	 * @return the resource delta visitor for the launch manager
	 */
	private LaunchManagerVisitor getDeltaVisitor() {
		if (fgVisitor == null) {
			fgVisitor= new LaunchManagerVisitor();
		}
		return fgVisitor;
	}

	/**
	 * Returns the resource delta visitor for auto-removal of mapped launch configurations
	 * @return the resource delta visitor for auto-removal of mapped launch configurations
	 *
	 * @since 3.4
	 */
	private MappedResourceVisitor getMappedResourceVisitor() {
		if(fgMRVisitor == null) {
			fgMRVisitor = new MappedResourceVisitor();
		}
		return fgMRVisitor;
	}

	@Override
	public String[] getEnvironment(ILaunchConfiguration configuration) throws CoreException {
		Map<String, String> configEnv = configuration.getAttribute(ATTR_ENVIRONMENT_VARIABLES, (Map<String, String>) null);
		if (configEnv == null) {
			return null;
		}
		Map<String, String> env = new HashMap<>();
		// build base environment
		boolean append = configuration.getAttribute(ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
		if (append) {
			env.putAll(getNativeEnvironmentCasePreserved());
		}

		// Add variables from config
		boolean win32= Platform.getOS().equals(Constants.OS_WIN32);
		String key = null;
		String value = null;
		Object nativeValue = null;
		String nativeKey = null;
		for (Entry<String, String> entry : configEnv.entrySet()) {
			key = entry.getKey();
			value = entry.getValue();
			// translate any string substitution variables
			if (value != null) {
				value = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(value);
			}
			boolean added= false;
			if (win32) {
				// First, check if the key is an exact match for an existing key.
				nativeValue = env.get(key);
				if (nativeValue != null) {
					// If an exact match is found, just replace the value
					env.put(key, value);
				} else {
					// Win32 variables are case-insensitive. If an exact match isn't found, iterate to
					// check for a case-insensitive match. We maintain the key's case (see bug 86725),
					// but do a case-insensitive comparison (for example, "pAtH" will still override "PATH").
					for (Entry<String, String> nativeEntry : env.entrySet()) {
						nativeKey = (nativeEntry).getKey();
						if (nativeKey.equalsIgnoreCase(key)) {
							nativeEntry.setValue(value);
							added = true;
							break;
						}
					}
				}
			}
			if (!added) {
				env.put(key, value);
			}
		}
		List<String> strings = new ArrayList<>(env.size());
		StringBuilder buffer = null;
		for (Entry<String, String> entry : env.entrySet()) {
			buffer = new StringBuilder(entry.getKey());
			buffer.append('=').append(entry.getValue());
			strings.add(buffer.toString());
		}
		return strings.toArray(new String[strings.size()]);
	}

	/**
	 * Returns the info object for the specified launch configuration.
	 * If the configuration exists, but is not yet in the cache,
	 * an info object is built and added to the cache.
	 * @param config the {@link ILaunchConfiguration} to get the info object from
	 * @return the {@link LaunchConfigurationInfo} object from the given {@link ILaunchConfiguration}
	 *
	 * @exception CoreException if an exception occurs building
	 *  the info object
	 * @exception DebugException if the config does not exist
	 * @since 3.5
	 */
	protected LaunchConfigurationInfo getInfo(LaunchConfiguration config) throws CoreException {
		LaunchConfigurationInfo info = fLaunchConfigurations.get(config);
		if (info == null) {
			IFileStore store = config.getFileStore();
			if (config.exists()) {
				try (BufferedInputStream stream = new BufferedInputStream(store.openInputStream(EFS.NONE, null))) {
					info = createInfoFromXML(stream, isPrototype(store));
					synchronized (this) {
						fLaunchConfigurations.put(config, info);
					}
				} catch (FileNotFoundException e) {
					throwException(config, e);
				} catch (SAXException e) {
					throwException(config, e);
				} catch (ParserConfigurationException e) {
					throwException(config, e);
				} catch (IOException e) {
					throwException(config, e);
				}

			} else if (store != null){
				throw createDebugException(MessageFormat.format(DebugCoreMessages.LaunchManager_does_not_exist, new Object[] {
						config.getName(), store.toURI().toString() }), null);
			} else {
				throw createDebugException(MessageFormat.format(DebugCoreMessages.LaunchManager_does_not_exist_no_store_found, new Object[] { config.getName() }), null);
			}
		}
		return info;
	}

	/**
	 * Check if the given {@link IFileStore} is a prototype.
	 *
	 * @param store the given {@link IFileStore}
	 * @return <code>true</code> if the given {@link IFileStore} is a prototype,
	 *         <code>false</code> otherwise.
	 *
	 * @since 3.12
	 */
	private boolean isPrototype(IFileStore store) {
		if (store.getName().endsWith("." + ILaunchConfiguration.LAUNCH_CONFIGURATION_PROTOTYPE_FILE_EXTENSION)) { //$NON-NLS-1$
			return true;
		}
		return false;
	}

	/**
	 * Check if the given {@link File} is a prototype.
	 *
	 * @param file the given {@link File}
	 * @return <code>true</code> if the given {@link File} is a prototype,
	 *         <code>false</code> otherwise.
	 *
	 * @since 3.12
	 */
	private boolean isPrototype(File file) {
		if (file.getName().endsWith("." + ILaunchConfiguration.LAUNCH_CONFIGURATION_PROTOTYPE_FILE_EXTENSION)) { //$NON-NLS-1$
			return true;
		}
		return false;
	}

	@Override
	public ILaunchConfiguration getLaunchConfiguration(IFile file) {
		hookResourceChangeListener();
		return new LaunchConfiguration(file);
	}

	@Override
	public ILaunchConfiguration getLaunchConfiguration(String memento) throws CoreException {
		hookResourceChangeListener();
		return new LaunchConfiguration(memento);
	}

	@Override
	public synchronized ILaunchConfiguration[] getLaunchConfigurations() {
		return getLaunchConfigurations(ILaunchConfiguration.CONFIGURATION);
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(int kinds) {
		List<ILaunchConfiguration> allConfigs = getAllLaunchConfigurations();
		if (((kinds & ILaunchConfiguration.CONFIGURATION) > 0) && ((kinds & ILaunchConfiguration.PROTOTYPE) > 0)) {
			// all kinds
			return allConfigs.toArray(new ILaunchConfiguration[allConfigs.size()]);
		} else {
			List<ILaunchConfiguration> select = new ArrayList<>(allConfigs.size());
			Iterator<ILaunchConfiguration> iterator = allConfigs.iterator();
			while (iterator.hasNext()) {
				ILaunchConfiguration config = iterator.next();
				try {
					if ((config.getKind() & kinds) > 0) {
						select.add(config);
					}
				} catch (CoreException e) {
					DebugPlugin.log(e);
				}
			}
			return select.toArray(new ILaunchConfiguration[select.size()]);
		}
	}

	@Override
	public synchronized ILaunchConfiguration[] getLaunchConfigurations(ILaunchConfigurationType type) throws CoreException {
		return getLaunchConfigurations(type, ILaunchConfiguration.CONFIGURATION);
	}

	@Override
	public synchronized ILaunchConfiguration[] getLaunchConfigurations(ILaunchConfigurationType type, int kinds) throws CoreException {
		List<ILaunchConfiguration> configs = new ArrayList<>();
		for (ILaunchConfiguration config : getAllLaunchConfigurations()) {
			if (config.getType().equals(type) && ((config.getKind() & kinds) > 0)) {
				configs.add(config);
			}
		}
		return configs.toArray(new ILaunchConfiguration[configs.size()]);
	}

	/**
	 * Returns all launch configurations that are stored as resources
	 * in the given project.
	 *
	 * @param project a project
	 * @return collection of launch configurations that are stored as resources
	 *  in the given project
	 */
	protected synchronized List<ILaunchConfiguration> getLaunchConfigurations(IProject project) {
		List<ILaunchConfiguration> configs = new ArrayList<>();
		for (ILaunchConfiguration config : getAllLaunchConfigurations()) {
			IFile file = config.getFile();
			if (file != null && file.getProject().equals(project)) {
				configs.add(config);
			}
		}
		return configs;
	}

	@Override
	public ILaunchConfigurationType getLaunchConfigurationType(String id) {
		for (ILaunchConfigurationType type : getLaunchConfigurationTypes()) {
			if (type.getIdentifier().equals(id)) {
				return type;
			}
		}
		return null;
	}

	@Override
	public ILaunchConfigurationType[] getLaunchConfigurationTypes() {
		initializeLaunchConfigurationTypes();
		return fLaunchConfigurationTypes.toArray(new ILaunchConfigurationType[fLaunchConfigurationTypes.size()]);
	}

	@Override
	public ILaunch[] getLaunches() {
		synchronized (fLaunches) {
			return fLaunches.toArray(new ILaunch[fLaunches.size()]);
		}
	}

	@Override
	public ILaunchMode getLaunchMode(String mode) {
		initializeLaunchModes();
		return fLaunchModes.get(mode);
	}

	@Override
	public ILaunchMode[] getLaunchModes() {
		initializeLaunchModes();
		Collection<ILaunchMode> collection = fLaunchModes.values();
		return collection.toArray(new ILaunchMode[collection.size()]);
	}

	/**
	 * Returns all of the launch delegates. The returned listing of delegates cannot be directly used to launch,
	 * instead the method <code>IlaunchDelegate.getDelegate</code> must be used to acquire an executable form of
	 * the delegate, allowing us to maintain lazy loading of the delegates themselves.
	 * @return all of the launch delegates
	 *
	 * @since 3.3
	 */
	public ILaunchDelegate[] getLaunchDelegates() {
		initializeLaunchDelegates();
		Collection<LaunchDelegate> col = fLaunchDelegates.values();
		return col.toArray(new ILaunchDelegate[col.size()]);
	}

	/**
	 * Returns the listing of launch delegates that apply to the specified
	 * <code>ILaunchConfigurationType</code> id
	 * @param typeid the id of the launch configuration type to get delegates for
	 * @return An array of <code>LaunchDelegate</code>s that apply to the specified launch configuration
	 * type, or an empty array, never <code>null</code>
	 *
	 * @since 3.3
	 */
	public LaunchDelegate[] getLaunchDelegates(String typeid) {
		initializeLaunchDelegates();
		ArrayList<LaunchDelegate> list = new ArrayList<>();
		for (Entry<String, LaunchDelegate> entry : fLaunchDelegates.entrySet()) {
			LaunchDelegate ld = entry.getValue();
			if (ld.getLaunchConfigurationTypeId().equals(typeid)) {
				list.add(ld);
			}
		}
		return list.toArray(new LaunchDelegate[list.size()]);
	}

	/**
	 * This method returns the <code>ILaunchDelegate</code> instance corresponding to the id
	 * of the launch delegate specified
	 * @param id the id of the <code>ILaunchDelegate</code> to find
	 * @return the <code>ILaunchDelegate</code> or <code>null</code> if not found
	 *
	 * @since 3.3
	 */
	public ILaunchDelegate getLaunchDelegate(String id) {
		if(id != null) {
			for (ILaunchDelegate delegate : getLaunchDelegates()) {
				if(id.equals(delegate.getId())) {
					return delegate;
				}
			}
		}
		return null;
	}

	/**
	 * Initializes the listing of delegates available to the launching framework
	 *
	 * @since 3.3
	 */
	private synchronized void initializeLaunchDelegates() {
		if(fLaunchDelegates == null) {
			fLaunchDelegates = new HashMap<>();
			//get all launch delegate contributions
			IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_LAUNCH_DELEGATES);
			LaunchDelegate delegate = null;
			for (IConfigurationElement info : extensionPoint.getConfigurationElements()) {
				delegate = new LaunchDelegate(info);
				fLaunchDelegates.put(delegate.getId(), delegate);
			}
			//get all delegates from launch configuration type contributions
			extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_LAUNCH_CONFIGURATION_TYPES);
			for (IConfigurationElement info : extensionPoint.getConfigurationElements()) {
				//must check to see if delegate is provided in contribution
				if(info.getAttribute(IConfigurationElementConstants.DELEGATE) != null) {
					delegate = new LaunchDelegate(info);
					fLaunchDelegates.put(delegate.getId(), delegate);
				}
			}
		}
	}

	/**
	 * This method is used to initialize a simple listing of all preferred delegates, which is then used by each
	 * <code>ILaunchConfigurationType</code> to find if they have preferred delegates. Once an <code>ILaunchConfigurationType</code>
	 * has used this listing to initialize its preferred delegates it will maintain changes to its preferred delegate, which are
	 * then written back to the preference store only when the launch manager shuts down.
	 * <p>
	 * This cache is not synchronized with the runtime preferred delegates stored in launch configuration types.
	 * </p>
	 * @since 3.3
	 */
	private synchronized void initializePreferredDelegates() {
		if(fPreferredDelegates == null) {
			fPreferredDelegates = new HashSet<>();
			String preferred = Platform.getPreferencesService().getString(DebugPlugin.getUniqueIdentifier(), LaunchManager.PREF_PREFERRED_DELEGATES, IInternalDebugCoreConstants.EMPTY_STRING, null);
			if(!IInternalDebugCoreConstants.EMPTY_STRING.equals(preferred)) {
				try {
					Element root = DebugPlugin.parseDocument(preferred);
					NodeList nodes = root.getElementsByTagName(IConfigurationElementConstants.DELEGATE);
					Element element = null;
					String typeid = null;
					Set<String> modeset = null;
					for(int i = 0; i < nodes.getLength(); i++) {
						element = (Element) nodes.item(i);
						String delegateid = element.getAttribute(IConfigurationElementConstants.ID);
						typeid = element.getAttribute(IConfigurationElementConstants.TYPE_ID);
						String[] modes = element.getAttribute(IConfigurationElementConstants.MODES).split(","); //$NON-NLS-1$
						modeset = new HashSet<>(Arrays.asList(modes));
						LaunchDelegate delegate = getLaunchDelegateExtension(typeid, delegateid, modeset);
						if (delegate != null) {
							//take type id, modeset, delegate and create entry
							if(!IInternalDebugCoreConstants.EMPTY_STRING.equals(typeid) && modeset != null) {
								fPreferredDelegates.add(new PreferredDelegate(delegate, typeid, modeset));
							}
						}
					}
				}
				catch (CoreException e) {DebugPlugin.log(e);}
			}
		}
	}

	/**
	 * Allows internal access to reset preferred delegates when re-importing
	 * preferences.
	 *
	 * @since 3.6
	 */
	protected void resetPreferredDelegates() {
		fPreferredDelegates = null;
	}

	/**
	 * Allows internal access to a preferred delegate for a given type and mode set
	 * @param typeid the id of the <code>ILaunchConfigurationType</code> to find a delegate for
	 * @param modes the set of modes for the delegate
	 * @return the preferred delegate for the specified type id and mode set, or <code>null</code> if none
	 *
	 * @since 3.3
	 */
	protected ILaunchDelegate getPreferredDelegate(String typeid, Set<String> modes) {
		// Retrieve preferred delegates using legacy mechanism for backward
		// compatibility.
		initializePreferredDelegates();
		for (PreferredDelegate pd : fPreferredDelegates) {
			if (pd.getModes().equals(modes) && pd.getTypeId().equals(typeid)) {
				return pd.getDelegate();
			}
		}

		// @since 3.5
		// If the legacy mechanism didn't work, try the new preference name for
		// the given launch type.
		String preferred = Platform.getPreferencesService().getString(DebugPlugin.getUniqueIdentifier(), "//" + LaunchManager.PREF_PREFERRED_DELEGATES + '/' + typeid, IInternalDebugCoreConstants.EMPTY_STRING, null); //$NON-NLS-1$
		if (preferred != null && preferred.length() != 0) {
			StringTokenizer tokenizer = new StringTokenizer(preferred, ";"); //$NON-NLS-1$
			while(tokenizer.hasMoreTokens()) {
				StringTokenizer tokenizer2 = new StringTokenizer(tokenizer.nextToken(), ","); //$NON-NLS-1$
				String delegateId = tokenizer2.nextToken();
				HashSet<String> modeset = new HashSet<>();
				while(tokenizer2.hasMoreTokens()) {
					modeset.add(tokenizer2.nextToken());
				}
				LaunchDelegate delegate = getLaunchDelegateExtension(typeid, delegateId, modeset);
				if (delegate != null && modeset.equals(modes)) {
					return delegate;
				}
			}

		}
		return null;
	}

	/**
	 * Returns the launch delegate extension that matches the given type, delegate ID, and
	 * set of modes.
	 *
	 * @param typeId Launch configuration type.
	 * @param id Launch delegate ID.
	 * @param modeset Set of modes that the launch delegate applies to.
	 * @return The launch delegate matching the specified parameters, or
	 * <code>null</code> if not found.
	 *
	 * @since 3.5
	 */
	private LaunchDelegate getLaunchDelegateExtension(String typeId, String id, Set<String> modeset) {
		for (LaunchDelegate extension : getLaunchDelegates(typeId)) {
			if(id.equals(extension.getId())) {
				List<Set<String>> modesets = extension.getModes();
				if(modesets.contains(modeset)) {
					return extension;
				}
			}
		}
		return null;
	}

	/**
	 * Returns all launch configurations that are stored locally.
	 *
	 * @return collection of launch configurations stored locally
	 */
	protected synchronized List<ILaunchConfiguration> getLocalLaunchConfigurations() {
		List<ILaunchConfiguration> configs = new ArrayList<>();
		for (ILaunchConfiguration config : getAllLaunchConfigurations()) {
			if (config.isLocal()) {
				configs.add(config);
			}
		}
		return configs;
	}

	/**
	 * Returns the launch configurations mapping to the specified resource
	 * @param resource the resource to collect mapped launch configurations for
	 * @return a list of launch configurations if found or an empty list, never null
	 * @since 3.2
	 */
	public ILaunchConfiguration[] getMappedConfigurations(IResource resource) {
		List<ILaunchConfiguration> configurations = new ArrayList<>();
		for (ILaunchConfiguration config : getAllLaunchConfigurations()) {
			try {
				IResource[] resources = config.getMappedResources();
				if(resources != null) {
					for (IResource res : resources) {
						if(res.equals(resource)) {
							configurations.add(config);
							break;
						} else if (resource.getType() == IResource.PROJECT && res.getType() == IResource.FILE){
							if (res.getProject().equals(resource)) {
								configurations.add(config);
								break;
							}
						}
					}
				}
			} catch (CoreException ce) {
				DebugPlugin.log(ce);
			}
		}
		return configurations.toArray(new ILaunchConfiguration[configurations.size()]);
	}

	@Override
	public ILaunchConfiguration[] getMigrationCandidates() throws CoreException {
		List<ILaunchConfiguration> configs = new ArrayList<>();
		for (ILaunchConfiguration config : getAllLaunchConfigurations()) {
			if (!config.isReadOnly() && config.isMigrationCandidate()) {
				configs.add(config);
			}
		}
		return configs.toArray(new ILaunchConfiguration[configs.size()]);
	}

	@Override
	public ILaunchConfiguration getMovedFrom(ILaunchConfiguration addedConfiguration) {
		if (addedConfiguration.equals(fTo)) {
			return fFrom;
		}
		return null;
	}

	@Override
	public ILaunchConfiguration getMovedTo(ILaunchConfiguration removedConfiguration) {
		if (removedConfiguration.equals(fFrom)) {
			return fTo;
		}
		return null;
	}

	@Override
	public synchronized Map<String, String> getNativeEnvironment() {
		if (fgNativeEnv == null) {
			Map<String, String> casePreserved = getNativeEnvironmentCasePreserved();
			if (Platform.getOS().equals(Constants.OS_WIN32)) {
				fgNativeEnv = new HashMap<>();
				for (Entry<String, String> entry : casePreserved.entrySet()) {
					fgNativeEnv.put(entry.getKey().toUpperCase(), entry.getValue());
				}
			} else {
				fgNativeEnv = new HashMap<>(casePreserved);
			}
		}
		return new HashMap<>(fgNativeEnv);
	}

	@Override
	public synchronized Map<String, String> getNativeEnvironmentCasePreserved() {
		if (fgNativeEnvCasePreserved == null) {
			fgNativeEnvCasePreserved = System.getenv();
		}
		return new HashMap<>(fgNativeEnvCasePreserved);
	}

	@Override
	public IProcess[] getProcesses() {
		synchronized (fLaunches) {
			List<IProcess> allProcesses = new ArrayList<>(fLaunches.size());
			IProcess[] processes = null;
			for (ILaunch launch : fLaunches) {
				processes = launch.getProcesses();
				Collections.addAll(allProcesses, processes);
			}
			return allProcesses.toArray(new IProcess[allProcesses.size()]);
		}
	}

	@Override
	public ISourceContainerType getSourceContainerType(String id) {
		initializeSourceContainerTypes();
		return sourceContainerTypes.get(id);
	}

	@Override
	public ISourceContainerType[] getSourceContainerTypes() {
		initializeSourceContainerTypes();
		Collection<ISourceContainerType> containers = sourceContainerTypes.values();
		return containers.toArray(new ISourceContainerType[containers.size()]);
	}

	@Override
	public ISourcePathComputer getSourcePathComputer(ILaunchConfiguration configuration) throws CoreException {
		String id = null;
		id = configuration.getAttribute(ISourcePathComputer.ATTR_SOURCE_PATH_COMPUTER_ID, (String)null);

		if (id == null) {
			//use default computer for configuration type, if any
			return configuration.getType().getSourcePathComputer();
		}
		return getSourcePathComputer(id);
	}

	@Override
	public ISourcePathComputer getSourcePathComputer(String id) {
		initializeSourceContainerTypes();
		return sourcePathComputers.get(id);
	}

	/**
	 * Starts listening for resource change events
	 */
	private synchronized void hookResourceChangeListener() {
		if (!fListening) {
			ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_DELETE);
			fListening = true;
		}
	}

	/**
	 * Load comparator extensions.
	 */
	private synchronized void initializeComparators() {
		if (fComparators == null) {
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_LAUNCH_CONFIGURATION_COMPARATORS);
			IConfigurationElement[] infos= extensionPoint.getConfigurationElements();
			fComparators = new HashMap<>(infos.length);
			IConfigurationElement configurationElement = null;
			String attr = null;
			for (IConfigurationElement info : infos) {
				configurationElement = info;
				attr = configurationElement.getAttribute("attribute"); //$NON-NLS-1$
				if (attr != null) {
					fComparators.put(attr, new LaunchConfigurationComparator(configurationElement));
				} else {
					// invalid status handler
					IStatus s = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugException.INTERNAL_ERROR,
							MessageFormat.format("Invalid launch configuration comparator extension defined by plug-in {0} - attribute not specified.", configurationElement.getContributor().getName()), null); //$NON-NLS-1$
					DebugPlugin.log(s);
				}
			}
		}
	}

	/**
	 * Initializes the listing of <code>LaunchConfigurationType</code>s.
	 */
	private synchronized void initializeLaunchConfigurationTypes() {
		if (fLaunchConfigurationTypes == null) {
			hookResourceChangeListener();
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_LAUNCH_CONFIGURATION_TYPES);
			IConfigurationElement[] infos = extensionPoint.getConfigurationElements();
			fLaunchConfigurationTypes = new ArrayList<>(infos.length);
			for (IConfigurationElement info : infos) {
				fLaunchConfigurationTypes.add(new LaunchConfigurationType(info));
			}
		}
	}

	/**
	 * Load comparator extensions.
	 */
	private synchronized void initializeLaunchModes() {
		if (fLaunchModes == null) {
			try {
				IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_LAUNCH_MODES);
				IConfigurationElement[] infos= extensionPoint.getConfigurationElements();
				fLaunchModes = new HashMap<>();
				ILaunchMode mode = null;
				for (IConfigurationElement info : infos) {
					mode = new LaunchMode(info);
					fLaunchModes.put(mode.getIdentifier(), mode);
				}
			}
			catch (CoreException e) {DebugPlugin.log(e);}
		}
	}

	/**
	 * Initializes source container type and source path computer extensions.
	 */
	private synchronized void initializeSourceContainerTypes() {
		if (sourceContainerTypes == null) {
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_SOURCE_CONTAINER_TYPES);
			IConfigurationElement[] extensions = extensionPoint.getConfigurationElements();
			sourceContainerTypes = new HashMap<>();
			for (IConfigurationElement extension : extensions) {
				sourceContainerTypes.put(
						extension.getAttribute(IConfigurationElementConstants.ID),
						new SourceContainerType(extension));
			}
			extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_SOURCE_PATH_COMPUTERS);
			extensions = extensionPoint.getConfigurationElements();
			sourcePathComputers = new HashMap<>();
			for (IConfigurationElement extension : extensions) {
				sourcePathComputers.put(
						extension.getAttribute(IConfigurationElementConstants.ID),
						new SourcePathComputer(extension));
			}
		}
	}

	/**
	 * Register source locators.
	 */
	private synchronized void initializeSourceLocators() {
		if (fSourceLocators == null) {
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_SOURCE_LOCATORS);
			IConfigurationElement[] infos= extensionPoint.getConfigurationElements();
			fSourceLocators = new HashMap<>(infos.length);
			IConfigurationElement configurationElement = null;
			String id = null;
			for (IConfigurationElement info : infos) {
				configurationElement = info;
				id = configurationElement.getAttribute(IConfigurationElementConstants.ID);
				if (id != null) {
					fSourceLocators.put(id,configurationElement);
				} else {
					// invalid status handler
					IStatus s = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugException.INTERNAL_ERROR,
							MessageFormat.format("Invalid source locator extension defined by plug-in \"{0}\": \"id\" not specified.", configurationElement.getContributor().getName()), null); //$NON-NLS-1$
					DebugPlugin.log(s);
				}
			}
		}
	}

	/**
	 * Adds the given launch object to the list of registered launches,
	 * and returns whether the launch was added.
	 *
	 * @param launch launch to register
	 * @return whether the launch was added
	 */
	protected boolean internalAddLaunch(ILaunch launch) {
		// ensure the step filter manager is created on the first launch
		getStepFilterManager();
		synchronized (fLaunches) {
			if (fLaunches.contains(launch)) {
				return false;
			}
			fLaunches.add(launch);
			fLaunchSet.add(launch);
			return true;
		}
	}

	/**
	 * Removes the given launch object from the collection of registered
	 * launches. Returns whether the launch was removed.
	 *
	 * @param launch the launch to remove
	 * @return whether the launch was removed
	 */
	protected boolean internalRemoveLaunch(ILaunch launch) {
		if (launch == null) {
			return false;
		}
		synchronized (fLaunches) {
			fLaunchSet.remove(launch);
			return fLaunches.remove(launch);
		}
	}

	@Override
	public boolean isExistingLaunchConfigurationName(String name) {
		String[] sortedConfigNames = getAllSortedConfigNames();
		int index = Arrays.binarySearch(sortedConfigNames, name);
		if (index < 0) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether the given String is composed solely of digits
	 * @param string the {@link String} to check
	 * @return <code>true</code> if the given {@link String} is a number <code>false</code> otherwise
	 */
	private boolean isNumber(String string) {
		int numChars= string.length();
		if (numChars == 0) {
			return false;
		}
		for (int i= 0; i < numChars; i++) {
			if (!Character.isDigit(string.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns whether the user has selected to delete associated configurations when a
	 * project is deleted.
	 *
	 * @return whether to auto-delete configurations
	 */
	private boolean isDeleteConfigurations() {
		return Platform.getPreferencesService().getBoolean(DebugPlugin.getUniqueIdentifier(), DebugPlugin.PREF_DELETE_CONFIGS_ON_PROJECT_DELETE, true, null);
	}

	@Override
	public boolean isRegistered(ILaunch launch) {
		synchronized (fLaunches) {
			return fLaunchSet.contains(launch);
		}
	}

	/**
	 * Returns whether the given launch configuration passes a basic
	 * integrity test by retrieving its type.
	 *
	 * @param config the configuration to verify
	 * @return whether the config meets basic integrity constraints
	 */
	protected boolean isValid(ILaunchConfiguration config) {
		try {
			config.getType();
		} catch (CoreException e) {
			if (e.getStatus().getCode() != DebugException.MISSING_LAUNCH_CONFIGURATION_TYPE) {
				// only log warnings due to something other than a missing
				// launch config type
				DebugPlugin.log(e);
			}
			return false;
		}
		return true;
	}

	/**
	 * Notifies the launch manager that a launch configuration
	 * has been added. The configuration is added to the index of
	 * configurations by project, and listeners are notified.
	 *
	 * @param config the launch configuration that was added
	 */
	protected void launchConfigurationAdded(ILaunchConfiguration config) {
		if (config.isWorkingCopy()) {
			return;
		}
		if (isValid(config)) {
			boolean added = false;
			synchronized (this) {
				List<ILaunchConfiguration> allConfigs = getAllLaunchConfigurations();
				if (!allConfigs.contains(config)) {
					allConfigs.add(config);
					added = true;
				}
			}
			if (added) {
				getConfigurationNotifier().notify(config, ADDED);
				clearConfigNameCache();
			}
		} else {
			launchConfigurationDeleted(config);
		}
	}

	/**
	 * Notifies the launch manager that a launch configuration
	 * has been changed. The configuration is removed from the
	 * cache of info objects such that the new attributes will
	 * be updated on the next access. Listeners are notified of
	 * the change.
	 *
	 * @param config the launch configuration that was changed
	 */
	protected void launchConfigurationChanged(ILaunchConfiguration config) {
		synchronized(this) {
			fLaunchConfigurations.remove(config);
		}
		clearConfigNameCache();
		if (isValid(config)) {
			// in case the config has been refreshed and it was removed from the
			// index due to 'out of synch with local file system' (see bug 36147),
			// add it back (will only add if required)
			launchConfigurationAdded(config);
			getConfigurationNotifier().notify(config, CHANGED);
		} else {
			launchConfigurationDeleted(config);
		}
	}

	/**
	 * Notifies the launch manager that a launch configuration
	 * has been deleted. The configuration is removed from the
	 * cache of info and from the index of configurations by
	 * project, and listeners are notified.
	 *
	 * @param config the launch configuration that was deleted
	 */
	protected void launchConfigurationDeleted(ILaunchConfiguration config) {
		boolean removed = false;
		synchronized (this) {
			Object key = fLaunchConfigurations.remove(config);
			removed = key != null;
			getAllLaunchConfigurations().remove(config);
		}
		if (removed) {
			getConfigurationNotifier().notify(config, REMOVED);
			clearConfigNameCache();
		}
	}

	@Override
	public IPersistableSourceLocator newSourceLocator(String identifier) throws CoreException {
		initializeSourceLocators();
		IConfigurationElement config = fSourceLocators.get(identifier);
		if (config == null) {
			throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugException.INTERNAL_ERROR,
 MessageFormat.format(DebugCoreMessages.LaunchManager_Source_locator_does_not_exist___0__13, new Object[] { identifier }), null));
		}
		IPersistableSourceLocator sourceLocator = (IPersistableSourceLocator)config.createExecutableExtension("class"); //$NON-NLS-1$
		if (sourceLocator instanceof AbstractSourceLookupDirector) {
			((AbstractSourceLookupDirector)sourceLocator).setId(identifier);
		}
		return sourceLocator;
	}

	/**
	 * The specified project has just closed - remove its
	 * launch configurations from the cached index.
	 *
	 * @param project the project that has been closed
	 */
	protected void projectClosed(IProject project) {
		// bug 12134
		terminateMappedConfigurations(project);
		for (ILaunchConfiguration config : getLaunchConfigurations(project)) {
			launchConfigurationDeleted(config);
		}

	}

	/**
	 * The specified project has just opened - add all launch
	 * configs in the project to the index of all configs.
	 *
	 * @param project the project that has been opened
	 */
	protected void projectOpened(IProject project) {
		for (ILaunchConfiguration config : findLaunchConfigurations(project)) {
			launchConfigurationAdded(config);
		}
	}

	@Override
	public void removeLaunch(final ILaunch launch) {
		if (internalRemoveLaunch(launch)) {
			fireUpdate(launch, REMOVED);
			fireUpdate(new ILaunch[] {launch}, REMOVED);
		}
	}

	@Override
	public void removeLaunchConfigurationListener(ILaunchConfigurationListener listener) {
		fLaunchConfigurationListeners.remove(listener);
	}

	@Override
	public void removeLaunches(ILaunch[] launches) {
		List<ILaunch> removed = new ArrayList<>(launches.length);
		for (ILaunch launch : launches) {
			if (internalRemoveLaunch(launch)) {
				removed.add(launch);
			}
		}
		if (!removed.isEmpty()) {
			ILaunch[] removedLaunches = removed.toArray(new ILaunch[removed.size()]);
			fireUpdate(removedLaunches, REMOVED);
			for (ILaunch launch : removedLaunches) {
				fireUpdate(launch, REMOVED);
			}
		}
	}

	@Override
	public void removeLaunchListener(ILaunchesListener listener) {
		fLaunchesListeners.remove(listener);
	}

	@Override
	public void removeLaunchListener(ILaunchListener listener) {
		fListeners.remove(listener);
	}

	/**
	 * Traverses the delta looking for added/removed/changed launch
	 * configuration files.
	 *
	 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta != null) {
			LaunchManagerVisitor visitor = getDeltaVisitor();
			MappedResourceVisitor v = null;
			if (isDeleteConfigurations()) {
				v = getMappedResourceVisitor();
			}
			try {
				delta.accept(visitor);
				if (v != null) {
					delta.accept(v);
				}
			} catch (CoreException e) {
				DebugPlugin.log(e.getStatus());
			}
		}
	}

	/**
	 * Gets the launch configuration associated with the specified <code>IResource</code>.
	 * This method relies on the resource mapping existing, if no such mapping
	 * exists the launch configuration is ignored.
	 *
	 * @param resource the resource to collect launch configurations for
	 * @return the list of associated launch configurations
	 */
	private ArrayList<ILaunchConfiguration> collectAssociatedLaunches(IResource resource) {
		ArrayList<ILaunchConfiguration> list = new ArrayList<>();
		try {
			ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations();
			IResource[] resources = null;
			for (ILaunchConfiguration config : configs) {
				if(config.isLocal()) {
					resources = config.getMappedResources();
					if(resources != null) {
						for (IResource res : resources) {
							if(resource.equals(res) ||
									resource.getFullPath().isPrefixOf(res.getFullPath())) {
								list.add(config);
								break;
							}
						}
					}
				}
			}
		} catch (CoreException e) {
			DebugPlugin.log(e);
		}
		return list;
	}

	/**
	 * Indicates the given launch configuration is being moved from the given
	 * location to the new location.
	 *
	 * @param from the location a launch configuration is being moved from, or
	 * <code>null</code>
	 * @param to the location a launch configuration is being moved to,
	 * or <code>null</code>
	 */
	protected void setMovedFromTo(ILaunchConfiguration from, ILaunchConfiguration to) {
		fFrom = from;
		fTo = to;
	}
	/**
	 * Terminates/Disconnects any active debug targets/processes.
	 * Clears launch configuration types.
	 */
	public void shutdown() {
		fListeners = new ListenerList<>();
		fLaunchesListeners = new ListenerList<>();
		fLaunchConfigurationListeners = new ListenerList<>();
		for (ILaunch launch : getLaunches()) {
			if(launch != null) {
				try {
					if (launch instanceof IDisconnect) {
						IDisconnect disconnect = (IDisconnect)launch;
						if (disconnect.canDisconnect()) {
							disconnect.disconnect();
						}
					}
					if (launch.canTerminate()) {
						launch.terminate();
					}
				} catch (DebugException e) {
					DebugPlugin.log(e);
				}
			}
		}

		persistPreferredLaunchDelegates();
		clearAllLaunchConfigurations();
		fStepFilterManager = null;
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	/**
	 * Saves the listings of preferred launch delegates from all of the launch configuration types
	 *
	 * @since 3.3
	 */
	public void persistPreferredLaunchDelegates() {
		ILaunchConfigurationType[] types = getLaunchConfigurationTypes();
		for (ILaunchConfigurationType type : types) {
			persistPreferredLaunchDelegate((LaunchConfigurationType)type);
		}
	}

	/**
	 * Persists the given launch configuration delegate.
	 * @param type Launch configuration type to persist
	 *
	 * @since 3.6
	 */
	public void persistPreferredLaunchDelegate(LaunchConfigurationType type) {
		String preferenceName = PREF_PREFERRED_DELEGATES + '/' + type.getIdentifier();
		Map<Set<String>, ILaunchDelegate> preferred = type.getPreferredDelegates();
		if(preferred != null && preferred.size() > 0) {
			StringBuilder str = new StringBuilder();
			for (Entry<Set<String>, ILaunchDelegate> entry : preferred.entrySet()) {
				Set<String> modes = entry.getKey();
				ILaunchDelegate delegate = entry.getValue();
				if (delegate != null) {
					str.append(delegate.getId());
					str.append(',');
					for (String mode : modes) {
						str.append(mode).append(',');
					}
					str.append(';');
				}
			}
			Preferences.setString(DebugPlugin.getUniqueIdentifier(), preferenceName, str.toString(), null);
		} else {
			Preferences.setToDefault(DebugPlugin.getUniqueIdentifier(), preferenceName);
		}

		// Reset the legacy preference string.
		Preferences.setToDefault(DebugPlugin.getUniqueIdentifier(), PREF_PREFERRED_DELEGATES);
	}

	/**
	 * finds and terminates any running launch configurations associated with the given resource
	 * @param resource the resource to search for launch configurations and hence launches for
	 * @since 3.2
	 */
	protected void terminateMappedConfigurations(IResource resource) {
		ILaunch[] launches = getLaunches();
		ILaunchConfiguration[] configs = getMappedConfigurations(resource);
		try {
			for (ILaunch launch : launches) {
				for (ILaunchConfiguration config : configs) {
					if (config.equals(launch.getLaunchConfiguration()) && launch.canTerminate()) {
						launch.terminate();
					}
				}
			}
		}
		catch(CoreException e) {DebugPlugin.log(e);}
	}

	/**
	 * Throws a debug exception with the given throwable that occurred
	 * while processing the given configuration.
	 * @param config the {@link ILaunchConfiguration} causing the exception
	 * @param e the {@link Exception} to throw
	 * @throws DebugException the new {@link DebugException} wrapping the given {@link Exception} and {@link ILaunchConfiguration}
	 * @since 3.5
	 */
	private void throwException(LaunchConfiguration config, Throwable e) throws DebugException {
		String uri = config.getName();
		try {
			IFileStore store = config.getFileStore();
			if (store != null) {
				uri = store.toString();
			}
		} catch (CoreException ce) {
		}
		throw createDebugException(MessageFormat.format(DebugCoreMessages.LaunchManager__0__occurred_while_reading_launch_configuration_file__1___1, new Object[] {
				e.toString(), uri }), e);
	}

	/**
	 * Verify basic integrity of launch configurations in the given list,
	 * adding valid configurations to the collection of all launch configurations.
	 * Exceptions are logged for invalid configurations.
	 *
	 * @param verify the list of configurations to verify
	 * @param valid the list to place valid configurations in
	 */
	protected void verifyConfigurations(List<ILaunchConfiguration> verify, List<ILaunchConfiguration> valid) {
		for (ILaunchConfiguration config : verify) {
			if (!valid.contains(config) && isValid(config)) {
				valid.add(config);
			}
		}
	}

	/**
	 * Returns the name of the given launch mode with accelerators removed,
	 * or <code>null</code> if none.
	 *
	 * @param id launch mode identifier
	 * @return launch mode name with accelerators removed or <code>null</code>
	 */
	public String getLaunchModeName(String id) {
		ILaunchMode launchMode = getLaunchMode(id);
		if (launchMode != null) {
			return removeAccelerators(launchMode.getLabel());
		}
		return null;
	}
	/**
	 * Returns the label with any accelerators removed.
	 *
	 * @param label label to process
	 * @return label without accelerators
	 */
	public static String removeAccelerators(String label) {
		String title = label;
		if (title != null) {
			// strip out any '&' (accelerators)
			int index = title.indexOf('&');
			if (index == 0) {
				title = title.substring(1);
			} else if (index > 0) {
				//DBCS languages use "(&X)" format
				if (title.charAt(index - 1) == '(' && title.length() >= index + 3 && title.charAt(index + 2) == ')') {
					String first = title.substring(0, index - 1);
					String last = title.substring(index + 3);
					title = first + last;
				} else if (index < (title.length() - 1)) {
					String first = title.substring(0, index);
					String last = title.substring(index + 1);
					title = first + last;
				}
			}
		}
		return title;
	}

	/**
	 * Returns the singleton step filter manager.
	 *
	 * @return the step filter manager
	 */
	public synchronized StepFilterManager getStepFilterManager() {
		if (fStepFilterManager == null) {
			fStepFilterManager = new StepFilterManager();
		}
		return fStepFilterManager;
	}

	/**
	 * Imports launch configurations represented by the given local files, overwriting
	 * any existing configurations. Sends launch configuration change notification
	 * as required (i.e. added or changed).
	 * <p>
	 * If a file is imported that has the same name as a configuration in the workspace
	 * (i.e. a shared configuration), the shared configuration is deleted (becomes local).
	 * </p>
	 * @param files files to import
	 * @param monitor progress monitor or <code>null</code>
	 * @throws CoreException if an exception occurs while importing configurations
	 * @since 3.4.0
	 */
	public void importConfigurations(File[] files, IProgressMonitor monitor) throws CoreException {
		Map<String, ILaunchConfiguration> sharedConfigs = new HashMap<>();
		for (ILaunchConfiguration config : getAllLaunchConfigurations()) {
			if (!config.isLocal()) {
				StringBuilder buf = new StringBuilder(config.getName());
				buf.append('.');
				if (config.isPrototype()) {
					buf.append(ILaunchConfiguration.LAUNCH_CONFIGURATION_PROTOTYPE_FILE_EXTENSION);
				} else {
					buf.append(ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION);
				}
				sharedConfigs.put(buf.toString(), config);
			}
		}
		List<Status> stati = null;
		SubMonitor lmonitor = SubMonitor.convert(monitor, DebugCoreMessages.LaunchManager_29, files.length);
		for (File source : files) {
			if (lmonitor.isCanceled()) {
				break;
			}
			lmonitor.subTask(MessageFormat.format(DebugCoreMessages.LaunchManager_28, new Object[] { source.getName() }));
			IPath location = IPath.fromOSString(LOCAL_LAUNCH_CONFIGURATION_CONTAINER_PATH.toOSString()).append(source.getName());
			File target = location.toFile();
			IPath locationdir = location.removeLastSegments(1);
			if(!locationdir.toFile().exists()) {
				locationdir.toFile().mkdirs();
			}
			boolean added = !target.exists();
			try {
				copyFile(source, target);
				ILaunchConfiguration configuration = new LaunchConfiguration(LaunchConfiguration.getSimpleName(source.getName()), null, isPrototype(source));
				ILaunchConfiguration shared = sharedConfigs.get(target.getName());
				if (shared != null) {
					setMovedFromTo(shared, configuration);
					shared.delete();
					launchConfigurationChanged(configuration);
				} else if (added) {
					launchConfigurationAdded(configuration);
				} else {
					launchConfigurationChanged(configuration);
				}
			} catch (IOException e) {
				if (stati == null) {
					stati = new ArrayList<>();
				}
				stati.add(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR,
						MessageFormat.format(DebugCoreMessages.LaunchManager_27, source.getPath()), e));
			}
			lmonitor.worked(1);
		}
		if (!lmonitor.isCanceled()) {
			lmonitor.done();
		}
		if (stati != null) {
			if (stati.size() > 1) {
				MultiStatus multi = new MultiStatus(DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, DebugCoreMessages.LaunchManager_26, null);
				for (Status status : stati) {
					multi.add(status);
				}
				throw new CoreException(multi);
			} else {
				throw new CoreException(stati.get(0));
			}
		}
	}

	/**
	 * Copies a file from one location to another, replacing any existing file.
	 *
	 * @param in the file to copy
	 * @param out the file to be copied out to
	 * @throws IOException if the file read fails
	 * @since 3.4.0
	 */
	private void copyFile(File in, File out) throws IOException {
		try (FileInputStream fis = new FileInputStream(in); FileOutputStream fos = new FileOutputStream(out)) {
			byte[] buf = new byte[1024];
			int i = 0;
			while ((i = fis.read(buf)) != -1) {
				fos.write(buf, 0, i);
			}
		}
	}

	/**
	 * Returns whether any launch config supports the given mode.
	 *
	 * @param mode launch mode
	 * @return whether any launch config supports the given mode
	 */
	public synchronized boolean launchModeAvailable(String mode) {
		if (fActiveModes == null) {
			fActiveModes = new HashSet<>(3);
			for (ILaunchConfigurationType type : getLaunchConfigurationTypes()) {
				for (ILaunchMode launchMode : getLaunchModes()) {
					if (type.supportsMode(launchMode.getIdentifier())) {
						fActiveModes.add(launchMode.getIdentifier());
					}
				}
			}
		}
		return fActiveModes.contains(mode);
	}

	@Override
	public String generateLaunchConfigurationName(String namePrefix) {
		String name = generateUniqueLaunchConfigurationNameFrom(namePrefix);
		try {
			isValidLaunchConfigurationName(name);
			return name;
		}
		catch(IllegalArgumentException iae) {
			//blanket change all reserved names
			if(Platform.OS_WIN32.equals(Platform.getOS())) {
				for (String element : UNSUPPORTED_WIN32_CONFIG_NAMES) {
					if(element.equals(name)) {
						name = "launch_configuration"; //$NON-NLS-1$
					}
				}
			}
			//blanket replace all invalid chars
			for (char element : DISALLOWED_CONFIG_NAME_CHARS) {
				name = name.replace(element, '_');
			}
		}
		//run it through the generator once more in case a replaced name has already been done
		return generateUniqueLaunchConfigurationNameFrom(name);
	}

	@Override
	public boolean isValidLaunchConfigurationName(String configname) throws IllegalArgumentException {
		if(Platform.OS_WIN32.equals(Platform.getOS())) {
			for (String element : UNSUPPORTED_WIN32_CONFIG_NAMES) {
				if(configname.equals(element)) {
					throw new IllegalArgumentException(MessageFormat.format(DebugCoreMessages.LaunchManager_invalid_config_name, new Object[] { configname }));
				}
			}
		}
		for (char element : DISALLOWED_CONFIG_NAME_CHARS) {
			if (configname.indexOf(element) > -1) {
				throw new IllegalArgumentException(MessageFormat.format(DebugCoreMessages.LaunchManager_invalid_config_name_char, new Object[] { String.valueOf(element) }));
			}
		}
		return true;
	}

}
