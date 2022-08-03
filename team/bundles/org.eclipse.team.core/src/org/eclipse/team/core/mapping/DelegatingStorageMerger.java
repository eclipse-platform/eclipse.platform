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
package org.eclipse.team.core.mapping;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.IFileContentManager;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.StorageMergerRegistry;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.core.mapping.IStreamMergerDelegate;

/**
 * This storage merger delegates to the appropriate merger or returns a conflict
 * if no merger is available or if a merge was not possible.
 * <p>
 * The target storage is used to look for an appropriate merger. If the target
 * is an {@link IFile}, the content type of the file is used. Otherwise, the
 * {@link IContentTypeManager} is used to find an appropriate content type. If an
 * appropriate merger is not found, a status containing the
 * <code>CONFLICT</code> is returned.
 * <p>
 * Clients may use this class directly or subclass it.
 * @since 3.4
 *
 */
public class DelegatingStorageMerger implements IStorageMerger {

	private static DelegatingStorageMerger instance;

	/**
	 * Return the storage merger associated with the <code>IContentTypeManager.CT_TEXT</code>
	 * content type.
	 * @return the storage merger associated with the <code>IContentTypeManager.CT_TEXT</code>
	 * content type
	 */
	public static IStorageMerger createTextMerger() {
		return Team.createMerger(Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT));
	}

	/**
	 * Default no-arg constructor.
	 */
	public DelegatingStorageMerger() {
		// Nothing to do
	}

	/**
	 * Helper method that returns a singleton instance that can be used to merge
	 * two {@link IStorage} instances.
	 * @return a storage merger that delegates the merge based on the type
	 * of the target storage.
	 */
	public static IStorageMerger getInstance() {
		if (instance == null)
			instance = new DelegatingStorageMerger();
		return instance;
	}

	@Override
	public IStatus merge(OutputStream output, String outputEncoding,
			IStorage ancestor, IStorage target, IStorage other,
			IProgressMonitor monitor) throws CoreException {
		IStorageMerger merger = createDelegateMerger(target);
		if (merger == null)
			return new Status(IStatus.WARNING, TeamPlugin.ID, CONFLICT,
					Messages.DelegatingStorageMerger_0, null);
		if (ancestor == null && !merger.canMergeWithoutAncestor()) {
			return new Status(IStatus.WARNING, TeamPlugin.ID, CONFLICT,
					NLS.bind(Messages.MergeContext_1, new String[] { target.getFullPath().toString() }), null);
		}
		return merger.merge(output, outputEncoding, ancestor, target, other, monitor);
	}

	/**
	 * Create a merger for the given storage or return <code>null</code>
	 * if an appropriate merger could not be created. This method is called
	 * by {@link #merge(OutputStream, String, IStorage, IStorage, IStorage, IProgressMonitor)}
	 * to create the merger to which the merge should be delegated.
	 * @param target the storage that contains the target contents of the merge.
	 * @return a merger for the given storage or <code>null</code>
	 * @throws CoreException no merger found
	 */
	protected IStorageMerger createDelegateMerger(IStorage target) throws CoreException {
		IStorageMerger merger = null;
		CoreException exception = null;
		try {
			IContentType type = getContentType(target);
			if (type != null)
				merger = getMerger(type);
		} catch (CoreException e) {
			exception = e;
		}
		// If an exception occurred trying to find a content type,
		// try using the extension before failing
		if (merger == null) {
			merger = getMerger(target.getName());
			if (merger == null) {
				// If team thinks the file is text, try to get a text merger for the file
				int type = getType(target);
				if (type == Team.TEXT)
					merger = createTextMerger();
				if (merger == null) {
					// As a last resort, look for a stream merger
					merger = findAndWrapStreamMerger(target);
				}
			}
		}
		if (exception != null) {
			if (merger == null) {
				// No merger was found so report the error
				throw exception;
			} else {
				// If an extension based merger was found, log the error
				TeamPlugin.log(exception);
			}
		}
		return merger;
	}

	/**
	 * Return the Team content type associated with the given
	 * target.
	 * @param target the storage that contains the target contents for the merge.
	 * @return the Team content type associated with the given
	 * target
	 * @see Team#getFileContentManager()
	 * @see IFileContentManager#getType(IStorage)
	 */
	protected int getType(IStorage target) {
		return Team.getFileContentManager().getType(target);
	}

	private IStorageMerger findAndWrapStreamMerger(IStorage target) {
		IStreamMergerDelegate mergerDelegate = TeamPlugin.getPlugin().getMergerDelegate();
		if (mergerDelegate != null) {
			IStorageMerger merger = mergerDelegate.findMerger(target);
			return merger;
		}
		return null;
	}

	private IStorageMerger getMerger(String name) {
		String extension = getExtension(name);
		if (extension != null)
			return StorageMergerRegistry.getInstance().createStreamMerger(extension);
		return null;
	}

	/**
	 * Helper method for returning the extension of a file name
	 * @param name the file name
	 * @return the extension of the file name or <code>null</code>
	 * if the file name does not have an extension
	 */
	public static String getExtension(String name) {
		int index = name.lastIndexOf('.');
		if (index == -1) {
			return null;
		}
		return name.substring(index + 1);
	}

	private IStorageMerger getMerger(IContentType type) {
		return Team.createMerger(type);
	}

	/**
	 * A helper method that finds the content type for the given storage or returns
	 * <code>null</code> if a content
	 * type cannot be found. Any exceptions that occur when trying to determine
	 * the content type are propagated.
	 * @param target the storage that contains the target contents of the merge.
	 * @return the content type of the storage or <code>null</code>
	 * @throws CoreException if an exception occurs
	 */
	public static IContentType getContentType(IStorage target) throws CoreException {
		if (target instanceof IFile) {
			IFile file = (IFile) target;
			IContentDescription contentDescription = file.getContentDescription();
			if (contentDescription != null) {
				IContentType contentType = contentDescription.getContentType();
				return contentType;
			}
		} else {
			IContentTypeManager manager = Platform.getContentTypeManager();
			try {
				IContentType type = manager.findContentTypeFor(target
						.getContents(), target.getName());
				return type;
			} catch (IOException e) {
				String name = target.getName();
				if (target.getFullPath() != null) {
					name = target.getFullPath().toString();
				}
				throw new TeamException(new Status(
					IStatus.ERROR,
					TeamPlugin.ID,
					INTERNAL_ERROR,
					NLS.bind(Messages.DelegatingStorageMerger_1,name), e));
			}
		}
		return null;
	}

	@Override
	public boolean canMergeWithoutAncestor() {
		return false;
	}

}
