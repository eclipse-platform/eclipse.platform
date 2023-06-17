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
 *     Richard Hoefter (richard.hoefter@web.de) - initial API and implementation, bug 95297, bug 97051, bug 128103, bug 201180, bug 161354, bug 313386
 *     IBM Corporation - nlsing and incorporating into Eclipse, bug 108276, bug 124210, bug 161845, bug 177833
 *     Nikolay Metchev (N.Metchev@teamphone.com) - bug 108276
 *     Ryan Fong (rfong@trapezenetworks.com) - bug 201143
 *******************************************************************************/

package org.eclipse.ant.internal.ui.datatransfer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.IAntModelConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.SAXException;

/**
 * Creates build.xml file.
 */
public class BuildFileCreator {
	protected static final String IMPORT_BUILDFILE_PROCESSING_TARGET = "eclipse.ant.import"; //$NON-NLS-1$
	protected static final String WARNING = " WARNING: Eclipse auto-generated file." + ExportUtil.NEWLINE + //$NON-NLS-1$
			"              Any modifications will be overwritten."; //$NON-NLS-1$
	protected static final String NOTE = "              To include a user specific buildfile here, " + //$NON-NLS-1$
			"simply create one in the same" + ExportUtil.NEWLINE + //$NON-NLS-1$
			"              directory with the processing instruction " + //$NON-NLS-1$
			"<?" + IMPORT_BUILDFILE_PROCESSING_TARGET + "?>" + ExportUtil.NEWLINE + //$NON-NLS-1$ //$NON-NLS-2$
			"              as the first entry and export the buildfile again. "; //$NON-NLS-1$

	protected static String BUILD_XML = "build.xml"; //$NON-NLS-1$
	protected static String JUNIT_OUTPUT_DIR = "junit"; //$NON-NLS-1$
	protected static boolean CHECK_SOURCE_CYCLES = true;
	protected static boolean CREATE_ECLIPSE_COMPILE_TARGET = true;

	private Document doc;
	private Element root;
	private IJavaProject project;
	private String projectName;
	private String projectRoot;
	private Map<String, String> variable2valueMap;
	private Shell shell;
	private Set<String> visited = new TreeSet<>(); // record used subclasspaths
	private Node classpathNode;

	/**
	 * Constructor. Please prefer {@link #createBuildFiles(Set, Shell, IProgressMonitor)} if you do not want call the various createXXX() methods
	 * yourself.
	 * 
	 * @param project
	 *            create buildfile for this project
	 * @param shell
	 *            parent instance for dialogs
	 */
	public BuildFileCreator(IJavaProject project, Shell shell) throws ParserConfigurationException {
		this.project = project;
		this.projectName = project.getProject().getName();
		this.projectRoot = ExportUtil.getProjectRoot(project);
		this.variable2valueMap = new LinkedHashMap<>();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		this.doc = dbf.newDocumentBuilder().newDocument();
		this.shell = shell;
	}

	/**
	 * Create buildfile for given projects.
	 * 
	 * @param projects
	 *            create buildfiles for these <code>IJavaProject</code> objects
	 * @param shell
	 *            parent instance for dialogs
	 * @return project names for which buildfiles were created
	 * @throws InterruptedException
	 *             thrown when user cancels task
	 */
	public static List<String> createBuildFiles(Set<IJavaProject> projects, Shell shell, IProgressMonitor pm) throws JavaModelException, ParserConfigurationException, TransformerConfigurationException, TransformerException, IOException, CoreException, InterruptedException {
		List<String> res = new ArrayList<>();
		try {
			createBuildFilesLoop(projects, shell, pm, res);
		}
		finally {
			if (pm != null) {
				pm.done();
			}
		}
		return res;
	}

	private static void createBuildFilesLoop(Set<IJavaProject> projects, Shell shell, IProgressMonitor pm, List<String> res) throws CoreException, ParserConfigurationException, JavaModelException, TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException, UnsupportedEncodingException {

		// determine files to create/change
		List<IFile> files = new ArrayList<>();
		for (IJavaProject currentProject : projects) {
			IFile file = currentProject.getProject().getFile(BuildFileCreator.BUILD_XML);
			files.add(file);
		}

		// trigger checkout
		Set<IFile> confirmedFiles = ExportUtil.validateEdit(shell, files);
		SubMonitor localmonitor = SubMonitor.convert(pm, DataTransferMessages.AntBuildfileExportPage_0, confirmedFiles.size());
		try {
			Iterator<IJavaProject> iter = projects.iterator();
			while (iter.hasNext()) {
				IJavaProject currentProject = iter.next();
				IFile file = currentProject.getProject().getFile(BuildFileCreator.BUILD_XML);
				if (!confirmedFiles.contains(file)) {
					continue;
				}

				localmonitor.setTaskName(NLS.bind(DataTransferMessages.BuildFileCreator_generating_buildfile_for, currentProject.getProject().getName()));

				BuildFileCreator instance = new BuildFileCreator(currentProject, shell);
				instance.createRoot();
				instance.createImports();
				EclipseClasspath classpath = new EclipseClasspath(currentProject);
				if (CHECK_SOURCE_CYCLES) {
					SourceAnalyzer.checkCycles(currentProject, classpath, shell);
				}
				instance.createClasspaths(classpath);
				instance.createInit(classpath.srcDirs, classpath.classDirs, classpath.inclusionLists, classpath.exclusionLists);
				instance.createClean(classpath.classDirs);
				instance.createCleanAll();
				instance.createBuild(classpath.srcDirs, classpath.classDirs, classpath.inclusionLists, classpath.exclusionLists);
				instance.createBuildRef();
				if (CREATE_ECLIPSE_COMPILE_TARGET) {
					instance.addInitEclipseCompiler();
					instance.addBuildEclipse();
				}
				instance.createRun();
				instance.addSubProperties(currentProject, classpath);
				instance.createProperty();

				// write build file
				String xml = ExportUtil.toString(instance.doc);
				try (InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"))) { //$NON-NLS-1$
					if (file.exists()) {
						file.setContents(is, true, true, null);
					} else {
						file.create(is, true, null);
					}
				}
				catch (IOException closException) {
					// ignored
				}
				if (localmonitor.isCanceled()) {
					return;
				}
				localmonitor.worked(1);
				res.add(instance.projectName);
			}
		}
		finally {
			if (!localmonitor.isCanceled()) {
				localmonitor.done();
			}
		}
	}

	/**
	 * Add property tag.
	 */
	public void createProperty() {
		// read debug options from Eclipse settings
		boolean source = JavaCore.GENERATE.equals(project.getOption(JavaCore.COMPILER_SOURCE_FILE_ATTR, true));
		boolean lines = JavaCore.GENERATE.equals(project.getOption(JavaCore.COMPILER_LINE_NUMBER_ATTR, true));
		boolean vars = JavaCore.GENERATE.equals(project.getOption(JavaCore.COMPILER_LOCAL_VARIABLE_ATTR, true));

		List<String> debuglevel = new ArrayList<>();
		if (source) {
			debuglevel.add("source"); //$NON-NLS-1$
		}
		if (lines) {
			debuglevel.add("lines"); //$NON-NLS-1$
		}
		if (vars) {
			debuglevel.add("vars"); //$NON-NLS-1$
		}
		if (debuglevel.isEmpty()) {
			debuglevel.add("none"); //$NON-NLS-1$
		}
		variable2valueMap.put("debuglevel", ExportUtil.toString(debuglevel, ",")); //$NON-NLS-1$ //$NON-NLS-2$

		// "Generated .class files compatibility"
		String target = project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true);
		variable2valueMap.put("target", target); //$NON-NLS-1$

