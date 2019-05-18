/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

import org.eclipse.core.resources.IStorage;

/**
 * This is the API to define mappings between file names, file extensions and
 * content types, typically used by repository providers in order to determine
 * whether a given file can be treated as text or must be considered binary.
 *
 * Mappings for names and extensions can either be contributed via an extension
 * point or via this interface.
 *
 * For methods that determine the content type for a given file, the following
 * rules apply:
 * <ul>
 * <li>Mappings for the entire file name take precedence over mappings for the
 * file extension only.</li>
 * <li>User-defined mappings take precedence over plugin-contributed mappings
 * </li>
 * </ul>
 *
 * If a mapping is added for a name or an extension that already has a mapping
 * which has been contributed by a plugin, it overrides the one contributed by
 * the plugin. If the user-defined mapping is deleted, the plugin-contributed
 * mapping is valid again. This interface is not intended to be implemented by
 * clients.
 *
 * @see org.eclipse.team.core.Team#getFileContentManager()
 *
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IFileContentManager {

	/**
	 * Get the content type for a given instance of <code>IStorage</code>. User-defined mappings
	 * take precedence over plugin-contributed mappings; further, mappings for the entire file name
	 * take precedence over mappings for the file extension only.
	 *
	 * @param storage the instance of <code>IStorage</code>.
	 * @return one of <code>Team.UNKNOWN</code>, <code>Team.TEXT</code> or <code>Team.BINARY</code>.
	 *
	 * @since 3.1
	 */
	int getType(IStorage storage);

	/**
	 * Check whether the given file name is assigned to a specific type in the content type registry.
	 * @param filename the file name to check for
	 * @return True if the file name is registered in the system and assigned to a content type, false
	 * if the file name is unknown.
	 *
	 * @since 3.1
	 */
	boolean isKnownFilename(String filename);

	/**
	 * Check whether the given file extension is assigned to a specific type in the content type registry.
	 * @param extension the extension to check for
	 * @return True if the extension is registered in the system and assigned to a content type, false
	 * if the extension is unknown.
	 *
	 * @since 3.1
	 */
	boolean isKnownExtension(String extension);

	/**
	 * Get the content type for a given file name.
	 * @param filename The file name
	 * @return one of <code>Team.UNKNOWN</code>, <code>Team.TEXT</code> or <code>Team.BINARY</code>.
	 *
	 * @since 3.1
	 */
	int getTypeForName(String filename);

	/**
	 * Get the content type for a given file extension.
	 * @param extension The extension
	 * @return one of <code>Team.UNKNOWN</code>, <code>Team.TEXT</code> or <code>Team.BINARY</code>.
	 *
	 * @since 3.1
	 */
	int getTypeForExtension(String extension);

	/**
	 * Map a set of file names to a set of content types and save the mappings in
	 * the preferences. Already existing mappings for these file names are updated
	 * with the new ones, other mappings will be preserved.
	 *
	 * @param names The file names
	 * @param types The corresponding types, each one being one of
	 *            <code>Team.UNKNOWN</code>,<code>Team.TEXT</code> or
	 *            <code>Team.BINARY</code>.
	 *
	 * @since 3.1
	 */
	void addNameMappings(String[] names, int[] types);

	/**
	 * Map a set of file extensions to a set of content types and save the mapping in
	 * the preferences. Already existing mappings for these extensions are updated
	 * with the new ones, other mappings will be preserved.
	 *
	 * @param extensions The extensions
	 * @param types The corresponding types, each one being one of
	 *            <code>Team.UNKNOWN</code>,<code>Team.TEXT</code> or
	 *            <code>Team.BINARY</code>.
	 *
	 * @since 3.1
	 */
	void addExtensionMappings(String[] extensions, int[] types);

	/**
	 * Map a set of file names to a set of content types and save the mappings in
	 * the preferences. All existing user-defined mappings for <b>any
	 * </b> file names are deleted and replaced by the new ones.
	 *
	 * @param names The file names
	 * @param types The corresponding types, each one being one of
	 *            <code>Team.UNKNOWN</code>,<code>Team.TEXT</code> or
	 *            <code>Team.BINARY</code>.
	 *
	 * @since 3.1
	 */
	void setNameMappings(String[] names, int[] types);

	/**
	 * Map a set of file extensions to a set of content types and save the
	 * mapping in the preferences. All existing user-defined mappings for <b>any
	 * </b> file extensions are deleted and replaced by the new ones.
	 *
	 * @param extensions The extensions
	 * @param types The corresponding types, each one being one of
	 *            <code>Team.UNKNOWN</code>,<code>Team.TEXT</code> or
	 *            <code>Team.BINARY</code>.
	 *
	 * @since 3.1
	 */
	void setExtensionMappings(String[] extensions, int[] types);

	/**
	 * Get all the currently defined mappings from file names to content types.
	 *
	 * @return the mappings
	 *
	 * @since 3.1
	 */
	IStringMapping [] getNameMappings();

	/**
	 * Get all the currently defined mappings from file names to content types.
	 *
	 * @return the mappings
	 *
	 * @since 3.1
	 */
	IStringMapping [] getExtensionMappings();

	/**
	 * Get all the plugin-contributed mappings from file names to content types.
	 *
	 * @return the mappings
	 *
	 * @since 3.1
	 */
	IStringMapping [] getDefaultNameMappings();

	/**
	 * Get all the plugin-contributed mappings from file extensions to content types.
	 *
	 * @return the mappings

	 * @since 3.1
	 */
	IStringMapping [] getDefaultExtensionMappings();
}
