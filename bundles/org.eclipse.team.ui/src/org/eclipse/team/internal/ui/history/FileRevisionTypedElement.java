/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.history;

import java.net.URI;
import java.text.DateFormat;
import java.util.Date;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.StorageTypedElement;
import org.eclipse.ui.IEditorInput;

/**
 * An {@link ITypedElement} wrapper for {@link IFileRevision} for use with the
 * Compare framework.
 */
public class FileRevisionTypedElement extends StorageTypedElement {

	private IFileRevision fileRevision;
	private String author;

	/**
	 * Create a typed element that wraps the given file revision.
	 * @param fileRevision the file revision
	 */
	public FileRevisionTypedElement(IFileRevision fileRevision){
		this(fileRevision, null);
	}

	/**
	 * Create a typed element that wraps the given file revision.
	 * @param fileRevision the file revision
	 * @param localEncoding the encoding of the local file that corresponds to the given file revision
	 */
	public FileRevisionTypedElement(IFileRevision fileRevision, String localEncoding){
		super(localEncoding);
		Assert.isNotNull(fileRevision);
		this.fileRevision = fileRevision;
	}

	@Override
	public String getName() {
		return fileRevision.getName();
	}

	@Override
	protected IStorage fetchContents(IProgressMonitor monitor) throws CoreException {
		return fileRevision.getStorage(monitor);

	}

	/**
	 * Returns the unique content identifier for this element
	 * @return String	the string contains a unique content id
	 */
	public String getContentIdentifier() {
		return fileRevision.getContentIdentifier();
	}

	/**
	 * Return the human readable timestamp of this element.
	 * @return the human readable timestamp of this element
	 */
	public String getTimestamp() {
		long date = fileRevision.getTimestamp();
		Date dateFromLong = new Date(date);
		return DateFormat.getDateTimeInstance().format(dateFromLong);
	}

	/**
	 * Return the file revision of this element.
	 * @return the file revision of this element
	 */
	public IFileRevision getFileRevision(){
		return fileRevision;
	}

	/**
	 * Return the human readable path of this element.
	 * @return the human readable path of this element
	 */
	public String getPath() {
		URI uri = fileRevision.getURI();
		if (uri != null)
			return uri.getPath();
		return getName();
	}

	@Override
	public IEditorInput getDocumentKey(Object element) {
		if (element == this && getBufferedStorage() != null) {
			return new FileRevisionEditorInput(fileRevision, getBufferedStorage(), getLocalEncoding());
		}
		return null;
	}

	@Override
	public int hashCode() {
		return fileRevision.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof FileRevisionTypedElement) {
			FileRevisionTypedElement other = (FileRevisionTypedElement) obj;
			return other.getFileRevision().equals(getFileRevision());
		}
		return false;
	}

	public String getAuthor() {
		if (author == null)
			author = fileRevision.getAuthor();
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void fetchAuthor(IProgressMonitor monitor) throws CoreException {
		if (getAuthor() == null && fileRevision.isPropertyMissing()) {
			IFileRevision other = fileRevision.withAllProperties(monitor);
			author = other.getAuthor();
		}
	}

	public IFileRevision getRevision() {
		return fileRevision;
	}

}
