/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.internal.core;


import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * A working copy launch configuration
 */
public class LaunchConfigurationWorkingCopy extends LaunchConfiguration implements ILaunchConfigurationWorkingCopy {

	/**
	 * Handle of original launch configuration this
	 * working copy is based on
	 */
	private LaunchConfiguration fOriginal;

	/**
	 * Handle to a parent working copy
	 * @since 3.3
	 */
	private LaunchConfigurationWorkingCopy fParent =  null;

	/**
	 * Working copy of attributes.
	 */
	private LaunchConfigurationInfo fInfo;

	/**
	 * Whether this working copy has been modified since
	 * it was created
	 */
	private boolean fDirty;

	/**
	 * Indicates whether this working copy has been explicitly renamed.
	 */
	private boolean fRenamed;

	/**
	 * Suppress change notification until created
	 */
	private boolean fSuppressChange ;

	/**
	 * Constructs a working copy of the specified launch
	 * configuration.
	 *
	 * @param original launch configuration to make
	 *  a working copy of
	 * @exception CoreException if unable to initialize this
	 *  working copy's attributes based on the original configuration
	 */
	protected LaunchConfigurationWorkingCopy(LaunchConfiguration original) throws CoreException {
		super(original.getName(), original.getContainer(), original.isPrototype());
		copyFrom(original);
		setOriginal(original);
		fSuppressChange = false;
	}

	@Override
	protected void initialize() {
		fDirty = false;
		fRenamed = false;
		fSuppressChange = true;
		super.initialize();
	}

	/**
	 * Constructs a working copy of the specified launch configuration as its parent.
	 *
	 * @param parent launch configuration to make
	 *  a working copy of
	 * @exception CoreException if unable to initialize this
	 *  working copy's attributes based on the original configuration
	 */
	protected LaunchConfigurationWorkingCopy(LaunchConfigurationWorkingCopy parent) throws CoreException {
		super(parent.getName(), parent.getContainer(), parent.isPrototype());
		copyFrom(parent);
		setOriginal((LaunchConfiguration) parent.getOriginal());
		fParent = parent;
		fSuppressChange = false;
	}

	/**
	 * Constructs a copy of the specified launch
	 * configuration, with the given (new) name.
	 *
	 * @param original launch configuration to make
	 *  a working copy of
	 * @param name the new name for the copy of the launch
	 *  configuration
	 * @exception CoreException if unable to initialize this
	 *  working copy's attributes based on the original configuration
	 */
	protected LaunchConfigurationWorkingCopy(LaunchConfiguration original, String name) throws CoreException {
		super(name, original.getContainer(), original.isPrototype());
		copyFrom(original);
		fSuppressChange = false;
	}

	/**
	 * Constructs a new working copy to be created in the specified
	 * location.
	 *
	 * @param container the container that the configuration will be created in
	 *  or <code>null</code> if to be local
	 * @param name the name of the new launch configuration
	 * @param type the type of this working copy
	 */
	protected LaunchConfigurationWorkingCopy(IContainer container, String name, ILaunchConfigurationType type) {
		this(container, name, type, false);
	}

	/**
	 * Constructs a new working copy to be created in the specified
	 * location.
	 *
	 * @param container the container that the configuration will be created in
	 *  or <code>null</code> if to be local
	 * @param name the name of the new launch configuration
	 * @param type the type of this working copy
	 * @param prototype if this copy is a prototype or not
	 *
	 * @since 3.12
	 */
	protected LaunchConfigurationWorkingCopy(IContainer container, String name, ILaunchConfigurationType type, boolean prototype) {
		super(name, container, prototype);
		setInfo(new LaunchConfigurationInfo());
		getInfo().setType(type);
		getInfo().setIsPrototype(prototype);
		fSuppressChange = false;
	}

	@Override
	public boolean isDirty() {
		return fDirty;
	}

	@Override
	public synchronized ILaunchConfiguration doSave() throws CoreException {
		return doSave(new NullProgressMonitor());
	}

