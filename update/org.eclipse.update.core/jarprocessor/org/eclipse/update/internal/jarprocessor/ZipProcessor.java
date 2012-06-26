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
import java.util.zip.*;

/**
 * @author aniefer@ca.ibm.com
 *
 */
public class ZipProcessor {

	private IProcessStep signStep = null;
	private IProcessStep packStep = null;
	private IProcessStep packUnpackStep = null;
	private IProcessStep unpackStep = null;

	private String workingDirectory = null;
	private Properties properties = null;
	private Set packExclusions = null;
	private Set signExclusions = null;
	private String command = null;
	private boolean packing = false;
	private boolean signing = false;
	private boolean repacking = false;
	private boolean unpacking = false;
	private boolean verbose = false;
	private boolean processAll = false;

	public void setWorkingDirectory(String dir) {
		workingDirectory = dir;
	}

	public String getWorkingDirectory() {
		if (workingDirectory == null)
			workingDirectory = "."; //$NON-NLS-1$
		return workingDirectory;
	}

	public void setSignCommand(String command) {
		this.command = command;
		this.signing = (command != null);
	}

	public void setPack(boolean pack) {
		this.packing = pack;
	}

	public void setRepack(boolean repack) {
		this.repacking = repack;
	}

	public void setUnpack(boolean unpack) {
		this.unpacking = unpack;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void setProcessAll(boolean all) {
		this.processAll = all;
	}

	public void processZip(File zipFile) throws ZipException, IOException {
		if (verbose)
			System.out.println("Processing " + zipFile.getPath()); //$NON-NLS-1$
		ZipFile zip = new ZipFile(zipFile);
		initialize(zip);

		String extension = unpacking ? "pack.gz" : ".jar"; //$NON-NLS-1$ //$NON-NLS-2$
		File tempDir = new File(getWorkingDirectory(), "temp_" + zipFile.getName()); //$NON-NLS-1$
		JarProcessor processor = new JarProcessor();
		processor.setVerbose(verbose);
		processor.setProcessAll(processAll);
		processor.setWorkingDirectory(tempDir.getCanonicalPath());
		if (unpacking) {
			processor.addProcessStep(unpackStep);
		}

		File outputFile = new File(getWorkingDirectory(), zipFile.getName() + ".temp"); //$NON-NLS-1$
		File parent = outputFile.getParentFile();
		if (!parent.exists())
			parent.mkdirs();
		ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outputFile));
		Enumeration entries = zip.entries();
		if (entries.hasMoreElements()) {
			for (ZipEntry entry = (ZipEntry) entries.nextElement(); entry != null; entry = entries.hasMoreElements() ? (ZipEntry) entries.nextElement() : null) {
				String name = entry.getName();

				InputStream entryStream = zip.getInputStream(entry);

				boolean pack = packing && !packExclusions.contains(name);
				boolean sign = signing && !signExclusions.contains(name);
				boolean repack = repacking && !packExclusions.contains(name);

				File extractedFile = null;

				if (entry.getName().endsWith(extension) && (pack || sign || repack || unpacking)) {
					extractedFile = new File(tempDir, name);
					parent = extractedFile.getParentFile();
					if (!parent.exists())
						parent.mkdirs();
					if (verbose)
						System.out.println("Extracting " + entry.getName()); //$NON-NLS-1$
					FileOutputStream extracted = new FileOutputStream(extractedFile);
					Utils.transferStreams(entryStream, extracted, true); // this will close the stream
					entryStream = null;

					boolean skip = Utils.shouldSkipJar(extractedFile, processAll, verbose);
					if (skip) {
						//skipping this file 
						entryStream = new FileInputStream(extractedFile);
						if (verbose)
							System.out.println(entry.getName() + " is not marked, skipping."); //$NON-NLS-1$
					} else {
						if (unpacking) {
							File result = processor.processJar(extractedFile);
							name = name.substring(0, name.length() - extractedFile.getName().length()) + result.getName();
							extractedFile = result;
						} else {
							if (repack || sign) {
								processor.clearProcessSteps();
								if (repack)
									processor.addProcessStep(packUnpackStep);
								if (sign)
									processor.addProcessStep(signStep);
								extractedFile = processor.processJar(extractedFile);
							}
							if (pack) {
								processor.clearProcessSteps();
								processor.addProcessStep(packStep);
								File modifiedFile = processor.processJar(extractedFile);
								if (modifiedFile.exists()) {
									try {
										String newName = name.substring(0, name.length() - extractedFile.getName().length()) + modifiedFile.getName();
										if (verbose) {
											System.out.println("Adding " + newName + " to " + outputFile.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
											System.out.println();
										}
										ZipEntry zipEntry = new ZipEntry(newName);
										entryStream = new FileInputStream(modifiedFile);
										zipOut.putNextEntry(zipEntry);
										Utils.transferStreams(entryStream, zipOut, false); //we want to keep zipOut open
										entryStream.close();
										Utils.clear(modifiedFile);
									} catch (IOException e) {
										Utils.close(entryStream);
										if (verbose) {
											e.printStackTrace();
											System.out.println("Warning: Problem reading " + modifiedFile.getPath() + ".");
										}
									}
									entryStream = null;
								} else if (verbose) {
									System.out.println("Warning: " + modifiedFile.getPath() + " not found.");
								}
							}
						}
						if (extractedFile.exists()) {
							try {
								entryStream = new FileInputStream(extractedFile);
							} catch (IOException e) {
								if (verbose) {
									e.printStackTrace();
									System.out.println("Warning: Problem reading " + extractedFile.getPath() + ".");
								}
							}
						}

						if (verbose && entryStream != null) {
							System.out.println("Adding " + name + " to " + outputFile.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
				}
				if (entryStream != null) {
					ZipEntry newEntry = new ZipEntry(name);
					try {
						zipOut.putNextEntry(newEntry);
						Utils.transferStreams(entryStream, zipOut, false);
						zipOut.closeEntry();
					} catch (ZipException e) {
						if(verbose) {
							System.out.println("Warning: " + name + " already exists in " + outputFile.getName() + ".  Skipping.");
						}
					}
					entryStream.close();
				}

				if (extractedFile != null)
					Utils.clear(extractedFile);
				
				if (verbose) {
					System.out.println();
					System.out.println("Processing " + zipFile.getPath()); //$NON-NLS-1$
				}
			}
		}
		zipOut.close();
		zip.close();

		File finalFile = new File(getWorkingDirectory(), zipFile.getName());
		if (finalFile.exists())
			finalFile.delete();
		outputFile.renameTo(finalFile);
		Utils.clear(tempDir);
	}

	private void initialize(ZipFile zip) {
		ZipEntry entry = zip.getEntry("pack.properties"); //$NON-NLS-1$
		properties = new Properties();
		if (entry != null) {
			InputStream stream = null;
			try {
				stream = zip.getInputStream(entry);
				properties.load(stream);
			} catch (IOException e) {
				if (verbose)
					e.printStackTrace();
			} finally {
				Utils.close(stream);
			}
		}

		packExclusions = Utils.getPackExclusions(properties);
		signExclusions = Utils.getSignExclusions(properties);

		packUnpackStep = new PackUnpackStep(properties, verbose);
		packStep = new PackStep(properties, verbose);
		signStep = new SignCommandStep(properties, command, verbose);
		unpackStep = new UnpackStep(properties, verbose);
	}
}
