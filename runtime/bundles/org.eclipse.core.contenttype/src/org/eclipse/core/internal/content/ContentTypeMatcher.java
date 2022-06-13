/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
 *     Mickael Istria (Red Hat Inc.) - [263316] regexp for file association
 *******************************************************************************/
package org.eclipse.core.internal.content;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.*;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * @since 3.1
 */
public class ContentTypeMatcher implements IContentTypeMatcher {

	private IScopeContext context;
	private IContentTypeManager.ISelectionPolicy policy;

	public ContentTypeMatcher(IContentTypeManager.ISelectionPolicy policy, IScopeContext context) {
		this.policy = policy;
		this.context = context;
	}

	@Override
	public IContentType findContentTypeFor(InputStream contents, String fileName) throws IOException {
		ContentTypeCatalog currentCatalog = getCatalog();
		IContentType[] all = currentCatalog.findContentTypesFor(this, contents, fileName);
		return all.length > 0 ? new ContentTypeHandler((ContentType) all[0], currentCatalog.getGeneration()) : null;
	}

	@Override
	public IContentType findContentTypeFor(String fileName) {
		// basic implementation just gets all content types
		ContentTypeCatalog currentCatalog = getCatalog();
		IContentType[] associated = currentCatalog.findContentTypesFor(this, fileName);
		return associated.length == 0 ? null : new ContentTypeHandler((ContentType) associated[0], currentCatalog.getGeneration());
	}

	@Override
	public IContentType[] findContentTypesFor(InputStream contents, String fileName) throws IOException {
		ContentTypeCatalog currentCatalog = getCatalog();
		IContentType[] types = currentCatalog.findContentTypesFor(this, contents, fileName);
		IContentType[] result = new IContentType[types.length];
		int generation = currentCatalog.getGeneration();
		for (int i = 0; i < result.length; i++)
			result[i] = new ContentTypeHandler((ContentType) types[i], generation);
		return result;
	}

	@Override
	public IContentType[] findContentTypesFor(String fileName) {
		ContentTypeCatalog currentCatalog = getCatalog();
		IContentType[] types = currentCatalog.findContentTypesFor(this, fileName);
		IContentType[] result = new IContentType[types.length];
		int generation = currentCatalog.getGeneration();
		for (int i = 0; i < result.length; i++)
			result[i] = new ContentTypeHandler((ContentType) types[i], generation);
		return result;
	}

	private ContentTypeCatalog getCatalog() {
		return ContentTypeManager.getInstance().getCatalog();
	}

	@Override
	public IContentDescription getDescriptionFor(InputStream contents, String fileName, QualifiedName[] options) throws IOException {
		return getCatalog().getDescriptionFor(this, contents, fileName, options);
	}

	@Override
	public IContentDescription getDescriptionFor(Reader contents, String fileName, QualifiedName[] options) throws IOException {
		return getCatalog().getDescriptionFor(this, contents, fileName, options);
	}

	public IScopeContext getContext() {
		return context;
	}

	public IContentTypeManager.ISelectionPolicy getPolicy() {
		return policy;
	}

	/**
	 * Enumerates all content types whose settings satisfy the given file spec type mask.
	 */
	public Collection<ContentType> getDirectlyAssociated(final ContentTypeCatalog catalog, final String fileSpec, final int typeMask) {
		if ((typeMask & (IContentType.FILE_EXTENSION_SPEC | IContentType.FILE_NAME_SPEC)) == 0) {
			throw new IllegalArgumentException("This method only apply to name or extension based associations"); //$NON-NLS-1$
		}
		//TODO: make sure we include built-in associations as well
		final IEclipsePreferences root = context.getNode(ContentTypeManager.CONTENT_TYPE_PREF_NODE);
		final Set<ContentType> result = new HashSet<>(3);
		try {
			root.accept(node -> {
				if (node == root)
					return true;
				String[] fileSpecs = ContentTypeSettings.getFileSpecs(node, typeMask);
				for (String fileSpecification : fileSpecs)
					if (fileSpecification.equalsIgnoreCase(fileSpec)) {
						ContentType associated = catalog.getContentType(node.name());
						if (associated != null)
							result.add(associated);
						break;
					}
				return false;
			});
		} catch (BackingStoreException bse) {
			ContentType.log(ContentMessages.content_errorLoadingSettings, bse);
		}
		return result == null ? Collections.EMPTY_SET : result;
	}

	public Collection<? extends ContentType> getMatchingRegexpAssociated(ContentTypeCatalog catalog,
			String fileName, final int typeMask) {
		if ((typeMask & IContentType.FILE_PATTERN_SPEC) == 0) {
			throw new IllegalArgumentException("This method only applies for FILE_REGEXP_SPEC."); //$NON-NLS-1$
		}
		final IEclipsePreferences root = context.getNode(ContentTypeManager.CONTENT_TYPE_PREF_NODE);
		final Set<ContentType> result = new HashSet<>(3);
		try {
			root.accept(node -> {
				if (node == root)
					return true;
				String[] fileSpecs = ContentTypeSettings.getFileSpecs(node, typeMask);
				for (String fileSpecification : fileSpecs)
					if (Pattern.matches(catalog.toRegexp(fileSpecification), fileName)) {
						ContentType associated = catalog.getContentType(node.name());
						if (associated != null)
							result.add(associated);
						break;
					}
				return false;
			});
		} catch (BackingStoreException bse) {
			ContentType.log(ContentMessages.content_errorLoadingSettings, bse);
		}
		return result == null ? Collections.EMPTY_SET : result;
	}

	public IContentDescription getSpecificDescription(BasicDescription description) {
		if (description == null || ContentTypeManager.getInstance().getContext().equals(getContext()))
			// no need for specific content descriptions
			return description;
		// default description
		if (description instanceof DefaultDescription)
			// return an context specific description instead
			return new DefaultDescription(new ContentTypeSettings((ContentType) description.getContentTypeInfo(), context));
		// non-default description
		// replace info object with context specific settings
		((ContentDescription) description).setContentTypeInfo(new ContentTypeSettings((ContentType) description.getContentTypeInfo(), context));
		return description;
	}

}
