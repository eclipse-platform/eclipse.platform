/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.jarprocessor;

import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * @author aniefer@ca.ibm.com
 *
 */
public class JarProcessor {
	private List steps = new ArrayList();
	private String workingDirectory = ""; //$NON-NLS-1$
	private int depth = -1;
	private boolean verbose = false;
	private boolean processAll = false;

	static public JarProcessor getUnpackProcessor(Properties properties) {
		if (!canPerformUnpack())
			throw new UnsupportedOperationException();
		JarProcessor processor = new JarProcessor();
		processor.addProcessStep(new UnpackStep(properties));
		return processor;
	}

	static public JarProcessor getPackProcessor(Properties properties) {
		if (!canPerformPack())
			throw new UnsupportedOperationException();
		JarProcessor processor = new JarProcessor();
		processor.addProcessStep(new PackStep(properties));
		return processor;
	}

	static public boolean canPerformPack() {
		return PackStep.canPack();
	}

	static public boolean canPerformUnpack() {
		return UnpackStep.canUnpack();
	}

	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public void setWorkingDirectory(String dir) {
		workingDirectory = dir;
	}
	
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	public void setProcessAll(boolean all){
		this.processAll = all;
	}

	public void addProcessStep(IProcessStep step) {
		steps.add(step);
	}

	public void clearProcessSteps() {
		steps.clear();
	}

