/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.net.URI;
import java.util.*;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.events.PathVariableChangeEvent;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

/**
 * Core's implementation of IPathVariableManager.
 */
public class PathVariableManager implements IPathVariableManager, IManager {

	static final String VARIABLE_PREFIX = "pathvariable."; //$NON-NLS-1$
	private Set<IPathVariableChangeListener> listeners;
	private Map<IProject, Collection<IPathVariableChangeListener>> projectListeners;

	private Preferences preferences;

	/**
	 * Constructor for the class.
	 */
	public PathVariableManager() {
		this.listeners = Collections.synchronizedSet(new HashSet<>());
		this.projectListeners = Collections.synchronizedMap(new HashMap<>());
		this.preferences = ResourcesPlugin.getPlugin().getPluginPreferences();
	}

	/**
	 * @see org.eclipse.core.resources.IPathVariableManager#addChangeListener(IPathVariableChangeListener)
	 */
	@Override
	public void addChangeListener(IPathVariableChangeListener listener) {
		listeners.add(listener);
	}

	synchronized public void addChangeListener(IPathVariableChangeListener listener, IProject project) {
		Collection<IPathVariableChangeListener> list = projectListeners.get(project);
		if (list == null) {
			list = Collections.synchronizedSet(new HashSet<>());
			projectListeners.put(project, list);
		}
		list.add(listener);
	}

	/**
	 * Throws a runtime exception if the given name is not valid as a path
	 * variable name.
	 */
	private void checkIsValidName(String name) throws CoreException {
		IStatus status = validateName(name);
		if (!status.isOK())
			throw new CoreException(status);
	}

	/**
	 * Throws an exception if the given path is not valid as a path variable
	 * value.
	 */
	private void checkIsValidValue(IPath newValue) throws CoreException {
		IStatus status = validateValue(newValue);
		if (!status.isOK())
			throw new CoreException(status);
	}

	/**
	 * Fires a property change event corresponding to a change to the
	 * current value of the variable with the given name.
	 *
	 * @param name the name of the variable, to be used as the variable
	 *      in the event object
	 * @param value the current value of the path variable or <code>null</code> if
	 *      the variable was deleted
	 * @param type one of <code>IPathVariableChangeEvent.VARIABLE_CREATED</code>,
	 *      <code>IPathVariableChangeEvent.VARIABLE_CHANGED</code>, or
	 *      <code>IPathVariableChangeEvent.VARIABLE_DELETED</code>
	 * @see IPathVariableChangeEvent
	 * @see IPathVariableChangeEvent#VARIABLE_CREATED
	 * @see IPathVariableChangeEvent#VARIABLE_CHANGED
	 * @see IPathVariableChangeEvent#VARIABLE_DELETED
	 */
	private void fireVariableChangeEvent(String name, IPath value, int type) {
		fireVariableChangeEvent(this.listeners, name, value, type);
	}

