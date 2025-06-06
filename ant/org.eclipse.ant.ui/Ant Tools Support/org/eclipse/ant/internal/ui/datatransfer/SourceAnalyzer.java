/*******************************************************************************
 * Copyright (c) 2005, 2013 Richard Hoefter and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Richard Hoefter (richard.hoefter@web.de) - initial API and implementation, bug 313386
 *     IBM Corporation - incorporating into Eclipse
 *******************************************************************************/

package org.eclipse.ant.internal.ui.datatransfer;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IConstantPoolEntry;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * Provides a method to analyze sources if it is possible to export projects to an Ant buildfile which compiles correctly.
 */
public class SourceAnalyzer {

	/**
	 * Utility class.
	 */
	private SourceAnalyzer() {

	}

	/**
	 * Check if source directories of project has cycles or if there are dependencies between them that are not conform with classpath order.
	 *
	 * <p>
	 * NOTE: Unused references in classes are not considered if they cause cycles or classpath order problems. This is because this class analyzes the
	 * bytecode and indeed the compiler throws unused references away.
	 */
	public static void checkCycles(IJavaProject currentProject, EclipseClasspath classpath, Shell shell) {
		StringBuffer message = new StringBuffer();
		Map<String, String> src2dir = new TreeMap<>(); // map string to string
		Map<String, Set<String>> srcdir2classes = new TreeMap<>(); // map string to Set of strings
		determineSources(currentProject, classpath, src2dir, srcdir2classes);
		Map<String, Set<String>> srcdir2sourcedirs = determineRequiredSrcDirs(src2dir, srcdir2classes);
		String projectName = currentProject.getProject().getName();

		List<String> cycle = new ArrayList<>();
		if (isCyclic(srcdir2sourcedirs, cycle)) {
			showCycleWarning(projectName, shell, cycle, message);
			return;
		}

		checkBuildOrder(classpath, projectName, shell, srcdir2sourcedirs);
	}

	/**
	 * Determine all sources belonging to a source directory.
	 */
	private static void determineSources(IJavaProject currentProject, EclipseClasspath classpath, Map<String, String> src2dir, Map<String, Set<String>> srcdir2classes) {
		for (int i = 0; i < classpath.srcDirs.size(); i++) {
			String srcDir = classpath.srcDirs.get(i);
			String classDir = classpath.classDirs.get(i);
			if (EclipseClasspath.isReference(srcDir)) {
				continue;
			}
			File dir;
			if (srcDir.equals(".")) { //$NON-NLS-1$
				dir = currentProject.getResource().getLocation().toFile();
			} else {
				IFile file = currentProject.getProject().getFile(srcDir);
				dir = file.getLocation().toFile();
			}
			if (EclipseClasspath.isLinkedResource(srcDir)) {
				String link = classpath.resolveLinkedResource(srcDir);
				dir = new File(link);
				if (!dir.isAbsolute()) {
					// make absolute
					dir = new File(ExportUtil.getProjectRoot(currentProject), link);
				}
			}
			// find all required classfiles for each source directory
			for (String srcFile : findFiles(dir, ".java")) { //$NON-NLS-1$
				src2dir.put(srcFile, srcDir);
				IFile classFile = currentProject.getProject().getFile(classDir + '/' + srcFile + ".class"); //$NON-NLS-1$
				if (!classFile.exists()) {
					// project was not compiled, check not possible
					continue;
				}
				Set<String> classes = srcdir2classes.get(srcDir);
				if (classes == null) {
					classes = new TreeSet<>();
				}
				classes.addAll(getRequiredClasses(classFile));
				srcdir2classes.put(srcDir, classes);
			}
		}
	}

	/**
	 * Determine for each source directory which other source directories it requires.
	 *
	 * @return Map string to Set of strings. (Maps source dir to Set of required source dirs.)
	 */
	private static Map<String, Set<String>> determineRequiredSrcDirs(Map<String, String> src2dir, Map<String, Set<String>> srcdir2classes) {
		Map<String, Set<String>> srcdir2sourcedirs = new TreeMap<>(); // map string to Set of strings
		for (String srcDir : srcdir2classes.keySet()) {
			for (String classname : srcdir2classes.get(srcDir)) {
				String classsrc = src2dir.get(classname);
				// don't add reference to itself
				if (classsrc != null && !classsrc.equals(srcDir)) {
					Set<String> sourcedirs = srcdir2sourcedirs.get(srcDir);
					if (sourcedirs == null) {
						sourcedirs = new TreeSet<>();
					}
					sourcedirs.add(classsrc);
					srcdir2sourcedirs.put(srcDir, sourcedirs);
				}
			}
		}
		return srcdir2sourcedirs;
	}

