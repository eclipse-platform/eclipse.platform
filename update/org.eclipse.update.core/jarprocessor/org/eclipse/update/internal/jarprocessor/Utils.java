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
import java.util.zip.ZipException;

/**
 * @author aniefer@ca.ibm.com
 *
 */
public class Utils {
	public static final String MARK_FILE_NAME = "META-INF/eclipse.inf"; //$NON-NLS-1$
	
	/*
	 * Properties found in outer pack.properties file
	 */
	//comma separated list of jars to exclude from sigining
	public static final String SIGN_EXCLUDES = "sign.excludes"; //$NON-NLS-1$
	//comma separated list of jars to exlclude from packing
	public static final String PACK_EXCLUDES = "pack.excludes"; //$NON-NLS-1$
	//Suffix used when specifying arguments to use when running pack200 on a jar
	public static final String PACK_ARGS_SUFFIX = ".pack.args"; //$NON-NLS-1$
	
	/*
	 * Properties found in both pack.properties and eclipse.inf
	 */
	//	Default arguments to use when running pack200.
	// Affects all jars when specified in pack.properties, affects children when specified in eclipse.inf
	public static final String DEFAULT_PACK_ARGS = "pack200.default.args"; //$NON-NLS-1$
	
	/*
	 * Properties found in eclipse.inf file
	 */
	//This jar has been conditioned with pack200
	public static final String MARK_PROPERTY = "pack200.conditioned"; //$NON-NLS-1$
	//Exclude this jar from processing
	public static final String MARK_EXCLUDE = "jarprocessor.exclude"; //$NON-NLS-1$
	//Exclude this jar from pack200
	public static final String MARK_EXCLUDE_PACK = "jarprocessor.exclude.pack"; //$NON-NLS-1$
	//Exclude this jar from signing
	public static final String MARK_EXCLUDE_SIGN = "jarprocessor.exclude.sign"; //$NON-NLS-1$
	//Exclude this jar's children from processing
	public static final String MARK_EXCLUDE_CHILDREN = "jarprocessor.exclude.children";
	//Exclude this jar's children from pack200
	public static final String MARK_EXCLUDE_CHILDREN_PACK = "jarprocessor.exclude.children.pack";
	//Exclude this jar's children from signing
	public static final String MARK_EXCLUDE_CHILDREN_SIGN = "jarprocessor.exclude.children.sign";
	//Arguments used in pack200 for this jar
	public static final String PACK_ARGS = "pack200.args"; //$NON-NLS-1$
	
	public static final String PACK200_PROPERTY = "org.eclipse.update.jarprocessor.pack200"; //$NON-NLS-1$
	public static final String JRE = "@jre"; //$NON-NLS-1$
	public static final String PATH = "@path"; //$NON-NLS-1$
	public static final String NONE = "@none"; //$NON-NLS-1$

	public static final String PACKED_SUFFIX = ".pack.gz"; //$NON-NLS-1$
	public static final String JAR_SUFFIX = ".jar"; //$NON-NLS-1$

	public static final FileFilter JAR_FILTER = new FileFilter() {
		public boolean accept(File pathname) {
			return pathname.isFile() && pathname.getName().endsWith(".jar"); //$NON-NLS-1$
		}
	};

	public static final FileFilter PACK_GZ_FILTER = new FileFilter() {
		public boolean accept(File pathname) {
			return pathname.isFile() && pathname.getName().endsWith(PACKED_SUFFIX);
		}
	};

	public static void close(Object stream) {
		if (stream != null) {
			try {
				if (stream instanceof InputStream)
					((InputStream) stream).close();
				else if (stream instanceof OutputStream)
					((OutputStream) stream).close();
				else if (stream instanceof JarFile)
					((JarFile) stream).close();
			} catch (IOException e) {
				//ignore
			}
		}
	}

	/**
	 * get the set of commands to try to execute pack/unpack 
	 * @param cmd, the command, either "pack200" or "unpack200"
	 * @return String [] or null
	 */
	public static String[] getPack200Commands(String cmd) {
		String[] locations = null;
		String prop = System.getProperty(PACK200_PROPERTY);
		String javaHome = System.getProperty("java.home"); //$NON-NLS-1$
		if (NONE.equals(prop)) {
			return null;
		} else if (JRE.equals(prop)) {
			locations = new String[] {javaHome + "/bin/" + cmd}; //$NON-NLS-1$
		} else if (PATH.equals(prop)) {
			locations = new String[] {cmd};
		} else if (prop == null) {
			locations = new String[] {javaHome + "/bin/" + cmd, cmd}; //$NON-NLS-1$ 
		} else {
			locations = new String[] {prop + "/" + cmd}; //$NON-NLS-1$
		}
		return locations;
	}

	/**
	 * Transfers all available bytes from the given input stream to the given
	 * output stream. Closes both streams if close == true, regardless of failure. 
	 * Flushes the destination stream if close == false
	 * 
	 * @param source
	 * @param destination
	 * @param close 
	 * @throws IOException
	 */
	public static void transferStreams(InputStream source, OutputStream destination, boolean close) throws IOException {
		source = new BufferedInputStream(source);
		destination = new BufferedOutputStream(destination);
		try {
			byte[] buffer = new byte[8192];
			while (true) {
				int bytesRead = -1;
				if ((bytesRead = source.read(buffer)) == -1)
					break;
				destination.write(buffer, 0, bytesRead);
			}
		} finally {
			if (close) {
				close(source);
				close(destination);
			} else {
				destination.flush();
			}
		}
	}

