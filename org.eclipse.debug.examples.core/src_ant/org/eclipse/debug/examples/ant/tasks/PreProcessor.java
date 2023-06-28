/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.ant.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;

/**
 * Java preprocessor for code examples. Used to export source code for
 * example plug-ins with parts of code missing/inserted etc., for
 * various exercises.
 * <p>
 * The preprocessor looks for #ifdef statements in java comments, and is
 * run with a set of symbols. For example:
 * <pre>
 * //#ifdef ex1
 * ... code to insert when 'ex1' symbol is on
 * //#else
 * ... code to insert when not 'ex1'
 * //#endif
 * </pre>
 * </p>
 */
public class PreProcessor extends Task {

	private Vector<FileSet> fFileSets = new Vector<>();
	private File fDestDir = null;
	private Set<String> fSymbols = new HashSet<>();
	private FileUtils fUtils = FileUtils.getFileUtils();

	// possible states
	private static final int STATE_OUTSIDE_CONDITION = 0;
	private static final int STATE_TRUE_CONDITION = 1;
	private static final int STATE_FALSE_CONDITION = 2;
	private static final int STATE_POST_TRUE_CONDITION = 3;

	// matchers
	private Matcher IF_DEF_MATCHER = Pattern.compile("#ifdef\\s+\\w+").matcher(""); //$NON-NLS-1$ //$NON-NLS-2$
	private Matcher ELSE_IF_MATCHER = Pattern.compile("#elseif\\s+\\w+").matcher(""); //$NON-NLS-1$ //$NON-NLS-2$
	private Matcher ELSE_MATCHER = Pattern.compile("#else$|#else\\W+").matcher(""); //$NON-NLS-1$ //$NON-NLS-2$
	private Matcher END_MATCHER = Pattern.compile("#endif").matcher(""); //$NON-NLS-1$ //$NON-NLS-2$


	/**
	 * Constructs a new preprocessor task
	 */
	public PreProcessor() {
	}

	/**
	 * Adds a set of files to process.
	 *
	 * @param set a set of files to process
	 */
	public void addFileset(FileSet set) {
		fFileSets.addElement(set);
	}

	/**
	 * Sets the destination directory for processed files.
	 *
	 * @param destDir destination directory for processed files
	 */
	public void setDestdir(File destDir) {
		fDestDir = destDir;
	}

	/**
	 * Sets the symbols that are "on" for the preprocessing.
	 *
	 * @param symbols symbols that are "on" for the preprocessing
	 */
	public void setSymbols(String symbols) {
		String[] strings = symbols.split(","); //$NON-NLS-1$
		for (int i = 0; i < strings.length; i++) {
			String string = strings[i].trim();
			if (string.length() > 0) {
				fSymbols.add(string);
			}
		}
	}

	@Override
	public void execute() throws BuildException {
		if (fSymbols.size() == 0) {
			throw new BuildException("No symbols specified for preprocessor"); //$NON-NLS-1$
		}
		if (fFileSets.isEmpty()) {
			throw new BuildException("No filesets specified for processing"); //$NON-NLS-1$
		}
		if (!fDestDir.exists()) {
			throw new BuildException("destdir does not exist: " + fDestDir.getAbsolutePath()); //$NON-NLS-1$
		}
		StringBuilder buf = new StringBuilder("Symbols: "); //$NON-NLS-1$
		String[] symbols = fSymbols.toArray(new String[fSymbols.size()]);
		for (int i = 0; i < symbols.length; i++) {
			String symbol = symbols[i];
			buf.append(symbol);
			if(i < (symbols.length -1)) {
				buf.append(", "); //$NON-NLS-1$
			}
		}
		log(buf.toString());

		for (FileSet fileSet : fFileSets) {
			DirectoryScanner scanner = fileSet.getDirectoryScanner(getProject());
			String[] includedFiles = scanner.getIncludedFiles();
			File baseDir = fileSet.getDir(getProject());
			for (int i = 0; i < includedFiles.length; i++) {
				String fileName = includedFiles[i];
				processFile(baseDir, fileName, fDestDir);
			}
		}
	}

	/**
	 * Process the file
	 * @param baseDir base directory source file is relative to
	 * @param fileName source file name
	 * @param destDir root destination directory
	 */
	private void processFile(File baseDir, String fileName, File destDir) throws BuildException {
		File destFile = new File(destDir, fileName);
		File srcFile = new File(baseDir, fileName);
		File dir = destFile.getParentFile();
		if (!dir.exists()) {
			dir.mkdirs();
		}
		String contents = null;
		if (fileName.endsWith(".java")) { //$NON-NLS-1$
			contents = preProcessFile(srcFile, "//#"); //$NON-NLS-1$
		} else if (fileName.equals("plugin.xml")) { //$NON-NLS-1$
			contents = preProcessFile(srcFile, null);
		}
		if (contents == null) {
			// no change, just copy file
			try {
				fUtils.copyFile(srcFile, destFile);
			} catch (IOException e) {
				throw new BuildException(e);
			}
		} else {
			// write new file
			try (FileWriter writer = new FileWriter(destFile)) {
				writer.write(contents);
			} catch (IOException e) {
				throw new BuildException(e);
			}
		}
	}