	private static void showCycleWarning(String projectName, Shell shell, List<String> cycle, StringBuffer message) {

		String m = MessageFormat.format(DataTransferMessages.SourceAnalyzer_0, projectName);
		message.append(m);
		message.append(ExportUtil.NEWLINE);

		// print cycle path
		for (String s : cycle) {
			s = EclipseClasspath.getLinkedResourceName(s);
			message.append(s);
			message.append(" -> "); //$NON-NLS-1$
		}
		message.append(EclipseClasspath.getLinkedResourceName(cycle.get(0)));
		MessageDialog.openWarning(shell, DataTransferMessages.SourceAnalyzer_1, message.toString());
	}

	/**
	 * Check if build order is correct.
	 */
	private static void checkBuildOrder(EclipseClasspath classpath, String projectName, Shell shell, Map<String, Set<String>> srcdir2sourcedirs) {
		for (String srcdir : srcdir2sourcedirs.keySet()) {
			int classpathIndex = classpath.srcDirs.indexOf(srcdir);
			for (String requiredSrc : srcdir2sourcedirs.get(srcdir)) {
				int i = classpath.srcDirs.indexOf(requiredSrc);
				if (i > classpathIndex) {
					String s = MessageFormat.format(DataTransferMessages.SourceAnalyzer_3, projectName);
					MessageDialog.openWarning(shell, DataTransferMessages.SourceAnalyzer_2, s + ExportUtil.NEWLINE + requiredSrc + " <-> " + srcdir //$NON-NLS-1$
							+ ExportUtil.NEWLINE);
					break;
				}
			}
		}
	}

	/**
	 * Find all classes that are required by given class file.
	 *
	 * @param file
	 *            a ".class" file
	 * @return set of strings, each contains a full qualified class name (forward slash as package separator)
	 */
	public static Set<String> getRequiredClasses(IFile file) {
		Set<String> classes = new TreeSet<>();
		IClassFile classFile = JavaCore.createClassFileFrom(file);
		IClassFileReader reader = ToolFactory.createDefaultClassFileReader(classFile, IClassFileReader.CONSTANT_POOL);
		if (reader == null) {
			// class not compiled
			return classes;
		}
		IConstantPool pool = reader.getConstantPool();
		for (int i = 0; i < pool.getConstantPoolCount(); i++) {
			if (pool.getEntryKind(i) == IConstantPoolConstant.CONSTANT_Class) {
				IConstantPoolEntry entry = pool.decodeEntry(i);
				String classname = new String(entry.getClassInfoName());
				// don't return inner classes
				int index = classname.indexOf('$');
				if (index != -1) {
					classname = classname.substring(0, index);
				}
				classes.add(classname);
			}
		}
		return classes;
	}

	/**
	 * Find all files with particular extension under given directory.
	 *
	 * @param dir
	 *            directory to start search
	 * @param extension
	 *            extension to search
	 * @return filenames relative to directory (without extension and with forward slashes)
	 */
	public static Set<String> findFiles(File dir, String extension) {
		Set<String> visited = new TreeSet<>();
		findFiles(dir, dir, extension, visited);
		return visited;
	}

	private static void findFiles(File base, File dir, String extension, Set<String> visited) {
		if (dir.isDirectory()) {
			for (File element : dir.listFiles()) {
				findFiles(base, element, extension, visited);
			}
		} else if (dir.getAbsolutePath().endsWith(extension)) {
			// remove base directory
			String filename = ExportUtil.removePrefixAndSuffix(dir.getAbsolutePath(), base.getAbsolutePath() + File.separator, extension);
			visited.add(filename.replace('\\', '/'));
		}
	}

	/**
	 * Check if given graph that is described through a map is cyclic.
	 *
	 * @param srcdir2sourcedirs
	 *            Maps string to set of strings. The keys are the graph nodes which are mapped to its neighbors.
	 * @param cycle
	 *            filled with name of nodes which cause cycle
	 */
	private static boolean isCyclic(Map<String, Set<String>> srcdir2sourcedirs, List<String> cycle) {
		return !isAcyclic(srcdir2sourcedirs, cycle);
	}

	private static boolean isAcyclic(Map<String, Set<String>> srcdir2sourcedirs, List<String> cycle) {
		// standard graph theory
		List<String> visited = new ArrayList<>();
		List<String> exited = new ArrayList<>();

		for (String srcdir : srcdir2sourcedirs.keySet()) {
			if (!visited.contains(srcdir)) {
				if (circleSearch(srcdir, srcdir2sourcedirs, visited, exited, cycle)) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean circleSearch(String srcdir, Map<String, Set<String>> srcdir2sourcedirs, List<String> visited, List<String> exited, List<String> cycle) {
		boolean res = false;
		visited.add(srcdir);
		cycle.add(srcdir);
		Set<String> sourcedirs = srcdir2sourcedirs.get(srcdir);
		if (sourcedirs != null) {
			for (String src : sourcedirs) {
				if (!visited.contains(src)) {
					res = circleSearch(src, srcdir2sourcedirs, visited, exited, cycle);
				} else if (!exited.contains(src)) {
					res = true;
				}
				if (res) {
					break;
				}
			}
		}
		if (!res) {
			cycle.clear();
		}
		exited.add(srcdir);
		return res;
	}
}
