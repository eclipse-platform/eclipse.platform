/*******************************************************************************
 *  Copyright (c) 2004, 2026 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.internal.localstore;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.harness.FileSystemHelper.getRandomLocation;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.internal.localstore.Bucket;
import org.eclipse.core.internal.localstore.BucketTree;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.util.FileStoreAutoDeleteExtension;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(WorkspaceResetExtension.class)
public class BucketTreeTests {

	@RegisterExtension
	private final FileStoreAutoDeleteExtension fileStoreExtension = new FileStoreAutoDeleteExtension();

	static class SimpleBucket extends Bucket {

		static class SimpleEntry extends Entry {
			private final Map<String, String> value;

			public SimpleEntry(IPath path, Map<String, String> value) {
				super(path);
				this.value = value;
			}

			@Override
			public int getOccurrences() {
				return value.size();
			}

			public String getProperty(String key) {
				return value.get(key);
			}

			@Override
			public Object getValue() {
				return value;
			}
		}

		public SimpleBucket() {
			super(true);
		}

		@Override
		protected String getIndexFileName() {
			return "simple_bucket.index";
		}

		@Override
		protected String getVersionFileName() {
			return "simple_bucket.version";
		}

		@Override
		@SuppressWarnings("unchecked")
		protected Entry createEntry(IPath path, Object value) {
			return new SimpleEntry(path, (Map<String, String>) value);
		}

		@Override
		protected byte getVersion() {
			return 0;
		}

		@Override
		protected Object readEntryValue(DataInputStream source) throws IOException {
			int length = source.readUnsignedShort();
			Map<String, String> value = new HashMap<>(length);
			for (int j = 0; j < length; j++) {
				value.put(source.readUTF(), source.readUTF());
			}
			return value;
		}

		public void set(IPath path, String key, String value) {
			String pathAsString = path.toString();
			@SuppressWarnings("unchecked")
			Map<String, String> existing = (Map<String, String>) getEntryValue(pathAsString);
			if (existing == null) {
				if (value != null) {
					existing = new HashMap<>();
					existing.put(key, value);
					setEntryValue(pathAsString, existing);
				}
				return;
			}
			if (value == null) {
				existing.remove(key);
				if (existing.isEmpty()) {
					existing = null;
				}
			} else {
				existing.put(key, value);
			}
			setEntryValue(pathAsString, existing);
		}

		@Override
		protected void writeEntryValue(DataOutputStream destination, Object entryValue) throws IOException {
			@SuppressWarnings("unchecked")
			Map<String, String> value = (Map<String, String>) entryValue;
			int length = value.size();
			destination.writeShort(length);
			for (Map.Entry<String, String> entry : value.entrySet()) {
				destination.writeUTF(entry.getKey());
				destination.writeUTF(entry.getValue());
			}
		}
	}

	@Test
	public void testVisitor() throws CoreException {
		IPath baseLocation = getRandomLocation();
		fileStoreExtension.deleteOnTearDown(baseLocation);

		// keep the reference around - it is the same returned by tree.getCurrent()
		SimpleBucket bucket = new SimpleBucket();
		BucketTree tree = new BucketTree((Workspace) getWorkspace(), bucket);
		IProject proj1 = getWorkspace().getRoot().getProject("proj1");
		IProject proj2 = getWorkspace().getRoot().getProject("proj2");
		IFile file1 = proj1.getFile("file1.txt");
		IFolder folder1 = proj1.getFolder("folder1");
		IFile file2 = folder1.getFile("file2.txt");
		createInWorkspace(new IResource[] { file1, file2, proj2 });
		IPath[] paths = { IPath.ROOT, proj1.getFullPath(), file1.getFullPath(), folder1.getFullPath(),
				file2.getFullPath(), proj2.getFullPath() };
		for (IPath path : paths) {
			tree.loadBucketFor(path);
			bucket.set(path, "path", path.toString());
			bucket.set(path, "segments", Integer.toString(path.segmentCount()));
		}
		bucket.save();
		verify(tree, IPath.ROOT, BucketTree.DEPTH_ZERO, Set.of(IPath.ROOT));
		verify(tree, IPath.ROOT, BucketTree.DEPTH_ONE,
				Set.of(IPath.ROOT, proj1.getFullPath(), proj2.getFullPath()));
		verify(tree, IPath.ROOT, BucketTree.DEPTH_INFINITE, Set.of(IPath.ROOT, proj1.getFullPath(),
				file1.getFullPath(), folder1.getFullPath(), file2.getFullPath(), proj2.getFullPath()));
		verify(tree, proj1.getFullPath(), BucketTree.DEPTH_ZERO, Arrays.asList(proj1.getFullPath()));
		verify(tree, proj1.getFullPath(), BucketTree.DEPTH_ONE,
				Arrays.asList(proj1.getFullPath(), file1.getFullPath(), folder1.getFullPath()));
		verify(tree, proj1.getFullPath(), BucketTree.DEPTH_INFINITE,
				Arrays.asList(proj1.getFullPath(), file1.getFullPath(), folder1.getFullPath(), file2.getFullPath()));
		verify(tree, file1.getFullPath(), BucketTree.DEPTH_ZERO, Arrays.asList(file1.getFullPath()));
		verify(tree, file1.getFullPath(), BucketTree.DEPTH_ONE, Arrays.asList(file1.getFullPath()));
		verify(tree, file1.getFullPath(), BucketTree.DEPTH_INFINITE, Arrays.asList(file1.getFullPath()));
		verify(tree, folder1.getFullPath(), BucketTree.DEPTH_ZERO, Arrays.asList(folder1.getFullPath()));
		verify(tree, folder1.getFullPath(), BucketTree.DEPTH_ONE,
				Arrays.asList(folder1.getFullPath(), file2.getFullPath()));
		verify(tree, folder1.getFullPath(), BucketTree.DEPTH_INFINITE,
				Arrays.asList(folder1.getFullPath(), file2.getFullPath()));
		verify(tree, file2.getFullPath(), BucketTree.DEPTH_ZERO, Arrays.asList(file2.getFullPath()));
		verify(tree, file2.getFullPath(), BucketTree.DEPTH_ONE, Arrays.asList(file2.getFullPath()));
		verify(tree, file2.getFullPath(), BucketTree.DEPTH_INFINITE, Arrays.asList(file2.getFullPath()));
		verify(tree, proj2.getFullPath(), BucketTree.DEPTH_ZERO, Arrays.asList(proj2.getFullPath()));
		verify(tree, proj2.getFullPath(), BucketTree.DEPTH_ONE, Arrays.asList(proj2.getFullPath()));
		verify(tree, proj2.getFullPath(), BucketTree.DEPTH_INFINITE, Arrays.asList(proj2.getFullPath()));
	}

	public void verify(BucketTree tree, IPath root, int depth, final Collection<IPath> expected)
			throws CoreException {
		final Set<IPath> visited = new HashSet<>();
		SimpleBucket.Visitor verifier = new SimpleBucket.Visitor() {
			@Override
			public int visit(org.eclipse.core.internal.localstore.Bucket.Entry entry) {
				SimpleBucket.SimpleEntry simple = (SimpleBucket.SimpleEntry) entry;
				IPath path = simple.getPath();
				assertTrue(expected.contains(path), path.toString());
				visited.add(path);
				assertEquals(path.toString(), simple.getProperty("path"), path.toString());
				assertEquals(Integer.toString(path.segmentCount()), simple.getProperty("segments"), path.toString());
				return CONTINUE;
			}
		};
		tree.accept(verifier, root, depth);
		assertEquals(expected.size(), visited.size());
		for (IPath path : expected) {
			assertTrue(visited.contains(path), path.toString());
		}
	}

}
