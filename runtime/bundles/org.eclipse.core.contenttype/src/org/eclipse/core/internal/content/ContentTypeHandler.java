/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
package org.eclipse.core.internal.content;

import java.io.*;
import java.lang.ref.SoftReference;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.*;
import org.eclipse.core.runtime.preferences.IScopeContext;

/**
 * The only content types exposed to clients. Allows the content type registry to change
 * underneath preserving handlers kept by clients.
 */
public class ContentTypeHandler implements IContentType {

	/**
	 * A dummy description object to be returned by getDescription when this
	 * handler's target cannot be determined.
	 */
	private class DummyContentDescription implements IContentDescription {
		@Override
		public String getCharset() {
			return null;
		}

		@Override
		public IContentType getContentType() {
			return ContentTypeHandler.this;
		}

		@Override
		public Object getProperty(QualifiedName key) {
			return null;
		}

		@Override
		public boolean isRequested(QualifiedName key) {
			return false;
		}

		@Override
		public void setProperty(QualifiedName key, Object value) {
			// don't do anything
		}
	}

	private int generation;
	String id;
	private SoftReference<ContentType> targetRef;

	ContentTypeHandler(ContentType target, int generation) {
		this.id = target.getId();
		this.targetRef = new SoftReference<>(target);
		this.generation = generation;
	}

	@Override
	public void addFileSpec(String fileSpec, int type) throws CoreException {
		final IContentType target = getTarget();
		if (target != null)
			target.addFileSpec(fileSpec, type);
	}

	@Override
	public boolean equals(Object another) {
		if (another instanceof ContentType)
			return id.equals(((ContentType) another).id);
		if (another instanceof ContentTypeHandler)
			return id.equals(((ContentTypeHandler) another).id);
		return false;
	}

	@Override
	public IContentType getBaseType() {
		final ContentType target = getTarget();
		if (target == null)
			return null;
		final ContentType baseType = (ContentType) target.getBaseType();
		return (baseType != null) ? new ContentTypeHandler(baseType, baseType.getCatalog().getGeneration()) : null;
	}

	@Override
	public String getDefaultCharset() {
		final IContentType target = getTarget();
		return (target != null) ? target.getDefaultCharset() : null;
	}

	@Override
	public IContentDescription getDefaultDescription() {
		final IContentType target = getTarget();
		return (target != null) ? target.getDefaultDescription() : new DummyContentDescription();
	}

	@Override
	public IContentDescription getDescriptionFor(InputStream contents, QualifiedName[] options) throws IOException {
		final IContentType target = getTarget();
		return (target != null) ? target.getDescriptionFor(contents, options) : null;
	}

	@Override
	public IContentDescription getDescriptionFor(Reader contents, QualifiedName[] options) throws IOException {
		final IContentType target = getTarget();
		return (target != null) ? target.getDescriptionFor(contents, options) : null;
	}

	@Override
	public String[] getFileSpecs(int type) {
		final IContentType target = getTarget();
		return (target != null) ? target.getFileSpecs(type) : new String[0];
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		final IContentType target = getTarget();
		return (target != null) ? target.getName() : id;
	}

	@Override
	public IContentTypeSettings getSettings(IScopeContext context) throws CoreException {
		final ContentType target = getTarget();
		if (target == null)
			return null;
		// the content type may returned itself as the settings object (instance scope context)
		final IContentTypeSettings settings = target.getSettings(context);
		// in that case, return this same handler; otherwise, just return the settings
		return settings == target ? this : settings;
	}

	/**
	 * Returns the content type this handler represents.
	 * Note that this handles the case of aliasing.
	 *
	 * Public for testing purposes only.
	 */
	public ContentType getTarget() {
		ContentType target = targetRef.get();
		ContentTypeCatalog catalog = ContentTypeManager.getInstance().getCatalog();
		if (target == null || catalog.getGeneration() != generation) {
			target = catalog.getContentType(id);
			targetRef = new SoftReference<>(target);
			generation = catalog.getGeneration();
		}
		return target == null ? null : target.getAliasTarget(true);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean isAssociatedWith(String fileName) {
		final IContentType target = getTarget();
		return (target != null) ? target.isAssociatedWith(fileName) : false;
	}

	@Override
	public boolean isAssociatedWith(String fileName, IScopeContext context) {
		final IContentType target = getTarget();
		return (target != null) ? target.isAssociatedWith(fileName, context) : false;
	}

	@Override
	public boolean isKindOf(IContentType another) {
		if (another instanceof ContentTypeHandler)
			another = ((ContentTypeHandler) another).getTarget();
		final IContentType target = getTarget();
		return (target != null) ? target.isKindOf(another) : false;
	}

	@Override
	public void removeFileSpec(String fileSpec, int type) throws CoreException {
		final IContentType target = getTarget();
		if (target != null)
			target.removeFileSpec(fileSpec, type);
	}

	@Override
	public void setDefaultCharset(String userCharset) throws CoreException {
		final IContentType target = getTarget();
		if (target != null)
			target.setDefaultCharset(userCharset);
	}

	@Override
	public String toString() {
		return id;
	}

	@Override
	public boolean isUserDefined() {
		ContentType target = getTarget();
		if (target != null) {
			return target.isUserDefined();
		}
		return false;
	}

}
