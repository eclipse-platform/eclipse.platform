/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids - bug 50567 Eclipse native environment support on Win98
 *******************************************************************************/
package org.eclipse.debug.internal.core;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.core.ILaunchesListener;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IDebugTarget;
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

/**
 * Manages launch configurations, launch configuration types, and registered launches.
 *
 * @see ILaunchManager
 */
public class LaunchManager extends PlatformObject implements ILaunchManager, IResourceChangeListener {
	
	/**
	 * Collection of defined launch configuration type
	 * extensions.
	 */
	private List fLaunchConfigurationTypes = null;
	
	/**
	 * Launch configuration cache. Keys are <code>LaunchConfiguration</code>,
	 * values are <code>LaunchConfigurationInfo</code>.
	 */
	private Map fLaunchConfigurations = new HashMap(10);
	
	/**
	 * A cache of launch configuration names currently in the workspace.
	 */
	private String[] fSortedConfigNames = null;
	
	/**
	 * Collection of all launch configurations in the workspace.
	 * <code>List</code> of <code>ILaunchConfiguration</code>.
	 */
	private List fLaunchConfigurationIndex = null;
	
	/**
	 * Launch configuration comparator extensions,
	 * keyed by attribute name.
	 */
	private Map fComparators = null;
	
	/**
	 * Registered launch modes, or <code>null</code> if not initialized.
	 * Keys are mode identifiers, values are <code>ILaunchMode</code>s.
	 */
	private Map fLaunchModes = null;
	
	/**
	 * List of contributed launch delegates (delegates contributed for existing
	 * launch configuration types).
	 */
	private List fContributedDelegates = null;
	
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
	private static HashMap fgNativeEnv= null;	

	/**
	 * Collection of launches
	 */
	private Vector fLaunches= new Vector(10);

	/**
	 * Collection of listeners
	 */
	private ListenerList fListeners= new ListenerList(5);
	
	/**
	 * Collection of "plural" listeners.
	 * @since 2.1
	 */
	private ListenerList fLaunchesListeners = new ListenerList(5);
	
	/**
	 * Visitor used to process resource deltas,
	 * to update launch configuration index.
	 */
	private LaunchManagerVisitor fgVisitor;
	
	/**
	 * Whether this manager is listening for resouce change events
	 */
	private boolean fListening = false;
	
	/**
	 * Launch configuration listeners
	 */
	private ListenerList fLaunchConfigurationListeners = new ListenerList(5);
	
	/**
	 * Table of source locator extensions. Keys
	 * are identifiers, and values are associated
	 * configuration elements.
	 */
	private Map fSourceLocators = null;

	/**
	 * The handles of launch configurations being moved, or <code>null</code>
	 */
	private ILaunchConfiguration fFrom;
	private ILaunchConfiguration fTo;
	
	/**
	 * Path to the local directory where local launch configurations
	 * are stored with the workspace.
	 */
	protected static final IPath LOCAL_LAUNCH_CONFIGURATION_CONTAINER_PATH =
		DebugPlugin.getDefault().getStateLocation().append(".launches"); //$NON-NLS-1$
		
	
	/**
	 * Map of source container type extensions. Keys are extension ids
	 * and values are associated configuration elements.
	 */
	private Map sourceContainerTypes;	
	
	/**
	 * Map of source path computer extensions. Keys are extension ids
	 * and values are associated configuration elements.
	 */
	private Map sourcePathComputers;
	
	/**
	 * Serializes a XML document into a string - encoded in UTF8 format,
	 * with platform line separators.
	 * 
	 * @param doc document to serialize
	 * @return the document as a string
	 * @throws TransformerException if an unrecoverable error occurs during the serialization
	 * @throws IOException if the encoding attempted to be used is not supported
	 */
	public static String serializeDocument(Document doc) throws TransformerException, IOException {
		ByteArrayOutputStream s= new ByteArrayOutputStream();
		
		TransformerFactory factory= TransformerFactory.newInstance();
		Transformer transformer= factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
		
		DOMSource source= new DOMSource(doc);
		StreamResult outputTarget= new StreamResult(s);
		transformer.transform(source, outputTarget);
		
		return s.toString("UTF8"); //$NON-NLS-1$			
	}
	
	/**
	 * Returns a Document that can be used to build a DOM tree
	 * @return the Document
	 * @throws ParserConfigurationException if an exception occurs creating the document builder
	 * @since 3.0
	 */
	public static Document getDocument() throws ParserConfigurationException {
		DocumentBuilderFactory dfactory= DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder= dfactory.newDocumentBuilder();
		Document doc= docBuilder.newDocument();
		return doc;
	}
			
	/**
	 * @see ILaunchManager#addLaunchListener(ILaunchListener)
	 */
	public void addLaunchListener(ILaunchListener listener) {
		fListeners.add(listener);
	}

	/**
	 * Returns a collection of all launch configuration handles in 
	 * the workspace. This collection is initialized lazily.
	 * 
	 * @return all launch configuration handles
	 */
	private List getAllLaunchConfigurations() {
		if (fLaunchConfigurationIndex == null) {
			try {			
				fLaunchConfigurationIndex = new ArrayList(20);
				List configs = findLocalLaunchConfigurations();
				verifyConfigurations(configs, fLaunchConfigurationIndex);
				configs = findLaunchConfigurations(getWorkspaceRoot());
				verifyConfigurations(configs, fLaunchConfigurationIndex);
			} finally {
				hookResourceChangeListener();				
			}
		}
		return fLaunchConfigurationIndex;
	}
	
