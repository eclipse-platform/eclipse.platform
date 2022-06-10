/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools.metadata;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tools.AbstractTreeContentProvider;
import org.eclipse.core.tools.TreeContentProviderNode;
import org.eclipse.jface.viewers.Viewer;

/**
 * A tree content provider for Metadata view. Its input is a metadata directory,
 * the root directory from which Eclipse metadata files
 * will be searched. The result is a tree containing directory that contains
 * (any directories that contain) metadata files and the metadata files
 * themselves.<br>
 * This content provider mantains its contents using
 * <code>TreeContentProviderNode</code>
 * objects.
 *
 * @see org.eclipse.core.tools.TreeContentProviderNode
 */
public class MetadataTreeContentProvider extends AbstractTreeContentProvider {
	/**
	 * The file filter.
	 *
	 * @see MetadataFileFilter
	 */
	private FileFilter fileFilter;

	/**
	 * The directory filter.
	 *
	 * @see DirectoryFilter
	 */
	private FileFilter directoryFilter;

	private MetadataTreeRebuilder treeRebuilder;

	/**
	 * Constructs a new content provider.
	 *
	 * @param registeredFileNames an array containing all metadata file names known
	 */
	public MetadataTreeContentProvider(String[] registeredFileNames) {
		super(true);
		this.fileFilter = new MetadataFileFilter(registeredFileNames);
		this.directoryFilter = File::isDirectory;
		this.treeRebuilder = new MetadataTreeRebuilder();
	}

	/**
	 * Returns true if the input is a <code>File</code> object pointing to
	 * a directory.
	 *
	 * @return true if this input object is a <code>File</code> pointing to a
	 * directory.
	 * @param input an input object
	 * @see org.eclipse.core.tools.AbstractTreeContentProvider#acceptInput(java.lang.Object)
	 */
	@Override
	protected boolean acceptInput(Object input) {
		return (input instanceof File) && ((File) input).isDirectory();
	}

	/**
	 * Updates the data model for this content provider upon the provided input.
	 *
	 * @param input a File object pointing to a metadata directory.
	 *
	 * @see org.eclipse.core.tools.AbstractTreeContentProvider#rebuild(Viewer, Object)
	 */
	@Override
	protected void rebuild(final Viewer viewer, final Object input) {
		final File metadataRootDir = (File) input;
		final TreeContentProviderNode metadataRootNode = makeNode(metadataRootDir);
		getRootNode().addChild(metadataRootNode);
		treeRebuilder.rebuild(viewer, metadataRootDir, metadataRootNode);
	}

	private class MetadataTreeRebuilder extends Job {
		private File rootDir;
		private TreeContentProviderNode rootNode;
		private Viewer viewer;

		public MetadataTreeRebuilder() {
			super("Updating metadata tree"); //$NON-NLS-1$
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				extractInfo(rootDir, rootNode, monitor);
				return Status.OK_STATUS;
			} finally {
				final Viewer tmpViewer = viewer;
				if (!tmpViewer.getControl().isDisposed())
					tmpViewer.getControl().getDisplay().asyncExec(() -> tmpViewer.refresh());
			}
		}

		public synchronized void rebuild(Viewer viewer, File rootDir, TreeContentProviderNode rootNode) {
			this.rootDir = rootDir;
			this.rootNode = rootNode;
			this.viewer = viewer;
			cancel();
			setName("Loading metadata tree from " + rootDir); //$NON-NLS-1$
			schedule();
		}
	}

	/**
	 * Builds this content provider data model from a given root directory. This
	 * method operates recursively, adding a tree node for each file of a registered
	 * type it finds and for each directory that contains (any directories that
	 * contain) a file of a registered type. This method returns a boolean value
	 * indicating that it (or at least one of its sub dirs) contains files with one
	 * of the registered types (so its parent will include it too).
	 *
	 * @param dir a directory potentially containing known metadata files.
	 * @param dirNode the node corresponding to that directory
	 * @return true if the provided dir (or at least one of its sub dirs)
	 * contains files with one of the registered types, false otherwise
	 */
	boolean extractInfo(File dir, TreeContentProviderNode dirNode, IProgressMonitor monitor) {

		if (monitor.isCanceled())
			return false;

		TreeContentProviderNode childNode;

		monitor.beginTask("Scanning dir " + dir, 100); //$NON-NLS-1$
		try {
			// looks for files of registered types in this directory
			File[] selectedFiles = dir.listFiles(fileFilter);
			monitor.worked(1);
			Arrays.sort(selectedFiles);
			for (File selectedFile : selectedFiles) {
				childNode = makeNode(selectedFile);
				dirNode.addChild(childNode);
			}
			// looks for files of registered types in its subdirectories
			File[] subDirs = dir.listFiles(directoryFilter);
			monitor.worked(1);
			Arrays.sort(subDirs);

			for (File subDir : subDirs) {
				// constructs a node for each subdir...
				childNode = makeNode(subDir);
				if (extractInfo(subDir, childNode, new SubProgressMonitor(monitor, 98 / subDirs.length)))
					// ...but only adds them if they have files of registered types
					dirNode.addChild(childNode);
			}
			// returns true if this dir has any file of any registered type
			return selectedFiles.length > 0 || dirNode.hasChildren();
		} finally {
			monitor.done();
		}
	}

	/**
	 * Helper method that creates a new TreeContentProviderNode object given a File
	 * object.
	 *
	 * @param file the file a node will created for
	 * @return a <code>TreeContentProviderNode</code>
	 */
	private TreeContentProviderNode makeNode(final File file) {
		return new TreeContentProviderNode(file.getName(), file) {
			// redefines toString so the root shows the full path while any other
			// node shows only the file / directory name
			@Override
			public String toString() {
				return isRoot() ? file.getAbsolutePath() : file.getName();
			}
		};
	}
}
