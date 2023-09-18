/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *     Mickael Istria (Red Hat Inc.) - Bug 488937
 *     Latha Patil (ETAS GmbH) - GitHub Issue 468
 *******************************************************************************/
package org.eclipse.core.internal.properties;

import java.io.File;
import java.util.*;
import org.eclipse.core.internal.localstore.Bucket;
import org.eclipse.core.internal.localstore.Bucket.Entry;
import org.eclipse.core.internal.localstore.BucketTree;
import org.eclipse.core.internal.properties.PropertyBucket.PropertyEntry;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

/**
 * @see org.eclipse.core.internal.properties.IPropertyManager
 */
public class PropertyManager2 implements IPropertyManager {
	private static final int MAX_VALUE_SIZE = 2 * 1024;

	class PropertyCopyVisitor extends Bucket.Visitor {
		private final List<PropertyEntry> changes = new ArrayList<>();
		private final IPath destination;
		private final IPath source;

		public PropertyCopyVisitor(IPath source, IPath destination) {
			this.source = source;
			this.destination = destination;
		}

		@Override
		public void afterSaving(Bucket bucket) throws CoreException {
			saveChanges((PropertyBucket) bucket);
			changes.clear();
		}

		private void saveChanges(PropertyBucket bucket) throws CoreException {
			if (changes.isEmpty())
				return;
			// make effective all changes collected
			Iterator<PropertyEntry> i = changes.iterator();
			PropertyEntry entry = i.next();
			tree.loadBucketFor(entry.getPath());
			bucket.setProperties(entry);
			while (i.hasNext())
				bucket.setProperties(i.next());
			bucket.save();
		}

		@Override
		public int visit(Entry entry) {
			PropertyEntry sourceEntry = (PropertyEntry) entry;
			IPath destinationPath = destination.append(sourceEntry.getPath().removeFirstSegments(source.segmentCount()));
			PropertyEntry destinationEntry = new PropertyEntry(destinationPath, sourceEntry);
			changes.add(destinationEntry);
			return CONTINUE;
		}
	}

	BucketTree tree;

	public PropertyManager2(Workspace workspace) {
		this.tree = new BucketTree(workspace, new PropertyBucket());
	}

	@Override
	public void closePropertyStore(IResource target) throws CoreException {
		// ensure any uncommitted are written to disk
		tree.getCurrent().save();
		// flush in-memory state to avoid confusion if another project is later
		// created with the same name
		tree.getCurrent().flush();
	}

	@Override
	public synchronized void copy(IResource source, IResource destination, int depth) throws CoreException {
		copyProperties(source.getFullPath(), destination.getFullPath());
	}

	/**
	 * Copies all properties from the source path to the target path, to the given depth.
	 */
	private void copyProperties(final IPath source, final IPath destination) throws CoreException {
		Assert.isLegal(source.segmentCount() > 0);
		Assert.isLegal(destination.segmentCount() > 0);
		Assert.isLegal(source.segmentCount() > 1 || destination.segmentCount() == 1);

		// copy history by visiting the source tree
		PropertyCopyVisitor copyVisitor = new PropertyCopyVisitor(source, destination);
		tree.accept(copyVisitor, source, BucketTree.DEPTH_INFINITE);
	}

	@Override
	public synchronized void deleteProperties(IResource target, int depth) throws CoreException {
		tree.accept(new PropertyBucket.Visitor() {
			@Override
			public int visit(Entry entry) {
				entry.delete();
				return CONTINUE;
			}
		}, target.getFullPath(), depth == IResource.DEPTH_INFINITE ? BucketTree.DEPTH_INFINITE : depth);
	}

	@Override
	public void deleteResource(IResource target) throws CoreException {
		deleteProperties(target, ((target instanceof IFile) && (null != target.getProject())) ? IResource.DEPTH_ZERO
				: IResource.DEPTH_INFINITE);
	}

	@Override
	public synchronized Map<QualifiedName, String> getProperties(IResource target) throws CoreException {
		final Map<QualifiedName, String> result = new HashMap<>();
		tree.accept(new PropertyBucket.Visitor() {
			@Override
			public int visit(Entry entry) {
				PropertyEntry propertyEntry = (PropertyEntry) entry;
				int propertyCount = propertyEntry.getOccurrences();
				for (int i = 0; i < propertyCount; i++)
					result.put(propertyEntry.getPropertyName(i), propertyEntry.getPropertyValue(i));
				return CONTINUE;
			}
		}, target.getFullPath(), BucketTree.DEPTH_ZERO);
		return result;
	}

	@Override
	public synchronized String getProperty(IResource target, QualifiedName name) throws CoreException {
		if (name.getQualifier() == null) {
			String message = Messages.properties_qualifierIsNull;
			throw new ResourceException(IResourceStatus.FAILED_READ_METADATA, target.getFullPath(), message, null);
		}
		IPath resourcePath = target.getFullPath();
		PropertyBucket current = (PropertyBucket) tree.getCurrent();
		tree.loadBucketFor(resourcePath);
		return current.getProperty(resourcePath, name);
	}

	public BucketTree getTree() {
		return tree;
	}

	public File getVersionFile() {
		return tree.getVersionFile();
	}

	@Override
	public synchronized void setProperty(IResource target, QualifiedName name, String value) throws CoreException {
		//resource may have been deleted concurrently
		//must check for existence within synchronized method
		Resource resource = (Resource) target;
		ResourceInfo info = resource.getResourceInfo(false, false);
		int flags = resource.getFlags(info);
		resource.checkAccessible(flags);
		// enforce the limit stated by the spec
		if (value != null && value.length() > MAX_VALUE_SIZE) {
			String message = NLS.bind(Messages.properties_valueTooLong, new Object[] {name.getQualifier(), name.getLocalName(), Integer.toString(MAX_VALUE_SIZE)});
			throw new ResourceException(IResourceStatus.FAILED_WRITE_METADATA, target.getFullPath(), message, null);
		}
		if (name.getQualifier() == null) {
			String message = Messages.properties_qualifierIsNull;
			throw new ResourceException(IResourceStatus.FAILED_WRITE_METADATA, target.getFullPath(), message, null);
		}

		IPath resourcePath = target.getFullPath();
		tree.loadBucketFor(resourcePath);
		PropertyBucket current = (PropertyBucket) tree.getCurrent();
		current.setProperty(resourcePath, name, value);
		current.save();
	}

	@Override
	public void shutdown(IProgressMonitor monitor) throws CoreException {
		tree.close();
	}

	@Override
	public void startup(IProgressMonitor monitor) {
		// nothing to do
	}
}