	/**
	 * Saves with progress.
	 *
	 * @param monitor the {@link IProgressMonitor}
	 * @return the saved <code>ILaunchConfiguration</code>
	 * @throws CoreException if a problem is encountered
	 *
	 * @since 3.3
	 */
	public synchronized ILaunchConfiguration doSave(IProgressMonitor monitor) throws CoreException {
		SubMonitor lmonitor = SubMonitor.convert(monitor, 1);
		try {
		if (getParent() != null) {
			// save to parent working copy
			LaunchConfigurationWorkingCopy wc = (LaunchConfigurationWorkingCopy) getParent();
			if(isMoved()) {
				wc.rename(getName());
				wc.setContainer(getContainer());
			}
			wc.setAttributes(getInfo().getAttributes());
			updateMonitor(lmonitor, 1);
			return wc;
		}
		else {
			boolean useRunnable= true;
			if (isLocal()) {
				if (isMoved()) {
					// If this config was moved from a shared location, saving
					// it will delete the original from the workspace. Use runnable.
					useRunnable= !isNew() && !getOriginal().isLocal();
				} else {
					useRunnable= false;
				}
			}
			if (useRunnable) {
					IWorkspaceRunnable wr = this::doSave0;
				ResourcesPlugin.getWorkspace().run(wr, null, 0, lmonitor.newChild(1));
			} else {
				//file is persisted in the metadata not the workspace
				doSave0(lmonitor.newChild(1));
			}
			getLaunchManager().setMovedFromTo(null, null);
		}
		}
		finally {
			if(lmonitor != null) {
				lmonitor.done();
			}
		}
		return new LaunchConfiguration(getName(), getContainer(), isPrototype());
	}

	/**
	 * Performs the actual saving of the launch configuration.
	 * @param monitor the {@link IProgressMonitor}
	 * @throws CoreException if a problem is encountered
	 */
	private void doSave0(IProgressMonitor monitor) throws CoreException {
		SubMonitor lmonitor = SubMonitor.convert(monitor, MessageFormat.format(DebugCoreMessages.LaunchConfigurationWorkingCopy_0, new Object[] { getName() }), 2);
		try {
			// set up from/to information if this is a move
			boolean moved = (!isNew() && isMoved());
			if (moved) {
				ILaunchConfiguration to = new LaunchConfiguration(getName(), getContainer(), isPrototype());
				ILaunchConfiguration from = getOriginal();
				getLaunchManager().setMovedFromTo(from, to);
			}
			ILaunchConfiguration orig = getOriginal();
			updateMonitor(lmonitor, 1);
			writeNewFile(lmonitor.newChild(1));
			// delete the old file if this is not a new configuration
			// or the file was renamed/moved
			if (moved) {
				orig.delete();
			}
			fDirty = false;
		}
		finally {
			if(lmonitor != null) {
				lmonitor.done();
			}
		}
	}

