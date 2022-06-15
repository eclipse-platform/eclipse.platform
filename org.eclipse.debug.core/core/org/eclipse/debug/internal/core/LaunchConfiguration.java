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
 *     Sascha Radike - bug 56642
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.internal.core;


import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.debug.core.model.IPersistableSourceLocator;
import org.eclipse.debug.core.sourcelookup.IPersistableSourceLocator2;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Launch configuration handle.
 *
 * @see ILaunchConfiguration
 */
public class LaunchConfiguration extends PlatformObject implements ILaunchConfiguration {

	/**
	 * Launch configuration attribute that specifies the resources paths mapped to it.
	 * Not all launch configurations will have a mapped resource unless migrated.
	 * Value is a list of resource paths stored as portable strings, or <code>null</code>
	 * if none.
	 *
	 * @since 3.2
	 */
	public static final String ATTR_MAPPED_RESOURCE_PATHS = DebugPlugin.getUniqueIdentifier() + ".MAPPED_RESOURCE_PATHS"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute that specifies the resources types mapped to it.
	 * Not all launch configurations will have a mapped resource unless migrated.
	 * Value is a list of resource type integers, or <code>null</code> if none.
	 *
	 * @since 3.2
	 */
	public static final String ATTR_MAPPED_RESOURCE_TYPES = DebugPlugin.getUniqueIdentifier() + ".MAPPED_RESOURCE_TYPES"; //$NON-NLS-1$

	/**
	 * The launch modes set on this configuration.
	 *
	 * @since 3.3
	 */
	public static final String ATTR_LAUNCH_MODES = DebugPlugin.getUniqueIdentifier() + ".LAUNCH_MODES"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute storing a list
	 * of preferred launchers for associated mode sets.
	 * This attribute is a list of launchers stored by mode set
	 * and relating to the id of the preferred launcher, which happens to be an <code>ILaunchDelegate</code>
	 *
	 * @since 3.3
	 */
	public static final String ATTR_PREFERRED_LAUNCHERS = DebugPlugin.getUniqueIdentifier() + ".preferred_launchers"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute storing a memento identifying the prototype
	 * this configuration was made from, possibly <code>null</code>.
	 *
	 *  @since 3.12
	 */
	public static final String ATTR_PROTOTYPE = DebugPlugin.getUniqueIdentifier() + ".ATTR_PROTOTYPE"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute storing if this configuration is a
	 * prototype or not.
	 *
	 * @since 3.12
	 */
	public static final String IS_PROTOTYPE = DebugPlugin.getUniqueIdentifier() + ".IS_PROTOTYPE"; //$NON-NLS-1$

	/**
	 * Status handler to prompt in the UI thread
	 *
	 * @since 3.3
	 */
	protected static final IStatus promptStatus = new Status(IStatus.INFO, "org.eclipse.debug.ui", 200, "", null);  //$NON-NLS-1$//$NON-NLS-2$

	/**
	 * Status handler to prompt the user to resolve the missing launch delegate issue
	 * @since 3.3
	 */
	protected static final IStatus delegateNotAvailable = new Status(IStatus.INFO, "org.eclipse.debug.core", 226, "", null); //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Status handle to prompt the user to resolve duplicate launch delegates being detected
	 *
	 *  @since 3.3
	 */
	protected static final IStatus duplicateDelegates = new Status(IStatus.INFO, "org.eclipse.debug.core", 227, "", null);  //$NON-NLS-1$//$NON-NLS-2$

	/**
	 * This configuration's name
	 * @since 3.5
	 */
	private String fName;

	/**
	 * The container this configuration is stored in or <code>null</code> if stored locally
	 * with workspace metadata.
	 * @since 3.5
	 */
	private IContainer fContainer;

	/**
	 * If this configuration is a prototype.
	 * @since 3.12
	 */
	private boolean fIsPrototype;