	private void fireVariableChangeEvent(Collection<IPathVariableChangeListener> list, String name, IPath value, int type) {
		if (list.isEmpty())
			return;
		// use a separate collection to avoid interference of simultaneous additions/removals
		IPathVariableChangeListener[] listenerArray = list.toArray(new IPathVariableChangeListener[list.size()]);
		final PathVariableChangeEvent pve = new PathVariableChangeEvent(this, name, value, type);
		for (final IPathVariableChangeListener listener : listenerArray) {
			ISafeRunnable job = new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					// already being logged in SafeRunner#run()
				}

				@Override
				public void run() throws Exception {
					listener.pathVariableChanged(pve);
				}
			};
			SafeRunner.run(job);
		}
	}

	public void fireVariableChangeEvent(IProject project, String name, IPath value, int type) {
		Collection<IPathVariableChangeListener> list = projectListeners.get(project);
		if (list != null)
			fireVariableChangeEvent(list, name, value, type);
	}

	/**
	 * Return a key to use in the Preferences.
	 */
	private String getKeyForName(String varName) {
		return VARIABLE_PREFIX + varName;
	}

	/**
	 * @see org.eclipse.core.resources.IPathVariableManager#getPathVariableNames()
	 */
	@Override
	public String[] getPathVariableNames() {
		List<String> result = new LinkedList<>();
		String[] names = preferences.propertyNames();
		for (String name : names) {
			if (name.startsWith(VARIABLE_PREFIX)) {
				String key = name.substring(VARIABLE_PREFIX.length());
				// filter out names for preferences which might be valid in the
				// preference store but does not have valid path variable names
				// and/or values. We can get in this state if the user has
				// edited the file on disk or set a preference using the prefix
				// reserved to path variables (#VARIABLE_PREFIX).
				// TODO: we may want to look at removing these keys from the
				// preference store as a garbage collection means
				if (validateName(key).isOK() && validateValue(getValue(key)).isOK())
					result.add(key);
			}
		}
		return result.toArray(new String[result.size()]);
	}

	/**
	 * Note that if a user changes the key in the preferences file to be invalid
	 * and then calls #getValue using that key, they will get the value back for
	 * that. But then if they try and call #setValue using the same key it will throw
	 * an exception. We may want to revisit this behaviour in the future.
	 *
	 * @see org.eclipse.core.resources.IPathVariableManager#getValue(String)
	 */
	@Deprecated
	@Override
	public IPath getValue(String varName) {
		String key = getKeyForName(varName);
		String value = preferences.getString(key);
		return value.length() == 0 ? null : Path.fromPortableString(value);
	}

	/**
	 * @see org.eclipse.core.resources.IPathVariableManager#isDefined(String)
	 */
	@Override
	public boolean isDefined(String varName) {
		return getValue(varName) != null;
	}

	/**
	 * @see org.eclipse.core.resources.IPathVariableManager#removeChangeListener(IPathVariableChangeListener)
	 */
	@Override
	public void removeChangeListener(IPathVariableChangeListener listener) {
		listeners.remove(listener);
	}

	synchronized public void removeChangeListener(IPathVariableChangeListener listener, IProject project) {
		Collection<IPathVariableChangeListener> list = projectListeners.get(project);
		if (list != null) {
			list.remove(listener);
			if (list.isEmpty())
				projectListeners.remove(project);
		}
	}

	/**
	 * @see org.eclipse.core.resources.IPathVariableManager#resolvePath(IPath)
	 */
	@Deprecated
	@Override
	public IPath resolvePath(IPath path) {
		if (path == null || path.segmentCount() == 0 || path.isAbsolute() || path.getDevice() != null)
			return path;
		IPath value = getValue(path.segment(0));
		return value == null ? path : value.append(path.removeFirstSegments(1));
	}

	@Override
	public URI resolveURI(URI uri) {
		if (uri == null || uri.isAbsolute())
			return uri;
		String schemeSpecificPart = uri.getSchemeSpecificPart();
		if (schemeSpecificPart == null || schemeSpecificPart.isEmpty()) {
			return uri;
		}
		IPath raw = new Path(schemeSpecificPart);
		IPath resolved = resolvePath(raw);
		return raw == resolved ? uri : URIUtil.toURI(resolved);
	}

	/**
	 * @see org.eclipse.core.resources.IPathVariableManager#setValue(String, IPath)
	 */
	@Override
	public void setValue(String varName, IPath newValue) throws CoreException {
		checkIsValidName(varName);
		//convert path value to canonical form
		if (newValue != null && newValue.isAbsolute())
			newValue = FileUtil.canonicalPath(newValue);
		checkIsValidValue(newValue);
		int eventType;
		// read previous value and set new value atomically in order to generate the right event
		synchronized (this) {
			IPath currentValue = getValue(varName);
			boolean variableExists = currentValue != null;
			if (!variableExists && newValue == null)
				return;
			if (variableExists && currentValue.equals(newValue))
				return;
			if (newValue == null) {
				preferences.setToDefault(getKeyForName(varName));
				eventType = IPathVariableChangeEvent.VARIABLE_DELETED;
			} else {
				preferences.setValue(getKeyForName(varName), newValue.toPortableString());
				eventType = variableExists ? IPathVariableChangeEvent.VARIABLE_CHANGED : IPathVariableChangeEvent.VARIABLE_CREATED;
			}
		}
		// notify listeners from outside the synchronized block to avoid deadlocks
		fireVariableChangeEvent(varName, newValue, eventType);
	}

	/**
	 * @see org.eclipse.core.internal.resources.IManager#shutdown(IProgressMonitor)
	 */
	@Override
	public void shutdown(IProgressMonitor monitor) {
		// The preferences for this plug-in are saved in the Plugin.shutdown
		// method so we don't have to do it here.
	}

	/**
	 * @see org.eclipse.core.internal.resources.IManager#startup(IProgressMonitor)
	 */
	@Override
	public void startup(IProgressMonitor monitor) {
		// since we are accessing the preference store directly, we don't
		// need to do any setup here.
	}

	/**
	 * @see org.eclipse.core.resources.IPathVariableManager#validateName(String)
	 */
	@Override
	public IStatus validateName(String name) {
		String message = null;
		if (name.length() == 0) {
			message = Messages.pathvar_length;
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
		}

		char first = name.charAt(0);
		if (!Character.isLetter(first) && first != '_') {
			message = NLS.bind(Messages.pathvar_beginLetter, String.valueOf(first));
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
		}

		for (int i = 1; i < name.length(); i++) {
			char following = name.charAt(i);
			if (Character.isWhitespace(following))
				return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, Messages.pathvar_whitespace);
			if (!Character.isLetter(following) && !Character.isDigit(following) && following != '_') {
				message = NLS.bind(Messages.pathvar_invalidChar, String.valueOf(following));
				return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
			}
		}
		return Status.OK_STATUS;
	}

	/**
	 * @see IPathVariableManager#validateValue(IPath)
	 */
	@Override
	public IStatus validateValue(IPath value) {
		if (value != null && (!value.isValidPath(value.toString()) || !value.isAbsolute())) {
			String message = Messages.pathvar_invalidValue;
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
		}
		return Status.OK_STATUS;
	}

	/**
	 * @see IPathVariableManager#convertToRelative(URI, boolean, String)
	 */
	@Override
	public URI convertToRelative(URI path, boolean force, String variableHint) throws CoreException {
		return PathVariableUtil.convertToRelative(this, path, null, false, variableHint);
	}

	/**
	 * see IPathVariableManager#getURIValue(String)
	 */
	@Override
	public URI getURIValue(String name) {
		IPath path = getValue(name);
		if (path != null)
			return URIUtil.toURI(path);
		return null;
	}

	/**
	 * see IPathVariableManager#setURIValue(String, URI)
	 */
	@Override
	public void setURIValue(String name, URI value) throws CoreException {
		setValue(name, (value != null ? URIUtil.toPath(value) : null));
	}

	/**
	 * @see IPathVariableManager#validateValue(URI)
	 */
	@Override
	public IStatus validateValue(URI path) {
		return validateValue(path != null ? URIUtil.toPath(path) : (IPath) null);
	}

	public URI resolveURI(URI uri, IResource resource) {
		return resolveURI(uri);
	}

	public String[] getPathVariableNames(IResource resource) {
		return getPathVariableNames();
	}

	@Override
	public URI getVariableRelativePathLocation(URI location) {
		try {
			URI result = convertToRelative(location, false, null);
			if (!result.equals(location))
				return result;
		} catch (CoreException e) {
			// handled by returning null
		}
		return null;
	}

	/**
	 * @see IPathVariableManager#convertToUserEditableFormat(String, boolean)
	 */
	@Override
	public String convertToUserEditableFormat(String value, boolean locationFormat) {
		return PathVariableUtil.convertToUserEditableFormatInternal(value, locationFormat);
	}

	@Override
	public String convertFromUserEditableFormat(String userFormat, boolean locationFormat) {
		return PathVariableUtil.convertFromUserEditableFormatInternal(this, userFormat, locationFormat);
	}

	@Override
	public boolean isUserDefined(String name) {
		return ProjectVariableProviderManager.getDefault().findDescriptor(name) == null;
	}
}
