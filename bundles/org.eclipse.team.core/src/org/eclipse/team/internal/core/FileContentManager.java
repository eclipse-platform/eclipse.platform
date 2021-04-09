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

package org.eclipse.team.internal.core;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.team.core.IFileContentManager;
import org.eclipse.team.core.IStringMapping;
import org.eclipse.team.core.Team;

/**
 * TODO: implement extension point
 */
public class FileContentManager implements IFileContentManager {

	private static final String PREF_TEAM_EXTENSION_TYPES= "file_types"; //$NON-NLS-1$
	private static final String PREF_TEAM_FILENAME_TYPES= "cvs_mode_for_file_without_extensions"; //$NON-NLS-1$

	private static class StringMapping implements IStringMapping {

		private final String fString;
		private final int fType;

		public StringMapping(String string, int type) {
			fString= string;
			fType= type;
		}

		@Override
		public String getString() {
			return fString;
		}

		@Override
		public int getType() {
			return fType;
		}
	}

	private static class UserExtensionMappings extends UserStringMappings {

		public UserExtensionMappings(String key) {
			super(key);
		}

		@Override
		protected Map<String, Integer> loadMappingsFromPreferences() {
			final Map<String, Integer> result= super.loadMappingsFromPreferences();
			if (loadMappingsFromOldWorkspace(result)) {
				TeamPlugin.getPlugin().savePluginPreferences();
			}
			return result;
		}

		/**
		 * If the workspace is an old 2.0 one, read the old file and delete it.
		 *
		 * @param A map where the new mappings should be added.
		 *
		 * @return true if the workspace was a 2.0 one and the old mappings have
		 * been added to the map, false otherwise.
		 *
		 */
		private boolean loadMappingsFromOldWorkspace(Map<String, Integer> map) {
			// File name of the persisted file type information
			String STATE_FILE = ".fileTypes"; //$NON-NLS-1$
			IPath pluginStateLocation = TeamPlugin.getPlugin().getStateLocation().append(STATE_FILE);
			File f = pluginStateLocation.toFile();

			if (!f.exists())
				return false;

			try (DataInputStream input = new DataInputStream(new FileInputStream(f))) {
					map.putAll(readOldFormatExtensionMappings(input));
			} catch (IOException ex) {
				TeamPlugin.log(IStatus.ERROR, ex.getMessage(), ex);
				return false;
			} finally {
				f.delete();
			}

			return true;
		}

		/**
		 * Read the saved file type state from the given input stream.
		 *
		 * @param input the input stream to read the saved state from
		 * @throws IOException if an I/O problem occurs
		 */
		private Map<String, Integer> readOldFormatExtensionMappings(DataInputStream input) throws IOException {
			final Map<String, Integer> result= new TreeMap<>();
			int numberOfMappings = 0;
			try {
				numberOfMappings = input.readInt();
			} catch (EOFException e) {
				// Ignore the exception, it will occur if there are no
				// patterns stored in the state file.
				return Collections.emptyMap();
			}
			for (int i = 0; i < numberOfMappings; i++) {
				final String extension = input.readUTF();
				final int type = input.readInt();
				result.put(extension, Integer.valueOf(type));
			}
			return result;
		}
	}

	private final UserStringMappings fUserExtensionMappings, fUserNameMappings;
	private PluginStringMappings fPluginExtensionMappings;//, fPluginNameMappings;
	private IContentType textContentType;

	public FileContentManager() {
		fUserExtensionMappings= new UserExtensionMappings(PREF_TEAM_EXTENSION_TYPES);
		fUserNameMappings= new UserStringMappings(PREF_TEAM_FILENAME_TYPES);
		fPluginExtensionMappings= new PluginStringMappings(TeamPlugin.FILE_TYPES_EXTENSION, "extension"); //$NON-NLS-1$
	}

	@Override
	public int getTypeForName(String filename) {
		final int userType= fUserNameMappings.getType(filename);
//        final int pluginType= fPluginNameMappings.getType(filename);
//        return userType != Team.UNKNOWN ? userType : pluginType;
		return userType;
	}

	@Override
	public int getTypeForExtension(String extension) {
		final int userType= fUserExtensionMappings.getType(extension);
		final int pluginType= fPluginExtensionMappings.getType(extension);
		return userType != Team.UNKNOWN ? userType : pluginType;
	}

	@Override
	public void addNameMappings(String[] names, int [] types) {
		fUserNameMappings.addStringMappings(names, types);
	}

	@Override
	public void addExtensionMappings(String[] extensions, int [] types) {
		fUserExtensionMappings.addStringMappings(extensions, types);
	}

	@Override
	public void setNameMappings(String[] names, int [] types) {
		fUserNameMappings.setStringMappings(names, types);
	}

	@Override
	public void setExtensionMappings(String[] extensions, int [] types) {
		fUserExtensionMappings.setStringMappings(extensions, types);
	}

	@Override
	public IStringMapping[] getNameMappings() {
		return getMappings(fUserNameMappings, null);//fPluginNameMappings);
	}

	@Override
	public IStringMapping[] getExtensionMappings() {
		return getMappings(fUserExtensionMappings, fPluginExtensionMappings);
	}

	@Override
	public int getType(IStorage storage) {
		int type;

		final String name= storage.getName();
		if (name != null && (type= getTypeForName(name)) != Team.UNKNOWN)
			return type;

		final String extension= getFileExtension(name);
		if (extension != null && (type= getTypeForExtension(extension)) != Team.UNKNOWN)
			return type;

		IContentType contentType = Platform.getContentTypeManager().findContentTypeFor(name);
		if (contentType != null) {
			IContentType textType = getTextContentType();
			if (contentType.isKindOf(textType)) {
				return Team.TEXT;
			}
		}

		return Team.UNKNOWN;
	}

	private IContentType getTextContentType() {
		if (textContentType == null)
			textContentType = Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT);
		return textContentType;
	}

	@Override
	public IStringMapping[] getDefaultNameMappings() {
		// TODO: There is currently no extension point for this
		return new IStringMapping[0];//getStringMappings(fPluginNameMappings.referenceMap());
	}

	@Override
	public IStringMapping[] getDefaultExtensionMappings() {
		return getStringMappings(fPluginExtensionMappings.referenceMap());
	}

	@Override
	public boolean isKnownExtension(String extension) {
		return fUserExtensionMappings.referenceMap().containsKey(extension)
		|| fPluginExtensionMappings.referenceMap().containsKey(extension);
	}

	@Override
	public boolean isKnownFilename(String filename) {
		return fUserNameMappings.referenceMap().containsKey(filename);
//        || fPluginNameMappings.referenceMap().containsKey(filename);
	}

	private static String getFileExtension(String name) {
		if (name == null)
			return null;
		int index = name.lastIndexOf('.');
		if (index == -1)
			return null;
		if (index == (name.length() - 1))
			return ""; //$NON-NLS-1$
		return name.substring(index + 1);
	}

	private static IStringMapping [] getStringMappings(Map map) {
		final IStringMapping [] result= new IStringMapping [map.size()];
		int index= 0;
		for (Object element : map.entrySet()) {
			final Map.Entry entry= (Map.Entry)element;
			result[index++]= new StringMapping((String)entry.getKey(), ((Integer)entry.getValue()).intValue());
		}
		return result;
	}

	private IStringMapping [] getMappings(UserStringMappings userMappings, PluginStringMappings pluginMappings) {
		final Map<String, Integer> mappings= new HashMap<>();
		if (pluginMappings != null)
			mappings.putAll(pluginMappings.referenceMap());
		mappings.putAll(userMappings.referenceMap());
		return getStringMappings(mappings);
	}
}