	/**
	 * Constructs a launch configuration with the given name. The configuration
	 * is stored in the given container or locally with workspace metadata if
	 * the specified container is <code>null</code>.
	 *
	 * @param name launch configuration name
	 * @param container parent container or <code>null</code>
	 * @since 3.5
	 */
	protected LaunchConfiguration(String name, IContainer container) {
		this(name, container, false);
	}

	/**
	 * Constructs a launch configuration with the given name. The configuration
	 * is stored in the given container or locally with workspace metadata if
	 * the specified container is <code>null</code>.
	 *
	 * @param name launch configuration name
	 * @param container parent container or <code>null</code>
	 * @param prototype if the configuration is a prototype or not
	 * @since 3.12
	 */
	protected LaunchConfiguration(String name, IContainer container, boolean prototype) {
		initialize();
		setName(name);
		setContainer(container);
		fIsPrototype = prototype;
	}

	/**
	 * Initialize any state variables - called first in the constructor.
	 * Subclasses must override as appropriate.
	 */
	protected void initialize() {
	}

	/**
	 * Constructs a launch configuration on the given workspace file.
	 *
	 * @param file workspace .launch file
	 * @since 3.5
	 */
	protected LaunchConfiguration(IFile file) {
		this(getSimpleName(file.getName()), file.getParent(), isPrototype(file));
	}

	/**
	 * Given a name that ends with .launch or .prototype, return the simple name of the configuration.
	 *
	 * @param fileName the name to parse
	 * @return simple name
	 * @since 3.5
	 */
	protected static String getSimpleName(String fileName) {
		IPath path = new Path(fileName);
		if(ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION.equals(path.getFileExtension())) {
			return path.removeFileExtension().toString();
		} else if (ILaunchConfiguration.LAUNCH_CONFIGURATION_PROTOTYPE_FILE_EXTENSION.equals(path.getFileExtension())) {
			return path.removeFileExtension().toString();
		}
		return fileName;
	}

	/**
	 * Constructs a launch configuration from the given
	 * memento.
	 *
	 * @param memento launch configuration memento
	 * @exception CoreException if the memento is invalid or
	 * 	an exception occurs reading the memento
	 */
	protected LaunchConfiguration(String memento) throws CoreException {
		Exception ex = null;
		try {
			Element root = null;
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			parser.setErrorHandler(new DefaultHandler());
			StringReader reader = new StringReader(memento);
			InputSource source = new InputSource(reader);
			root = parser.parse(source).getDocumentElement();

			String localString = root.getAttribute(IConfigurationElementConstants.LOCAL);
			String path = root.getAttribute(IConfigurationElementConstants.PATH);

			String message = null;
			if (path == null || IInternalDebugCoreConstants.EMPTY_STRING.equals(path)) {
				message = DebugCoreMessages.LaunchConfiguration_18;
			} else if (localString == null || IInternalDebugCoreConstants.EMPTY_STRING.equals(localString)) {
				message = DebugCoreMessages.LaunchConfiguration_19;
			}
			if (message != null) {
				throw new CoreException(newStatus(message, DebugException.INTERNAL_ERROR, null));
			}


			boolean local = Boolean.parseBoolean(localString);
			IPath iPath = new Path(path);
			String name = getSimpleName(iPath.lastSegment());
			IContainer container = null;
			if (!local) {
				container = ResourcesPlugin.getWorkspace().getRoot().getFile(iPath).getParent();
			}
			setName(name);
			setContainer(container);
			return;
		} catch (ParserConfigurationException e) {
			ex = e;
		} catch (SAXException e) {
			ex = e;
		} catch (IOException e) {
			ex = e;
		}
		IStatus s = newStatus(DebugCoreMessages.LaunchConfiguration_17, DebugException.INTERNAL_ERROR, ex);
		throw new CoreException(s);
	}

