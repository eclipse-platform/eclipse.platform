/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core;

import java.util.HashMap;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.mapping.IStorageMerger;

public class StorageMergerRegistry {

	private final static String ID_ATTRIBUTE = "id"; //$NON-NLS-1$
	private final static String EXTENSIONS_ATTRIBUTE = "extensions"; //$NON-NLS-1$
	private final static String CONTENT_TYPE_ID_ATTRIBUTE = "contentTypeId"; //$NON-NLS-1$
	private static final String STORAGE_MERGER_EXTENSION_POINT = "storageMergers"; //$NON-NLS-1$
	private static final Object STORAGE_MERGER = "storageMerger"; //$NON-NLS-1$
	private static final String CONTENT_TYPE_BINDING= "contentTypeBinding"; //$NON-NLS-1$
	private static final String STORAGE_MERGER_ID_ATTRIBUTE= "storageMergerId"; //$NON-NLS-1$

	private static boolean NORMALIZE_CASE= true;

	private static StorageMergerRegistry instance;

	private HashMap<String, Object> fIdMap; // maps ids to datas
	private HashMap<String, Object> fExtensionMap; // maps extensions to datas
	private HashMap<IContentType, Object> fContentTypeBindings; // maps content type bindings to datas
	private boolean fRegistriesInitialized;

	public static StorageMergerRegistry getInstance() {
		if (instance == null) {
			instance = new StorageMergerRegistry();
		}
		return instance;
	}

	/**
	 * Returns a stream merger for the given type.
	 *
	 * @param type the type for which to find a stream merger
	 * @return a stream merger for the given type, or <code>null</code> if no
	 *   stream merger has been registered
	 */
	public IStorageMerger createStreamMerger(String type) {
		initializeRegistry();
		StorageMergerDescriptor descriptor= (StorageMergerDescriptor) search(type);
		if (descriptor != null)
			return descriptor.createStreamMerger();
		return null;
	}

	/**
	 * Returns a stream merger for the given content type.
	 *
	 * @param type the type for which to find a stream merger
	 * @return a stream merger for the given type, or <code>null</code> if no
	 *   stream merger has been registered
	 */
	public IStorageMerger createStreamMerger(IContentType type) {
		initializeRegistry();
		StorageMergerDescriptor descriptor= (StorageMergerDescriptor) search(type);
		if (descriptor != null)
			return descriptor.createStreamMerger();
		return null;
	}

	private void initializeRegistry() {
		if (!fRegistriesInitialized) {
			registerExtensions();
			fRegistriesInitialized= true;
		}
	}

	/**
	 * Registers all stream mergers, structure creators, content merge viewers, and structure merge viewers
	 * that are found in the XML plugin files.
	 */
	private void registerExtensions() {
		IExtensionRegistry registry= Platform.getExtensionRegistry();

		// collect all IStreamMergers
		IConfigurationElement[] elements= registry.getConfigurationElementsFor(TeamPlugin.ID, STORAGE_MERGER_EXTENSION_POINT);
		for (IConfigurationElement element : elements) {
			if (STORAGE_MERGER.equals(element.getName()))
				register(element, new StorageMergerDescriptor(element));
			else if (CONTENT_TYPE_BINDING.equals(element.getName()))
				createBinding(element, STORAGE_MERGER_ID_ATTRIBUTE);
		}
	}

	private static String normalizeCase(String s) {
		if (NORMALIZE_CASE && s != null)
			return s.toUpperCase();
		return s;
	}

	void register(IConfigurationElement element, Object data) {
		String id = element.getAttribute(ID_ATTRIBUTE);
		if (id != null) {
			if (fIdMap == null)
				fIdMap = new HashMap<>();
			fIdMap.put(id, data);
		}

		String types = element.getAttribute(EXTENSIONS_ATTRIBUTE);
		if (types != null) {
			if (fExtensionMap == null)
				fExtensionMap = new HashMap<>();
			StringTokenizer tokenizer = new StringTokenizer(types, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreElements()) {
				String extension = tokenizer.nextToken().trim();
				fExtensionMap.put(normalizeCase(extension), data);
			}
		}
	}

	void createBinding(IConfigurationElement element, String idAttributeName) {
		String type = element.getAttribute(CONTENT_TYPE_ID_ATTRIBUTE);
		String id = element.getAttribute(idAttributeName);
		if (id == null)
			logErrorMessage(NLS.bind("Target attribute id '{0}' missing", idAttributeName)); //$NON-NLS-1$
		if (type != null && id != null && fIdMap != null) {
			Object o = fIdMap.get(id);
			if (o != null) {
				IContentType ct = Platform.getContentTypeManager().getContentType(type);
				if (ct != null) {
					if (fContentTypeBindings == null)
						fContentTypeBindings = new HashMap<>();
					fContentTypeBindings.put(ct, o);
				} else {
					logErrorMessage(NLS.bind("Content type id '{0}' not found", type)); //$NON-NLS-1$
				}
			} else {
				logErrorMessage(NLS.bind("Target '{0}' not found", id)); //$NON-NLS-1$$
			}
		}
	}

	private void logErrorMessage(String string) {
		TeamPlugin.log(IStatus.ERROR, string, null);
	}

	Object search(IContentType type) {
		if (fContentTypeBindings != null) {
			for (; type != null; type = type.getBaseType()) {
				Object data = fContentTypeBindings.get(type);
				if (data != null)
					return data;
			}
		}
		return null;
	}

	Object search(String extension) {
		if (fExtensionMap != null)
			return fExtensionMap.get(normalizeCase(extension));
		return null;
	}
}
