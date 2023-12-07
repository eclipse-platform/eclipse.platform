/*******************************************************************************
 *  Copyright (c) 2011, 2015 Broadcom Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     James Blackburn (Broadcom Corp.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.junit.Assert.assertThrows;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.function.Function;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.tests.internal.filesystem.wrapper.WrapperFileStore;
import org.eclipse.core.tests.internal.filesystem.wrapper.WrapperFileSystem;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * This tests that I/O Exception on OuptuStream#close() after IFile#setContents
 * is correctly reported.
 */
public class Bug_332543 extends ResourceTest {
	/**
	 * Wrapper FS which throws an IOException when someone closes an output
	 * stream...
	 */
	public static class IOErrOnCloseFileStore extends WrapperFileStore {
		public IOErrOnCloseFileStore(IFileStore store) {
			super(store);
		}

		@Override
		public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
			OutputStream os = super.openOutputStream(options, monitor);
			os = new BufferedOutputStream(os) {
				@Override
				public void close() throws java.io.IOException {
					// We close the output stream (so there aren't issues deleting the project
					// during tear-down)
					super.close();
					// But we also throw IOException as if the operation had failed.
					throw new IOException("Whoops I dunno how to close!");
				}
			};
			return os;
		}
	}

	@Override
	protected void tearDown() throws Exception {
		WrapperFileSystem.setCustomFileStore(null);
		super.tearDown();
	}

	public void testBugForByteArrayInputStream() throws Exception {
		testCancel(s -> s);
	}

	public void testBugForInputStream() throws Exception {
		testCancel(delegate -> new InputStream() { // Not ArrayInputStream
			@Override
			public int read() throws IOException {
				return delegate.read();
			}

			@Override
			public int read(byte b[], int off, int len) throws IOException {
				return delegate.read(b, off, len);
			}

		});
	}

	private void testCancel(Function<ByteArrayInputStream, InputStream> wrap) throws Exception {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		String proj_name = createUniqueString();
		IPath proj_loc = root.getLocation().append(proj_name);
		URI proj_uri = WrapperFileSystem.getWrappedURI(URIUtil.toURI(proj_loc));

		IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(proj_name);
		desc.setLocationURI(proj_uri);
		// Create the project on the wrapped file system
		IProject project = root.getProject(desc.getName());
		project.create(desc, createTestMonitor());

		// Create a file in the project
		IFile file = project.getFile("foo.txt");
		createInFileSystem(file);

		// Now open the project
		project.open(createTestMonitor());

		// Set our evil IOException on close() fs.
		WrapperFileSystem.setCustomFileStore(IOErrOnCloseFileStore.class);

		// Try #setContents on an existing file
		assertThrows(CoreException.class, () -> file.setContents(wrap.apply(new ByteArrayInputStream("Random".getBytes())),
				false, true, createTestMonitor()));

		// Try create on a non-existent file
		IFile nonExistentFile = project.getFile("foo1.txt");
		assertThrows(CoreException.class,
				() -> nonExistentFile.create(wrap.apply(new ByteArrayInputStream("Random".getBytes())), false,
						createTestMonitor()));
	}

}