	/**
     * Starts listening for resource change events
     */
    private synchronized void hookResourceChangeListener() {
        if (!fListening) {
            getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_DELETE);
            fListening = true;
        }
    }

    /**
	 * Verify basic integrity of launch configurations in the given list,
	 * adding valid configs to the collection of all launch configurations.
	 * Exceptions are logged for invalid configs.
	 * 
	 * @param verify the list of configs to verify
	 * @param valid the list to place valid configrations in
	 */
	protected void verifyConfigurations(List verify, List valid) {
		Iterator configs = verify.iterator();
		while (configs.hasNext()) {
			ILaunchConfiguration config = (ILaunchConfiguration)configs.next();
			if (isValid(config)) {
				valid.add(config);
			}
		}		
	}
	
	/**
	 * Returns whether the given launch configuration passes a basic
	 * integritiy test by retrieving its type.
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
		
	/**
	 * @see ILaunchManager#removeLaunch(ILaunch)
	 */
	public void removeLaunch(ILaunch launch) {
		if (internalRemoveLaunch(launch)) {
			fireUpdate(launch, REMOVED);
			fireUpdate(new ILaunch[] {launch}, REMOVED);
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
		return fLaunches.remove(launch);
	}
	
	/**
	 * Fires notification to (single) listeners that a launch has been
	 * added/changed/removed..
	 */
	public void fireUpdate(ILaunch launch, int update) {
		getLaunchNotifier().notify(launch, update);
	}	
	
	/**
	 * Fires notification to (multi) listeners that a launch has been
	 * added/changed/removed.
	 */
	public void fireUpdate(ILaunch[] launches, int update) {
		getLaunchesNotifier().notify(launches, update);
	}	
	
	/**
	 * Returns whether the given launch is currently registered.
	 * 
	 * @param launch a launch
	 * @return whether the given launch is currently registered
	 */
	protected boolean isRegistered(ILaunch launch) {
		return fLaunches.contains(launch);
	}

	/**
	 * @see ILaunchManager#getDebugTargets()
	 */
	public IDebugTarget[] getDebugTargets() {
		List allTargets= new ArrayList(fLaunches.size());
		if (fLaunches.size() > 0) {
			Iterator e= fLaunches.iterator();
			while (e.hasNext()) {
				IDebugTarget[] targets= ((ILaunch) e.next()).getDebugTargets();
				for (int i = 0; i < targets.length; i++) {
					allTargets.add(targets[i]);
				}
			}
		}
		return (IDebugTarget[])allTargets.toArray(new IDebugTarget[allTargets.size()]);
	}
			
	/**
	 * @see ILaunchManager#getLaunches()
	 */
	public ILaunch[] getLaunches() {
		return (ILaunch[])fLaunches.toArray(new ILaunch[fLaunches.size()]);
	}

	/**
	 * @see ILaunchManager#getProcesses()
	 */
	public IProcess[] getProcesses() {
		List allProcesses= new ArrayList(fLaunches.size());
		Iterator e= fLaunches.iterator();
		while (e.hasNext()) {
			IProcess[] processes= ((ILaunch) e.next()).getProcesses();
			for (int i= 0; i < processes.length; i++) {
				allProcesses.add(processes[i]);
			}
		}
		return (IProcess[])allProcesses.toArray(new IProcess[allProcesses.size()]);
	}
	
	/**
	 * @see ILaunchManager#addLaunch(ILaunch)
	 */
	public void addLaunch(ILaunch launch) {
		if (internalAddLaunch(launch)) {
			fireUpdate(launch, ADDED);
			fireUpdate(new ILaunch[] {launch}, ADDED);
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
		if (fLaunches.contains(launch)) {
			return false;
		}
		fLaunches.add(launch);
		return true;
	}
	
	/**
	 * @see ILaunchManager#removeLaunchListener(ILaunchListener)
	 */
	public void removeLaunchListener(ILaunchListener listener) {
		fListeners.remove(listener);
	}
		
	/**
	 * Return a LaunchConfigurationInfo object initialized from XML contained in
	 * the specified stream.  Simply pass out any exceptions encountered so that
	 * caller can deal with them.  This is important since caller may need access to the
	 * actual exception.
	 */
	protected LaunchConfigurationInfo createInfoFromXML(InputStream stream) throws CoreException,
																			 ParserConfigurationException,
																			 IOException,
																			 SAXException {
		Element root = null;
		DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		root = parser.parse(new InputSource(stream)).getDocumentElement();
		LaunchConfigurationInfo info = new LaunchConfigurationInfo();
		info.initializeFromXML(root);
		return info;
	}

	/**
	 * Terminates/Disconnects any active debug targets/processes.
	 * Clears launch configuration types.
	 */
	public void shutdown() {
		fListeners.removeAll();
		ILaunch[] launches = getLaunches();
		for (int i= 0; i < launches.length; i++) {
			ILaunch launch= launches[i];
			try {
				launch.terminate();
			} catch (DebugException e) {
				DebugPlugin.log(e);
			}
		}
		
		clearAllLaunchConfigurations();

		getWorkspace().removeResourceChangeListener(this);
	}
							
	/**
	 * Returns the info object for the specified launch configuration.
	 * If the configuration exists, but is not yet in the cache,
	 * an info object is built and added to the cache.
	 * 
	 * @exception CoreException if an exception occurs building
	 *  the info object
	 * @exception DebugException if the config does not exist
	 */
	protected LaunchConfigurationInfo getInfo(ILaunchConfiguration config) throws CoreException {
		LaunchConfigurationInfo info = (LaunchConfigurationInfo)fLaunchConfigurations.get(config);
		if (info == null) {
			if (config.exists()) {
				InputStream stream = null;
				try {
					if (config.isLocal()) {
						IPath path = config.getLocation();
						File file = path.toFile();				
						stream = new FileInputStream(file);
					} else {
						IFile file = ((LaunchConfiguration) config).getFile();
						if (file == null) {
							throw createDebugException(MessageFormat.format(DebugCoreMessages.getString("LaunchManager.30"), new String[] {config.getName()}), null); //$NON-NLS-1$
						}
						stream = file.getContents();
					}
					info = createInfoFromXML(stream);
					fLaunchConfigurations.put(config, info);
				} catch (FileNotFoundException e) {
					throwException(config, e);					
				} catch (SAXException e) {
					throwException(config, e);					
				} catch (ParserConfigurationException e) {
					throwException(config, e);					
				} catch (IOException e) {
					throwException(config, e);					
				} finally {
					if (stream != null) {
						try {
							stream.close();
						} catch (IOException e) {
							throwException(config, e);					
						}
					}
				}
		
			} else {
				throw createDebugException(
					MessageFormat.format(DebugCoreMessages.getString("LaunchManager.does_not_exist"), new String[]{config.getName(), config.getLocation().toOSString()}), null); //$NON-NLS-1$
			}
		}
		return info;
	}
	
	/**
	 * Throws a debug exception with the given throwable that occurred
	 * while processing the given configuration.
	 */
	private void throwException(ILaunchConfiguration config, Throwable e) throws DebugException {
		IPath path = config.getLocation();
		throw createDebugException(MessageFormat.format(DebugCoreMessages.getString("LaunchManager.{0}_occurred_while_reading_launch_configuration_file_{1}._1"), new String[]{e.toString(), path.toOSString()}), e); //$NON-NLS-1$
	}
	
	/**
	 * Return an instance of DebugException containing the specified message and Throwable.
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
	 * Removes the given launch configuration from the cache of configurations.
	 * When a local configuration is deleted, this method is called, as there will
	 * be no resource delta generated to auto-update the cache.
	 * 
	 * @param configuration the configuration to remove
	 */
	private void removeInfo(ILaunchConfiguration configuration) {
		fLaunchConfigurations.remove(configuration);
	}
	
	/**
	 * @see ILaunchManager#getLaunchConfigurations()
	 */
	public ILaunchConfiguration[] getLaunchConfigurations() {
		List allConfigs = getAllLaunchConfigurations();
		return (ILaunchConfiguration[])allConfigs.toArray(new ILaunchConfiguration[allConfigs.size()]);
	}	
	
	/**
	 * @see ILaunchManager#getLaunchConfigurations(ILaunchConfigurationType)
	 */
	public ILaunchConfiguration[] getLaunchConfigurations(ILaunchConfigurationType type) throws CoreException {
		Iterator iter = getAllLaunchConfigurations().iterator();
		List configs = new ArrayList();
		while (iter.hasNext()) {
			ILaunchConfiguration config = (ILaunchConfiguration)iter.next();
			if (config.getType().equals(type)) {
				configs.add(config);
			}
		}
		return (ILaunchConfiguration[])configs.toArray(new ILaunchConfiguration[configs.size()]);
	}
	
	/**
	 * Returns all launch configurations that are stored as resources
	 * in the given project.
	 * 
	 * @param project a project
	 * @return collection of launch configurations that are stored as resources
	 *  in the given project
	 */
	protected List getLaunchConfigurations(IProject project) {
		Iterator iter = getAllLaunchConfigurations().iterator();
		List configs = new ArrayList();
		while (iter.hasNext()) {
			ILaunchConfiguration config = (ILaunchConfiguration)iter.next();
			IFile file = config.getFile();
			if (file != null && file.getProject().equals(project)) {
				configs.add(config);
			}
		}
		return configs;
	}	
	
	/**
	 * Returns all launch configurations that are stored locally.
	 * 
	 * @return collection of launch configurations stored lcoally
	 */
	protected List getLocalLaunchConfigurations() {
		Iterator iter = getAllLaunchConfigurations().iterator();
		List configs = new ArrayList();
		while (iter.hasNext()) {
			ILaunchConfiguration config = (ILaunchConfiguration)iter.next();
			if (config.isLocal()) {
				configs.add(config);
			}
		}
		return configs;
	}		
	
	/**
	 * @see ILaunchManager#getLaunchConfiguration(IFile)
	 */
	public ILaunchConfiguration getLaunchConfiguration(IFile file) {
		hookResourceChangeListener();				
		return new LaunchConfiguration(file.getLocation());
	}
	
	/**
	 * @see ILaunchManager#getLaunchConfiguration(String)
	 */
	public ILaunchConfiguration getLaunchConfiguration(String memento) throws CoreException {
		hookResourceChangeListener();
		return new LaunchConfiguration(memento);
	}
	
	/**
	 * @see ILaunchManager#getLaunchConfigurationTypes()
	 */
	public ILaunchConfigurationType[] getLaunchConfigurationTypes() {
		List types= getLaunchConfigurationTypeList();
		return (ILaunchConfigurationType[])types.toArray(new ILaunchConfigurationType[types.size()]);
	}
	
	/**
	 * @see ILaunchManager#getLaunchConfigurationType(String)
	 */
	public ILaunchConfigurationType getLaunchConfigurationType(String id) {
		Iterator iter = getLaunchConfigurationTypeList().iterator();
		while (iter.hasNext()) {
			ILaunchConfigurationType type = (ILaunchConfigurationType)iter.next();
			if (type.getIdentifier().equals(id)) {
				return type;
			}
		}
		return null;
	}
	
	private List getLaunchConfigurationTypeList() {
		initializeLaunchConfigurationTypes();
		return fLaunchConfigurationTypes;
	}
	
	private synchronized void initializeLaunchConfigurationTypes() {
		if (fLaunchConfigurationTypes == null) {
			hookResourceChangeListener();
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_LAUNCH_CONFIGURATION_TYPES);
			IConfigurationElement[] infos= extensionPoint.getConfigurationElements();
			fLaunchConfigurationTypes= new ArrayList(infos.length);
			for (int i= 0; i < infos.length; i++) {
				IConfigurationElement configurationElement = infos[i];
				LaunchConfigurationType configType = new LaunchConfigurationType(configurationElement); 			
				fLaunchConfigurationTypes.add(configType);
			}
		}
	}
	
	/**
	 * Initializes contributed launch delegates (i.e. delegates contributed
	 * to an existing launch configuration type).
	 */
	private synchronized void initializeContributedDelegates() {
		if (fContributedDelegates == null) {
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_LAUNCH_DELEGATES);
			IConfigurationElement[] infos= extensionPoint.getConfigurationElements();
			fContributedDelegates= new ArrayList(infos.length);
			for (int i= 0; i < infos.length; i++) {
				IConfigurationElement configurationElement = infos[i];
				ContributedDelegate delegate = new ContributedDelegate(configurationElement); 			
				fContributedDelegates.add(delegate);
			}
		}
	}	
	
	/**
	 * Returns a list of launch delegates contributed for existing launch configuration
	 * types.
	 * 
	 * @return list of ContributedDelegate
	 */
	protected List getContributedDelegates() {
		initializeContributedDelegates();
		return fContributedDelegates;
	}
	/**
	 * Notifies the launch manager that a launch configuration
	 * has been deleted. The configuration is removed from the
	 * cache of infos and from the index of configurations by
	 * project, and listeners are notified.
	 * 
	 * @param config the launch configuration that was deleted
	 */
	protected void launchConfigurationDeleted(ILaunchConfiguration config) {
		removeInfo(config);
		getAllLaunchConfigurations().remove(config);
		getConfigurationNotifier().notify(config, REMOVED);
		clearConfigNameCache();			
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
			List allConfigs = getAllLaunchConfigurations();
			if (!allConfigs.contains(config)) {
				allConfigs.add(config);
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
		removeInfo(config);
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
	 * @see ILaunchManager#isExistingLaunchConfigurationName(String)
	 */
	public boolean isExistingLaunchConfigurationName(String name) {
		String[] sortedConfigNames = getAllSortedConfigNames();
		int index = Arrays.binarySearch(sortedConfigNames, name);
		if (index < 0) {
			return false;
		} 
		return true;
	}
	
	/**
	 * @see org.eclipse.debug.core.ILaunchManager#generateUniqueLaunchConfigurationNameFrom(String)
	 */
	public String generateUniqueLaunchConfigurationNameFrom(String baseName) {
		int index = 1;
		int length= baseName.length();
		int copyIndex = baseName.lastIndexOf(" ("); //$NON-NLS-1$
		if (copyIndex > -1 && length > copyIndex + 2 && baseName.charAt(length - 1) == ')') {
			String trailer = baseName.substring(copyIndex + 2, length -1);
			if (isNumber(trailer)) {
				try {
					index = Integer.parseInt(trailer);
					baseName = baseName.substring(0, copyIndex);
				} catch (NumberFormatException nfe) {
				}
			}
		} 
		String newName = baseName;
		
		StringBuffer buffer= null;
		while (isExistingLaunchConfigurationName(newName)) {
			buffer = new StringBuffer(baseName);
			buffer.append(" ("); //$NON-NLS-1$
			buffer.append(String.valueOf(index));
			index++;
			buffer.append(')');
			newName = buffer.toString();		
		}		
		
		return newName;
	}
	
	/**
	 * Returns whether the given String is composed solely of digits
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
	 * Return a sorted array of the names of all <code>ILaunchConfiguration</code>s in 
	 * the workspace.  These are cached, and cache is cleared when a new config is added,
	 * deleted or changed.
	 */
	protected String[] getAllSortedConfigNames() {
		if (fSortedConfigNames == null) {
			ILaunchConfiguration[] configs = getLaunchConfigurations();
			fSortedConfigNames = new String[configs.length];
			for (int i = 0; i < configs.length; i++) {
				fSortedConfigNames[i] = configs[i].getName();
			}
			Arrays.sort(fSortedConfigNames);
		}
		return fSortedConfigNames;
	}
	
	/**
	 * The launch config name cache is cleared when a config is added, deleted or changed.
	 */
	protected void clearConfigNameCache() {
		fSortedConfigNames = null;
	}
		
	/**
	 * Finds and returns all local launch configurations.
	 *
	 * @return all local launch configurations
	 * @exception CoreException if there is a lower level
	 *  IO exception
	 */
	protected List findLocalLaunchConfigurations() {
		IPath containerPath = LOCAL_LAUNCH_CONFIGURATION_CONTAINER_PATH;
		List configs = new ArrayList(10);
		final File directory = containerPath.toFile();
		if (directory.isDirectory()) {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return dir.equals(directory) &&
							name.endsWith(ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION);
				}
			};
			String[] files = directory.list(filter);
			for (int i = 0; i < files.length; i++) {
				LaunchConfiguration config = new LaunchConfiguration(containerPath.append(files[i]));
				configs.add(config);
			}
		}
		return configs;
	}
	
	/**
	 * Finds and returns all launch configurations in the given
	 * container (and subcontainers)
	 * 
	 * @param container the container to search
	 * @exception CoreException an exception occurs traversing
	 *  the container.
	 * @return all launch configurations in the given container
	 */
	protected List findLaunchConfigurations(IContainer container) {
		List list = new ArrayList(10);
		if (container instanceof IProject && !((IProject)container).isOpen()) {
			return list;
		}
		ResourceProxyVisitor visitor= new ResourceProxyVisitor(list);
		try {
			container.accept(visitor, IResource.NONE);
		} catch (CoreException ce) {
			//Closed project...should not be possible with previous check
		}
		Iterator iter = list.iterator();
		List configs = new ArrayList(list.size());
		while (iter.hasNext()) {
			IFile file = (IFile)iter.next();
			configs.add(getLaunchConfiguration(file));
		}
		return configs;
	}
	
	/**
	 * Traverses the delta looking for added/removed/changed launch
	 * configuration files.
	 * 
	 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta= event.getDelta();
		if (delta == null) {
		    // pre-delete
		    LaunchManagerVisitor visitor = getDeltaVisitor();
		    IResource resource = event.getResource();
		    if (resource instanceof IProject) {
                IProject project = (IProject) resource;
                visitor.preDelete(project);
            }
		} else {
			try {
			    LaunchManagerVisitor visitor = getDeltaVisitor();
				delta.accept(visitor);
				visitor.reset();
			} catch (CoreException e) {
				DebugPlugin.log(e);
			}
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
	 * Returns the launch configurations specified by the given
	 * XML document.
	 * 
	 * @param root XML document
	 * @return list of launch configurations
	 * @exception IOException if an exception occurs reading the XML
	 */	
	protected List getConfigsFromXML(Element root) throws CoreException {
		DebugException invalidFormat = 
			new DebugException(
				new Status(
				 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
				 DebugException.REQUEST_FAILED, DebugCoreMessages.getString("LaunchManager.Invalid_launch_configuration_index._18"), null //$NON-NLS-1$
				)
			);		
			
		if (!root.getNodeName().equalsIgnoreCase("launchConfigurations")) { //$NON-NLS-1$
			throw invalidFormat;
		}
		
		// read each launch configuration 
		List configs = new ArrayList(4);	
		NodeList list = root.getChildNodes();
		int length = list.getLength();
		for (int i = 0; i < length; ++i) {
			Node node = list.item(i);
			short type = node.getNodeType();
			if (type == Node.ELEMENT_NODE) {
				Element entry = (Element) node;
				String nodeName = entry.getNodeName();
				if (!nodeName.equals("launchConfiguration")) { //$NON-NLS-1$
					throw invalidFormat;
				}
				String memento = entry.getAttribute("memento"); //$NON-NLS-1$
				if (memento == null) {
					throw invalidFormat;
				}
				configs.add(getLaunchConfiguration(memento));
			}
		}
		return configs;
	}		
	
	/**
	 * The specified project has just opened - add all launch
	 * configs in the project to the index of all configs.
	 * 
	 * @param project the project that has been opened
	 * @exception CoreException if reading the index fails
	 */
	protected void projectOpened(IProject project) {
		List configs = findLaunchConfigurations(project);
		if (!configs.isEmpty()) {
			Iterator iterator = configs.iterator();
			while (iterator.hasNext()) {
				ILaunchConfiguration config = (ILaunchConfiguration) iterator.next();
				launchConfigurationAdded(config);
			}			
		}
	}
	
	/**
	 * The specified project has just closed - remove its
	 * launch configurations from the cached index.
	 * 
	 * @param project the project that has been closed
	 * @exception CoreException if writing the index fails
	 */
	protected void projectClosed(IProject project) {
		List configs = getLaunchConfigurations(project);
		if (!configs.isEmpty()) {
			Iterator iterator = configs.iterator();
			while (iterator.hasNext()) {
				ILaunchConfiguration configuration = (ILaunchConfiguration)iterator.next();
				launchConfigurationDeleted(configuration);
			}
		}
	}	
	
	/**
	 * Visitor for handling resource deltas.
	 */
	class LaunchManagerVisitor implements IResourceDeltaVisitor {
	    
	    /**
	     * Map of files to associated (shared) launch configs in a project
	     * that is going to be deleted.
	     */
	    private Map fFileToConfig = new HashMap();
	    
		/**
		 * @see IResourceDeltaVisitor#visit(IResourceDelta)
		 */
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
				if (ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION.equals(file.getFileExtension())) {
					IPath configPath = file.getLocation();
					ILaunchConfiguration handle = null;
					// If the file has already been deleted, reconstruct the handle from our cache
					if (configPath == null) {
					    handle = (ILaunchConfiguration) fFileToConfig.get(file);
					} else {
					    handle = new LaunchConfiguration(configPath);
					}
					if (handle != null) {
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
						}
					}
				}
				return false;
			} else if (resource instanceof IContainer) {
				return true;
			}
			return true;
		}	
		
		/**
         * Builds a cache of configs that will be deleted in the given project
         */
        public void preDelete(IProject project) {
            List list = findLaunchConfigurations(project);
            Iterator configs = list.iterator();
            while (configs.hasNext()) {
                ILaunchConfiguration configuration = (ILaunchConfiguration) configs.next();
                IFile file = configuration.getFile();
                if (file != null) {
                    fFileToConfig.put(file, configuration);
                }
            }
        }

        /**
		 * Resets this resource delta visitor for a new pass.
		 */
		public void reset() {
		      fFileToConfig.clear();
		}
	}
	
	/**
	 * @see ILaunchManager#addLaunchConfigurationListener(ILaunchConfigurationListener)
	 */
	public void addLaunchConfigurationListener(ILaunchConfigurationListener listener) {
		fLaunchConfigurationListeners.add(listener);
	}

	/**
	 * @see ILaunchManager#removeLaunchConfigurationListener(ILaunchConfigurationListener)
	 */
	public void removeLaunchConfigurationListener(ILaunchConfigurationListener listener) {
		fLaunchConfigurationListeners.remove(listener);
	}

	/**
	 * Register source locators.
	 * 
	 * @exception CoreException if an exception occurs reading
	 *  the extensions
	 */
	private synchronized void initializeSourceLocators() {
		if (fSourceLocators == null) {
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_SOURCE_LOCATORS);
			IConfigurationElement[] infos= extensionPoint.getConfigurationElements();
			fSourceLocators= new HashMap(infos.length);
			for (int i= 0; i < infos.length; i++) {
				IConfigurationElement configurationElement = infos[i];
				String id = configurationElement.getAttribute("id"); //$NON-NLS-1$			
				if (id != null) {
					fSourceLocators.put(id,configurationElement);
				} else {
					// invalid status handler
					IStatus s = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugException.INTERNAL_ERROR,
					MessageFormat.format(DebugCoreMessages.getString("LaunchManager.Invalid_source_locator_extentsion_defined_by_plug-in___{0}______id___not_specified_12"), new String[] {configurationElement.getDeclaringExtension().getNamespace()} ), null);  //$NON-NLS-1$
					DebugPlugin.log(s);
				}
			}
		}
	}
	
	
	/**
	 * Load comparator extensions.
	 */
	private synchronized void initializeComparators() {
		if (fComparators == null) {
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_LAUNCH_CONFIGURATION_COMPARATORS);
			IConfigurationElement[] infos= extensionPoint.getConfigurationElements();
			fComparators = new HashMap(infos.length);
			for (int i= 0; i < infos.length; i++) {
				IConfigurationElement configurationElement = infos[i];
				String attr = configurationElement.getAttribute("attribute"); //$NON-NLS-1$			
				if (attr != null) {
					fComparators.put(attr, new LaunchConfigurationComparator(configurationElement));
				} else {
					// invalid status handler
					IStatus s = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugException.INTERNAL_ERROR,
					MessageFormat.format(DebugCoreMessages.getString("LaunchManager.Invalid_launch_configuration_comparator_extension_defined_by_plug-in_{0}_-_attribute_not_specified_1"), new String[] {configurationElement.getDeclaringExtension().getNamespace()}), null); //$NON-NLS-1$
					DebugPlugin.log(s);
				}
			}
		}
	}
		
	/**
	 * @see ILaunchManager#newSourceLocator(String)
	 */
	public IPersistableSourceLocator newSourceLocator(String identifier) throws CoreException {
		initializeSourceLocators();
		IConfigurationElement config = (IConfigurationElement)fSourceLocators.get(identifier);
		if (config == null) {
			throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugException.INTERNAL_ERROR,
				MessageFormat.format(DebugCoreMessages.getString("LaunchManager.Source_locator_does_not_exist__{0}_13"), new String[] {identifier} ), null)); //$NON-NLS-1$
		} 
		IPersistableSourceLocator sourceLocator = (IPersistableSourceLocator)config.createExecutableExtension("class"); //$NON-NLS-1$
		if (sourceLocator instanceof AbstractSourceLookupDirector) {
			((AbstractSourceLookupDirector)sourceLocator).setId(identifier);
		}
		return sourceLocator;
	}

	/**
	 * Returns the comparator registered for the given attribute, or
	 * <code>null</code> if none.
	 * 
	 * @param attributeName attribute for which a comparator is required
	 * @return comparator, or <code>null</code> if none
	 */
	protected Comparator getComparator(String attributeName) {
		 Map map = getComparators();
		 return (Comparator)map.get(attributeName);
	}
	
	/**
	 * Returns comparators, loading if required
	 */
	protected Map getComparators() {
		initializeComparators();
		return fComparators;
	}
	
	private IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}
	
	private IWorkspaceRoot getWorkspaceRoot() {
		return getWorkspace().getRoot();
	}
	
	/**
	 * @see org.eclipse.debug.core.ILaunchManager#addLaunches(org.eclipse.debug.core.ILaunch)
	 */
	public void addLaunches(ILaunch[] launches) {
		List added = new ArrayList(launches.length);
		for (int i = 0; i < launches.length; i++) {
			if (internalAddLaunch(launches[i])) {
				added.add(launches[i]);
			}
		}
		if (!added.isEmpty()) {
			ILaunch[] addedLaunches = (ILaunch[])added.toArray(new ILaunch[added.size()]);
			fireUpdate(addedLaunches, ADDED);
			for (int i = 0; i < addedLaunches.length; i++) {
				fireUpdate(launches[i], ADDED);
			}
		}
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchManager#addLaunchListener(org.eclipse.debug.core.ILaunchesListener)
	 */
	public void addLaunchListener(ILaunchesListener listener) {
		fLaunchesListeners.add(listener);
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchManager#removeLaunches(org.eclipse.debug.core.ILaunch)
	 */
	public void removeLaunches(ILaunch[] launches) {
		List removed = new ArrayList(launches.length);
		for (int i = 0; i < launches.length; i++) {
			if (internalRemoveLaunch(launches[i])) {
				removed.add(launches[i]);
			}
		}
		if (!removed.isEmpty()) {
			ILaunch[] removedLaunches = (ILaunch[])removed.toArray(new ILaunch[removed.size()]);
			fireUpdate(removedLaunches, REMOVED);
			for (int i = 0; i < removedLaunches.length; i++) {
				fireUpdate(removedLaunches[i], REMOVED);
			}
		}
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchManager#removeLaunchListener(org.eclipse.debug.core.ILaunchesListener)
	 */
	public void removeLaunchListener(ILaunchesListener listener) {
		fLaunchesListeners.remove(listener);
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
	 * @see org.eclipse.debug.core.ILaunchManager#getMovedFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public ILaunchConfiguration getMovedFrom(ILaunchConfiguration addedConfiguration) {
		if (addedConfiguration.equals(fTo)) {
			return fFrom;
		}
		return null;
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchManager#getMovedTo(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public ILaunchConfiguration getMovedTo(ILaunchConfiguration removedConfiguration) {
		if (removedConfiguration.equals(fFrom)) {
			return fTo;
		}
		return null;
	}
	
	/**
	 * Collects files whose extension matches the launch configuration file
	 * extension.
	 */
	class ResourceProxyVisitor implements IResourceProxyVisitor {
		
		private List fList;
		
		protected ResourceProxyVisitor(List list) {
			fList= list;
		}
		/**
		 * @see org.eclipse.core.resources.IResourceProxyVisitor#visit(org.eclipse.core.resources.IResourceProxy)
		 */
		public boolean visit(IResourceProxy proxy) {
			if (proxy.getType() == IResource.FILE) {
				if (ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION.equalsIgnoreCase(proxy.requestFullPath().getFileExtension())) {
					fList.add(proxy.requestResource());
				}
				return false;
			}
			return true;
		}
	}

	private LaunchNotifier getLaunchNotifier() {
		return new LaunchNotifier();
	}
	
	/**
	 * Notifies a launch listener (single launch) in a safe runnable to handle
	 * exceptions.
	 */
	class LaunchNotifier implements ISafeRunnable {
		
		private ILaunchListener fListener;
		private int fType;
		private ILaunch fLaunch;
		
		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#handleException(java.lang.Throwable)
		 */
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, DebugCoreMessages.getString("LaunchManager.An_exception_occurred_during_launch_change_notification._1"), exception); //$NON-NLS-1$
			DebugPlugin.log(status);
		}

		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#run()
		 */
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
			}			
		}

		/**
		 * Notifies the given listener of the add/change/remove
		 * 
		 * @param listener the listener to notify
		 * @param launch the launch that has changed
		 * @param update the type of change
		 */
		public void notify(ILaunch launch, int update) {
			fLaunch = launch;
			fType = update;
			Object[] copiedListeners= fListeners.getListeners();
			for (int i= 0; i < copiedListeners.length; i++) {
				fListener = (ILaunchListener)copiedListeners[i];
				Platform.run(this);
			}	
			fLaunch = null;
			fListener = null;		
		}
	}
	
	private LaunchesNotifier getLaunchesNotifier() {
		return new LaunchesNotifier();
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
		
		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#handleException(java.lang.Throwable)
		 */
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, DebugCoreMessages.getString("LaunchManager.An_exception_occurred_during_launch_change_notification._1"), exception); //$NON-NLS-1$
			DebugPlugin.log(status);
		}

		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#run()
		 */
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
						List registered = null;
						for (int j = 0; j < fNotifierLaunches.length; j++) {
							if (isRegistered(fNotifierLaunches[j])) {
								if (registered != null) {
									registered.add(fNotifierLaunches[j]);
								} 
							} else {
								if (registered == null) {
									registered = new ArrayList(fNotifierLaunches.length);
									for (int k = 0; k < j; k++) {
										registered.add(fNotifierLaunches[k]);
									}
								}
							}
						}
						if (registered == null) {
							fRegistered = fNotifierLaunches;
						} else {
							fRegistered = (ILaunch[])registered.toArray(new ILaunch[registered.size()]);
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
			}
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
			Object[] copiedListeners= fLaunchesListeners.getListeners();
			for (int i= 0; i < copiedListeners.length; i++) {
				fListener = (ILaunchesListener)copiedListeners[i];
				Platform.run(this);
			}	
			fNotifierLaunches = null;
			fRegistered = null;
			fListener = null;			
		}
	}
	
	protected ConfigurationNotifier getConfigurationNotifier() {
		return new ConfigurationNotifier();
	}
	
	/**
	 * Notifies a launch config listener in a safe runnable to handle
	 * exceptions.
	 */
	class ConfigurationNotifier implements ISafeRunnable {
		
		private ILaunchConfigurationListener fListener;
		private int fType;
		private ILaunchConfiguration fConfiguration;
		
		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#handleException(java.lang.Throwable)
		 */
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, DebugCoreMessages.getString("LaunchManager.An_exception_occurred_during_launch_configuration_change_notification._3"), exception); //$NON-NLS-1$
			DebugPlugin.log(status);
		}

		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#run()
		 */
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
			}			
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
			if (fLaunchConfigurationListeners.size() > 0) {
				Object[] listeners = fLaunchConfigurationListeners.getListeners();
				for (int i = 0; i < listeners.length; i++) {
					fListener = (ILaunchConfigurationListener)listeners[i];
					Platform.run(this);
				}
			}
			fConfiguration = null;
			fListener = null;			
		}
	}
	
	/**
	 * Load comparator extensions.
	 * 
	 * @exception CoreException if an exception occurs reading
	 *  the extensions
	 */
	private synchronized void initializeLaunchModes() {
		if (fLaunchModes == null) {
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_LAUNCH_MODES);
			IConfigurationElement[] infos= extensionPoint.getConfigurationElements();
			fLaunchModes = new HashMap();
			for (int i= 0; i < infos.length; i++) {
				IConfigurationElement configurationElement = infos[i];
				try {
					ILaunchMode mode = new LaunchMode(configurationElement);
					fLaunchModes.put(mode.getIdentifier(), mode);
				} catch (CoreException e) {
					DebugPlugin.log(e);
				}
				
			}
		}
	}
	
	/** 
	 * Returns an array of environment variables to be used when
	 * launching the given configuration or <code>null</code> if unspecified.
	 * 
	 * @param configuration launch configuration
	 * @throws CoreException if unable to access associated attribute or if
	 * unable to resolve a variable in an environment variable's value
	 */
	public String[] getEnvironment(ILaunchConfiguration configuration) throws CoreException {
		Map envMap = configuration.getAttribute(ATTR_ENVIRONMENT_VARIABLES, (Map) null);
		if (envMap == null) {
			return null;
		}
		Map env = null;
		// build base environment
		env= new HashMap();
		boolean append= configuration.getAttribute(ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
		if (append) {
			env.putAll(getNativeEnvironment());
		}
		
		// Add variables from config
		Iterator iter= envMap.entrySet().iterator();
		boolean win32= Platform.getOS().equals(Constants.OS_WIN32);
		while (iter.hasNext()) {
			Map.Entry entry= (Map.Entry) iter.next();
			String key= (String) entry.getKey();
			if (win32) {
				// Win32 vars are case insensitive. Uppercase everything so
				// that (for example) "pAtH" will correctly replace "PATH"
				key= key.toUpperCase();
			}
			String value = (String) entry.getValue();
			// translate any string substitution variables
			String translated = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(value);
			env.put(key, translated);
		}		
		
		iter= env.entrySet().iterator();
		List strings= new ArrayList(env.size());
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			StringBuffer buffer= new StringBuffer((String) entry.getKey());
			buffer.append('=').append((String) entry.getValue());
			strings.add(buffer.toString());
		}
		return (String[]) strings.toArray(new String[strings.size()]);
	}
	
	
	/**
	 * Returns a copy of the native system environment variables. On WIN32,
	 * all keys (variable names) are returned in uppercase. Note
	 * that WIN32's environment is not case sensitive.
	 * 
	 * @return the a copy of the native system environment variables
	 */
	public Map getNativeEnvironment() {
		if (fgNativeEnv != null) {
			return new HashMap(fgNativeEnv);
		}
		fgNativeEnv= new HashMap();
		try {
			String nativeCommand= null;
			boolean windowsOS= false;
			boolean isWin9xME= false; //see bug 50567
			String fileName= null;
			if (Platform.getOS().equals(Constants.OS_WIN32)) {
				windowsOS= true;
				String osName= System.getProperty("os.name"); //$NON-NLS-1$
				isWin9xME= osName != null && (osName.startsWith("Windows 9") || osName.startsWith("Windows ME")); //$NON-NLS-1$ //$NON-NLS-2$
				if (isWin9xME) {
					// Win 95, 98, and ME
					// SET might not return therefore we pipe into a file
					IPath stateLocation= DebugPlugin.getDefault().getStateLocation();
					fileName= stateLocation.toOSString() + File.separator  + "env.txt"; //$NON-NLS-1$
					nativeCommand= "command.com /C set > " + fileName; //$NON-NLS-1$
				} else {
					// Win NT, 2K, XP
					nativeCommand= "cmd.exe /C set"; //$NON-NLS-1$
				}
			} else if (!Platform.getOS().equals(Constants.OS_UNKNOWN)){
				nativeCommand= "printenv";		 //$NON-NLS-1$
			}
			if (nativeCommand == null) {
				return fgNativeEnv;
			}
			Process process= Runtime.getRuntime().exec(nativeCommand);
			if (isWin9xME) {
				//read piped data on Win 95, 98, and ME
				Properties p= new Properties();
				File file= new File(fileName);
				FileInputStream stream= new FileInputStream(file);
				p.load(stream);
				stream.close();
				if (!file.delete()) {
					file.deleteOnExit(); // if delete() fails try again on VM close
				}
				for (Enumeration enumeration = p.keys(); enumeration.hasMoreElements();) {
					// Win32's environment vars are case insensitive. Put everything
					// to uppercase so that (for example) the "PATH" variable will match
					// "pAtH" correctly on Windows.
					String key= ((String) enumeration.nextElement()).toUpperCase();
					//no need to cast value
					fgNativeEnv.put(key, p.get(key));
				}
			} else {
				//read process directly on other platforms
				BufferedReader reader= new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line= reader.readLine();
				while (line != null) {
					int separator= line.indexOf('=');
					if (separator > 0) {
						String key= line.substring(0, separator);
						if (windowsOS) {
							// Win32's environment vars are case insensitive. Put everything
							// to uppercase so that (for example) the "PATH" variable will match
							// "pAtH" correctly on Windows.
							key= key.toUpperCase();
						}
						String value= line.substring(separator + 1);
						fgNativeEnv.put(key, value);
					}
					line= reader.readLine();
				}
				reader.close();
			}
		} catch (IOException e) {
			// Native environment-fetching code failed.
			// This can easily happen and is not useful to log.
		}
		return new HashMap(fgNativeEnv);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchManager#newSourcePathComputer(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public ISourcePathComputer getSourcePathComputer(ILaunchConfiguration configuration) throws CoreException {
		String id = null;
		id = configuration.getAttribute(ISourcePathComputer.ATTR_SOURCE_PATH_COMPUTER_ID, (String)null);
		
		if (id == null) {
			//use default computer for configuration type, if any			
			return configuration.getType().getSourcePathComputer();							
		}
		return getSourcePathComputer(id);
	}
	
	/**
	 * Initializes source container type and source path computer extensions.
	 */
	private synchronized void initializeSourceContainerTypes() {
		if (sourceContainerTypes == null) {
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_SOURCE_CONTAINER_TYPES);
			IConfigurationElement[] extensions = extensionPoint.getConfigurationElements();
			sourceContainerTypes = new HashMap();
			for (int i = 0; i < extensions.length; i++) {
				sourceContainerTypes.put(
						extensions[i].getAttribute("id"), //$NON-NLS-1$
						new SourceContainerType(extensions[i]));
			}
			extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_SOURCE_PATH_COMPUTERS);
			extensions = extensionPoint.getConfigurationElements();
			sourcePathComputers = new HashMap();
			for (int i = 0; i < extensions.length; i++) {
				sourcePathComputers.put(
						extensions[i].getAttribute("id"), //$NON-NLS-1$
						new SourcePathComputer(extensions[i]));
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchManager#getSourceContainerType(java.lang.String)
	 */
	public ISourceContainerType getSourceContainerType(String id) {
		initializeSourceContainerTypes();
		return (ISourceContainerType) sourceContainerTypes.get(id);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchManager#getSourceContainerTypes()
	 */
	public ISourceContainerType[] getSourceContainerTypes() {
		initializeSourceContainerTypes();
		Collection containers = sourceContainerTypes.values();
		return (ISourceContainerType[]) containers.toArray(new ISourceContainerType[containers.size()]);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchManager#getSourcePathComputer(java.lang.String)
	 */
	public ISourcePathComputer getSourcePathComputer(String id) {
		initializeSourceContainerTypes();
		return (ISourcePathComputer) sourcePathComputers.get(id);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchManager#getLaunchModes()
	 */
	public ILaunchMode[] getLaunchModes() {
		initializeLaunchModes();
		Collection collection = fLaunchModes.values();
		return (ILaunchMode[]) collection.toArray(new ILaunchMode[collection.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchManager#getLaunchMode(java.lang.String)
	 */
	public ILaunchMode getLaunchMode(String mode) {
		initializeLaunchModes();
		return (ILaunchMode) fLaunchModes.get(mode);
	}
}