	/**
	 * Deletes all the files and directories from the given root down (inclusive).
	 * Returns false if we could not delete some file or an exception occurred
	 * at any point in the deletion.
	 * Even if an exception occurs, a best effort is made to continue deleting.
	 */
	public static boolean clear(java.io.File root) {
		boolean result = clearChildren(root);
		try {
			if (root.exists())
				result &= root.delete();
		} catch (Exception e) {
			result = false;
		}
		return result;
	}

	/**
	 * Deletes all the files and directories from the given root down, except for 
	 * the root itself.
	 * Returns false if we could not delete some file or an exception occurred
	 * at any point in the deletion.
	 * Even if an exception occurs, a best effort is made to continue deleting.
	 */
	public static boolean clearChildren(java.io.File root) {
		boolean result = true;
		if (root.isDirectory()) {
			String[] list = root.list();
			// for some unknown reason, list() can return null.  
			// Just skip the children If it does.
			if (list != null)
				for (int i = 0; i < list.length; i++)
					result &= clear(new java.io.File(root, list[i]));
		}
		return result;
	}

	public static Set getPackExclusions(Properties properties) {
		if (properties == null)
			return Collections.EMPTY_SET;

		String packExcludes = properties.getProperty(PACK_EXCLUDES);
		if (packExcludes != null) {
			String[] excludes = toStringArray(packExcludes, ","); //$NON-NLS-1$
			Set packExclusions = new HashSet();
			for (int i = 0; i < excludes.length; i++) {
				packExclusions.add(excludes[i]);
			}
			return packExclusions;
		}
		return Collections.EMPTY_SET;
	}

	public static Set getSignExclusions(Properties properties) {
		if (properties == null)
			return Collections.EMPTY_SET;
		String signExcludes = properties.getProperty(SIGN_EXCLUDES);
		if (signExcludes != null) {
			String[] excludes = toStringArray(signExcludes, ","); //$NON-NLS-1$
			Set signExclusions = new HashSet();
			for (int i = 0; i < excludes.length; i++) {
				signExclusions.add(excludes[i]);
			}
			return signExclusions;
		}
		return Collections.EMPTY_SET;
	}

	public static String concat(String[] array) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < array.length; i++) {
			if (i > 0)
				buffer.append(' ');
			buffer.append(array[i]);
		}
		return buffer.toString();
	}

	public static String[] toStringArray(String input, String separator) {
		StringTokenizer tokenizer = new StringTokenizer(input, separator);
		int count = tokenizer.countTokens();
		String[] result = new String[count];
		for (int i = 0; i < count; i++) {
			result[i] = tokenizer.nextToken().trim();
		}
		return result;
	}

	/**
	 * Get the properties from the eclipse.inf file from the given jar.  If the file is not a jar, null is returned.
	 * If the file is a jar, but does not contain an eclipse.inf file, an empty Properties object is returned.
	 * @param jarFile
	 * @return The eclipse.inf properties for the given jar file
	 */
	public static Properties getEclipseInf(File jarFile, boolean verbose) {
		if (jarFile == null || !jarFile.exists()) {
			if (verbose)
				System.out.println("Failed to obtain eclipse.inf due to missing jar file: " + jarFile);
			return null;
		}
		JarFile jar = null;
		try {
			jar = new JarFile(jarFile, false);
		} catch (ZipException e) {
			//not a jar, don't bother logging this.
			return null;
		} catch (IOException e) {
			if (verbose) {
				System.out.println("Failed to obtain eclipse.inf due to IOException: " + jarFile);
				e.printStackTrace();
			}
			return null;
		}
		try {
			JarEntry mark = jar.getJarEntry(MARK_FILE_NAME);
			if (mark != null) {
				InputStream in = jar.getInputStream(mark);
				Properties props = new Properties();
				props.load(in);
				in.close();
				return props;
			}
			return new Properties();
		} catch (IOException e) {
			if (verbose) {
				System.out.println("Failed to obtain eclipse.inf due to IOException: " + jarFile);
				e.printStackTrace();
			}
			return null;
		} finally {
			close(jar);
		}
	}

	public static boolean shouldSkipJar(File input, boolean processAll, boolean verbose) {
		Properties inf = getEclipseInf(input, verbose);
		if (inf == null) {
			//not a jar, could be a pack.gz
			return false;
		}
		String exclude = inf.getProperty(MARK_EXCLUDE);

		//was marked as exclude, we should skip
		if (exclude != null && Boolean.valueOf(exclude).booleanValue())
			return true;

		//process all was set, don't skip
		if (processAll)
			return false;

		//otherwise, we skip if not marked marked
		String marked = inf.getProperty(MARK_PROPERTY);
		return !Boolean.valueOf(marked).booleanValue();
	}

	/**
	 * Stores the given properties in the output stream.  We store the properties 
	 * in sorted order so that the signing hash doesn't change if the properties didn't change. 
	 * @param props
	 * @param stream
	 */
	public static void storeProperties(Properties props, OutputStream stream) {
		PrintStream printStream = new PrintStream(stream);
		printStream.print("#Processed using Jarprocessor\n"); //$NON-NLS-1$
		SortedMap sorted = new TreeMap(props);
		for (Iterator iter = sorted.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			printStream.print(key);
			printStream.print(" = "); //$NON-NLS-1$
			printStream.print(sorted.get(key));
			printStream.print("\n");

		}
		printStream.flush();
	}
}
