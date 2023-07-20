/*******************************************************************************
 * Copyright (c) 2004, 2013 Richard Hoefter and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Richard Hoefter (richard.hoefter@web.de) - initial API and implementation, bug 95298, bug 192726, bug 201180
 *     IBM Corporation - nlsing and incorporating into Eclipse, bug 108276
 *******************************************************************************/

package org.eclipse.ant.internal.ui.datatransfer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class to inspect classpath of an Eclipse project.
 */
public class EclipseClasspath {

	protected List<String> srcDirs = new ArrayList<>();
	protected List<String> classDirs = new ArrayList<>();
	protected List<List<String>> inclusionLists = new ArrayList<>();
	protected List<List<String>> exclusionLists = new ArrayList<>();

	protected Map<String, String> variable2valueMap = new LinkedHashMap<>();
	protected List<String> rawClassPathEntries = new ArrayList<>();
	protected List<String> rawClassPathEntriesAbsolute = new ArrayList<>();

	private IJavaProject project;

	private static Map<String, IClasspathContainer> userLibraryCache = new HashMap<>();

	/**
	 * Initialize object with classpath of given project.
	 */
	public EclipseClasspath(IJavaProject project) throws JavaModelException {
		this.project = project;
		handle(project.getRawClasspath());
	}

	/**
	 * Initialize object with runtime classpath of given launch configuration.
	 * 
	 * @param project
	 *            project that contains given launch configuration conf
	 * @param conf
	 *            launch configuration
	 * @param bootstrap
	 *            if true only bootstrap entries are added, if false only non-bootstrap entries are added
	 */
	public EclipseClasspath(IJavaProject project, ILaunchConfiguration conf, boolean bootstrap) throws CoreException {
		this.project = project;

		// convert IRuntimeClasspathEntry to IClasspathEntry
		IRuntimeClasspathEntry[] runtimeEntries;
		// see AbstractJavaLaunchConfigurationDelegate
		runtimeEntries = JavaRuntime.computeUnresolvedRuntimeClasspath(conf);
		List<IClasspathEntry> classpathEntries = new ArrayList<>(runtimeEntries.length);
		for (IRuntimeClasspathEntry entry : runtimeEntries) {
			if (bootstrap && (entry.getClasspathProperty() == IRuntimeClasspathEntry.BOOTSTRAP_CLASSES) || !bootstrap
					&& (entry.getClasspathProperty() != IRuntimeClasspathEntry.BOOTSTRAP_CLASSES)) {
				// NOTE: See AbstractJavaLaunchConfigurationDelegate.getBootpathExt()
				// for an alternate bootclasspath detection
				if (entry.getClass().getName().equals("org.eclipse.jdt.internal.launching.VariableClasspathEntry")) //$NON-NLS-1$
				{
					IClasspathEntry e = convertVariableClasspathEntry(entry);
					if (e != null) {
						classpathEntries.add(e);
					}
				} else if (entry.getClass().getName().equals("org.eclipse.jdt.internal.launching.DefaultProjectClasspathEntry")) //$NON-NLS-1$
				{
					IClasspathEntry e = JavaCore.newProjectEntry(entry.getPath());
					classpathEntries.add(e);
				} else if (entry.getClasspathEntry() != null) {
					classpathEntries.add(entry.getClasspathEntry());
				}
			} else if (bootstrap && entry.toString().startsWith(JavaRuntime.JRE_CONTAINER)) {
				classpathEntries.add(entry.getClasspathEntry());
			} else if (bootstrap && entry.toString().startsWith(JavaCore.USER_LIBRARY_CONTAINER_ID)) {
				classpathEntries.add(entry.getClasspathEntry());
			}
		}
		IClasspathEntry[] entries = classpathEntries.toArray(new IClasspathEntry[classpathEntries.size()]);

		handle(entries);
	}

	private void handle(IClasspathEntry[] entries) throws JavaModelException {
		for (IClasspathEntry entry : entries) {
			handleSources(entry);
			handleVariables(entry);
			handleJars(entry);
			handleLibraries(entry);
			handleProjects(entry);
		}
	}