	/**
	 * Pre-processes a file
	 *
	 * @param srcFile the file to process
	 * @param strip chars to strip off lines in a true condition, or <code>null</code>
	 * @return
	 */
	public String preProcessFile(File srcFile, String strip) {
		try {
			boolean changed = false;
			StringBuilder buffer = new StringBuilder();
			try (FileReader fileReader = new FileReader(srcFile); BufferedReader reader = new BufferedReader(fileReader)) {
				String line = reader.readLine();
				String activeSymbol = null;
				int state = STATE_OUTSIDE_CONDITION;
				while (line != null) {
					boolean ifdef = IF_DEF_MATCHER.reset(line).find();
					boolean elseif = ELSE_IF_MATCHER.reset(line).find();
					boolean elze = ELSE_MATCHER.reset(line).find();
					boolean endif = END_MATCHER.reset(line).find();
					boolean commandLine = ifdef || elseif || elze || endif;
					boolean written = false;
					switch (state) {
						case STATE_OUTSIDE_CONDITION:
							if (ifdef) {
								String condition = line.substring(IF_DEF_MATCHER.start(), IF_DEF_MATCHER.end());
								String[] strings = condition.split("\\s+"); //$NON-NLS-1$
								activeSymbol = strings[1].trim();
								if (fSymbols.contains(activeSymbol)) {
									state = STATE_TRUE_CONDITION;
								} else {
									state = STATE_FALSE_CONDITION;
								}
							} else if (elseif) {
								throw new BuildException("#elseif encountered without corresponding #ifdef"); //$NON-NLS-1$
							} else if (elze) {
								throw new BuildException("#else encountered without corresponding #ifdef (" + srcFile.getPath() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
							} else if (endif) {
								throw new BuildException("#endif encountered without corresponding #ifdef"); //$NON-NLS-1$
							}
							break;
						case STATE_TRUE_CONDITION:
							if (elze || elseif) {
								state = STATE_POST_TRUE_CONDITION;
								break;
							} else if (endif) {
								state = STATE_OUTSIDE_CONDITION;
								break;
							} else if (ifdef) {
								throw new BuildException("illegal nested #ifdef"); //$NON-NLS-1$
							}
							break;
						case STATE_FALSE_CONDITION:
							if (elseif) {
								String condition = line.substring(ELSE_IF_MATCHER.start(), ELSE_IF_MATCHER.end());
								String[] strings = condition.split("\\s+"); //$NON-NLS-1$
								activeSymbol = strings[1].trim();
								if (fSymbols.contains(activeSymbol)) {
									state = STATE_TRUE_CONDITION;
								} else {
									state = STATE_FALSE_CONDITION;
								}
							} else if (elze) {
								state = STATE_TRUE_CONDITION;
								break;
							} else if (endif) {
								state = STATE_OUTSIDE_CONDITION;
								break;
							} else if (ifdef) {
								throw new BuildException("illegal nested #ifdef"); //$NON-NLS-1$
							}
							break;
						case STATE_POST_TRUE_CONDITION:
							if (endif) {
								state = STATE_OUTSIDE_CONDITION;
								break;
							} else if (ifdef) {
								throw new BuildException("illegal nested #ifdef"); //$NON-NLS-1$
							}
							break;
						default:
							break;
					}
					if (!commandLine) {
						if (state == STATE_OUTSIDE_CONDITION || state == STATE_TRUE_CONDITION) {
							if (state == STATE_TRUE_CONDITION && strip != null) {
								if (line.startsWith(strip)) {
									line = line.substring(strip.length());
								}
							}
							buffer.append(line);
							buffer.append("\n"); //$NON-NLS-1$
							written = true;
						}
					}
					changed = changed || !written;
					line = reader.readLine();
				}
			}
			if (!changed) {
				return null;
			}
			return buffer.toString();
		} catch (IOException e) {
			throw new BuildException(e);
		}
	}

	public static void main(String[] args) {
		PreProcessor processor = new PreProcessor();
		processor.setSymbols("ex2"); //$NON-NLS-1$
		String string = processor.preProcessFile(new File("c:\\eclipse3.1\\dev\\example.debug.core\\src\\example\\debug\\core\\launcher\\PDALaunchDelegate.java"), "//#"); //$NON-NLS-1$ //$NON-NLS-2$
		//String string = processor.preProcessFile(new File("c:\\eclipse3.1\\dev\\example.debug.core\\plugin.xml"), null);
		System.out.println(string);
	}
}