		// "Compiler compliance level"
		// String compliance = project.getOption(JavaCore.COMPILER_COMPLIANCE, true);

		// "Source compatibility"
		String sourceLevel = project.getOption(JavaCore.COMPILER_SOURCE, true);
		variable2valueMap.put("source", sourceLevel); //$NON-NLS-1$

		// <property name="x" value="y"/>
		boolean first = true;
		Node node = root.getFirstChild();
		for (String key : variable2valueMap.keySet()) {
			String value = variable2valueMap.get(key);
			Element prop = doc.createElement("property"); //$NON-NLS-1$
			prop.setAttribute(IAntCoreConstants.NAME, key);
			prop.setAttribute(IAntCoreConstants.VALUE, value);
			if (first) {
				first = false;
			} else {
				node = node.getNextSibling();
			}
			node = root.insertBefore(prop, node);
		}

		// <property environment="env"/>
		Element env = doc.createElement("property"); //$NON-NLS-1$
		env.setAttribute("environment", "env"); //$NON-NLS-1$ //$NON-NLS-2$
		root.insertBefore(env, root.getFirstChild());
	}

	/**
	 * Create project tag.
	 */
	public void createRoot() {
		// <project name="hello" default="build" basedir=".">
		root = doc.createElement("project"); //$NON-NLS-1$
		root.setAttribute(IAntCoreConstants.NAME, projectName);
		root.setAttribute(IAntCoreConstants.DEFAULT, "build"); //$NON-NLS-1$
		root.setAttribute("basedir", "."); //$NON-NLS-1$ //$NON-NLS-2$
		doc.appendChild(root);

		// <!-- warning -->
		Comment comment = doc.createComment(WARNING + ExportUtil.NEWLINE + NOTE);
		doc.insertBefore(comment, root);
	}

	/**
	 * Find buildfiles in projectroot directory and automatically import them.
	 */
	public void createImports() {
		// <import file="javadoc.xml"/>
		File dir = new File(projectRoot);
		FilenameFilter filter = (acceptDir, name) -> name.endsWith(".xml"); //$NON-NLS-1$

		File[] files = dir.listFiles(filter);
		if (files == null) {
			return;
		}
		for (File file : files) {
			// import file if it is an XML document with marker comment as first
			// child
			Document docCandidate;
			try {
				docCandidate = ExportUtil.parseXmlFile(file);
				NodeList nodes = docCandidate.getChildNodes();
				for (int j = 0; j < nodes.getLength(); j++) {
					Node node = nodes.item(j);
					if (node instanceof ProcessingInstruction
							&& IMPORT_BUILDFILE_PROCESSING_TARGET.equals(((ProcessingInstruction) node).getTarget().trim())) {
						Element element = doc.createElement("import"); //$NON-NLS-1$
						element.setAttribute(IAntCoreConstants.FILE, file.getName());
						root.appendChild(element);
						break;
					}
				}
			}
			catch (ParserConfigurationException e) {
				AntUIPlugin.log("invalid XML file not imported: " + file.getAbsolutePath(), e); //$NON-NLS-1$
			}
			catch (SAXException e) {
				AntUIPlugin.log("invalid XML file not imported: " + file.getAbsolutePath(), e); //$NON-NLS-1$
			}
			catch (IOException e) {
				AntUIPlugin.log("invalid XML file not imported: " + file.getAbsolutePath(), e); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Create classpath tags.
	 */
	public void createClasspaths(EclipseClasspath classpath) throws JavaModelException {
		createClasspaths(null, project, classpath);
	}

	/**
	 * Create classpath tags. Allows to specify ID.
	 * 
	 * @param pathId
	 *            specify id, if null project name is used
	 */
	public void createClasspaths(String pathId, IJavaProject currentProject, EclipseClasspath classpath) throws JavaModelException {
		if (currentProject == null) {
			AntUIPlugin.log("project is not loaded in workspace: " + pathId, null); //$NON-NLS-1$
			return;
		}
		// <path id="hello.classpath">
		// <pathelement location="${hello.location}/classes"/>
		// <pathelement location="${hello.location}/x.jar"/>
		// <path refid="${goodbye.classpath}"/>
		// </path>
		Element element = doc.createElement("path"); //$NON-NLS-1$
		String pathid = pathId;
		if (pathid == null) {
			pathid = currentProject.getProject().getName() + ".classpath"; //$NON-NLS-1$
		}
		element.setAttribute("id", pathid); //$NON-NLS-1$
		visited.add(pathid);
		variable2valueMap.putAll(classpath.variable2valueMap);
		for (String entry : ExportUtil.removeDuplicates(classpath.rawClassPathEntries)) {
			if (EclipseClasspath.isProjectReference(entry)) {
				Element pathElement = doc.createElement("path"); //$NON-NLS-1$
				IJavaProject referencedProject = EclipseClasspath.resolveProjectReference(entry);
				if (referencedProject == null) {
					AntUIPlugin.log("project is not loaded in workspace: " + pathid, null); //$NON-NLS-1$
					continue;
				}
				String refPathId = referencedProject.getProject().getName() + ".classpath"; //$NON-NLS-1$
				pathElement.setAttribute("refid", refPathId); //$NON-NLS-1$
				element.appendChild(pathElement);
				if (visited.add(refPathId)) {
					createClasspaths(null, referencedProject, new EclipseClasspath(referencedProject)); // recursion
				}
			} else if (EclipseClasspath.isUserLibraryReference(entry) || EclipseClasspath.isLibraryReference(entry)) {
				addUserLibrary(element, entry);
			} else if (EclipseClasspath.isUserSystemLibraryReference(entry)) {
				if (pathid.endsWith(".bootclasspath")) { //$NON-NLS-1$
					addUserLibrary(element, entry);
				}
			} else if (EclipseClasspath.isJreReference(entry)) {
				if (pathid.endsWith(".bootclasspath")) { //$NON-NLS-1$
					addJre(element);
				}
			} else {
				// prefix with ${project.location}
				String prefix = IAntCoreConstants.EMPTY_STRING;
				if (!entry.startsWith("${") && // no variable ${var}/classes //$NON-NLS-1$
						!projectName.equals(currentProject.getProject().getName())) // not main project
				{
					String currentProjectRoot = ExportUtil.getProjectRoot(currentProject);
					entry = ExportUtil.getRelativePath(entry, currentProjectRoot);
					if (!IPath.fromOSString(entry).isAbsolute()) {
						prefix = "${" + currentProject.getProject().getName() + ".location}/"; //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				Element pathElement = doc.createElement("pathelement"); //$NON-NLS-1$
				String path = ExportUtil.getRelativePath(prefix + entry, projectRoot);
				pathElement.setAttribute(IAntModelConstants.ATTR_LOCATION, path);
				element.appendChild(pathElement);
			}
		}
		addToClasspathBlock(element);
	}

	private void addUserLibrary(Element element, String entry) {
		// add classpath reference
		Element pathElement = doc.createElement("path"); //$NON-NLS-1$
		IClasspathContainer container = EclipseClasspath.resolveUserLibraryReference(entry);
		String name = ExportUtil.removePrefixAndSuffix(entry, "${", "}"); //$NON-NLS-1$ //$NON-NLS-2$
		pathElement.setAttribute("refid", name); //$NON-NLS-1$
		element.appendChild(pathElement);

		// add classpath
		if (visited.add(entry)) {
			Element userElement = doc.createElement("path"); //$NON-NLS-1$
			userElement.setAttribute("id", name); //$NON-NLS-1$
			for (IClasspathEntry cpentry : container.getClasspathEntries()) {
				String jarFile = cpentry.getPath().toString();
				// use ECLIPSE_HOME variable for library jars
				if (EclipseClasspath.isLibraryReference(entry)) {
					IPath home = JavaCore.getClasspathVariable("ECLIPSE_HOME"); //$NON-NLS-1$
					if (home != null && home.isPrefixOf(cpentry.getPath())) {
						variable2valueMap.put("ECLIPSE_HOME", home.toString()); //$NON-NLS-1$
						jarFile = "${ECLIPSE_HOME}" + jarFile.substring(home.toString().length()); //$NON-NLS-1$
					} else if (!new File(jarFile).exists() && jarFile.startsWith('/' + projectName)
							&& new File(projectRoot, jarFile.substring(('/' + projectName).length())).exists()) {
						// workaround that additional jars are stored with
						// leading project root in container object, although
						// they are relative and indeed correctly stored in
						// build.properties (jars.extra.classpath)
						jarFile = jarFile.substring(('/' + projectName).length() + 1);
					}
				}
				jarFile = ExportUtil.getRelativePath(jarFile, projectRoot);
				Element userPathElement = doc.createElement("pathelement"); //$NON-NLS-1$
				userPathElement.setAttribute(IAntModelConstants.ATTR_LOCATION, jarFile);
				userElement.appendChild(userPathElement);
			}
			addToClasspathBlock(userElement);
		}
	}

	/**
	 * Add JRE to given classpath.
	 * 
	 * @param element
	 *            classpath tag
	 */
	private void addJre(Element element) {
		// <fileset dir="${java.home}/lib" includes="*.jar"/>
		// <fileset dir="${java.home}/lib/ext" includes="*.jar"/>
		Element pathElement = doc.createElement("fileset"); //$NON-NLS-1$
		pathElement.setAttribute(IAntCoreConstants.DIR, "${java.home}/lib"); //$NON-NLS-1$
		pathElement.setAttribute("includes", "*.jar"); //$NON-NLS-1$ //$NON-NLS-2$
		element.appendChild(pathElement);
		pathElement = doc.createElement("fileset"); //$NON-NLS-1$
		pathElement.setAttribute(IAntCoreConstants.DIR, "${java.home}/lib/ext"); //$NON-NLS-1$
		pathElement.setAttribute("includes", "*.jar"); //$NON-NLS-1$ //$NON-NLS-2$
		element.appendChild(pathElement);
	}

	private void addToClasspathBlock(Element element) {
		// remember node to insert all classpaths at same location
		if (classpathNode == null) {
			classpathNode = root.appendChild(element);
		} else {
			classpathNode = classpathNode.getNextSibling();
			classpathNode = root.insertBefore(element, classpathNode);
		}
	}

	/**
	 * Add properties of sub-projects to internal properties map.
	 */
	public void addSubProperties(IJavaProject subproject, EclipseClasspath classpath) throws JavaModelException {
		for (IJavaProject subProject : ExportUtil.getClasspathProjectsRecursive(subproject)) {
			String location = subProject.getProject().getName() + ".location"; //$NON-NLS-1$
			// add subproject properties to variable2valueMap
			String subProjectRoot = ExportUtil.getProjectRoot(subProject);
			String relativePath = ExportUtil.getRelativePath(subProjectRoot, projectRoot);
			variable2valueMap.put(location, relativePath);
			variable2valueMap.putAll(classpath.variable2valueMap);
		}
	}

	/**
	 * Create init target. Creates directories and copies resources.
	 * 
	 * @param srcDirs
	 *            source directories to copy resources from
	 * @param classDirs
	 *            classes directories to copy resources to
	 */
	public void createInit(List<String> srcDirs, List<String> classDirs, List<List<String>> inclusionLists, List<List<String>> exclusionLists) {
		// <target name="init">
		// <mkdir dir="classes"/>
		// </target>
		Element element = doc.createElement("target"); //$NON-NLS-1$
		element.setAttribute(IAntCoreConstants.NAME, "init"); //$NON-NLS-1$
		for (String classDir : ExportUtil.removeDuplicates(classDirs)) {
			if (!classDir.equals(".") && !EclipseClasspath.isReference(classDir)) { //$NON-NLS-1$
				Element pathElement = doc.createElement("mkdir"); //$NON-NLS-1$
				pathElement.setAttribute(IAntCoreConstants.DIR, classDir);
				element.appendChild(pathElement);
			}
		}
		root.appendChild(element);
		createCopyResources(srcDirs, classDirs, element, inclusionLists, exclusionLists);
	}

	private void createCopyResources(List<String> srcDirs, List<String> classDirs, Element element, List<List<String>> inclusionLists, List<List<String>> exclusionLists) {
		// Check filter for copying resources
		String filter = project.getOption(JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER, true);
		StringTokenizer tokenizer = new StringTokenizer(filter, ","); //$NON-NLS-1$
		ArrayList<String> filters = new ArrayList<>();
		while (tokenizer.hasMoreTokens()) {
			filters.add(tokenizer.nextToken());
		}
		filters.add("*.java"); //$NON-NLS-1$

		// prefix filters with wildcard
		for (int i = 0; i < filters.size(); i++) {
			String item = filters.get(i).trim();
			if (item.equals("*")) //$NON-NLS-1$
			{
				// everything is excluded from copying
				return;
			}
			filters.set(i, "**/" + item); //$NON-NLS-1$
		}

		// <copy todir="classes" includeemptydirs="false">
		// <fileset dir="src" excludes="**/*.java"/>
		// </copy>
		for (int i = 0; i < srcDirs.size(); i++) {
			String srcDir = srcDirs.get(i);
			String classDir = classDirs.get(i);
			if (!EclipseClasspath.isReference(classDir)) {
				Element copyElement = doc.createElement("copy"); //$NON-NLS-1$
				copyElement.setAttribute("todir", classDir); //$NON-NLS-1$
				copyElement.setAttribute("includeemptydirs", "false"); //$NON-NLS-1$ //$NON-NLS-2$
				Element filesetElement = doc.createElement("fileset"); //$NON-NLS-1$
				filesetElement.setAttribute(IAntCoreConstants.DIR, srcDir);

				List<String> inclusions = inclusionLists.get(i);
				List<String> exclusions = exclusionLists.get(i);

				for (String inclusion : inclusions) {
					Element includeElement = doc.createElement("include"); //$NON-NLS-1$
					includeElement.setAttribute(IAntCoreConstants.NAME, inclusion);
					filesetElement.appendChild(includeElement);
				}
				for (String exclusion : filters) {
					Element excludeElement = doc.createElement("exclude"); //$NON-NLS-1$
					excludeElement.setAttribute(IAntCoreConstants.NAME, exclusion);
					filesetElement.appendChild(excludeElement);
				}

				for (String exclusion : exclusions) {
					Element excludeElement = doc.createElement("exclude"); //$NON-NLS-1$
					excludeElement.setAttribute(IAntCoreConstants.NAME, exclusion);
					filesetElement.appendChild(excludeElement);
				}

				copyElement.appendChild(filesetElement);
				element.appendChild(copyElement);
			}
		}
	}

	/**
	 * Create clean target.
	 * 
	 * @param classDirs
	 *            classes directories to delete
	 */
	public void createClean(List<String> classDirs) {
		// <target name="clean">
		// <delete dir="classes"/>
		// </target>
		Element element = doc.createElement("target"); //$NON-NLS-1$
		element.setAttribute(IAntCoreConstants.NAME, "clean"); //$NON-NLS-1$
		for (String classDir : ExportUtil.removeDuplicates(classDirs)) {
			if (!classDir.equals(".") && //$NON-NLS-1$
					!EclipseClasspath.isReference(classDir)) {
				Element deleteElement = doc.createElement("delete"); //$NON-NLS-1$
				deleteElement.setAttribute(IAntCoreConstants.DIR, classDir);
				element.appendChild(deleteElement);
			}
		}
		root.appendChild(element);

		// <target name="clean">
		// <delete>
		// <fileset dir="." includes="**/*.class"/>
		// </delete>
		// </target>
		if (classDirs.contains(".")) //$NON-NLS-1$
		{
			Element deleteElement = doc.createElement("delete"); //$NON-NLS-1$
			Element filesetElement = doc.createElement("fileset"); //$NON-NLS-1$
			filesetElement.setAttribute(IAntCoreConstants.DIR, "."); //$NON-NLS-1$
			filesetElement.setAttribute("includes", "**/*.class"); //$NON-NLS-1$ //$NON-NLS-2$
			deleteElement.appendChild(filesetElement);
			element.appendChild(deleteElement);
		}
	}

	/**
	 * Create cleanall target.
	 */
	public void createCleanAll() throws JavaModelException {
		// <target name="cleanall" depends="clean">
		// <ant antfile="build.xml" dir="${hello.location}" inheritAll="false" target="clean"/>
		// </target>
		Element element = doc.createElement("target"); //$NON-NLS-1$
		element.setAttribute(IAntCoreConstants.NAME, "cleanall"); //$NON-NLS-1$
		element.setAttribute("depends", "clean"); //$NON-NLS-1$ //$NON-NLS-2$
		for (IJavaProject subProject : ExportUtil.getClasspathProjectsRecursive(project)) {
			Element antElement = doc.createElement("ant"); //$NON-NLS-1$
			antElement.setAttribute("antfile", BUILD_XML); //$NON-NLS-1$
			antElement.setAttribute(IAntCoreConstants.DIR, "${" + subProject.getProject().getName() + ".location}"); //$NON-NLS-1$ //$NON-NLS-2$
			antElement.setAttribute("target", "clean"); //$NON-NLS-1$ //$NON-NLS-2$
			antElement.setAttribute("inheritAll", "false"); //$NON-NLS-1$ //$NON-NLS-2$
			element.appendChild(antElement);
		}
		root.appendChild(element);
	}

	/**
	 * Create build target.
	 * 
	 * @param srcDirs
	 *            source directories of mainproject
	 * @param classDirs
	 *            class directories of mainproject
	 * @param inclusionLists
	 *            inclusion filters of mainproject
	 * @param exclusionLists
	 *            exclusion filters of mainproject
	 */
	public void createBuild(List<String> srcDirs, List<String> classDirs, List<List<String>> inclusionLists, List<List<String>> exclusionLists) throws JavaModelException {
		// <target name="build" depends="build-subprojects,build-project"/>
		Element element = doc.createElement("target"); //$NON-NLS-1$
		element.setAttribute(IAntCoreConstants.NAME, "build"); //$NON-NLS-1$
		element.setAttribute("depends", "build-subprojects,build-project"); //$NON-NLS-1$ //$NON-NLS-2$
		root.appendChild(element);

		// <target name="build-subprojects">
		// <ant antfile="build.xml" dir="${hello.location}" inheritAll="false" target="build-project"/>
		// </target>
		element = doc.createElement("target"); //$NON-NLS-1$
		element.setAttribute(IAntCoreConstants.NAME, "build-subprojects"); //$NON-NLS-1$
		for (IJavaProject subProject : ExportUtil.getClasspathProjectsRecursive(project)) {
			Element antElement = doc.createElement("ant"); //$NON-NLS-1$
			antElement.setAttribute("antfile", BUILD_XML); //$NON-NLS-1$
			antElement.setAttribute(IAntCoreConstants.DIR, "${" + subProject.getProject().getName() + ".location}"); //$NON-NLS-1$ //$NON-NLS-2$
			antElement.setAttribute("target", "build-project"); //$NON-NLS-1$ //$NON-NLS-2$
			antElement.setAttribute("inheritAll", "false"); //$NON-NLS-1$ //$NON-NLS-2$
			if (CREATE_ECLIPSE_COMPILE_TARGET) {
				Element propertysetElement = doc.createElement("propertyset"); //$NON-NLS-1$
				Element propertyrefElement = doc.createElement("propertyref"); //$NON-NLS-1$
				propertyrefElement.setAttribute(IAntCoreConstants.NAME, "build.compiler"); //$NON-NLS-1$
				propertysetElement.appendChild(propertyrefElement);
				antElement.appendChild(propertysetElement);
			}
			element.appendChild(antElement);
		}
		root.appendChild(element);

		// Bug 313386 optimization:
		// Source directories with the same classes directory get only one <javac> tag
		// Side effect: Eclipse inclusion and exclusion filters apply to the specified source directory.
		// Ant inclusion and exclusion filters apply to all source directories.
		// This may lead to unexpected behavior.
		HashMap<String, List<String>> class2sources = new HashMap<>();
		HashMap<String, List<String>> class2includes = new HashMap<>();
		HashMap<String, List<String>> class2excludes = new HashMap<>();
		for (int i = 0; i < srcDirs.size(); i++) {
			String srcDir = srcDirs.get(i);
			if (!EclipseClasspath.isReference(srcDir)) {
				String classDir = classDirs.get(i);
				List<String> inclusions = inclusionLists.get(i);
				List<String> exclusions = exclusionLists.get(i);
				List<String> list = class2sources.get(classDir);
				List<String> list2 = class2includes.get(classDir);
				List<String> list3 = class2excludes.get(classDir);
				if (list == null) {
					list = new ArrayList<>();
					list2 = new ArrayList<>();
					list3 = new ArrayList<>();
					class2sources.put(classDir, list);
					class2includes.put(classDir, list2);
					class2excludes.put(classDir, list3);
				}
				list.add(srcDir);
				list2.addAll(inclusions);
				list3.addAll(exclusions);
			}
		}

		// <target name="build-project" depends="init">
		// <echo message="${ant.project.name}: ${ant.file}"/>
		// <javac destdir="classes">
		// <src path="src"/>
		// <include name=""/>
		// <exclude name=""/>
		// <classpath refid="project.classpath"/>
		// </javac>
		// </target>
		element = doc.createElement("target"); //$NON-NLS-1$
		element.setAttribute(IAntCoreConstants.NAME, "build-project"); //$NON-NLS-1$
		element.setAttribute("depends", "init"); //$NON-NLS-1$ //$NON-NLS-2$
		Element echoElement = doc.createElement("echo"); //$NON-NLS-1$
		echoElement.setAttribute("message", "${ant.project.name}: ${ant.file}"); //$NON-NLS-1$ //$NON-NLS-2$
		element.appendChild(echoElement);
		for (int i = 0; i < srcDirs.size(); i++) {
			String srcDir = srcDirs.get(i);
			if (!EclipseClasspath.isReference(srcDir)) {
				String classDir = classDirs.get(i);
				List<String> sources = class2sources.get(classDir);
				List<String> inclusions = class2includes.get(classDir);
				List<String> exclusions = class2excludes.get(classDir);
				if (sources != null && sources.size() > 1) {
					// remove list to exclude it from the next iteration
					class2sources.put(classDir, null);
				} else if (sources == null) {
					continue;
				}

				Element javacElement = doc.createElement("javac"); //$NON-NLS-1$
				javacElement.setAttribute("includeantruntime", "false"); //$NON-NLS-1$ //$NON-NLS-2$
				javacElement.setAttribute("destdir", classDir); //$NON-NLS-1$
				javacElement.setAttribute("debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
				javacElement.setAttribute("debuglevel", "${debuglevel}"); //$NON-NLS-1$ //$NON-NLS-2$
				javacElement.setAttribute("source", "${source}"); //$NON-NLS-1$ //$NON-NLS-2$
				javacElement.setAttribute("target", "${target}"); //$NON-NLS-1$ //$NON-NLS-2$

				// Bug 313386: <javac> tag with several source directories
				for (String s : sources) {
					Element srcElement = doc.createElement("src"); //$NON-NLS-1$
					srcElement.setAttribute("path", s); //$NON-NLS-1$
					javacElement.appendChild(srcElement);
				}
				for (String inclusion : inclusions) {
					Element includeElement = doc.createElement("include"); //$NON-NLS-1$
					includeElement.setAttribute(IAntCoreConstants.NAME, inclusion);
					javacElement.appendChild(includeElement);
				}
				for (String exclusion : exclusions) {
					Element excludeElement = doc.createElement("exclude"); //$NON-NLS-1$
					excludeElement.setAttribute(IAntCoreConstants.NAME, exclusion);
					javacElement.appendChild(excludeElement);
				}
				Element classpathRefElement = doc.createElement("classpath"); //$NON-NLS-1$
				classpathRefElement.setAttribute("refid", projectName + ".classpath"); //$NON-NLS-1$ //$NON-NLS-2$
				javacElement.appendChild(classpathRefElement);
				element.appendChild(javacElement);
				if (!JavaRuntime.isModularProject(project))
					addCompilerBootClasspath(srcDirs, javacElement);
			}
		}
		root.appendChild(element);
	}

	/**
	 * Create target build-refprojects which compiles projects which reference current project.
	 */
	private void createBuildRef() throws JavaModelException {
		Set<IJavaProject> refProjects = new TreeSet<>(ExportUtil.getJavaProjectComparator());
		for (IJavaProject javaProject : project.getJavaModel().getJavaProjects()) {
			for (IJavaProject subProject : ExportUtil.getClasspathProjects(javaProject)) {
				if (projectName.equals(subProject.getProject().getName())) {
					refProjects.add(javaProject);
				}
			}
		}

		// <target name="build-refprojects">
		// <ant antfile="build.xml" dir="${hello.location}" target="clean" inheritAll="false"/>
		// <ant antfile="build.xml" dir="${hello.location}" target="build" inheritAll="false"/>
		// </target>
		Element element = doc.createElement("target"); //$NON-NLS-1$
		element.setAttribute(IAntCoreConstants.NAME, "build-refprojects"); //$NON-NLS-1$
		element.setAttribute(IAntCoreConstants.DESCRIPTION, "Build all projects which " + //$NON-NLS-1$
				"reference this project. Useful to propagate changes."); //$NON-NLS-1$
		for (IJavaProject p : refProjects) {
			String location = p.getProject().getName() + ".location"; //$NON-NLS-1$
			String refProjectRoot = ExportUtil.getProjectRoot(p);
			String relativePath = ExportUtil.getRelativePath(refProjectRoot, projectRoot);
			variable2valueMap.put(location, relativePath);

			Element antElement = doc.createElement("ant"); //$NON-NLS-1$
			antElement.setAttribute("antfile", BUILD_XML); //$NON-NLS-1$
			antElement.setAttribute(IAntCoreConstants.DIR, "${" + p.getProject().getName() + ".location}"); //$NON-NLS-1$ //$NON-NLS-2$
			antElement.setAttribute("target", "clean"); //$NON-NLS-1$ //$NON-NLS-2$
			antElement.setAttribute("inheritAll", "false"); //$NON-NLS-1$ //$NON-NLS-2$
			element.appendChild(antElement);

			antElement = doc.createElement("ant"); //$NON-NLS-1$
			antElement.setAttribute("antfile", BUILD_XML); //$NON-NLS-1$
			antElement.setAttribute(IAntCoreConstants.DIR, "${" + p.getProject().getName() + ".location}"); //$NON-NLS-1$ //$NON-NLS-2$
			antElement.setAttribute("target", "build"); //$NON-NLS-1$ //$NON-NLS-2$
			antElement.setAttribute("inheritAll", "false"); //$NON-NLS-1$ //$NON-NLS-2$
			if (CREATE_ECLIPSE_COMPILE_TARGET) {
				Element propertysetElement = doc.createElement("propertyset"); //$NON-NLS-1$
				Element propertyrefElement = doc.createElement("propertyref"); //$NON-NLS-1$
				propertyrefElement.setAttribute(IAntCoreConstants.NAME, "build.compiler"); //$NON-NLS-1$
				propertysetElement.appendChild(propertyrefElement);
				antElement.appendChild(propertysetElement);
			}
			element.appendChild(antElement);
		}
		root.appendChild(element);
	}

	/**
	 * Add target to initialize Eclipse compiler. It copies required jars to ant lib directory.
	 */
	public void addInitEclipseCompiler() {
		// use ECLIPSE_HOME classpath variable
		IPath value = JavaCore.getClasspathVariable("ECLIPSE_HOME"); //$NON-NLS-1$
		if (value != null) {
			variable2valueMap.put("ECLIPSE_HOME", ExportUtil.getRelativePath( //$NON-NLS-1$
					value.toString(), projectRoot));
		}
		// <target name="init-eclipse-compiler" description="copy Eclipse compiler jars to ant lib directory">
		// <property name="ECLIPSE_HOME" value="C:/Programme/eclipse-3.1" />
		// <copy todir="${ant.library.dir}">
		// <fileset dir="${ECLIPSE_HOME}/plugins" includes="org.eclipse.jdt.core_*.jar" />
		// </copy>
		// <unzip dest="${ant.library.dir}">
		// <patternset includes="jdtCompilerAdapter.jar" />
		// <fileset dir="${ECLIPSE_HOME}/plugins" includes="org.eclipse.jdt.core_*.jar" />
		// </unzip>
		// </target>
		Element element = doc.createElement("target"); //$NON-NLS-1$
		element.setAttribute(IAntCoreConstants.NAME, "init-eclipse-compiler"); //$NON-NLS-1$
		element.setAttribute(IAntCoreConstants.DESCRIPTION, "copy Eclipse compiler jars to ant lib directory"); //$NON-NLS-1$
		Element copyElement = doc.createElement("copy"); //$NON-NLS-1$
		copyElement.setAttribute("todir", "${ant.library.dir}"); //$NON-NLS-1$ //$NON-NLS-2$
		Element filesetElement = doc.createElement("fileset"); //$NON-NLS-1$
		filesetElement.setAttribute(IAntCoreConstants.DIR, "${ECLIPSE_HOME}/plugins"); //$NON-NLS-1$
		filesetElement.setAttribute("includes", "org.eclipse.jdt.core_*.jar"); //$NON-NLS-1$ //$NON-NLS-2$
		copyElement.appendChild(filesetElement);
		element.appendChild(copyElement);
		Element unzipElement = doc.createElement("unzip"); //$NON-NLS-1$
		unzipElement.setAttribute("dest", "${ant.library.dir}"); //$NON-NLS-1$ //$NON-NLS-2$
		Element patternsetElement = doc.createElement("patternset"); //$NON-NLS-1$
		patternsetElement.setAttribute("includes", "jdtCompilerAdapter.jar"); //$NON-NLS-1$ //$NON-NLS-2$
		unzipElement.appendChild(patternsetElement);
		unzipElement.appendChild(filesetElement.cloneNode(false));
		element.appendChild(unzipElement);
		root.appendChild(element);
	}

	/**
	 * Add target to compile project using Eclipse compiler.
	 */
	public void addBuildEclipse() {
		// <target name="build-eclipse-compiler" description="compile project with Eclipse compiler">
		// <property name="build.compiler" value="org.eclipse.jdt.core.JDTCompilerAdapter" />
		// <antcall target="build" />
		// </target>
		Element element = doc.createElement("target"); //$NON-NLS-1$
		element.setAttribute(IAntCoreConstants.NAME, "build-eclipse-compiler"); //$NON-NLS-1$
		element.setAttribute(IAntCoreConstants.DESCRIPTION, "compile project with Eclipse compiler"); //$NON-NLS-1$
		Element propertyElement = doc.createElement("property"); //$NON-NLS-1$
		propertyElement.setAttribute(IAntCoreConstants.NAME, "build.compiler"); //$NON-NLS-1$
		propertyElement.setAttribute(IAntCoreConstants.VALUE, "org.eclipse.jdt.core.JDTCompilerAdapter"); //$NON-NLS-1$
		element.appendChild(propertyElement);
		Element antcallElement = doc.createElement("antcall"); //$NON-NLS-1$
		antcallElement.setAttribute("target", "build"); //$NON-NLS-1$ //$NON-NLS-2$
		element.appendChild(antcallElement);
		root.appendChild(element);
	}

	/**
	 * Add all bootclasspaths in srcDirs to given javacElement.
	 */
	private void addCompilerBootClasspath(List<String> srcDirs, Element javacElement) {
		// <bootclasspath>
		// <path refid="mylib.bootclasspath"/>
		// <fileset dir="${java.home}/lib" includes="*.jar"/>
		// <fileset dir="${java.home}/lib/ext" includes="*.jar"/>
		// </bootclasspath>
		Element bootclasspathElement = doc.createElement("bootclasspath"); //$NON-NLS-1$
		boolean bootclasspathUsed = false;
		for (String entry : srcDirs) {
			if (EclipseClasspath.isUserSystemLibraryReference(entry)) {
				Element pathElement = doc.createElement("path"); //$NON-NLS-1$
				pathElement.setAttribute("refid", ExportUtil.removePrefixAndSuffix(entry, "${", "}")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				bootclasspathElement.appendChild(pathElement);
				bootclasspathUsed = true;
			} else if (EclipseClasspath.isJreReference(entry)) {
				addJre(bootclasspathElement);
			}
		}
		if (bootclasspathUsed) {
			javacElement.appendChild(bootclasspathElement);
		}
	}

	/**
	 * Add run targets.
	 * 
	 * @throws CoreException
	 *             thrown if problem accessing the launch configuration
	 * @throws TransformerFactoryConfigurationError
	 *             thrown if applet file could not get created
	 * @throws UnsupportedEncodingException
	 *             thrown if applet file could not get created
	 */
	public void createRun() throws CoreException, TransformerFactoryConfigurationError, UnsupportedEncodingException {
		// <target name="run">
		// <java fork="yes" classname="class" failonerror="true" dir="." newenvironment="true">
		// <env key="a" value="b"/>
		// <jvmarg value="-Dx=y"/>
		// <arg value="arg"/>
		// <classpath refid="project.classpath"/>
		// </java>
		// </target>
		boolean junitUsed = false;
		for (ILaunchConfiguration conf : DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations()) {
			if (!projectName.equals(conf.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, IAntCoreConstants.EMPTY_STRING))) {
				continue;
			}

			if (conf.getType().getIdentifier().equals(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION)) {
				addJavaApplication(variable2valueMap, conf);
			} else if (conf.getType().getIdentifier().equals(IJavaLaunchConfigurationConstants.ID_JAVA_APPLET)) {
				addApplet(variable2valueMap, conf);
			} else if (conf.getType().getIdentifier().equals("org.eclipse.jdt.junit.launchconfig" /* JUnitLaunchConfiguration.ID_JUNIT_APPLICATION */)) //$NON-NLS-1$
			{
				addJUnit(variable2valueMap, conf);
				junitUsed = true;
			}
		}

		if (junitUsed) {
			addJUnitReport();
		}
	}

	/**
	 * Convert Java application launch configuration to ant target and add it to a document.
	 * 
	 * @param variable2value
	 *            adds Eclipse variables to this map, if run configuration makes use of this feature
	 * @param conf
	 *            Java application launch configuration
	 */
	public void addJavaApplication(Map<String, String> variable2value, ILaunchConfiguration conf) throws CoreException {
		Element element = doc.createElement("target"); //$NON-NLS-1$
		element.setAttribute(IAntCoreConstants.NAME, conf.getName());
		Element javaElement = doc.createElement("java"); //$NON-NLS-1$
		javaElement.setAttribute("fork", "yes"); //$NON-NLS-1$ //$NON-NLS-2$
		javaElement.setAttribute("classname", conf.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, IAntCoreConstants.EMPTY_STRING)); //$NON-NLS-1$
		javaElement.setAttribute("failonerror", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		String dir = conf.getAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, IAntCoreConstants.EMPTY_STRING);
		ExportUtil.addVariable(variable2value, dir, projectRoot);
		if (!dir.equals(IAntCoreConstants.EMPTY_STRING)) {
			javaElement.setAttribute(IAntCoreConstants.DIR, ExportUtil.getRelativePath(dir, projectRoot));
		}
		if (!conf.getAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true)) {
			javaElement.setAttribute("newenvironment", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Map<String, String> props = conf.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, new TreeMap<>());
		addElements(props, doc, javaElement, "env", "key", IAntCoreConstants.VALUE); //$NON-NLS-1$ //$NON-NLS-2$
		addElement(conf.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, IAntCoreConstants.EMPTY_STRING), doc, javaElement, "jvmarg", "line", variable2value, projectRoot); //$NON-NLS-1$ //$NON-NLS-2$
		addElement(conf.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, IAntCoreConstants.EMPTY_STRING), doc, javaElement, "arg", "line", variable2value, projectRoot); //$NON-NLS-1$ //$NON-NLS-2$
		element.appendChild(javaElement);

		addRuntimeClasspath(conf, javaElement);
		addRuntimeBootClasspath(conf, javaElement);
		root.appendChild(element);
	}

	/**
	 * Convert applet launch configuration to Ant target and add it to a document.
	 * 
	 * @param variable2value
	 *            adds Eclipse variables to this map, if run configuration makes use of this feature
	 * @param conf
	 *            applet configuration
	 * @throws CoreException
	 *             thrown if problem dealing with launch configuration or underlying resources
	 * @throws TransformerFactoryConfigurationError
	 *             thrown if applet file could not get created
	 * @throws UnsupportedEncodingException
	 *             thrown if applet file could not get created
	 */
	public void addApplet(Map<String, String> variable2value, ILaunchConfiguration conf) throws CoreException, TransformerFactoryConfigurationError, UnsupportedEncodingException {
		String dir = conf.getAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, IAntCoreConstants.EMPTY_STRING);
		if (dir.equals(IAntCoreConstants.EMPTY_STRING)) {
			dir = projectRoot;
		}
		ExportUtil.addVariable(variable2value, dir, projectRoot);
		String value;
		try {
			value = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(dir);
		}
		catch (CoreException e) {
			// cannot resolve variable
			value = null;
		}

		String htmlfile = ((value != null) ? value : dir) + '/' + conf.getName() + ".html"; //$NON-NLS-1$
		// confirm overwrite
		if (ExportUtil.existsUserFile(htmlfile)
				&& !MessageDialog.openConfirm(shell, DataTransferMessages.AntBuildfileExportPage_4, DataTransferMessages.AntBuildfileExportPage_4
						+ ": " + htmlfile)) //$NON-NLS-1$
		{
			return;
		}
		IJavaProject javaProject = ExportUtil.getJavaProjectByName(projectName);
		IFile file = javaProject.getProject().getFile(conf.getName() + ".html"); //$NON-NLS-1$
		if (ExportUtil.validateEdit(shell, file)) // checkout file if required
		{
			// write build file
			String html = AppletUtil.buildHTMLFile(conf);
			InputStream is = new ByteArrayInputStream(html.getBytes("UTF-8")); //$NON-NLS-1$
			if (file.exists()) {
				file.setContents(is, true, true, null);
			} else {
				file.create(is, true, null);
			}
		}

		Element element = doc.createElement("target"); //$NON-NLS-1$
		element.setAttribute(IAntCoreConstants.NAME, conf.getName());
		Element javaElement = doc.createElement("java"); //$NON-NLS-1$
		javaElement.setAttribute("fork", "yes"); //$NON-NLS-1$//$NON-NLS-2$
		javaElement.setAttribute("classname", conf.getAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_APPLETVIEWER_CLASS, "sun.applet.AppletViewer")); //$NON-NLS-1$ //$NON-NLS-2$
		javaElement.setAttribute("failonerror", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		if (value != null) {
			javaElement.setAttribute(IAntCoreConstants.DIR, ExportUtil.getRelativePath(dir, projectRoot));
		}
		addElement(conf.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, IAntCoreConstants.EMPTY_STRING), doc, javaElement, "jvmarg", "line", variable2value, projectRoot); //$NON-NLS-1$//$NON-NLS-2$
		addElement(conf.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, IAntCoreConstants.EMPTY_STRING), doc, javaElement, "arg", "line", variable2value, projectRoot); //$NON-NLS-1$//$NON-NLS-2$
		addElement(conf.getName() + ".html", doc, javaElement, "arg", "line", variable2value, projectRoot); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		element.appendChild(javaElement);
		addRuntimeClasspath(conf, javaElement);
		addRuntimeBootClasspath(conf, javaElement);
		root.appendChild(element);
	}

	/**
	 * Convert JUnit launch configuration to JUnit task and add it to a document.
	 * 
	 * @param variable2value
	 *            adds Eclipse variables to this map, if run configuration makes use of this feature
	 * @param conf
	 *            applet configuration
	 */
	public void addJUnit(Map<String, String> variable2value, ILaunchConfiguration conf) throws CoreException {
		// <target name="runtest">
		// <mkdir dir="junit"/>
		// <junit fork="yes" printsummary="withOutAndErr">
		// <formatter type="xml"/>
		// <test name="testclass"/>
		// <env key="a" value="b"/>
		// <jvmarg value="-Dx=y"/>
		// <classpath refid="project.classpath"/>
		// </junit>
		// </target>
		String testClass = conf.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, IAntCoreConstants.EMPTY_STRING);
		Element element = doc.createElement("target"); //$NON-NLS-1$
		element.setAttribute(IAntCoreConstants.NAME, conf.getName());

		Element mkdirElement = doc.createElement("mkdir"); //$NON-NLS-1$
		mkdirElement.setAttribute(IAntCoreConstants.DIR, "${junit.output.dir}"); //$NON-NLS-1$
		element.appendChild(mkdirElement);

		Element junitElement = doc.createElement("junit"); //$NON-NLS-1$
		junitElement.setAttribute("fork", "yes"); //$NON-NLS-1$ //$NON-NLS-2$
		junitElement.setAttribute("printsummary", "withOutAndErr"); //$NON-NLS-1$ //$NON-NLS-2$
		String dir = conf.getAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, IAntCoreConstants.EMPTY_STRING);
		ExportUtil.addVariable(variable2value, dir, projectRoot);
		if (!dir.equals(IAntCoreConstants.EMPTY_STRING)) {
			junitElement.setAttribute(IAntCoreConstants.DIR, ExportUtil.getRelativePath(dir, projectRoot));
		}
		if (!conf.getAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true)) {
			junitElement.setAttribute("newenvironment", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Element formatterElement = doc.createElement("formatter"); //$NON-NLS-1$
		formatterElement.setAttribute("type", "xml"); //$NON-NLS-1$//$NON-NLS-2$
		junitElement.appendChild(formatterElement);
		if (!testClass.equals(IAntCoreConstants.EMPTY_STRING)) {
			// Case 1: Single JUnit class
			Element testElement = doc.createElement("test"); //$NON-NLS-1$
			testElement.setAttribute(IAntCoreConstants.NAME, testClass);
			testElement.setAttribute("todir", "${junit.output.dir}"); //$NON-NLS-1$ //$NON-NLS-2$
			junitElement.appendChild(testElement);
		} else {
			// Case 2: Run all tests in project, package or source folder
			String container = conf.getAttribute("org.eclipse.jdt.junit.CONTAINER" /* JUnitBaseLaunchConfiguration.LAUNCH_CONTAINER_ATTR */, IAntCoreConstants.EMPTY_STRING); //$NON-NLS-1$
			IType[] types = ExportUtil.findTestsInContainer(container);
			Set<IType> sortedTypes = new TreeSet<>(ExportUtil.getITypeComparator());
			sortedTypes.addAll(Arrays.asList(types));
			for (IType type : sortedTypes) {
				Element testElement = doc.createElement("test"); //$NON-NLS-1$
				testElement.setAttribute(IAntCoreConstants.NAME, type.getFullyQualifiedName());
				testElement.setAttribute("todir", "${junit.output.dir}"); //$NON-NLS-1$ //$NON-NLS-2$
				junitElement.appendChild(testElement);
			}
		}
		Map<String, String> props = conf.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, new TreeMap<>());
		addElements(props, doc, junitElement, "env", "key", IAntCoreConstants.VALUE); //$NON-NLS-1$ //$NON-NLS-2$
		addElement(conf.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, IAntCoreConstants.EMPTY_STRING), doc, junitElement, "jvmarg", "line", variable2value, projectRoot); //$NON-NLS-1$ //$NON-NLS-2$
		element.appendChild(junitElement);
		addRuntimeClasspath(conf, junitElement);
		addRuntimeBootClasspath(conf, junitElement);
		root.appendChild(element);
	}

	/**
	 * Add junitreport target.
	 */
	public void addJUnitReport() {
		variable2valueMap.put("junit.output.dir", JUNIT_OUTPUT_DIR); //$NON-NLS-1$

		// <target name="junitreport">
		// <junitreport todir="junit">
		// <fileset dir="junit">
		// <include name="TEST-*.xml"/>
		// </fileset>
		// <report format="frames" todir="junit"/>
		// </junitreport>
		// </target>
		Element element = doc.createElement("target"); //$NON-NLS-1$
		element.setAttribute(IAntCoreConstants.NAME, "junitreport"); //$NON-NLS-1$
		Element junitreport = doc.createElement("junitreport"); //$NON-NLS-1$
		junitreport.setAttribute("todir", "${junit.output.dir}"); //$NON-NLS-1$ //$NON-NLS-2$
		Element fileset = doc.createElement("fileset"); //$NON-NLS-1$
		fileset.setAttribute(IAntCoreConstants.DIR, "${junit.output.dir}"); //$NON-NLS-1$
		junitreport.appendChild(fileset);
		Element include = doc.createElement("include"); //$NON-NLS-1$
		include.setAttribute(IAntCoreConstants.NAME, "TEST-*.xml"); //$NON-NLS-1$
		fileset.appendChild(include);
		Element report = doc.createElement("report"); //$NON-NLS-1$
		report.setAttribute("format", "frames"); //$NON-NLS-1$ //$NON-NLS-2$
		report.setAttribute("todir", "${junit.output.dir}"); //$NON-NLS-1$ //$NON-NLS-2$
		junitreport.appendChild(report);
		element.appendChild(junitreport);
		root.appendChild(element);
	}

	/**
	 * Add classpath tag to given javaElement.
	 */
	private void addRuntimeClasspath(ILaunchConfiguration conf, Element javaElement) throws CoreException {
		// <classpath refid="hello.classpath"/>
		Element classpathRefElement = doc.createElement("classpath"); //$NON-NLS-1$
		EclipseClasspath runtimeClasspath = new EclipseClasspath(project, conf, false);
		if (ExportUtil.isDefaultClasspath(project, runtimeClasspath)) {
			classpathRefElement.setAttribute("refid", projectName + ".classpath"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			String pathId = "run." + conf.getName() + ".classpath"; //$NON-NLS-1$ //$NON-NLS-2$
			classpathRefElement.setAttribute("refid", pathId); //$NON-NLS-1$
			createClasspaths(pathId, project, runtimeClasspath);
		}
		javaElement.appendChild(classpathRefElement);
	}

	/**
	 * Add bootclasspath tag to given javaElement.
	 */
	private void addRuntimeBootClasspath(ILaunchConfiguration conf, Element javaElement) throws CoreException {
		// <bootclasspath>
		// <path refid="run.hello.bootclasspath"/>
		// </bootclasspath>
		if (JavaRuntime.isModularConfiguration(conf)) {
			// there is no bootclasspath entry from Java 9 onwards
			return;
		}
		EclipseClasspath bootClasspath = new EclipseClasspath(project, conf, true);
		if (bootClasspath.rawClassPathEntries.size() == 1 && EclipseClasspath.isJreReference(bootClasspath.rawClassPathEntries.get(0))) {
			// the default boot classpath contains exactly one element (the JRE)
			return;
		}

		String pathId = "run." + conf.getName() + ".bootclasspath"; //$NON-NLS-1$ //$NON-NLS-2$
		createClasspaths(pathId, project, bootClasspath);
		Element bootclasspath = doc.createElement("bootclasspath"); //$NON-NLS-1$
		Element classpathRefElement = doc.createElement("path"); //$NON-NLS-1$
		classpathRefElement.setAttribute("refid", pathId); //$NON-NLS-1$
		bootclasspath.appendChild(classpathRefElement);
		javaElement.appendChild(bootclasspath);
	}

	/**
	 * Create child node from <code>cmdLine</code> and add it to <code>element</code> which is part of <code>doc</code>.
	 * 
	 * @param cmdLineArgs
	 *            command line arguments, separated with spaces or within double quotes, may also contain Eclipse variables
	 * @param doc
	 *            XML document
	 * @param element
	 *            node to add child to
	 * @param elementName
	 *            name of new child node
	 * @param attributeName
	 *            name of attribute for child node
	 * @param variable2valueMap
	 *            adds Eclipse variables to this map, if command line makes use of this feature
	 */
	private static void addElement(String cmdLineArgs, Document doc, Element element, String elementName, String attributeName, Map<String, String> variable2valueMap, String projectRoot) {

		if (cmdLineArgs == null || cmdLineArgs.length() == 0) {
			return;
		}
		ExportUtil.addVariable(variable2valueMap, cmdLineArgs, projectRoot);
		Element itemElement = doc.createElement(elementName);
		itemElement.setAttribute(attributeName, cmdLineArgs);
		element.appendChild(itemElement);
	}

	/**
	 * Create child nodes from string map and add them to <code>element</code> which is part of <code>doc</code>.
	 * 
	 * @param map
	 *            key/value string pairs
	 * @param doc
	 *            XML document
	 * @param element
	 *            node to add children to
	 * @param elementName
	 *            name of new child node
	 * @param keyAttributeName
	 *            name of key attribute
	 * @param valueAttributeName
	 *            name of value attribute
	 */
	private static void addElements(Map<String, String> map, Document doc, Element element, String elementName, String keyAttributeName, String valueAttributeName) {
		for (String key : map.keySet()) {
			String value = map.get(key);
			Element itemElement = doc.createElement(elementName);
			itemElement.setAttribute(keyAttributeName, key);
			itemElement.setAttribute(valueAttributeName, value);
			element.appendChild(itemElement);
		}
	}

	/**
	 * Set config options.
	 * 
	 * @param buildfilename
	 *            name for Ant buildfile
	 * @param junitdir
	 *            name of JUnit output directory
	 * @param checkcycles
	 *            check project for Ant compatibility
	 * @param eclipsecompiler
	 *            generate target for compiling project with Eclipse compiler
	 */
	public static void setOptions(String buildfilename, String junitdir, boolean checkcycles, boolean eclipsecompiler) {

		if (buildfilename.length() > 0) {
			BUILD_XML = buildfilename;
		}
		if (junitdir.length() > 0) {
			JUNIT_OUTPUT_DIR = junitdir;
		}
		CHECK_SOURCE_CYCLES = checkcycles;
		CREATE_ECLIPSE_COMPILE_TARGET = eclipsecompiler;
	}
}