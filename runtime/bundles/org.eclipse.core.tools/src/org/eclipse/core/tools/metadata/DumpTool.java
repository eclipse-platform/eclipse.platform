/*******************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others.
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
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/**
 * A command-line interface for running dumpers on metadata files.
 * This application requires the name of the file to be dumped as its unique
 * argument.
 */
public class DumpTool implements IApplication {

	// list of files to dump
	String[] files = null;

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


	/**
	 * Dumps a given file using the associated dumper, sending its contents to the
	 * standard output.
	 *
	 * @param context Application Context
	 * @see DumperFactory#getDumper(String)
	 */
	@Override
	public Object start(IApplicationContext context) throws Exception {
		String fileName = System.getProperty("dump.file"); //$NON-NLS-1$
		if (fileName == null) {
			System.err.println("Use \"dump.file\" system property to point to the metadata file to be dumped"); //$NON-NLS-1$
			return Integer.valueOf(0);
		}

		File toDump = new File(fileName);
		if (!toDump.exists()) {
			System.err.println("File \"" + toDump.getAbsolutePath() + "\" does not exist"); //$NON-NLS-1$ //$NON-NLS-2$
			return Integer.valueOf(1);
		}

		// ready to parse
		DumperFactory factory = DumperFactory.getInstance();
		String[] registeredFileNames = factory.getRegisteredFileNames();
		this.fileFilter = new MetadataFileFilter(registeredFileNames);
		this.directoryFilter = File::isDirectory;
		System.out.println("DumpTool started...");
		System.out.println("Analyzing: "+fileName);

		if (toDump.isFile()) {
			files = new String[]{fileName};
		} else {
			files = extractFiles(fileName);
		}

		for (String file : files) {
			dump(file);
		}

		System.out.println("DumpTool finished...");
		return Integer.valueOf(0);
	}

	private void dump(String fileName) {
		IDumper dumper = null;
		try {
			dumper = DumperFactory.getInstance().getDumper(fileName);
		} catch (DumpException de) {
			System.err.println("Error: \n" + de); //$NON-NLS-1$
			return;
		}
		IDump dump = dumper.dump(new File(fileName));
		System.out.println("*****************************************************");
		System.out.print("Dump for file: "); //$NON-NLS-1$
		System.out.println(dump.getFile().getAbsolutePath());
		System.out.print("Contents: "); //$NON-NLS-1$
		System.out.println(dump.getContents());
		if (dump.isFailed()) {
			System.out.print("*** Dump failed. Reason: "); //$NON-NLS-1$
			System.out.print(dump.getFailureReason());
			dump.getFailureReason().printStackTrace();
		} else {
			System.out.print(">>> File is ok"); //$NON-NLS-1$
		}
		System.out.print(". Bytes read: "); //$NON-NLS-1$
		System.out.print(dump.getOffset());
		System.out.print(" / Total: "); //$NON-NLS-1$
		System.out.println(dump.getFile().length());
	}

	private String[] extractFiles(String directory) {
		List<String> fileNames = new ArrayList<>();
		extractInfo(new File(directory), fileNames, new NullProgressMonitor());

		String[] result = new String[fileNames.size()];
		if (!fileNames.isEmpty()){
			result = fileNames.toArray(new String[fileNames.size()]);
		}
		return result;
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
	void extractInfo(File dir, List<String> fileList, IProgressMonitor monitor) {

		if (monitor.isCanceled())
			return;

		monitor.beginTask("Scanning dir " + dir, 100); //$NON-NLS-1$
		try {
			// looks for files of registered types in this directory
			File[] selectedFiles = dir.listFiles(fileFilter);
			monitor.worked(1);
			Arrays.sort(selectedFiles);
			for (File selectedFile : selectedFiles) {
				fileList.add(selectedFile.getAbsolutePath());
			}
			// looks for files of registered types in its subdirectories
			File[] subDirs = dir.listFiles(directoryFilter);
			monitor.worked(1);
			Arrays.sort(subDirs);

			for (File subDir : subDirs) {
				// Recursive call
				extractInfo(subDir, fileList, new SubProgressMonitor(monitor, 98 / subDirs.length));
			}
		} finally {
			monitor.done();
		}
	}

	@Override
	public void stop() {
		// Does not do anything
	}
}
