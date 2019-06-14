/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.core.DefaultProjectSetCapability;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * This class represents things you can ask/do with a type of provider. This is
 * in the absence of a project, as opposed to RepositoryProvider which requires
 * a concrete project in order to be instantiated.
 * <p>
 * A repository provider type class is associated with it's provider ID along
 * with it's corresponding repository provider class. To add a repository
 * provider type and have it registered with the platform, a client must
 * minimally:
 * </p>
 * <ol>
 * <li>extend <code>RepositoryProviderType</code>
 * <li>add the typeClass field to the repository extension in
 * <code>plugin.xml</code>. Here is an example extension point definition:
 *
 * <code>
 *	<br>&lt;extension point="org.eclipse.team.core.repository"&gt;
 *  <br>&nbsp;&lt;repository
 *  <br>&nbsp;&nbsp;class="org.eclipse.myprovider.MyRepositoryProvider"
 *  <br>&nbsp;&nbsp;typeClass="org.eclipse.myprovider.MyRepositoryProviderType"
 *  <br>&nbsp;&nbsp;id="org.eclipse.myprovider.myProviderID"&gt;
 *  <br>&nbsp;&lt;/repository&gt;
 *	<br>&lt;/extension&gt;
 *  </code>
 * </ol>
 *
 * <p>
 * Once a repository provider type is registered with Team, then you can access
 * the singleton instance of the class by invoking
 * <code>RepositoryProviderType.getProviderType()</code>.
 * </p>
 *
 * @see RepositoryProviderType#getProviderType(String)
 *
 * @since 2.1
 */

public abstract class RepositoryProviderType extends PlatformObject {
	private static Map<String, RepositoryProviderType> allProviderTypes = new HashMap<>();

	private String id;

	private String scheme;

	public RepositoryProviderType() {
	}

	/**
	 * Return the RepositoryProviderType for the given provider ID.
	 *
	 * @param id the ID of the provider
	 * @return RepositoryProviderType
	 *
	 * @see #getID()
	 */
	public static RepositoryProviderType getProviderType(String id) {
		RepositoryProviderType type = allProviderTypes.get(id);

		if(type != null)
			return type;

		//If there isn't one in the table, we'll try to create one from the extension point
		//Its possible that newProviderType() will return null, but in that case it will have also logged the error	so just return the result
		return newProviderType(id);
	}

	/**
	 * Return the repository type for the given file system scheme or
	 * <code>null</code> if there isn't one. The scheme corresponds to
	 * the scheme used for the <code>org.eclipse.core.filesystem.filesystems</code>
	 * extension point.
	 * @param scheme the file system scheme
	 * @return the repository type for the given file system scheme or
	 * <code>null</code>
	 * @since 3.2
	 */
	public static RepositoryProviderType getTypeForScheme(String scheme) {
		for (RepositoryProviderType type : allProviderTypes.values()) {
			if (type.getFileSystemScheme() != null && type.getFileSystemScheme().equals(scheme))
				return type;
		}
		return findProviderForScheme(scheme);
	}

	private static RepositoryProviderType findProviderForScheme(String scheme) {
		IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(TeamPlugin.ID, TeamPlugin.REPOSITORY_EXTENSION);
		if (extension != null) {
			IExtension[] extensions =  extension.getExtensions();
			for (IExtension ext : extensions) {
				IConfigurationElement[] configElements = ext.getConfigurationElements();
				for (IConfigurationElement configElement : configElements) {
					String extensionId = configElement.getAttribute("id"); //$NON-NLS-1$
					String typeScheme = configElement.getAttribute("fileSystemScheme"); //$NON-NLS-1$
					if (typeScheme != null && typeScheme.equals(scheme) && extensionId != null) {
						return newProviderType(extensionId);
					}
				}
			}
		}
		return null;
	}

	private void setID(String id) {
		this.id = id;
	}