	private static String getLineDelimiterPreference(IFile file) {
		IScopeContext[] scopeContext;
		if (file != null && file.getProject() != null) {
			// project preference
			scopeContext = new IScopeContext[] {
					new ProjectScope(file.getProject()) };
			String lineDelimiter = Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
			if (lineDelimiter != null) {
				return lineDelimiter;
			}
		}
		// workspace preference
		scopeContext = new IScopeContext[] { InstanceScope.INSTANCE };
		return Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, System.lineSeparator(), scopeContext);
	}

	/**
	 * Writes the new configuration information to a file.
	 * @param monitor the {@link IProgressMonitor}
	 *
	 * @exception CoreException if writing the file fails
	 */
	protected void writeNewFile(IProgressMonitor monitor) throws CoreException {
		String lineDelimeter = getLineDelimiterPreference(getFile());
		String xml = null;
		try {
			xml = getInfo().getAsXML(lineDelimeter);
		} catch (Exception e) {
			throw new DebugException(
					new Status(
						IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
 DebugException.REQUEST_FAILED, MessageFormat.format(DebugCoreMessages.LaunchConfigurationWorkingCopy__0__occurred_generating_launch_configuration_XML__1, new Object[] { e.toString() }), null
						)
					);
		}
		SubMonitor lmonitor = SubMonitor.convert(monitor, IInternalDebugCoreConstants.EMPTY_STRING, 5);
		try {
			boolean added = false;
			if (isLocal()) {
				// use java.io to update configuration file
				try {
					lmonitor.subTask(DebugCoreMessages.LaunchConfigurationWorkingCopy_1);
					IFileStore file = getFileStore();
					if (file == null) {
						throw new DebugException(
								new Status(
								 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
								 DebugException.REQUEST_FAILED, DebugCoreMessages.LaunchConfigurationWorkingCopy_4, null
								)
							);
					}
					IFileStore dir = file.getParent();
					dir.mkdir(EFS.SHALLOW, null);
					if (!file.fetchInfo().exists()) {
						added = true;
						updateMonitor(lmonitor, 1);
					}
					try (BufferedOutputStream stream = new BufferedOutputStream(file.openOutputStream(EFS.NONE, null))) {
						stream.write(xml.getBytes(StandardCharsets.UTF_8));
					}
					//notify file saved
					updateMonitor(lmonitor, 1);
				} catch (IOException ie) {
					lmonitor.done();
					throw new DebugException(
						new Status(
						 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
 DebugException.REQUEST_FAILED, MessageFormat.format(DebugCoreMessages.LaunchConfigurationWorkingCopy__0__occurred_generating_launch_configuration_XML__1, new Object[] { ie.toString() }), null
						)
					);
				}
			} else {
				// use resource API to update configuration file
				IFile file = getFile();
				if (file == null) {
					lmonitor.done();
					throw new DebugException(
							new Status(
								 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
								 DebugException.REQUEST_FAILED, DebugCoreMessages.LaunchConfigurationWorkingCopy_5, null
							));
				}
				IContainer dir = file.getParent();
				if (!dir.exists()) {
					lmonitor.done();
					throw new DebugException(
						new Status(
						 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
						 DebugException.REQUEST_FAILED, DebugCoreMessages.LaunchConfigurationWorkingCopy_Specified_container_for_launch_configuration_does_not_exist_2, null
						)
					);
				}
				ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
				SubMonitor smonitor = null;
				if (!file.exists()) {
					added = true;
					//create file input stream: work one unit in a sub monitor
					smonitor = lmonitor.newChild(1);
					smonitor.setTaskName(MessageFormat.format(DebugCoreMessages.LaunchConfigurationWorkingCopy_2, new Object[] { getName() }));
					file.create(stream, false, smonitor);
				} else {
					// validate edit
					if (file.isReadOnly()) {
						IStatus status = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] {file}, null);
						if (!status.isOK()) {
							lmonitor.done();
							throw new CoreException(status);
						}
					}
					//set the contents of the file: work 1 unit in a sub monitor
					smonitor = lmonitor.newChild(1);
					smonitor.setTaskName(MessageFormat.format(DebugCoreMessages.LaunchConfigurationWorkingCopy_3, new Object[] { getName() }));
					file.setContents(stream, true, false, smonitor);
				}
			}
			// notify of add/change for both local and shared configurations - see bug 288368
			if (added) {
				getLaunchManager().launchConfigurationAdded(new LaunchConfiguration(getName(), getContainer(), isPrototype()));
			} else {
				getLaunchManager().launchConfigurationChanged(new LaunchConfiguration(getName(), getContainer(), isPrototype()));
			}
		}
		finally {
			if(lmonitor != null) {
				lmonitor.done();
			}
		}
	}

	/**
	 * Updates the given monitor with the given tick count and polls for cancellation. If the monitor
	 * is cancelled an {@link OperationCanceledException} is thrown
	 * @param monitor the {@link IProgressMonitor}
	 * @param ticks the amount of work to advance the monitor
	 * @throws OperationCanceledException if the user cancels the operation
	 */
	private void updateMonitor(IProgressMonitor monitor, int ticks) throws OperationCanceledException {
		if(monitor != null) {
			monitor.worked(ticks);
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
		}
	}

	@Override
	public void setAttribute(String attributeName, int value) {
		getInfo().setAttribute(attributeName, Integer.valueOf(value));
		setDirty();
	}

	@Override
	public void setAttribute(String attributeName, String value) {
		getInfo().setAttribute(attributeName, value);
		setDirty();
	}

	@Override
	public void setAttribute(String attributeName, boolean value) {
		getInfo().setAttribute(attributeName, Boolean.valueOf(value));
		setDirty();
	}

	@Override
	public void setAttribute(String attributeName, List<String> value) {
		getInfo().setAttribute(attributeName, value);
		setDirty();
	}

	@Override
	public void setAttribute(String attributeName, Map<String, String> value) {
		getInfo().setAttribute(attributeName, value);
		setDirty();
	}

	@Override
	public void setAttribute(String attributeName, Set<String> value) {
		getInfo().setAttribute(attributeName, value);
		setDirty();
	}

	@Override
	public void setAttribute(String attributeName, Object value) {
		getInfo().setAttribute(attributeName, value);
		setDirty();
	}

	@Override
	public ILaunchConfiguration getOriginal() {
		ILaunchConfiguration config = fOriginal;
		ILaunchConfigurationWorkingCopy parent = fParent;
		while(parent != null) {
			config = parent.getOriginal();
			parent = parent.getParent();
		}
		return config;
	}

	@Override
	public ILaunchConfigurationWorkingCopy getParent() {
		return fParent;
	}

	/**
	 * Sets the launch configuration this working copy
	 * is based on. Initializes the attributes of this
	 * working copy to the current values of the given
	 * configuration.
	 *
	 * @param original the launch configuration this working
	 *  copy is based on.
	 * @exception CoreException if unable to initialize this
	 *  working copy based on the original's current attribute
	 *  set
	 */
	private void copyFrom(LaunchConfiguration original) throws CoreException {
		LaunchConfigurationInfo info = original.getInfo();
		setInfo(info.getCopy());
		fDirty = false;
	}

	/**
	 * Sets the launch configuration this working copy
	 * is based on.
	 *
	 * @param original the launch configuration this working
	 *  copy is based on.
	 */
	private void setOriginal(LaunchConfiguration original) {
		fOriginal = original;
	}

	/**
	 * Sets the working copy info object for this working copy.
	 *
	 * @param info a copy of attributes from this working copy's
	 * 	original launch configuration
	 */
	protected void setInfo(LaunchConfigurationInfo info) {
		fInfo = info;
	}

	@Override
	public boolean isWorkingCopy() {
		return true;
	}

	/**
	 * A working copy keeps a local info object that is not
	 * cached with the launch manager.
	 *
	 * @see LaunchConfiguration#getInfo()
	 */
	@Override
	protected LaunchConfigurationInfo getInfo() {
		return fInfo;
	}

	/**
	 * Sets this working copy's state to dirty.
	 * Notifies listeners that this working copy has
	 * changed.
	 */
	private void setDirty() {
		fDirty = true;
		if (!suppressChangeNotification()) {
			getLaunchManager().getConfigurationNotifier().notify(this, LaunchManager.CHANGED);
		}
	}

	@Override
	public void setModes(Set<String> modes) {
		getInfo().setAttribute(ATTR_LAUNCH_MODES, (modes.size() > 0 ? modes : null));
		setDirty();
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationWorkingCopy#addModes(java.util.Set)
	 */
	@Override
	public void addModes(Set<String> modes) {
		try {
			Set<String> opts = getModes();
			if(opts.addAll(modes)) {
				getInfo().setAttribute(ATTR_LAUNCH_MODES, opts);
				setDirty();
			}
		}
		catch (CoreException e) {
			DebugPlugin.log(e);
		}
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationWorkingCopy#removeModes(java.util.Set)
	 */
	@Override
	public void removeModes(Set<String> options) {
		try {
			Set<String> opts = getModes();
			if(opts.removeAll(options)) {
				getInfo().setAttribute(ATTR_LAUNCH_MODES, (opts.size() < 1 ? null : opts));
				setDirty();
			}
		}
		catch (CoreException e) {
			DebugPlugin.log(e);
		}
	}

	/**
	 * @see ILaunchConfigurationWorkingCopy#rename(String)
	 */
	@Override
	public void rename(String name) {
		if (!getName().equals(name)) {
			setName(name);
			fRenamed = isNew() || !(getOriginal().getName().equals(name));
		}
	}

	/**
	 * Sets the new name for this configuration.
	 *
	 * @param name the new name for this configuration
	 */
	@Override
	protected void setName(String name) {
		super.setName(name);
		setDirty();
	}

	/**
	 * Returns whether this working copy is new, or is a
	 * working copy of another launch configuration.
	 *
	 * @return whether this working copy is new, or is a
	 *  working copy of another launch configuration
	 */
	protected boolean isNew() {
		return getOriginal() == null;
	}

	/**
	 * Returns whether this working copy is new or if its
	 * location has changed from that of its original.
	 *
	 * @return whether this working copy is new or if its
	 * location has changed from that of its original
	 */
	protected boolean isMoved() {
		if (isNew() || fRenamed) {
			return true;
		}
		IContainer newContainer = getContainer();
		IContainer originalContainer = ((LaunchConfiguration)getOriginal()).getContainer();

		return !isSameContainerLocation(newContainer, originalContainer);
	}

	/**
	 * A working copy cannot generate a memento.
	 *
	 * @see ILaunchConfiguration#getMemento()
	 */
	@Override
	public String getMemento() {
		return null;
	}

	/**
	 * Returns whether change notification should be
	 * suppressed
	 * @return if changes notification should be suppressed
	 */
	protected boolean suppressChangeNotification() {
		return fSuppressChange;
	}

	private boolean isSameContainerLocation(IContainer newContainer, IContainer originalContainer) {
		// Verify that containers are not nested
		if (newContainer != null && originalContainer != null) {
			IPath newPath = newContainer.getLocation();
			IPath originalPath = originalContainer.getLocation();

			if (Objects.equals(newPath, originalPath)) {
				return true;
			}
		}
		return Objects.equals(newContainer, originalContainer);
	}

	@Override
	public void setContainer(IContainer container) {
		if (isSameContainerLocation(container, getContainer())) {
			return;
		}
		super.setContainer(container);
		setDirty();
	}

	@Override
	public void setAttributes(Map<String, ? extends Object> attributes) {
		getInfo().setAttributes(attributes);
		setDirty();
	}

	@Override
	public void setMappedResources(IResource[] resources) {
		ArrayList<String> paths = null;
		ArrayList<String> types = null;
		if(resources != null && resources.length > 0) {
			paths = new ArrayList<>(resources.length);
			types = new ArrayList<>(resources.length);
			for (IResource resource : resources) {
				if(resource != null) {
					paths.add(resource.getFullPath().toPortableString());
					types.add(Integer.valueOf(resource.getType()).toString());
				}
			}
		}
		setAttribute(LaunchConfiguration.ATTR_MAPPED_RESOURCE_PATHS, paths);
		setAttribute(LaunchConfiguration.ATTR_MAPPED_RESOURCE_TYPES, types);
	}

	@Override
	public void setPreferredLaunchDelegate(Set<String> modes, String delegateId) {
		if(modes != null) {
			try {
				Map<String, String> delegates = getAttribute(LaunchConfiguration.ATTR_PREFERRED_LAUNCHERS, (Map<String, String>) null);
					//copy map to avoid pointer issues
				Map<String, String> map = new HashMap<>();
				if (delegates != null) {
					map.putAll(delegates);
				}
				if (delegateId == null) {
					map.remove(modes.toString());
				} else {
					map.put(modes.toString(), delegateId);
				}
				setAttribute(LaunchConfiguration.ATTR_PREFERRED_LAUNCHERS, map);
			}
			catch (CoreException ce) {DebugPlugin.log(ce);}
		}
	}

	@Override
	public ILaunchConfigurationWorkingCopy getWorkingCopy() throws CoreException {
		return new LaunchConfigurationWorkingCopy(this);
	}

	@Override
	public Object removeAttribute(String attributeName) {
		return getInfo().removeAttribute(attributeName);
	}

	@Override
	public void copyAttributes(ILaunchConfiguration prototype) throws CoreException {
		Map<String, Object> map = prototype.getAttributes();
		LaunchConfigurationInfo info = getInfo();
		info.setPrototype(prototype);
		Set<String> prototypeVisibleAttributes = prototype.getPrototypeVisibleAttributes();
		if (prototypeVisibleAttributes != null) {
			prototypeVisibleAttributes.forEach(key -> {
				Object value = map.get(key);
				if (value != null) {
					info.setAttribute(key, value);
				}
			});
		}
	}

	@Override
	public void setPrototype(ILaunchConfiguration prototype, boolean copy) throws CoreException {
		if (prototype != null && !prototype.isPrototype()) {
			throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugCoreMessages.LaunchConfigurationWorkingCopy_6));
		}
		if (prototype != null && prototype.isWorkingCopy()) {
			throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugCoreMessages.LaunchConfigurationWorkingCopy_7));
		}
		if (prototype == null) {
			getInfo().setPrototype(null);
			removeAttribute(ATTR_PROTOTYPE);
		} else {
			if (isPrototype()) {
				throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugCoreMessages.LaunchConfigurationWorkingCopy_8));
			}
			getInfo().setPrototype(prototype);
			if (copy) {
				copyAttributes(prototype);
			}
			setAttribute(ATTR_PROTOTYPE, prototype.getMemento());
			setAttribute(IS_PROTOTYPE, false);
		}
	}

	@Override
	public ILaunchConfiguration doSave(int flag) throws CoreException {
		Collection<ILaunchConfiguration> children = null;
		if (UPDATE_PROTOTYPE_CHILDREN == flag) {
			if (!isNew() && isMoved() && getParent() == null) {
				children = getOriginal().getPrototypeChildren();
			}
		}
		ILaunchConfiguration saved = doSave();
		if (children != null) {
			for (ILaunchConfiguration child : children) {
				ILaunchConfigurationWorkingCopy wc = child.getWorkingCopy();
				wc.setPrototype(saved, false);
				wc.doSave();
			}
		}
		return saved;
	}

	@Override
	public void setPrototypeAttributeVisibility(String attribute, boolean visible) throws CoreException {
		super.setPrototypeAttributeVisibility(attribute, visible);
		setDirty();
	}
}