	@Override
	public boolean contentsEqual(ILaunchConfiguration object) {
		try {
			if (object instanceof LaunchConfiguration) {
				LaunchConfiguration otherConfig = (LaunchConfiguration) object;
				return getName().equals(otherConfig.getName())
					 && getType().equals(otherConfig.getType())
					 && equalOrNull(getContainer(), otherConfig.getContainer())
					 && getInfo().equals(otherConfig.getInfo());
			}
			return false;
		} catch (CoreException ce) {
			return false;
		}
	}

	@Override
	public ILaunchConfigurationWorkingCopy copy(String name) throws CoreException {
		ILaunchConfigurationWorkingCopy copy = new LaunchConfigurationWorkingCopy(this, name);
		return copy;
	}

	@Override
	public void delete() throws CoreException {
		if (exists()) {
			IFile file = getFile();
			if (file == null) {
				IFileStore store = getFileStore();
				if (store != null) {
					store.delete(EFS.NONE, null);
					if ((store.fetchInfo().exists())) {
						throw new DebugException(
							new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
							 DebugException.REQUEST_FAILED, DebugCoreMessages.LaunchConfiguration_Failed_to_delete_launch_configuration__1, null)
						);
					}
				}
			} else {
				// Delete the resource using IFile API such that
				// resource deltas are fired.
				// First do validate edit to ensure the resource is local
				if (file.isReadOnly()) {
					IStatus status = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] {file}, null);
					if (!status.isOK()) {
						throw new CoreException(status);
					}
				}
				file.delete(true, null);
			}
			// update the launch manager cache synchronously
			getLaunchManager().launchConfigurationDeleted(this);
		}
	}

	@Override
	public void delete(int flag) throws CoreException {
		if (flag == UPDATE_PROTOTYPE_CHILDREN && isPrototype()) {
			// clear back pointers to this configuration
			Collection<ILaunchConfiguration> children = getPrototypeChildren();
			for (ILaunchConfiguration child : children) {
				ILaunchConfigurationWorkingCopy childWC = child.getWorkingCopy();
				childWC.setPrototype(null, false);
				childWC.doSave();
			}
		}
		delete();
	}

	/**
	 * Returns whether this configuration is equal to the
	 * given configuration. Two configurations are equal if
	 * they are stored in the same location (and neither one
	 * is a working copy).
	 *
	 * @return whether this configuration is equal to the
	 *  given configuration
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(Object object) {
		if (object instanceof ILaunchConfiguration) {
			if (isWorkingCopy()) {
				return this == object;
			}
			LaunchConfiguration config = (LaunchConfiguration) object;
			if (!config.isWorkingCopy()) {
				return getName().equals(config.getName()) &&
					equalOrNull(getContainer(), config.getContainer());
			}
		}
		return false;
	}

	/**
	 * Returns whether the given objects are equal or both <code>null</code>.
	 *
	 * @param o1 the object
	 * @param o2 the object to be compared to o1
	 * @return whether the given objects are equal or both <code>null</code>
	 * @since 3.5
	 */
	protected boolean equalOrNull(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		} else if (o2 != null) {
			return o1.equals(o2);
		}
		return false;
	}

	@Override
	public boolean exists() {
		IFile file = getFile();
		if (file != null) {
			return file.exists();
		}
		try {
			IFileStore store = getFileStore();
			if (store != null) {
				return store.fetchInfo().exists();
			}
		} catch (CoreException e) {
		}
		return false;
	}

	@Override
	public boolean getAttribute(String attributeName, boolean defaultValue) throws CoreException {
		return getInfo().getBooleanAttribute(attributeName, defaultValue);
	}

	@Override
	public int getAttribute(String attributeName, int defaultValue) throws CoreException {
		return getInfo().getIntAttribute(attributeName, defaultValue);
	}

	@Override
	public List<String> getAttribute(String attributeName, List<String> defaultValue) throws CoreException {
		return getInfo().getListAttribute(attributeName, defaultValue);
	}

	@Override
	public Set<String> getAttribute(String attributeName, Set<String> defaultValue) throws CoreException {
		return getInfo().getSetAttribute(attributeName, defaultValue);
	}

	@Override
	public Map<String, String> getAttribute(String attributeName, Map<String, String> defaultValue) throws CoreException {
		return getInfo().getMapAttribute(attributeName, defaultValue);
	}

	@Override
	public String getAttribute(String attributeName, String defaultValue) throws CoreException {
		return getInfo().getStringAttribute(attributeName, defaultValue);
	}

	@Override
	public Map<String, Object> getAttributes() throws CoreException {
		LaunchConfigurationInfo info = getInfo();
		return info.getAttributes();
	}

	@Override
	public String getCategory() throws CoreException {
		return getType().getCategory();
	}

	@Override
	public IFile getFile() {
		IContainer container = getContainer();
		if (container != null) {
			return container.getFile(new Path(getFileName()));
		}
		return null;
	}

	/**
	 * Returns the simple file name of this launch configuration.
	 *
	 * @return the simple file name of this launch configuration - for example
	 *  	"Abc.launch" or "Abc.prototype"
	 */
	protected String getFileName() {
		StringBuilder buf = new StringBuilder(getName());
		buf.append('.');
		if (isPrototype()) {
			buf.append(ILaunchConfiguration.LAUNCH_CONFIGURATION_PROTOTYPE_FILE_EXTENSION);
		} else {
			buf.append(ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION);
		}
		return buf.toString();
	}

	/**
	 * Returns the info object containing the attributes
	 * of this configuration
	 *
	 * @return info for this handle
	 * @exception CoreException if unable to retrieve the
	 *  info object
	 */
	protected LaunchConfigurationInfo getInfo() throws CoreException {
		return getLaunchManager().getInfo(this);
	}

	/**
	 * Returns the launch manager
	 *
	 * @return launch manager
	 */
	protected LaunchManager getLaunchManager() {
		return (LaunchManager)DebugPlugin.getDefault().getLaunchManager();
	}

	@Override
	public IPath getLocation() {
		try {
			IFileStore store = getFileStore();
			if (store != null) {
				File localFile = store.toLocalFile(EFS.NONE, null);
				if (localFile != null) {
					return new Path(localFile.getAbsolutePath());
				}
			}
		} catch (CoreException e) {
		}
		return null;
	}

	/**
	 * Returns the file store this configuration is persisted in or <code>null</code> if
	 * a file store cannot be derived. The file may or may not exist. If this configuration
	 * is in a project that is closed or does not exist, <code>null</code> is returned.
	 *
	 * @return file store this configuration is persisted in or <code>null</code>
	 * @throws CoreException if a problem is encountered
	 * @since 3.5
	 */
	public IFileStore getFileStore() throws CoreException {
		if (isLocal()) {
			return EFS.getLocalFileSystem().fromLocalFile(
				LaunchManager.LOCAL_LAUNCH_CONFIGURATION_CONTAINER_PATH.append(getFileName()).toFile());
		}
		URI uri = getFile().getLocationURI();
		if (uri != null) {
			return EFS.getStore(uri);
		}
		return null;
	}

	@Override
	public IResource[] getMappedResources() throws CoreException {
		List<String> paths = getAttribute(ATTR_MAPPED_RESOURCE_PATHS, (List<String>) null);
		if (paths == null || paths.isEmpty()) {
			return null;
		}
		List<String> types = getAttribute(ATTR_MAPPED_RESOURCE_TYPES, (List<String>) null);
		if (types == null || types.size() != paths.size()) {
			throw new CoreException(newStatus(DebugCoreMessages.LaunchConfiguration_0, DebugPlugin.ERROR, null));
		}
		ArrayList<IResource> list = new ArrayList<>();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		for(int i = 0; i < paths.size(); i++) {
			String pathStr = paths.get(i);
			String typeStr= types.get(i);
			int type = -1;
			try {
				type = Integer.decode(typeStr).intValue();
			} catch (NumberFormatException e) {
				throw new CoreException(newStatus(DebugCoreMessages.LaunchConfiguration_0, DebugPlugin.ERROR, e));
			}
			IPath path = Path.fromPortableString(pathStr);
			IResource res = null;
			switch (type) {
				case IResource.FILE:
					res = root.getFile(path);
					break;
				case IResource.PROJECT:
					pathStr = path.makeRelative().toPortableString();
					if(Path.ROOT.isValidSegment(pathStr)) {
						res = root.getProject(pathStr);
					}
					break;
				case IResource.FOLDER:
					res = root.getFolder(path);
					break;
				case IResource.ROOT:
					res = root;
					break;
				default:
					throw new CoreException(newStatus(DebugCoreMessages.LaunchConfiguration_0, DebugPlugin.ERROR, null));
			}
			if(res != null) {
				list.add(res);
			}
		}
		if (list.isEmpty()) {
			return null;
		}
		return list.toArray(new IResource[list.size()]);
	}

	@Override
	public String getMemento() throws CoreException {
		IPath relativePath = null;
		IFile file = getFile();
		String lineDelimeter = getLineSeparator();
		boolean local = true;
		if (file == null) {
			relativePath = new Path(getName());
		} else {
			local = false;
			relativePath = file.getFullPath();
		}
		Exception e= null;
		try {
			Document doc = LaunchManager.getDocument();
			Element node = doc.createElement(IConfigurationElementConstants.LAUNCH_CONFIGURATION);
			doc.appendChild(node);
			node.setAttribute(IConfigurationElementConstants.LOCAL, Boolean.toString(local));
			node.setAttribute(IConfigurationElementConstants.PATH, relativePath.toString());
			return LaunchManager.serializeDocument(doc, lineDelimeter);
		} catch (IOException ioe) {
			e= ioe;
		} catch (ParserConfigurationException pce) {
			e= pce;
		} catch (TransformerException te) {
			e= te;
		}
		IStatus status = newStatus(DebugCoreMessages.LaunchConfiguration_16, DebugException.INTERNAL_ERROR,  e);
		throw new CoreException(status);
	}

	@Override
	public String getName() {
		return fName;
	}

	/**
	 * Returns the container this configuration is stored in, or <code>null</code>
	 * if this configuration is local.
	 *
	 * @return the container this configuration is stored in, or <code>null</code>
	 * if this configuration is local
	 * @since 3.5
	 */
	protected IContainer getContainer() {
		return fContainer;
	}

	@Override
	public Set<String> getModes() throws CoreException {
		Set<String> options = getAttribute(ATTR_LAUNCH_MODES, (Set<String>) null);
		return (options != null ? new HashSet<>(options) : new HashSet<>(0));
	}

	@Override
	public ILaunchConfigurationType getType() throws CoreException {
		return getInfo().getType();
	}

	@Override
	public ILaunchConfigurationWorkingCopy getWorkingCopy() throws CoreException {
		return new LaunchConfigurationWorkingCopy(this);
	}

	@Override
	public int hashCode() {
		IContainer container = getContainer();
		if (container == null) {
			return getName().hashCode();
		} else {
			return getName().hashCode() + container.hashCode();
		}
	}

	@Override
	public boolean hasAttribute(String attributeName) throws CoreException {
		return getInfo().hasAttribute(attributeName);
	}

	/**
	 * Set the source locator to use with the launch, if specified
	 * by this configuration.
	 *
	 * @param launch the launch on which to set the source locator
	 * @throws CoreException if a problem is encountered
	 */
	protected void initializeSourceLocator(ILaunch launch) throws CoreException {
		if (launch.getSourceLocator() == null) {
			String type = getAttribute(ATTR_SOURCE_LOCATOR_ID, (String)null);
			if (type == null) {
				type = getType().getSourceLocatorId();
			}
			if (type != null) {
				IPersistableSourceLocator locator = getLaunchManager().newSourceLocator(type);
				String memento = getAttribute(ATTR_SOURCE_LOCATOR_MEMENTO, (String)null);
				if (memento == null) {
					locator.initializeDefaults(this);
				} else if(locator instanceof IPersistableSourceLocator2) {
					((IPersistableSourceLocator2)locator).initializeFromMemento(memento, this);
				} else {
					locator.initializeFromMemento(memento);
				}
				launch.setSourceLocator(locator);
			}
		}
	}

	@Override
	public boolean isLocal() {
		return getContainer() == null;
	}

	@Override
	public boolean isMigrationCandidate() throws CoreException {
		return ((LaunchConfigurationType)getType()).isMigrationCandidate(this);
	}

	@Override
	public boolean isWorkingCopy() {
		return false;
	}

	@Override
	public ILaunch launch(String mode, IProgressMonitor monitor) throws CoreException {
		return launch(mode, monitor, false);
	}

	@Override
	public ILaunch launch(String mode, IProgressMonitor monitor, boolean build) throws CoreException {
		return launch(mode, monitor, build, true);
	}

	@Override
	public ILaunch launch(String mode, IProgressMonitor monitor, boolean build, boolean register) throws CoreException {
		/* Setup progress monitor
		 * - Prepare delegate (0)
		 * - Pre-launch check (1)
		 * - [Build before launch (7)]					if build
		 * - [Incremental build before launch (3)]		if build
		 * - Final launch validation (1)
		 * - Initialize source locator (1)
		 * - Launch delegate (10) */
		SubMonitor lmonitor = SubMonitor.convert(monitor, DebugCoreMessages.LaunchConfiguration_9, 23);
		try {
			// bug 28245 - force the delegate to load in case it is interested in launch notifications
			ILaunchConfigurationDelegate delegate = getPreferredLaunchDelegate(mode);

			ILaunchConfigurationDelegate2 delegate2 = null;
			if (delegate instanceof ILaunchConfigurationDelegate2) {
				delegate2 = (ILaunchConfigurationDelegate2) delegate;
			}
			// allow the delegate to provide a launch implementation
			ILaunch launch = null;
			try {
				if (delegate2 != null) {
					launch = delegate2.getLaunch(this, mode);
				}
				if (launch == null) {
					launch = new Launch(this, mode, null);
				} else // ensure the launch mode is valid
				if (!mode.equals(launch.getLaunchMode())) {
					IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR,
							MessageFormat.format(DebugCoreMessages.LaunchConfiguration_14, mode, launch.getLaunchMode()), null);
					throw new CoreException(status);
				}
				launch.setAttribute(DebugPlugin.ATTR_LAUNCH_TIMESTAMP, Long.toString(System.currentTimeMillis()));
				boolean captureOutput = getAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, true);
				if (!captureOutput) {
					launch.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, "false"); //$NON-NLS-1$
				} else {
					launch.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, null);
				}
				launch.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING, getLaunchManager().getEncoding(this));
				if (register) {
					getLaunchManager().addLaunch(launch);
				}
				// perform initial pre-launch sanity checks
				lmonitor.subTask(DebugCoreMessages.LaunchConfiguration_8);

				if (delegate2 != null) {
					if (!(delegate2.preLaunchCheck(this, mode, lmonitor.split(1)))) {
						getLaunchManager().removeLaunch(launch);
						return launch;
					}
				}
				lmonitor.setWorkRemaining(22);
				// perform pre-launch build
				if (build) {
					lmonitor.subTask(DebugCoreMessages.LaunchConfiguration_7 + DebugCoreMessages.LaunchConfiguration_6);
					boolean tempbuild = build;
					if (delegate2 != null) {
						tempbuild = delegate2.buildForLaunch(this, mode, lmonitor.split(7));
					}
					if (tempbuild) {
						lmonitor.subTask(DebugCoreMessages.LaunchConfiguration_7 + DebugCoreMessages.LaunchConfiguration_5);
						ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, lmonitor.split(3));
					}
				}
				lmonitor.setWorkRemaining(12);
				// final validation
				lmonitor.subTask(DebugCoreMessages.LaunchConfiguration_4);
				if (delegate2 != null) {
					if (!(delegate2.finalLaunchCheck(this, mode, lmonitor.split(1)))) {
						getLaunchManager().removeLaunch(launch);
						return launch;
					}
				}
				lmonitor.setWorkRemaining(11);

				//initialize the source locator
				lmonitor.subTask(DebugCoreMessages.LaunchConfiguration_3);
				initializeSourceLocator(launch);
				lmonitor.worked(1);

				/* Launch the delegate */
				lmonitor.subTask(DebugCoreMessages.LaunchConfiguration_2);
				delegate.launch(this, mode, launch, lmonitor.split(10));
			} catch (CoreException e) {
				// if there was an exception, and the launch is empty, remove it
				if (launch != null && !launch.hasChildren()) {
					getLaunchManager().removeLaunch(launch);
				}
				throw e;
			} catch (RuntimeException e) {
				// if there was a runtime exception, and the launch is empty, remove it
				if (launch != null && !launch.hasChildren()) {
					getLaunchManager().removeLaunch(launch);
				}
				throw e;
			}
			if (lmonitor.isCanceled()) {
				getLaunchManager().removeLaunch(launch);
			}
			return launch;
		}
		finally {
			lmonitor.done();
		}
	}

	@Override
	public void migrate() throws CoreException {
		((LaunchConfigurationType)getType()).migrate(this);
	}

	/**
	 * Creates and returns a new error status based on
	 * the given message, code, and exception.
	 *
	 * @param message error message
	 * @param code error code
	 * @param e exception or <code>null</code>
	 * @return status
	 */
	protected IStatus newStatus(String message, int code, Throwable e) {
		return new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), code, message, e);
	}

	/**
	 * Sets the new name for this configuration.
	 *
	 * @param name the new name for this configuration
	 * @since 3.5
	 */
	protected void setName(String name) {
		fName = name;
	}

	/**
	 * Sets this configurations container or <code>null</code> if stored in the
	 * local metadata.
	 *
	 * @param container or <code>null</code>
	 * @since 3.5
	 */
	protected void setContainer(IContainer container) {
		fContainer = container;
	}

	@Override
	public boolean supportsMode(String mode) throws CoreException {
		return getType().supportsMode(mode);
	}

	@Override
	public boolean isReadOnly() {
		try {
			IFileStore fileStore = getFileStore();
			if (fileStore != null) {
				return fileStore.fetchInfo().getAttribute(EFS.ATTRIBUTE_READ_ONLY);
			}
		} catch (CoreException e) {
		}
		return true;
	}

	@Override
	public ILaunchDelegate getPreferredDelegate(Set<String> modes) throws CoreException {
		Map<String, String> delegates = getAttribute(LaunchConfiguration.ATTR_PREFERRED_LAUNCHERS, (Map<String, String>) null);
		if(delegates != null) {
			String id = delegates.get(modes.toString());
			if(id != null) {
				return getLaunchManager().getLaunchDelegate(id);
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public ILaunchConfiguration getPrototype() throws CoreException {
		String memento = getAttribute(ATTR_PROTOTYPE, (String)null);
		if (memento != null) {
			LaunchConfiguration prototype = new LaunchConfiguration(memento);
			prototype.setIsPrototype(true);
			return prototype;
		}
		return null;
	}

	@Override
	public Collection<ILaunchConfiguration> getPrototypeChildren() throws CoreException {
		ILaunchConfiguration[] configurations = getLaunchManager().getLaunchConfigurations(getType());
		List<ILaunchConfiguration> proteges = new ArrayList<>();
		for (ILaunchConfiguration config : configurations) {
			if (this.equals(config.getPrototype())) {
				proteges.add(config);
			}
		}
		return proteges;
	}

	@Override
	public boolean isPrototype() {
		return fIsPrototype;
	}

	/**
	 * Set the prototype state of this configuration.
	 *
	 * @param isPrototype the prototype state.
	 *
	 * @since 3.12
	 */
	protected void setIsPrototype(boolean isPrototype) {
		fIsPrototype = isPrototype;
	}

	/**
	 * Check if the given file is a launch configuration prototype or not.
	 *
	 * @param file the given {@link IFile}.
	 * @return <code>true</code> if the given file is a launch configuration
	 *         prototype, false otherwise.
	 *
	 * @since 3.12
	 */
	protected static boolean isPrototype(IFile file) {
		if (ILaunchConfiguration.LAUNCH_CONFIGURATION_PROTOTYPE_FILE_EXTENSION.equals(file.getFileExtension())) {
			return true;
		}
		return false;
	}

	@Override
	public int getKind() throws CoreException {
		if (fIsPrototype) {
			return PROTOTYPE;
		}
		return CONFIGURATION;
	}

	@Override
	public boolean isAttributeModified(String attribute) throws CoreException {
		ILaunchConfiguration prototype = getPrototype();
		if (prototype != null) {
			Object prototypeValue = prototype.getAttributes().get(attribute);
			Object attributeValue = getAttributes().get(attribute);
			return !LaunchConfigurationInfo.compareAttribute(attribute, prototypeValue, attributeValue);
		}
		return false;
	}

	@Override
	public Set<String> getPrototypeVisibleAttributes() throws CoreException {
		return getInfo().getVisibleAttributes();
	}

	@Override
	public void setPrototypeAttributeVisibility(String attribute, boolean visible) throws CoreException {
		getInfo().setAttributeVisibility(attribute, visible);
	}

	/*
	 * Get Preferred delegate with all fallbacks
	 *
	 */
	public ILaunchConfigurationDelegate getPreferredLaunchDelegate(String mode) throws CoreException {
		Set<String> modes = getModes();
		modes.add(mode);
		ILaunchDelegate[] delegates = getType().getDelegates(modes);
		ILaunchConfigurationDelegate delegate = null;
		switch (delegates.length) {
			case 1:
				delegate = delegates[0].getDelegate();
				break;
			case 0: {
				IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(promptStatus);
				if (handler != null) {
					handler.handleStatus(delegateNotAvailable, new Object[] {
							this, mode });
				}
				IStatus status = new Status(IStatus.CANCEL, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, DebugCoreMessages.LaunchConfiguration_11, null);
				throw new CoreException(status);
			}
			default: {
				ILaunchDelegate del = getPreferredDelegate(modes);
				if (del == null) {
					del = getType().getPreferredDelegate(modes);
				}
				if (del == null) {
					IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(promptStatus);
					IStatus status = null;
					if (handler != null) {
						status = (IStatus) handler.handleStatus(duplicateDelegates, new Object[] {
								this, mode });
					}
					if (status != null && status.isOK()) {
						del = getPreferredDelegate(modes);
						if (del == null) {
							del = getType().getPreferredDelegate(modes);
						}
						if (del != null) {
							delegate = del.getDelegate();
						} else {
							status = new Status(IStatus.CANCEL, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, DebugCoreMessages.LaunchConfiguration_13, null);
							throw new CoreException(status);
						}
					} else {
						status = new Status(IStatus.CANCEL, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, DebugCoreMessages.LaunchConfiguration_13, null);
						throw new CoreException(status);
					}
				} else {
					delegate = del.getDelegate();
				}
				break;
			}
		}

		return delegate;
	}

	protected String getLineSeparator() throws CoreException {
		IFile file = getFile();
		if (file != null) {
			// file is in workspace
			return file.getLineSeparator(true);
		}

		// file is not in workspace, use platform default
		return System.lineSeparator();
	}
}