	private static RepositoryProviderType newProviderType(String id) {
		IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(TeamPlugin.ID, TeamPlugin.REPOSITORY_EXTENSION);
		if (extension != null) {
			IExtension[] extensions =  extension.getExtensions();
			for (IExtension ext : extensions) {
				IConfigurationElement[] configElements = ext.getConfigurationElements();
				for (IConfigurationElement configElement : configElements) {
					String extensionId = configElement.getAttribute("id"); //$NON-NLS-1$
					if (extensionId != null && extensionId.equals(id)) {
						try {
							RepositoryProviderType providerType;
							//Its ok not to have a typeClass extension.  In this case, a default instance will be created.
							if (configElement.getAttribute("typeClass") == null) { //$NON-NLS-1$
								providerType = new DefaultRepositoryProviderType();
							} else {
								providerType = (RepositoryProviderType) configElement.createExecutableExtension("typeClass"); //$NON-NLS-1$
							}
							providerType.setID(id);
							allProviderTypes.put(id, providerType);
							String scheme = configElement.getAttribute("fileSystemScheme"); //$NON-NLS-1$
							providerType.setFileSystemScheme(scheme);
							return providerType;
						} catch (CoreException e) {
							TeamPlugin.log(e);
						} catch (ClassCastException e) {
							String className = configElement.getAttribute("typeClass"); //$NON-NLS-1$
							TeamPlugin.log(IStatus.ERROR, "Class " + className + " registered for repository provider type id " + id + " is not a subclass of RepositoryProviderType", e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
						return null;
					}
				}
			}
		}
		return null;
	}

	private void setFileSystemScheme(String scheme) {
		this.scheme = scheme;
	}

	/**
	 * Answer the id of this provider type. The id will be the repository
	 * provider type's id as defined in the provider plugin's plugin.xml.
	 *
	 * @return the id of this provider type
	 */
	public final String getID() {
		return this.id;
	}

	/**
	 * Answers an object for serializing and deserializing
	 * of references to projects.  Given a project, it can produce a
	 * UTF-8 encoded String which can be stored in a file.
	 * Given this String, it can load a project into the workspace.
	 * It also provides a mechanism
	 * by which repository providers can be notified when a project set is created and exported.
	 * If the provider doesn't wish to provide this
	 * feature, return null.
	 * <p>
	 * Subclasses should override this method to return the appropriate
	 * serializer for the associated repository type.
	 * It is recommended that serializers not have any references to UI classes
	 * so that they can be used in a headless environment.
	 * <p>
	 * At this time, the default implementation wrappers the <code>IProjectSetSerializer</code>
	 * interface if one exists, providing backward compatibility with existing code.
	 * At some time in the future, the <code>IProjectSetSerializer</code> interface will be removed
	 * and the default implementation will revert to having limited functionality.
	 *
	 * @return the project set serializer (or <code>null</code>)
	 */
	public ProjectSetCapability getProjectSetCapability() {
		// Provide backward compatibility with the old IProjectSetSerializer interface
		IProjectSetSerializer oldSerializer = Team.getProjectSetSerializer(getID());
		if (oldSerializer != null) {
			ProjectSetCapability capability = new DefaultProjectSetCapability();
			capability.setSerializer(oldSerializer);
			return capability;
		}
		return null;
	}

	/**
	 * Callback from team when the meta-files for a repository type are detected in an
	 * unshared project. The meta-file paths are provided as part of the <code>repository</code>
	 * entry in the plugin manifest file.
	 * <p>
	 * By default, nothing is done (except that the repository type's
	 * plugin will have been loaded. Subclass may wish to mark the met-data as team-private.
	 * This method is called from a resource delta so subclasses may not obtain scheduling rules
	 * or in any way modify workspace resources (including auto-sharing the project). However,
	 * auto-sharing (or other modification) could be performed by a background job scheduled from
	 * this callback.
	 *
	 * @since 3.1
	 *
	 * @param project the project that contains the detected meta-files.
	 * @param containers the folders (possibly including the project folder) in which meta-files were found
	 */
	public void metaFilesDetected(IProject project, IContainer[] containers) {
		// Do nothing by default
	}

	/**
	 * Return a {@link Subscriber} that describes the synchronization state
	 * of the resources contained in the project associated with this
	 * provider type. By default, <code>null</code> is returned. Subclasses
	 * may override.
	 * @return a subscriber that provides resource synchronization state or <code>null</code>
	 * @since 3.2
	 */
	public Subscriber getSubscriber() {
		return null;
	}

	/**
	 * Return the file system scheme for this provider type or
	 * <code>null</code> if the type doesn't support file systems
	 * as defined by the <code>org.eclipse.core.filesystem.filesystems</code>
	 * extension point.
	 * @return the file system scheme for this provider type or
	 * <code>null</code>
	 * @since 3.2
	 */
	public final String getFileSystemScheme() {
		return scheme;
	}
}
