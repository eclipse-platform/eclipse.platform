/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
	private LinkedList containingInfs = new LinkedList();

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
		if(dir != null)
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
					if (name != null) {
						if (replacement.exists()) {
							try {
								in = new BufferedInputStream(new FileInputStream(replacement));
								newEntry = new JarEntry(name);
							} catch (Exception e) {
								if (verbose) {
									e.printStackTrace();
									System.out.println("Warning: Problem reading " +replacement.getPath() + ", using " + jar.getName() + File.separator + entry.getName()  + " instead.");
								}
							}
						} else if (verbose) {
							System.out.println("Warning: " + replacement.getPath() + " not found, using " + jar.getName() + File.separator + entry.getName() + " instead.");	
						}
					}
				}
				if (newEntry == null) {
					try {
						in = new BufferedInputStream(jar.getInputStream(entry));
						newEntry = new JarEntry(entry.getName());
					} catch( Exception e ) {
						if(verbose) {
							e.printStackTrace();
							System.out.println("ERROR: problem reading " + entry.getName() + " from " + jar.getName());
						}
						continue;
					}
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

	private void extractEntries(JarFile jar, File tempDir, Map data, Properties inf) throws IOException {
		if(inf != null ) {
			//skip if excluding children
			if(inf.containsKey(Utils.MARK_EXCLUDE_CHILDREN)){
				String excludeChildren = inf.getProperty(Utils.MARK_EXCLUDE_CHILDREN);
				if( Boolean.valueOf(excludeChildren).booleanValue() )
					if(verbose){
						for(int i = 0; i <= depth; i++)
							System.out.print("  "); //$NON-NLS-1$
						System.out.println("Children of " + jar.getName() + "are excluded from processing.");
					}
					return;
			}
		}
		
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

					//recurse
					containingInfs.addFirst(inf);
					String dir = getWorkingDirectory();
					setWorkingDirectory(parentDir.getCanonicalPath());
					File result = processJar(extracted);

					newName = name.substring(0, name.length() - extracted.getName().length()) + result.getName();
					data.put(name, newName);

					setWorkingDirectory(dir);
					containingInfs.removeFirst();

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
			result = step.preProcess(input, tempDir, containingInfs);
			if (result != null)
				input = result;
		}
		return input;
	}

	private File postProcess(File input, File tempDir) {
		File result = null;
		for (Iterator iter = steps.iterator(); iter.hasNext();) {
			IProcessStep step = (IProcessStep) iter.next();
			result = step.postProcess(input, tempDir, containingInfs);
			if (result != null)
				input = result;
		}
		return input;
	}

	private void adjustInf(File input, Properties inf) {
		for (Iterator iter = steps.iterator(); iter.hasNext();) {
			IProcessStep step = (IProcessStep) iter.next();
			step.adjustInf(input, inf, containingInfs);
		}
	}

	public File processJar(File input) throws IOException {
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
			return input;
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
		Properties inf = Utils.getEclipseInf(workingFile, verbose);
		extractEntries(jar, tempDir, replacements, inf);

		if (inf != null)
			adjustInf(workingFile, inf);

		//Recreate the jar with replacements. 
		//TODO: This is not strictly necessary if we didn't change the inf file and didn't change any content
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
		
		//have to normalize after the post steps
		normalize(result, workingDir);
		
		if (!result.equals(workingFile) && !workingFile.equals(input))
			workingFile.delete();
		if (!result.getParentFile().equals(workingDir)) {
			File finalFile = new File(workingDir, result.getName());
			if (finalFile.exists())
				finalFile.delete();
			result.renameTo(finalFile);
			result = finalFile;
		}

		if (tempDir.exists())
			Utils.clear(tempDir);

		result.setLastModified(lastModified);
		--depth;
		return result;
	}
	
	private void normalize(File input, File workingDirectory) {
		if(input.getName().endsWith(Utils.PACKED_SUFFIX)) {
			//not a jar
			return;
		}
		try {
			File tempJar = new File(workingDirectory, "temp_" + input.getName()); //$NON-NLS-1$
			JarFile jar = null;
			try {
				jar = new JarFile(input, false);
			} catch (JarException e) {
				//not a jar
				return ;
			}
			JarOutputStream jarOut = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(tempJar)));
			InputStream in = null;
			try {
				Enumeration entries = jar.entries();
				for (JarEntry entry = (JarEntry) entries.nextElement(); entry != null; entry = entries.hasMoreElements() ? (JarEntry) entries.nextElement() : null) {
					JarEntry newEntry = new JarEntry(entry.getName());
					newEntry.setTime(entry.getTime());
					in = new BufferedInputStream(jar.getInputStream(entry));
					jarOut.putNextEntry(newEntry);
					Utils.transferStreams(in, jarOut, false);
					jarOut.closeEntry();
					in.close();
				}
			} finally {
				Utils.close(jarOut);
				Utils.close(jar);
				Utils.close(in);
			}
			tempJar.setLastModified(input.lastModified());
			input.delete();
			tempJar.renameTo(input);
		} catch (IOException e) {
			if (verbose) {
				System.out.println("Error normalizing jar " + input.getName()); //$NON-NLS-1$
				e.printStackTrace();
			}
		}
	}
}