	private void handleSources(IClasspathEntry entry) throws JavaModelException {
		String projectRoot = ExportUtil.getProjectRoot(project);
		String defaultClassDir = project.getOutputLocation().toString();
		String defaultClassDirAbsolute = ExportUtil.resolve(project.getOutputLocation());

		if (entry.getContentKind() == IPackageFragmentRoot.K_SOURCE && entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
			// found source path
			IPath srcDirPath = entry.getPath();
			IPath classDirPath = entry.getOutputLocation();
			String srcDir = handleLinkedResource(srcDirPath);
			ExportUtil.removeProjectRoot((srcDirPath != null) ? srcDirPath.toString() : projectRoot, project.getProject());
			String classDir = ExportUtil.removeProjectRoot((classDirPath != null) ? classDirPath.toString() : defaultClassDir, project.getProject());
			srcDirs.add(srcDir);
			classDirs.add(classDir);
			String classDirAbsolute = (classDirPath != null) ? ExportUtil.resolve(classDirPath) : defaultClassDirAbsolute;
			rawClassPathEntries.add(classDir);
			rawClassPathEntriesAbsolute.add(classDirAbsolute);
			List<String> inclusionList = new ArrayList<>();
			for (IPath inclusion : entry.getInclusionPatterns()) {
				if (inclusion != null) {
					inclusionList.add(ExportUtil.removeProjectRoot(inclusion.toString(), project.getProject()));
				}
			}
			inclusionLists.add(inclusionList);
			List<String> exclusionList = new ArrayList<>();
			for (IPath exclusion : entry.getExclusionPatterns()) {
				if (exclusion != null) {
					exclusionList.add(ExportUtil.removeProjectRoot(exclusion.toString(), project.getProject()));
				}
			}
			exclusionLists.add(exclusionList);
		}
	}

