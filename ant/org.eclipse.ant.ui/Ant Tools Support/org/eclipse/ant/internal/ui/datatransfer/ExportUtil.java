/*******************************************************************************
 * Copyright (c) 2004, 2017 Richard Hoefter and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Richard Hoefter (richard.hoefter@web.de) - initial API and implementation, bug 95300, bug 95297, bug 128104, bug 201180, bug 288830 
 *     IBM Corporation - NLS'ing and incorporating into Eclipse. 
 *                     - Bug 177833 Class created from combination of all utility classes of contribution 
 *                     - Bug 267459 Java project with an external jar file from C:\ on the build path throws a NPE during the Ant Buildfile generation.
 *                     - bug fixing
 *******************************************************************************/

package org.eclipse.ant.internal.ui.datatransfer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Collection of utility methods to help when exporting to an Ant build file.
 */
public class ExportUtil {
	private ExportUtil() {
	}

	/**
	 * Get resource from selection.
	 */
	public static IResource getResource(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			for (Iterator<IAdaptable> iter = ((IStructuredSelection) selection).iterator(); iter.hasNext();) {
				IAdaptable adaptable = iter.next();
				return adaptable.getAdapter(IResource.class);
			}
		}
		return null;
	}

	/**
	 * Get Java project from resource.
	 */
	public static IJavaProject getJavaProjectByName(String name) {
		try {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
			if (project.exists()) {
				return JavaCore.create(project);
			}
		}
		catch (IllegalArgumentException iae) {
			// do nothing
		}
		return null;
	}

	/**
	 * Get project root for given project.
	 */
	public static String getProjectRoot(IJavaProject project) {
		if (project == null) {
			return null;
		}
		IResource resource = project.getResource();
		if (resource == null) {
			return null;
		}
		IPath location = resource.getLocation();
		if (location == null) {
			return null;
		}
		return location.toString();
	}

	/**
	 * Convert Eclipse path to absolute filename.
	 * 
	 * @param file
	 *            Project root optionally followed by resource name. An absolute path is simply converted to a string.
	 * @return full qualified path
	 */
	public static String resolve(IPath file) {
		if (file == null) {
			return null;
		}
		try {
			IFile f = ResourcesPlugin.getWorkspace().getRoot().getFile(file);
			URI uri = f.getLocationURI();
			return (uri != null) ? uri.toString() : f.toString();
		}
		catch (IllegalArgumentException e) {
			// resource is missing
			String projectName = removePrefix(file.toString(), "/"); //$NON-NLS-1$
			IJavaProject project = getJavaProjectByName(projectName);
			if (project != null) {
				return getProjectRoot(project);
			}
			// project is null because file is not enclosed in a project i.e.
			// external jar
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=267459
			return file.toOSString();
		}
	}

	/**
	 * Get Java project for given root.
	 */
	public static IJavaProject getJavaProject(String root) {
		IPath path = IPath.fromOSString(root);
		if (path.segmentCount() == 1) {
			return getJavaProjectByName(root);
		}
		IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		if (resource != null && resource.getType() == IResource.PROJECT) {
			if (resource.exists()) {
				return (IJavaProject) JavaCore.create(resource);
			}
		}
		return null;
	}

	/**
	 * Remove project root from given project file.
	 */
	public static String removeProjectRoot(String file, IProject project) {
		String res = removePrefix(file, '/' + project.getName() + '/');
		if (res.equals('/' + project.getName())) {
			return "."; //$NON-NLS-1$
		}
		return res;
	}

	/**
	 * Remove project root from given project file.
	 * 
	 * @param newProjectRoot
	 *            replace project root, e.g. with a variable ${project.location}
	 */
	public static String replaceProjectRoot(String file, IProject project, String newProjectRoot) {
		String res = removeProjectRoot(file, project);
		if (res.equals(".")) //$NON-NLS-1$
		{
			return newProjectRoot;
		}
		if (newProjectRoot == null) {
			return res;
		}
		if (!res.equals(file)) {
			return newProjectRoot + '/' + res;
		}
		return res;
	}

	/**
	 * Get for given project all directly dependent projects.
	 * 
	 * @return set of IJavaProject objects
	 */
	public static List<IJavaProject> getClasspathProjects(IJavaProject project) throws JavaModelException {
		List<IJavaProject> projects = new ArrayList<>();
		IClasspathEntry entries[] = project.getRawClasspath();
		addClasspathProjects(projects, entries);
		return sortProjectsUsingBuildOrder(projects);
	}

	private static void addClasspathProjects(List<IJavaProject> projects, IClasspathEntry[] entries) {
		for (IClasspathEntry classpathEntry : entries) {
			if (classpathEntry.getContentKind() == IPackageFragmentRoot.K_SOURCE && classpathEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
				// found required project on build path
				String subProjectRoot = classpathEntry.getPath().toString();
				IJavaProject subProject = getJavaProject(subProjectRoot);
				// is project available in workspace
				if (subProject != null) {
					projects.add(subProject);
				}
			}
		}
	}

	/**
	 * Get for given project all directly and indirectly dependent projects.
	 * 
	 * @return set of IJavaProject objects
	 */
	public static List<IJavaProject> getClasspathProjectsRecursive(IJavaProject project) throws JavaModelException {
		LinkedList<IJavaProject> result = new LinkedList<>();
		getClasspathProjectsRecursive(project, result);
		return sortProjectsUsingBuildOrder(result);
	}

	private static void getClasspathProjectsRecursive(IJavaProject project, LinkedList<IJavaProject> result) throws JavaModelException {
		for (IJavaProject javaProject : getClasspathProjects(project)) {
			if (!result.contains(javaProject)) {
				result.addFirst(javaProject);
				getClasspathProjectsRecursive(javaProject, result); // recursion
			}
		}
	}

	/**
	 * Sort projects according to General -&gt; Workspace -&gt; Build Order.
	 * 
	 * @param javaProjects
	 *            list of IJavaProject objects
	 * @return list of IJavaProject objects with new order
	 */
	private static List<IJavaProject> sortProjectsUsingBuildOrder(List<IJavaProject> javaProjects) {
		if (javaProjects.isEmpty()) {
			return javaProjects;
		}
		List<IJavaProject> result = new ArrayList<>(javaProjects.size());
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		String[] buildOrder = workspace.getDescription().getBuildOrder();
		if (buildOrder == null) {// default build order
			IProject[] projects = new IProject[javaProjects.size()];
			int i = 0;
			for (Iterator<IJavaProject> iter = javaProjects.iterator(); iter.hasNext(); i++) {
				IJavaProject javaProject = iter.next();
				projects[i] = javaProject.getProject();
			}
			IWorkspace.ProjectOrder po = ResourcesPlugin.getWorkspace().computeProjectOrder(projects);
			projects = po.projects;
			buildOrder = new String[projects.length];
			for (i = 0; i < projects.length; i++) {
				buildOrder[i] = projects[i].getName();
			}
		}

		for (int i = 0; i < buildOrder.length && !javaProjects.isEmpty(); i++) {
			String projectName = buildOrder[i];
			for (Iterator<IJavaProject> iter = javaProjects.iterator(); iter.hasNext();) {
				IJavaProject javaProject = iter.next();
				if (javaProject.getProject().getName().equals(projectName)) {
					result.add(javaProject);
					iter.remove();
				}
			}
		}
		// add any remaining projects not specified in the build order
		result.addAll(javaProjects);
		return result;
	}

	/**
	 * Returns cyclic dependency marker for a given project.
	 * 
	 * <p>
	 * See org.eclipse.jdt.core.tests.model.ClasspathTests.numberOfCycleMarkers.
	 * 
	 * @param javaProject
	 *            project for which cyclic dependency marker should be found
	 * @return cyclic dependency marker for a given project or <code>null</code> if there is no such marker
	 * @throws CoreException
	 */
	public static IMarker getCyclicDependencyMarker(IJavaProject javaProject) throws CoreException {
		for (IMarker marker : javaProject.getProject().findMarkers(IJavaModelMarker.BUILDPATH_PROBLEM_MARKER, false, IResource.DEPTH_ONE)) {
			String cycleAttr = (String) marker.getAttribute(IJavaModelMarker.CYCLE_DETECTED);
			if (cycleAttr != null && cycleAttr.equals("true")) //$NON-NLS-1$
			{
				return marker;
			}
		}
		return null;
	}

	/**
	 * Find JUnit tests. Same tests are also returned by Eclipse run configuration wizard.
	 * 
	 * @param containerHandle
	 *            project, package or source folder
	 */
	public static IType[] findTestsInContainer(String containerHandle) {
		IJavaElement container = JavaCore.create(containerHandle);
		if (container == null) {
			return new IType[0];
		}
		try {
			return JUnitCore.findTestTypes(container, new NullProgressMonitor());
		}
		catch (OperationCanceledException e) {
			AntUIPlugin.log(e);
		}
		catch (CoreException e) {
			AntUIPlugin.log(e);
		}
		return new IType[0];
	}

	/**
	 * Compares projects by project name.
	 */
	public static synchronized Comparator<IJavaProject> getJavaProjectComparator() {
		if (javaProjectComparator == null) {
			javaProjectComparator = new JavaProjectComparator();
		}
		return javaProjectComparator;
	}

	private static Comparator<IJavaProject> javaProjectComparator;

	private static class JavaProjectComparator implements Comparator<IJavaProject> {
		@Override
		public int compare(IJavaProject o1, IJavaProject o2) {
			IJavaProject j1 = o1;
			IJavaProject j2 = o2;
			return j1.getProject().getName().compareTo(j2.getProject().getName());
		}
	}

	/**
	 * Compares IFile objects.
	 */
	public static synchronized Comparator<IFile> getIFileComparator() {
		if (fileComparator == null) {
			fileComparator = new IFileComparator();
		}
		return fileComparator;
	}

	private static Comparator<IFile> fileComparator;

	private static class IFileComparator implements Comparator<IFile> {
		@Override
		public int compare(IFile o1, IFile o2) {
			IFile f1 = o1;
			IFile f2 = o2;
			return f1.toString().compareTo(f2.toString());
		}
	}

	/**
	 * Compares IType objects.
	 */
	public static synchronized Comparator<IType> getITypeComparator() {
		if (typeComparator == null) {
			typeComparator = new TypeComparator();
		}
		return typeComparator;
	}

	private static Comparator<IType> typeComparator;

	private static class TypeComparator implements Comparator<IType> {
		@Override
		public int compare(IType o1, IType o2) {
			IType t1 = o1;
			IType t2 = o2;
			return t1.getFullyQualifiedName().compareTo(t2.getFullyQualifiedName());
		}
	}

	/**
	 * Platform specific newline character(s).
	 */
	public static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

	public static String removePrefix(String s, String prefix) {
		if (s == null) {
			return null;
		}
		if (s.startsWith(prefix)) {
			return s.substring(prefix.length());
		}
		return s;
	}

	/**
	 * Remove suffix from given string.
	 */
	public static String removeSuffix(String s, String suffix) {
		if (s == null) {
			return null;
		}
		if (s.endsWith(suffix)) {
			return s.substring(0, s.length() - suffix.length());
		}
		return s;
	}

	/**
	 * Remove prefix and suffix from given string.
	 */
	public static String removePrefixAndSuffix(String s, String prefix, String suffix) {
		return removePrefix(removeSuffix(s, suffix), prefix);
	}

	/**
	 * Convert document to formatted XML string.
	 */
	public static String toString(Document doc) throws TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {
		// NOTE: There are different transformer implementations in the wild,
		// which are configured differently
		// regarding the indent size:
		// Java 1.4: org.apache.xalan.transformer.TransformerIdentityImpl
		// Java 1.5:
		// com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl

		StringWriter writer = new StringWriter();
		Source source = new DOMSource(doc);
		Result result = new StreamResult(writer);
		TransformerFactory factory = TransformerFactory.newInstance();
		// https://ant.apache.org/manual/Tasks/style.html
		// Need this feature to set true for Java 9 to enable extension Functions in the presence of Security manager
		factory.setFeature("http://www.oracle.com/xml/jaxp/properties/enableExtensionFunctions", Boolean.TRUE); //$NON-NLS-1$
		boolean indentFallback = false;
		try {
			// indent using TransformerImpl
			factory.setAttribute("indent-number", "4"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (IllegalArgumentException e) {
			// option not supported, set indent size below
			indentFallback = true;
		}
		Transformer transformer = factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
		if (indentFallback) {
			// indent using TransformerIdentityImpl
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		transformer.transform(source, result);
		return writer.toString();
	}

	/**
	 * Read XML file.
	 */
	public static Document parseXmlFile(File file) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		Document doc = factory.newDocumentBuilder().parse(file);
		return doc;
	}

	/**
	 * Read XML string.
	 */
	public static Document parseXmlString(String s) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(s.getBytes()));
		return doc;
	}

	/**
	 * Converts collection to a separated string.
	 * 
	 * @param c
	 *            collection
	 * @param separator
	 *            string to separate items
	 * @return collection items separated with given separator
	 */
	public static String toString(Collection<String> c, String separator) {
		StringBuilder b = new StringBuilder();
		for (String string : c) {
			b.append(string);
			b.append(separator);
		}
		if (c.size() > 0) {
			b.delete(b.length() - separator.length(), b.length());
		}
		return b.toString();
	}

	/**
	 * Remove duplicates preserving original order.
	 * 
	 * @param l
	 *            list to remove duplicates from
	 * @return new list without duplicates
	 */
	public static List<String> removeDuplicates(List<String> l) {
		List<String> res = new ArrayList<>();
		for (String element : l) {
			if (!res.contains(element)) {
				res.add(element);
			}
		}
		return res;
	}

	/**
	 * Check if given file exists that was not written by this export.
	 */
	public static boolean existsUserFile(String filename) {
		File buildFile = new File(filename);
		if (buildFile.exists()) {
			try (BufferedReader in = new BufferedReader(new FileReader(buildFile))) {
				int i = BuildFileCreator.WARNING.indexOf(NEWLINE);
				String warning = BuildFileCreator.WARNING.substring(0, i);
				String line;
				while ((line = in.readLine()) != null) {
					if (line.contains(warning)) {
						return false;
					}
				}
				return true;
			}
			catch (FileNotFoundException e) {
				return false;
			}
			catch (IOException e) {
				return false;
			}
		}
		return false;
	}

	/**
	 * Request write access to given file. Depending on the version control plug-in opens a confirm checkout dialog.
	 * 
	 * @param shell
	 *            parent instance for dialogs
	 * @param file
	 *            file to request write access for
	 * @return <code>true</code> if user confirmed checkout
	 */
	public static boolean validateEdit(Shell shell, IFile file) {
		return file.getWorkspace().validateEdit(new IFile[] { file }, shell).isOK();
	}

	/**
	 * Request write access to given files. Depending on the version control plug-in opens a confirm checkout dialog.
	 * 
	 * @param shell
	 *            parent instance for dialogs
	 * @return <code>IFile</code> objects for which user confirmed checkout
	 * @throws CoreException
	 *             thrown if project is under version control, but not connected
	 */
	public static Set<IFile> validateEdit(Shell shell, List<IFile> files) throws CoreException {
		Set<IFile> confirmedFiles = new TreeSet<>(getIFileComparator());
		if (files.isEmpty()) {
			return confirmedFiles;
		}
		IStatus status = files.get(0).getWorkspace().validateEdit(files.toArray(new IFile[files.size()]), shell);
		if (status.isMultiStatus() && status.getChildren().length > 0) {
			for (int i = 0; i < status.getChildren().length; i++) {
				IStatus statusChild = status.getChildren()[i];
				if (statusChild.isOK()) {
					confirmedFiles.add(files.get(i));
				}
			}
		} else if (status.isOK()) {
			for (IFile file : files) {
				confirmedFiles.add(file);
			}
		}
		if (status.getSeverity() == IStatus.ERROR) {
			// not possible to checkout files: not connected to version
			// control plugin or hijacked files and made read-only, so
			// collect error messages provided by validator and re-throw
			StringBuilder message = new StringBuilder(status.getPlugin() + ": " //$NON-NLS-1$
					+ status.getMessage() + NEWLINE);
			if (status.isMultiStatus()) {
				for (int i = 0; i < status.getChildren().length; i++) {
					IStatus statusChild = status.getChildren()[i];
					message.append(statusChild.getMessage() + NEWLINE);
				}
			}
			throw new CoreException(new Status(IStatus.ERROR, AntUIPlugin.PI_ANTUI, 0, message.toString(), null));
		}

		return confirmedFiles;
	}

	/**
	 * Check if given classpath is a reference to the default classpath of the project. Ideal for testing if runtime classpath was customized.
	 */
	public static boolean isDefaultClasspath(IJavaProject project, EclipseClasspath classpath) {
		// default classpath contains exactly the JRE and the project reference
		List<String> list = removeDuplicates(classpath.rawClassPathEntries);
		if (list.size() != 2) {
			return false;
		}
		String entry1 = list.get(0);
		String entry2 = list.get(1);
		if (!EclipseClasspath.isJreReference(entry1)) {
			return false;
		}
		if (EclipseClasspath.isProjectReference(entry2)) {
			IJavaProject referencedProject = EclipseClasspath.resolveProjectReference(entry2);
			if (referencedProject == null) {
				// project was not loaded in workspace
				return false;
			} else if (referencedProject.getProject().getName().equals(project.getProject().getName())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Add variable/value for Eclipse variable. If given string is no variable, nothing is added.
	 * 
	 * @param variable2valueMap
	 *            property map to add variable/value
	 * @param s
	 *            String which may contain Eclipse variables, e.g. ${project_name}
	 */
	public static void addVariable(Map<String, String> variable2valueMap, String s, String projectRoot) {
		if (s == null || s.equals(IAntCoreConstants.EMPTY_STRING)) {
			return;
		}
		Pattern pattern = Pattern.compile("\\$\\{.*?\\}"); // ${var} //$NON-NLS-1$
		Matcher matcher = pattern.matcher(s);
		while (matcher.find()) {
			String variable = matcher.group();
			String value;
			try {
				value = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(variable);
			}
			catch (CoreException e) {
				// cannot resolve variable
				value = variable;
			}
			variable = removePrefixAndSuffix(variable, "${", "}"); //$NON-NLS-1$ //$NON-NLS-2$
			// if it is an environment variable, convert to Ant environment
			// syntax
			if (variable.startsWith("env_var:")) //$NON-NLS-1$
			{
				value = "env." + variable.substring("env_var:".length()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			File file = new File(value);
			if (file.exists()) {
				value = getRelativePath(file.getAbsolutePath(), projectRoot);
			}
			variable2valueMap.put(variable, value);
		}
	}

	/**
	 * Returns a path which is equivalent to the given location relative to the specified base path.
	 */
	public static String getRelativePath(String otherLocation, String basePath) {

		IPath location = IPath.fromOSString(otherLocation);
		IPath base = IPath.fromOSString(basePath);
		if ((location.getDevice() != null && !location.getDevice().equalsIgnoreCase(base.getDevice())) || !location.isAbsolute()) {
			return otherLocation;
		}
		int baseCount = base.segmentCount();
		int count = base.matchingFirstSegments(location);
		String temp = IAntCoreConstants.EMPTY_STRING;
		for (int j = 0; j < baseCount - count; j++) {
			temp += "../"; //$NON-NLS-1$
		}
		String relative = IPath.fromOSString(temp).append(location.removeFirstSegments(count)).toString();
		if (relative.length() == 0) {
			relative = "."; //$NON-NLS-1$
		}

		return relative;
	}
}