	public void process(File input, FileFilter filter) throws FileNotFoundException {
		if (!input.exists())
			throw new FileNotFoundException();

		File[] files = null;
		if (input.isDirectory()) {
			files = input.listFiles();
		} else if (filter.accept(input)) {
			files = new File[] {input};
		}
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				String dir = getWorkingDirectory();
				setWorkingDirectory(dir + "/" + files[i].getName()); //$NON-NLS-1$
				process(files[i], filter);
				setWorkingDirectory(dir);
			} else if (filter.accept(files[i])) {
				try {
					processJar(files[i]);
				} catch (IOException e) {
					if(verbose)
						e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Recreate a jar file.  The replacements map specifies entry names to be replaced, the replacements are
	 * expected to be found in directory.
	 * 
	 * @param jar - The input jar
	 * @param outputJar - the output
	 * @param replacements - map of entryName -> new entryName
	 * @param directory - location to find file for new entryName
	 * @throws IOException
	 */
	private void recreateJar(JarFile jar, JarOutputStream outputJar, Map replacements, File directory, Properties inf) throws IOException {
		InputStream in = null;
		boolean marked = false;
		try {
			Enumeration entries = jar.entries();
			for (JarEntry entry = (JarEntry) entries.nextElement(); entry != null; entry = entries.hasMoreElements() ? (JarEntry) entries.nextElement() : null) {
				File replacement = null;
				JarEntry newEntry = null;
				if (replacements.containsKey(entry.getName())) {
					String name = (String) replacements.get(entry.getName());
					replacement = new File(directory, name);
					in = new BufferedInputStream(new FileInputStream(replacement));
					newEntry = new JarEntry(name);
				} else {
					in = new BufferedInputStream(jar.getInputStream(entry));
					newEntry = new JarEntry(entry.getName());
				}
				newEntry.setTime(entry.getTime());
				outputJar.putNextEntry(newEntry);
				if (entry.getName().equals(Utils.MARK_FILE_NAME)) {
					//The eclipse.inf file was read in earlier, don't need to reread it, just write it out now
					Utils.storeProperties(inf, outputJar);
					marked = true;
				} else {
					Utils.transferStreams(in, outputJar, false);
				}
				outputJar.closeEntry();
				in.close();

				//delete the nested jar file
				if (replacement != null) {
					replacement.delete();
				}
			}
			if (!marked) {
				JarEntry entry = new JarEntry(Utils.MARK_FILE_NAME);
				outputJar.putNextEntry(entry);
				Utils.storeProperties(inf, outputJar);
				outputJar.closeEntry();
			}
		} finally {
			Utils.close(outputJar);
			Utils.close(jar);
			Utils.close(in);
		}
	}

	private String recursionEffect(String entryName) {
		String result = null;
		for (Iterator iter = steps.iterator(); iter.hasNext();) {
			IProcessStep step = (IProcessStep) iter.next();

			result = step.recursionEffect(entryName);
			if (result != null)
				entryName = result;
		}
		return result;
	}

	private void extractEntries(JarFile jar, File tempDir, Map data) throws IOException {
		Enumeration entries = jar.entries();
		if (entries.hasMoreElements()) {
			for (JarEntry entry = (JarEntry) entries.nextElement(); entry != null; entry = entries.hasMoreElements() ? (JarEntry) entries.nextElement() : null) {
				String name = entry.getName();
				String newName = recursionEffect(name);
				if (newName != null) {
					if(verbose){
						for(int i = 0; i <= depth; i++)
							System.out.print("  "); //$NON-NLS-1$
						System.out.println("Processing nested file: " + name); //$NON-NLS-1$
					}
					//extract entry to temp directory
					File extracted = new File(tempDir, name);
					File parentDir = extracted.getParentFile();
					if (!parentDir.exists())
						parentDir.mkdirs();

					InputStream in = null;
					OutputStream out = null;
					try {
						in = jar.getInputStream(entry);
						out = new BufferedOutputStream(new FileOutputStream(extracted));
						Utils.transferStreams(in, out, true); //this will close both streams
					} finally {
						Utils.close(in);
						Utils.close(out);
					}
					extracted.setLastModified(entry.getTime());
					data.put(name, newName);

					//recurse
					String dir = getWorkingDirectory();
					setWorkingDirectory(parentDir.getCanonicalPath());
					processJar(extracted);
					setWorkingDirectory(dir);

					//delete the extracted item leaving the recursion result
					if (!name.equals(newName))
						extracted.delete();
				}
			}
		}
	}

	private File preProcess(File input, File tempDir) {
		File result = null;
		for (Iterator iter = steps.iterator(); iter.hasNext();) {
			IProcessStep step = (IProcessStep) iter.next();
			result = step.preProcess(input, tempDir);
			if (result != null)
				input = result;
		}
		return input;
	}

	private File postProcess(File input, File tempDir) {
		File result = null;
		for (Iterator iter = steps.iterator(); iter.hasNext();) {
			IProcessStep step = (IProcessStep) iter.next();
			result = step.postProcess(input, tempDir);
			if (result != null)
				input = result;
		}
		return input;
	}

	private void adjustInf(File input, Properties inf) {
		for (Iterator iter = steps.iterator(); iter.hasNext();) {
			IProcessStep step = (IProcessStep) iter.next();
			step.adjustInf(input, inf);
		}
	}

	public void processJar(File input) throws IOException {
		++depth;
		long lastModified = input.lastModified();
		File workingDir = new File(getWorkingDirectory());
		if (!workingDir.exists())
			workingDir.mkdirs();

		boolean skip = Utils.shouldSkipJar(input, processAll, verbose);
		if (depth == 0 && verbose) {
			if (skip)
				System.out.println("Skipping " + input.getPath()); //$NON-NLS-1$
			else {
				System.out.print("Running "); //$NON-NLS-1$ 
				for (Iterator iter = steps.iterator(); iter.hasNext();) {
					IProcessStep step = (IProcessStep) iter.next();
					System.out.print(step.getStepName() + " "); //$NON-NLS-1$
				}
				System.out.println("on " + input.getPath()); //$NON-NLS-1$
			}
		}

		if (skip) {
			//This jar was not marked as conditioned, and we are only processing conditioned jars, so do nothing
			--depth;
			return;
		}

		//pre
		File workingFile = preProcess(input, workingDir);

		//Extract entries from jar and recurse on them
		File tempDir = null;
		if (depth == 0) {
			tempDir = new File(workingDir, "temp." + workingFile.getName()); //$NON-NLS-1$
		} else {
			File parent = workingDir.getParentFile();
			tempDir = new File(parent, "temp_" + depth + '_' + workingFile.getName()); //$NON-NLS-1$
		}

		JarFile jar = new JarFile(workingFile, false);
		Map replacements = new HashMap();
		extractEntries(jar, tempDir, replacements);

		Properties inf = Utils.getEclipseInf(workingFile);
		if (inf != null)
			adjustInf(workingFile, inf);

		//Recreate the jar with replacements.  This also has the effect of normalizing the jar, so we want to do this even if
		//we aren't actually replacing anything
		File tempJar = null;
		tempJar = new File(tempDir, workingFile.getName());
		File parent = tempJar.getParentFile();
		if (!parent.exists())
			parent.mkdirs();
		JarOutputStream jarOut = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(tempJar)));
		recreateJar(jar, jarOut, replacements, tempDir, inf);

		jar.close();
		if (tempJar != null) {
			if (!workingFile.equals(input)) {
				workingFile.delete();
			}
			workingFile = tempJar;
		}

		//post
		File result = postProcess(workingFile, workingDir);
		if (!result.equals(workingFile) && !workingFile.equals(input))
			workingFile.delete();
		if (!result.getParentFile().equals(workingDir)) {
			File finalFile = new File(workingDir, result.getName());
			if (finalFile.exists())
				finalFile.delete();
			result.renameTo(finalFile);
		}

		if (tempDir.exists())
			Utils.clear(tempDir);

		result.setLastModified(lastModified);
		--depth;
	}
}