	/**
	 * Check if given source path is a linked resource. Add values to {@link #variable2valueMap} accordingly.
	 * 
	 * @param srcDirPath
	 *            source dir as IPath
	 * @return source directory with reference, e.g. ${MYPATH}/src, if it is no link, orginal source dir is returned
	 */
	private String handleLinkedResource(IPath srcDirPath) {
		String projectRoot = ExportUtil.getProjectRoot(project);
		String srcDir = ExportUtil.removeProjectRoot((srcDirPath != null) ? srcDirPath.toString() : projectRoot, project.getProject());
		if (srcDirPath == null) {
			return srcDir;
		}
		IFile file;
		try {
			file = ResourcesPlugin.getWorkspace().getRoot().getFile(srcDirPath);
		}
		catch (IllegalArgumentException e) {
			return srcDir;
		}
		if (file.isLinked()) {
			String pathVariable = file.getRawLocation().segment(0).toString();
			URI pathVariableValue = file.getWorkspace().getPathVariableManager().getURIValue(pathVariable);
			if (pathVariableValue != null) {
				// path variable was used
				String pathVariableExtension = file.getRawLocation().removeFirstSegments(1).toString(); // Bug 192726
				String relativePath = ExportUtil.getRelativePath(pathVariableValue.toString(), projectRoot);
				variable2valueMap.put(pathVariable + ".pathvariable", relativePath); //$NON-NLS-1$
				variable2valueMap.put(srcDir + ".link", //$NON-NLS-1$
						"${" + pathVariable + ".pathvariable}/" + pathVariableExtension); //$NON-NLS-1$  //$NON-NLS-2$
			} else {
				String relativePath = ExportUtil.getRelativePath(file.getLocation() + IAntCoreConstants.EMPTY_STRING, projectRoot);
				variable2valueMap.put(srcDir + ".link", relativePath); //$NON-NLS-1$
			}
			srcDir = "${" + srcDir + ".link}"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return srcDir;
	}

	private void handleJars(IClasspathEntry entry) {
		if (entry.getContentKind() == IPackageFragmentRoot.K_BINARY && entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
			String jarFile = entry.getPath().toString();
			StringBuffer jarFileBuffer = new StringBuffer();
			StringBuffer jarFileAbsoluteBuffer = new StringBuffer();
			String jarFileAbsolute = ExportUtil.resolve(entry.getPath());
			if (jarFileAbsolute == null) {
				jarFileAbsolute = jarFile; // jarFile was already absolute
				if (handleSubProjectClassesDirectory(jarFile, jarFileBuffer, jarFileAbsoluteBuffer)) {
					jarFile = jarFileBuffer.toString();
					jarFileAbsolute = jarFileAbsoluteBuffer.toString();
				}
			}
			String jarFileOld = jarFile;
			jarFile = ExportUtil.removeProjectRoot(jarFile, project.getProject());
			if (jarFile.equals(jarFileOld)) {
				if (handleSubProjectClassesDirectory(jarFile, jarFileBuffer, jarFileAbsoluteBuffer)) {
					jarFile = jarFileBuffer.toString();
					jarFileAbsolute = jarFileAbsoluteBuffer.toString();
				}
			}
			rawClassPathEntries.add(jarFile);
			rawClassPathEntriesAbsolute.add(jarFileAbsolute);
		}
	}

	/**
	 * Checks if file is a class directory of a subproject and fills string buffers with resolved values.
	 * 
	 * @param file
	 *            file to check
	 * @param jarFile
	 *            filled with file location with variable reference ${project.location}, which is also added to variable2valueMap
	 * @param jarFileAbsolute
	 *            filled with absolute file location
	 * @return true if file is a classes directory
	 */
	private boolean handleSubProjectClassesDirectory(String file, StringBuffer jarFile, StringBuffer jarFileAbsolute) {
		// class directory of a subproject?
		if (file != null && file.indexOf('/') == 0) {
			int i = file.indexOf('/', 1);
			i = (i != -1) ? i : file.length();
			String subproject = file.substring(1, i);
			IJavaProject javaproject = ExportUtil.getJavaProjectByName(subproject);
			if (javaproject != null) {
				jarFile.setLength(0);
				jarFileAbsolute.setLength(0);
				String location = javaproject.getProject().getName() + ".location"; //$NON-NLS-1$
				jarFileAbsolute.append(ExportUtil.replaceProjectRoot(file, javaproject.getProject(), ExportUtil.getProjectRoot(javaproject)));
				jarFile.append(ExportUtil.replaceProjectRoot(file, javaproject.getProject(), "${" + location + "}")); //$NON-NLS-1$ //$NON-NLS-2$
				String projectRoot = ExportUtil.getProjectRoot(project);
				String relativePath = ExportUtil.getRelativePath(ExportUtil.getProjectRoot(javaproject), projectRoot);
				variable2valueMap.put(location, relativePath);
				return true;
			}
		}
		return false;
	}

	private void handleVariables(IClasspathEntry entry) {
		if (entry.getContentKind() == IPackageFragmentRoot.K_SOURCE && entry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
			// found variable
			String e = entry.getPath().toString();
			int index = e.indexOf('/');
			if (index == -1) {
				index = e.indexOf('\\');
			}
			String variable = e;
			String path = IAntCoreConstants.EMPTY_STRING;
			if (index != -1) {
				variable = e.substring(0, index);
				path = e.substring(index);
			}
			IPath value = JavaCore.getClasspathVariable(variable);
			if (value != null) {
				String projectRoot = ExportUtil.getProjectRoot(project);
				String relativePath = ExportUtil.getRelativePath(value.toString(), projectRoot);
				variable2valueMap.put(variable, relativePath);
			} else if (variable2valueMap.get(variable) == null) {
				// only add empty value, if variable is new
				variable2valueMap.put(variable, IAntCoreConstants.EMPTY_STRING);
			}
			rawClassPathEntriesAbsolute.add(value + path);
			rawClassPathEntries.add("${" + variable + "}" + path); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void handleLibraries(IClasspathEntry entry) throws JavaModelException {
		if (entry.getContentKind() == IPackageFragmentRoot.K_SOURCE && entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
			// found library
			IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), project);
			if (container == null) {
				// jar missing (project not compile clean)
				return;
			}
			String jar = entry.getPath().toString();
			String refName;
			if (jar.startsWith(JavaRuntime.JRE_CONTAINER)) {
				// JRE System Library
				refName = "${jre.container}"; //$NON-NLS-1$
			} else if (jar.startsWith(JavaCore.USER_LIBRARY_CONTAINER_ID)) {
				// User Library
				String libraryName = container.getDescription();
				refName = "${" + libraryName + ".userclasspath}"; //$NON-NLS-1$ //$NON-NLS-2$
				if (container.getKind() == IClasspathContainer.K_SYSTEM) {
					refName = "${" + libraryName + ".bootclasspath}"; //$NON-NLS-1$ //$NON-NLS-2$
				}
			} else {
				// Library dependencies: e.g. Plug-in Dependencies
				String libraryName = container.getDescription();
				refName = "${" + libraryName + ".libraryclasspath}"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			userLibraryCache.put(refName, container);
			srcDirs.add(refName);
			classDirs.add(refName);
			rawClassPathEntries.add(refName);
			rawClassPathEntriesAbsolute.add(refName);
			inclusionLists.add(new ArrayList<>());
			exclusionLists.add(new ArrayList<>());
		}
	}

	private void handleProjects(IClasspathEntry entry) {
		if (entry.getContentKind() == IPackageFragmentRoot.K_SOURCE && entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
			// found required project on build path
			String subProjectRoot = entry.getPath().toString();
			IJavaProject subProject = ExportUtil.getJavaProject(subProjectRoot);
			if (subProject == null) {
				// project was not loaded in workspace
				AntUIPlugin.log("project is not loaded in workspace: " + subProjectRoot, null); //$NON-NLS-1$
				return;
			}
			// only add an indicator that this is a project reference
			String classpathRef = "${" + subProject.getProject().getName() + ".classpath}"; //$NON-NLS-1$ //$NON-NLS-2$
			srcDirs.add(classpathRef);
			classDirs.add(classpathRef);
			rawClassPathEntries.add(classpathRef);
			rawClassPathEntriesAbsolute.add(classpathRef);
			inclusionLists.add(new ArrayList<>());
			exclusionLists.add(new ArrayList<>());
		}
	}

	/**
	 * Get runtime classpath items for given project separated with path separator.
	 */
	public static String getClasspath(IJavaProject project) throws CoreException {
		List<String> items = getClasspathList(project);
		return ExportUtil.toString(items, File.pathSeparator);
	}

	/**
	 * Get runtime classpath items for given project.
	 */
	public static List<String> getClasspathList(IJavaProject project) throws CoreException {
		String[] classpath = JavaRuntime.computeDefaultRuntimeClassPath(project);
		return Arrays.asList(classpath);
	}

	/**
	 * Check if given string is a reference.
	 */
	public static boolean isReference(String s) {
		return isProjectReference(s) || isUserLibraryReference(s) || isUserSystemLibraryReference(s) || isLibraryReference(s) || isJreReference(s);
		// NOTE: A linked resource is no reference
	}

	/**
	 * Check if given string is a project reference.
	 */
	public static boolean isProjectReference(String s) {
		return s.startsWith("${") && s.endsWith(".classpath}"); //$NON-NLS-1$ //$NON-NLS-2$ 
	}

	/**
	 * Resolves given project reference to a project.
	 * 
	 * @return <code>null</code> if project is not resolvable
	 */
	public static IJavaProject resolveProjectReference(String s) {
		String name = ExportUtil.removePrefixAndSuffix(s, "${", ".classpath}"); //$NON-NLS-1$ //$NON-NLS-2$
		return ExportUtil.getJavaProjectByName(name);
	}

	/**
	 * Check if given string is a user library reference.
	 */
	public static boolean isUserLibraryReference(String s) {
		return s.startsWith("${") && s.endsWith(".userclasspath}"); //$NON-NLS-1$ //$NON-NLS-2$ 
	}

	/**
	 * Check if given string is a user system library reference. This library is added to the compiler boot classpath.
	 */
	public static boolean isUserSystemLibraryReference(String s) {
		return s.startsWith("${") && s.endsWith(".bootclasspath}"); //$NON-NLS-1$ //$NON-NLS-2$ 
	}

	/**
	 * Check if given string is a library reference. e.g. Plug-in dependencies are library references.
	 * 
	 */
	public static boolean isLibraryReference(String s) {
		return s.startsWith("${") && s.endsWith(".libraryclasspath}"); //$NON-NLS-1$ //$NON-NLS-2$ 
	}

	/**
	 * Check if given string is a JRE reference.
	 */
	public static boolean isJreReference(String s) {
		return s.equals("${jre.container}"); //$NON-NLS-1$
	}

	/**
	 * Resolves given user (system) library or plugin reference to its container.
	 * 
	 * <p>
	 * NOTE: The library can only be resolved if an EclipseClasspath object was created which had a reference to this library. The class holds an
	 * internal cache to circumvent that UserLibraryManager is an internal class.
	 * 
	 * @return null if library is not resolvable
	 */
	public static IClasspathContainer resolveUserLibraryReference(String s) {
		return userLibraryCache.get(s);
	}

	/**
	 * Check if given string is a linked resource.
	 * 
	 */
	public static boolean isLinkedResource(String s) {
		return s.startsWith("${") && s.endsWith(".link}"); //$NON-NLS-1$ //$NON-NLS-2$ 
	}

	/**
	 * Get source folder name of a linked resource.
	 * 
	 * @see #isLinkedResource(String)
	 */
	public static String getLinkedResourceName(String s) {
		return ExportUtil.removePrefixAndSuffix(s, "${", ".link}"); //$NON-NLS-1$ //$NON-NLS-2$ 
	}

	/**
	 * Resolves given linked resource to an absolute file location.
	 */
	public String resolveLinkedResource(String s) {
		String name = ExportUtil.removePrefixAndSuffix(s, "${", "}"); //$NON-NLS-1$ //$NON-NLS-2$
		String value = variable2valueMap.get(name);
		String suffix = ".pathvariable}"; //$NON-NLS-1$
		int i = value.indexOf(suffix);
		if (i != -1) {
			// path variable
			String pathVariable = value.substring(0, i + suffix.length() - 1);
			pathVariable = ExportUtil.removePrefix(pathVariable, "${"); //$NON-NLS-1$
			return variable2valueMap.get(pathVariable) + value.substring(i + suffix.length());
		}
		return value;
	}

	/**
	 * Convert a VariableClasspathEntry to a IClasspathEntry.
	 * 
	 * <p>
	 * This is a workaround as entry.getClasspathEntry() returns null.
	 */
	private IClasspathEntry convertVariableClasspathEntry(IRuntimeClasspathEntry entry) {
		try {
			ByteArrayInputStream is = new ByteArrayInputStream(entry.getMemento().getBytes());
			@SuppressWarnings("restriction")
			Document doc = org.eclipse.core.internal.runtime.XmlProcessorFactory.parseWithErrorOnDOCTYPE(is);
			Element element = (Element) doc.getElementsByTagName("memento").item(0); //$NON-NLS-1$
			String variableString = element.getAttribute("variableString"); //$NON-NLS-1$
			ExportUtil.addVariable(variable2valueMap, variableString, ExportUtil.getProjectRoot(project));
			// remove ${...} from string to be conform for handleVariables()
			variableString = ExportUtil.removePrefix(variableString, "${");//$NON-NLS-1$
			int i = variableString.indexOf('}');
			if (i != -1) {
				variableString = variableString.substring(0, i) + variableString.substring(i + 1);
			}
			IPath path = IPath.fromOSString(variableString);
			return JavaCore.newVariableEntry(path, null, null);
		}
		catch (Exception e) {
			AntUIPlugin.log(e);
			return null;
		}

	}
}
