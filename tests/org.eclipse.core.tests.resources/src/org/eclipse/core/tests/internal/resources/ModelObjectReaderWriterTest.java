/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *     Mickael Istria (Red Hat Inc.) - Bug 488938
 *******************************************************************************/
package org.eclipse.core.tests.internal.resources;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.resources.ResourceTest;
import org.xml.sax.InputSource;

public class ModelObjectReaderWriterTest extends ResourceTest {
	static final IPath LONG_LOCATION = new Path("/eclipse/dev/i0218/eclipse/pffds/fds//fds///fdsfsdfsd///fdsfdsf/fsdfsdfsd/lugi/dsds/fsd//f/ffdsfdsf/fsdfdsfsd/fds//fdsfdsfdsf/fdsfdsfds/fdsfdsfdsf/fdsfdsfdsds/ns/org.eclipse.help.ui_2.1.0/contexts.xml").setDevice(isWindows() ? "D:" : null);
	static final URI LONG_LOCATION_URI = LONG_LOCATION.toFile().toURI();
	private static final String PATH_STRING = new Path("/abc/def").setDevice(isWindows() ? "D:" : null).toString();

	private HashMap<String, ProjectDescription> buildBaselineDescriptors() {
		HashMap<String, ProjectDescription> result = new HashMap<>();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		ProjectDescription desc = new ProjectDescription();
		desc.setName("abc.project");
		ICommand[] commands = new ICommand[1];
		commands[0] = desc.newCommand();
		commands[0].setBuilderName("org.eclipse.jdt.core.javabuilder");
		desc.setBuildSpec(commands);
		String[] natures = new String[1];
		natures[0] = "org.eclipse.jdt.core.javanature";
		desc.setNatureIds(natures);
		HashMap<IPath, LinkDescription> linkMap = new HashMap<>();
		LinkDescription link = createLinkDescription("newLink", IResource.FOLDER, "d:/abc/def");
		linkMap.put(link.getProjectRelativePath(), link);
		desc.setLinkDescriptions(linkMap);
		result.put(desc.getName(), desc);
		commands = null;
		natures = null;
		link = null;
		linkMap = null;
		desc = null;

		desc = new ProjectDescription();
		desc.setName("def.project");
		commands = new ICommand[1];
		commands[0] = desc.newCommand();
		commands[0].setBuilderName("org.eclipse.jdt.core.javabuilder");
		desc.setBuildSpec(commands);
		natures = new String[1];
		natures[0] = "org.eclipse.jdt.core.javanature";
		desc.setNatureIds(natures);
		linkMap = new HashMap<>();
		link = createLinkDescription("newLink", IResource.FOLDER, "d:/abc/def");
		linkMap.put(link.getProjectRelativePath(), link);
		link = createLinkDescription("link2", IResource.FOLDER, "d:/abc");
		linkMap.put(link.getProjectRelativePath(), link);
		link = createLinkDescription("link3", IResource.FOLDER, "d:/abc/def/ghi");
		linkMap.put(link.getProjectRelativePath(), link);
		link = createLinkDescription("link4", IResource.FILE, "d:/abc/def/afile.txt");
		linkMap.put(link.getProjectRelativePath(), link);
		desc.setLinkDescriptions(linkMap);
		result.put(desc.getName(), desc);
		commands = null;
		natures = null;
		link = null;
		linkMap = null;
		desc = null;

		desc = new ProjectDescription();
		desc.setName("org.apache.lucene.project");
		IProject[] refProjects = new Project[2];
		refProjects[0] = root.getProject("org.eclipse.core.boot");
		refProjects[1] = root.getProject("org.eclipse.core.runtime");
		desc.setReferencedProjects(refProjects);
		commands = new ICommand[3];
		commands[0] = desc.newCommand();
		commands[0].setBuilderName("org.eclipse.jdt.core.javabuilder");
		commands[1] = desc.newCommand();
		commands[1].setBuilderName("org.eclipse.pde.ManifestBuilder");
		commands[2] = desc.newCommand();
		commands[2].setBuilderName("org.eclipse.pde.SchemaBuilder");
		desc.setBuildSpec(commands);
		natures = new String[2];
		natures[0] = "org.eclipse.jdt.core.javanature";
		natures[1] = "org.eclipse.pde.PluginNature";
		desc.setNatureIds(natures);
		result.put(desc.getName(), desc);
		refProjects = null;
		commands = null;
		natures = null;
		desc = null;

		desc = new ProjectDescription();
		desc.setName("org.eclipse.ant.core.project");
		refProjects = new Project[4];
		refProjects[0] = root.getProject("org.apache.ant");
		refProjects[1] = root.getProject("org.apache.xerces");
		refProjects[2] = root.getProject("org.eclipse.core.boot");
		refProjects[3] = root.getProject("org.eclipse.core.runtime");
		desc.setReferencedProjects(refProjects);
		commands = new ICommand[2];
		commands[0] = desc.newCommand();
		commands[0].setBuilderName("org.eclipse.jdt.core.javabuilder");
		commands[1] = desc.newCommand();
		commands[1].setBuilderName("org.eclipse.ui.externaltools.ExternalToolBuilder");
		HashMap<String, String> argMap = new HashMap<>();
		argMap.put("!{tool_show_log}", "true");
		argMap.put("!{tool_refresh}", "${none}");
		argMap.put("!{tool_name}", "org.eclipse.ant.core extra builder");
		argMap.put("!{tool_dir}", "");
		argMap.put("!{tool_args}", "-DbuildType=${build_type}");
		argMap.put("!{tool_loc}", "${workspace_loc:/org.eclipse.ant.core/scripts/buildExtraJAR.xml}");
		argMap.put("!{tool_type}", "org.eclipse.ui.externaltools.type.ant");
		commands[1].setArguments(argMap);
		desc.setBuildSpec(commands);
		natures = new String[1];
		natures[0] = "org.eclipse.jdt.core.javanature";
		desc.setNatureIds(natures);
		result.put(desc.getName(), desc);
		refProjects = null;
		commands = null;
		natures = null;
		desc = null;

		return result;
	}

	private void compareBuildSpecs(int errorTag, ICommand[] commands, ICommand[] commands2) {
		// ASSUMPTION:  commands and commands2 are non-null
		assertEquals(errorTag + ".2.0", commands.length, commands2.length);
		for (int i = 0; i < commands.length; i++) {
			assertTrue(errorTag + ".2." + (i + 1) + "0", commands[i].getBuilderName().equals(commands2[i].getBuilderName()));
			Map<String, String> args = commands[i].getArguments();
			Map<String, String> args2 = commands2[i].getArguments();
			assertEquals(errorTag + ".2." + (i + 1) + "0", args.size(), args2.size());
			int x = 1;
			for (Entry<String, String> entry : args.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				String value2 = args2.get(key);
				if (value == null) {
					assertNull(errorTag + ".2." + (i + 1) + x, value2);
				} else {
					assertTrue(errorTag + ".3." + (i + 1) + x, args.get(key).equals((args2.get(key))));
				}
				x++;
			}
		}
	}

	private void compareLinks(int errorTag, HashMap<IPath, LinkDescription> links, HashMap<IPath, LinkDescription> links2) {
		if (links == null) {
			assertNull(errorTag + ".4.0", links2);
			return;
		}
		assertEquals(errorTag + ".4.01", links.size(), links2.size());
		int x = 1;
		for (Entry<IPath, LinkDescription> entry : links.entrySet()) {
			IPath key = entry.getKey();
			LinkDescription value = entry.getValue();
			LinkDescription value2 = links2.get(key);
			assertTrue(errorTag + ".4." + x, value.getProjectRelativePath().equals(value2.getProjectRelativePath()));
			assertEquals(errorTag + ".5." + x, value.getType(), value2.getType());
			assertEquals(errorTag + ".6." + x, value.getLocationURI(), value2.getLocationURI());
			x++;
		}
	}

	private void compareNatures(int errorTag, String[] natures, String[] natures2) {
		// ASSUMPTION:  natures and natures2 are non-null
		assertEquals(errorTag + ".3.0", natures.length, natures2.length);
		for (int i = 0; i < natures.length; i++) {
			assertTrue(errorTag + ".3." + (i + 1), natures[i].equals(natures2[i]));
		}
	}

	private void compareProjectDescriptions(int errorTag, ProjectDescription description, ProjectDescription description2) {
		assertTrue(errorTag + ".0", description.getName().equals(description2.getName()));
		String comment = description.getComment();
		if (comment == null) {
			// The old reader previously returned null for an empty comment.  We
			// are changing this so it now returns an empty string.
			assertEquals(errorTag + ".1", 0, description2.getComment().length());
		} else {
			assertTrue(errorTag + ".2", description.getComment().equals(description2.getComment()));
		}

		IProject[] projects = description.getReferencedProjects();
		IProject[] projects2 = description2.getReferencedProjects();
		compareProjects(errorTag, projects, projects2);

		ICommand[] commands = description.getBuildSpec();
		ICommand[] commands2 = description2.getBuildSpec();
		compareBuildSpecs(errorTag, commands, commands2);

		String[] natures = description.getNatureIds();
		String[] natures2 = description2.getNatureIds();
		compareNatures(errorTag, natures, natures2);

		HashMap<IPath, LinkDescription> links = description.getLinks();
		HashMap<IPath, LinkDescription> links2 = description2.getLinks();
		compareLinks(errorTag, links, links2);
	}

	private void compareProjects(int errorTag, IProject[] projects, IProject[] projects2) {
		// ASSUMPTION:  projects and projects2 are non-null
		assertEquals(errorTag + ".1.0", projects.length, projects2.length);
		for (int i = 0; i < projects.length; i++) {
			assertTrue(errorTag + ".1." + (i + 1), projects[i].getName().equals(projects2[i].getName()));
		}
	}

	protected boolean contains(Object key, Object[] array) {
		for (Object element : array) {
			if (key.equals(element)) {
				return true;
			}
		}
		return false;
	}

	private LinkDescription createLinkDescription(IPath path, int type, URI location) {
		LinkDescription result = new LinkDescription();
		result.setPath(path);
		result.setType(type);
		result.setLocationURI(location);
		return result;
	}

	private LinkDescription createLinkDescription(String path, int type, String location) {
		return createLinkDescription(new Path(path), type, uriFromPortableString(location));
	}

	private String getLongDescription() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<projectDescription>" + "<name>org.eclipse.help.ui</name>" + "<comment></comment>" + "<charset>UTF-8</charset>" + "	<projects>" + "	<project>org.eclipse.core.boot</project>" + "	<project>org.eclipse.core.resources</project>" + "	<project>org.eclipse.core.runtime</project>" + "	<project>org.eclipse.help</project>" + "	<project>org.eclipse.help.appserver</project>" + "	<project>org.eclipse.search</project>" + "	<project>org.eclipse.ui</project>" + "	</projects>" + "	<buildSpec>" + "	<buildCommand>" + "	<name>org.eclipse.jdt.core.javabuilder</name>" + "	<arguments>" + "	</arguments>" + "	</buildCommand>" + "	<buildCommand>" + "	<name>org.eclipse.pde.ManifestBuilder</name>" + "	<arguments>"
				+ "	</arguments>" + "	</buildCommand>" + "	<buildCommand>" + "	<name>org.eclipse.pde.SchemaBuilder</name>" + "	<arguments>" + "	</arguments>" + "	</buildCommand>" + "	</buildSpec>" + "	<natures>" + "	<nature>org.eclipse.jdt.core.javanature</nature>" + "	<nature>org.eclipse.pde.PluginNature</nature>" + "	</natures>" + "	<linkedResources>" + "	<link>" + "	<name>contexts.xml</name>" + "	<type>1</type>" + "	<location>" + LONG_LOCATION + "</location>" + "	</link>" + "	<link>" + "	<name>doc</name>" + "	<type>2</type>" + "	<location>" + LONG_LOCATION + "</location>" + "	</link>" + "	<link>" + "	<name>icons</name>" + "	<type>2</type>" + "	<location>" + LONG_LOCATION + "</location>" + "	</link>" + "	<link>" + "	<name>preferences.ini</name>"
				+ "	<type>1</type>" + "	<location>" + LONG_LOCATION + "</location>" + "	</link>" + "	<link>" + "	<name>.options</name>" + "	<type>1</type>" + "	<location>" + LONG_LOCATION + "</location>" + "	</link>" + "	<link>" + "	<name>plugin.properties</name>" + "	<type>1</type>" + "	<location>" + LONG_LOCATION + "</location>" + "	</link>" + "	<link>" + "	<name>plugin.xml</name>" + "	<type>1</type>" + "	<location>" + LONG_LOCATION + "</location>" + "	</link>" + "	<link>" + "	<name>about.html</name>" + "	<type>1</type>" + "	<location>" + LONG_LOCATION + "</location>" + "	</link>" + "	<link>" + "	<name>helpworkbench.jar</name>" + "	<type>1</type>" + "	<location>" + LONG_LOCATION + "</location>" + "	</link>" + "</linkedResources>" + "</projectDescription>";
	}

	private String getLongDescriptionURI() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<projectDescription>" + "<name>org.eclipse.help.ui</name>" + "<comment></comment>" + "<charset>UTF-8</charset>" + "	<projects>" + "	<project>org.eclipse.core.boot</project>" + "	<project>org.eclipse.core.resources</project>" + "	<project>org.eclipse.core.runtime</project>" + "	<project>org.eclipse.help</project>" + "	<project>org.eclipse.help.appserver</project>" + "	<project>org.eclipse.search</project>" + "	<project>org.eclipse.ui</project>" + "	</projects>" + "	<buildSpec>" + "	<buildCommand>" + "	<name>org.eclipse.jdt.core.javabuilder</name>" + "	<arguments>" + "	</arguments>" + "	</buildCommand>" + "	<buildCommand>" + "	<name>org.eclipse.pde.ManifestBuilder</name>" + "	<arguments>"
				+ "	</arguments>" + "	</buildCommand>" + "	<buildCommand>" + "	<name>org.eclipse.pde.SchemaBuilder</name>" + "	<arguments>" + "	</arguments>" + "	</buildCommand>" + "	</buildSpec>" + "	<natures>" + "	<nature>org.eclipse.jdt.core.javanature</nature>" + "	<nature>org.eclipse.pde.PluginNature</nature>" + "	</natures>" + "	<linkedResources>" + "	<link>" + "	<name>contexts.xml</name>" + "	<type>1</type>" + "	<locationURI>" + LONG_LOCATION_URI + "</locationURI>" + "	</link>" + "	<link>" + "	<name>doc</name>" + "	<type>2</type>" + "	<locationURI>" + LONG_LOCATION_URI + "</locationURI>" + "	</link>" + "	<link>" + "	<name>icons</name>" + "	<type>2</type>" + "	<locationURI>" + LONG_LOCATION_URI + "</locationURI>" + "	</link>" + "	<link>"
				+ "	<name>preferences.ini</name>" + "	<type>1</type>" + "	<locationURI>" + LONG_LOCATION_URI + "</locationURI>" + "	</link>" + "	<link>" + "	<name>.options</name>" + "	<type>1</type>" + "	<locationURI>" + LONG_LOCATION_URI + "</locationURI>" + "	</link>" + "	<link>" + "	<name>plugin.properties</name>" + "	<type>1</type>" + "	<locationURI>" + LONG_LOCATION_URI + "</locationURI>" + "	</link>" + "	<link>" + "	<name>plugin.xml</name>" + "	<type>1</type>" + "	<locationURI>" + LONG_LOCATION_URI + "</locationURI>" + "	</link>" + "	<link>" + "	<name>about.html</name>" + "	<type>1</type>" + "	<locationURI>" + LONG_LOCATION_URI + "</locationURI>" + "	</link>" + "	<link>" + "	<name>helpworkbench.jar</name>" + "	<type>1</type>" + "	<locationURI>"
				+ LONG_LOCATION_URI + "</locationURI>" + "	</link>" + "</linkedResources>" + "</projectDescription>";
	}

	/**
	 * Reads and returns the project description stored in the given file store.
	 * @param store
	 * @return
	 * @throws CoreException
	 * @throws IOException
	 */
	private ProjectDescription readDescription(IFileStore store) throws CoreException {
		InputStream input = null;
		try {
			input = store.openInputStream(EFS.NONE, getMonitor());
			InputSource in = new InputSource(input);
			return new ProjectDescriptionReader(getWorkspace()).read(in);
		} finally {
			assertClose(input);
		}
	}

	/**
	 * Verifies that project description file is written in a consistent way.
	 * (bug 177148)
	 */
	public void testConsistentWrite() throws Throwable {
		String locationA = getTempDir().append("testPath1").toPortableString();
		String locationB = getTempDir().append("testPath1").toPortableString();
		String newline = System.lineSeparator();
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + newline + "<projectDescription>" + newline + "	<name>MyProjectDescription</name>" + newline + "	<comment></comment>" + newline + "	<projects>" + newline + "	</projects>" + newline + "	<buildSpec>" + newline + "		<buildCommand>" + newline + "			<name>MyCommand</name>" + newline + "			<arguments>" + newline + "				<dictionary>" + newline + "					<key>aA</key>" + newline + "					<value>2 x ARGH!</value>" + newline + "				</dictionary>" + newline + "				<dictionary>" + newline + "					<key>b</key>" + newline + "					<value>ARGH!</value>" + newline + "				</dictionary>" + newline
				+ "			</arguments>" + newline + "		</buildCommand>" + newline + "	</buildSpec>" + newline + "	<natures>" + newline + "	</natures>" + newline + "	<linkedResources>" + newline + "		<link>" + newline + "			<name>pathA</name>" + newline + "			<type>2</type>" + newline + "			<location>" + locationA + "</location>" + newline + "		</link>" + newline + "		<link>" + newline + "			<name>pathB</name>" + newline + "			<type>2</type>" + newline + "			<location>" + locationB + "</location>" + newline + "		</link>" + newline + "	</linkedResources>" + newline + "</projectDescription>" + newline;

		IFileStore tempStore = getTempStore();
		URI location = tempStore.toURI();

		ProjectDescription description = new ProjectDescription();
		description.setLocationURI(location);
		description.setName("MyProjectDescription");
		HashMap<String, String> args = new HashMap<>(2);
		// key values are important
		args.put("b", "ARGH!");
		args.put("aA", "2 x ARGH!");
		ICommand[] commands = new ICommand[1];
		commands[0] = description.newCommand();
		commands[0].setBuilderName("MyCommand");
		commands[0].setArguments(args);
		description.setBuildSpec(commands);
		HashMap<IPath, LinkDescription> linkDescriptions = new HashMap<>(2);
		LinkDescription link = createLinkDescription("pathB", IResource.FOLDER, locationB);
		// key values are important
		linkDescriptions.put(link.getProjectRelativePath(), link);
		link = createLinkDescription("pathA", IResource.FOLDER, locationA);
		linkDescriptions.put(link.getProjectRelativePath(), link);
		description.setLinkDescriptions(linkDescriptions);

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		new ModelObjectWriter().write(description, buffer, System.lineSeparator());
		String result = buffer.toString();

		// order of keys in serialized file should be exactly the same as expected
		assertEquals("1.0", expected, result);
	}

	public void testInvalidProjectDescription1() throws Throwable {
		String invalidProjectDescription = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<homeDescription>\n" + "	<name>abc</name>\n" + "	<comment></comment>\n" + "	<projects>\n" + "	</projects>\n" + "	<buildSpec>\n" + "		<buildCommand>\n" + "			<name>org.eclipse.jdt.core.javabuilder</name>\n" + "			<arguments>\n" + "			</arguments>\n" + "		</buildCommand>\n" + "	</buildSpec>\n" + "	<natures>\n" + "	<nature>org.eclipse.jdt.core.javanature</nature>\n" + "	</natures>\n" + "	<linkedResources>\n" + "		<link>\n" + "			<name>newLink</name>\n" + "			<type>2</type>\n" + "			<location>" + PATH_STRING + "</location>\n" + "		</link>\n" + "	</linkedResources>\n" + "</homeDescription>";

		IWorkspace workspace = getWorkspace();
		IPath root = workspace.getRoot().getLocation();
		IPath location = root.append("ModelObjectReaderWriterTest.txt");
		ProjectDescriptionReader reader = new ProjectDescriptionReader(workspace);
		// Write out the project description file
		ensureDoesNotExistInFileSystem(location.toFile());
		InputStream stream = new ByteArrayInputStream(invalidProjectDescription.getBytes());
		createFileInFileSystem(location, stream);
		try {
			ProjectDescription projDesc = reader.read(location);
			assertNull("1.0", projDesc);
		} finally {
			Workspace.clear(location.toFile());
		}
	}

	public void testInvalidProjectDescription2() throws Throwable {
		String invalidProjectDescription = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<projectDescription>\n" + "	<bogusname>abc</bogusname>\n" + "</projectDescription>";

		IFileStore store = getTempStore();
		// Write out the project description file
		InputStream stream = new ByteArrayInputStream(invalidProjectDescription.getBytes());
		createFileInFileSystem(store, stream);
		ProjectDescription projDesc = readDescription(store);
		assertNotNull("2.0", projDesc);
		assertNull("2.1", projDesc.getName());
		assertEquals("2.2", 0, projDesc.getComment().length());
		assertNull("2.3", projDesc.getLocationURI());
		assertEquals("2.4", new IProject[0], projDesc.getReferencedProjects());
		assertEquals("2.5", new String[0], projDesc.getNatureIds());
		assertEquals("2.6", new ICommand[0], projDesc.getBuildSpec());
		assertNull("2.7", projDesc.getLinks());
	}

	public void testInvalidProjectDescription3() throws Throwable {
		String invalidProjectDescription = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<projectDescription>\n" + "	<name>abc</name>\n" + "	<comment></comment>\n" + "	<projects>\n" + "	</projects>\n" + "	<buildSpec>\n" + "		<badBuildCommand>\n" + "			<name>org.eclipse.jdt.core.javabuilder</name>\n" + "			<arguments>\n" + "			</arguments>\n" + "		</badBuildCommand>\n" + "	</buildSpec>\n" + "</projectDescription>";

		IFileStore store = getTempStore();
		// Write out the project description file
		InputStream stream = new ByteArrayInputStream(invalidProjectDescription.getBytes());
		createFileInFileSystem(store, stream);

		ProjectDescription projDesc = readDescription(store);
		assertNotNull("3.0", projDesc);
		assertTrue("3.1", projDesc.getName().equals("abc"));
		assertEquals("3.2", 0, projDesc.getComment().length());
		assertNull("3.3", projDesc.getLocationURI());
		assertEquals("3.4", new IProject[0], projDesc.getReferencedProjects());
		assertEquals("3.5", new String[0], projDesc.getNatureIds());
		assertEquals("3.6", new ICommand[0], projDesc.getBuildSpec());
		assertNull("3.7", projDesc.getLinks());
	}

	public void testInvalidProjectDescription4() throws Throwable {
		String invalidProjectDescription = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<projectDescription>\n" + "	<name>abc</name>\n" + "	<comment></comment>\n" + "	<projects>\n" + "	</projects>\n" + "	<buildSpec>\n" + "	</buildSpec>\n" + "	<natures>\n" + "	</natures>\n" + "	<linkedResources>\n" + "		<link>\n" + "			<name>newLink</name>\n" + "			<type>foobar</type>\n" + "			<location>" + PATH_STRING + "</location>\n" + "		</link>\n" + "	</linkedResources>\n" + "</projectDescription>";

		IFileStore store = getTempStore();
		// Write out the project description file
		InputStream stream = new ByteArrayInputStream(invalidProjectDescription.getBytes());
		createFileInFileSystem(store, stream);
		ProjectDescription projDesc = readDescription(store);
		assertNotNull("3.0", projDesc);
		assertTrue("3.1", projDesc.getName().equals("abc"));
		assertEquals("3.2", 0, projDesc.getComment().length());
		assertNull("3.3", projDesc.getLocationURI());
		assertEquals("3.4", new IProject[0], projDesc.getReferencedProjects());
		assertEquals("3.5", new String[0], projDesc.getNatureIds());
		assertEquals("3.6", new ICommand[0], projDesc.getBuildSpec());
		LinkDescription link = projDesc.getLinks().values().iterator().next();
		assertEquals("3.7", new Path("newLink"), link.getProjectRelativePath());
		assertEquals("3.8", PATH_STRING, URIUtil.toPath(link.getLocationURI()).toString());
	}

	/**
	 * Tests a project description with a very long local location for a linked resource.
	 */
	public void testLongProjectDescription() throws Throwable {
		String longProjectDescription = getLongDescription();

		IPath location = getRandomLocation();
		try {
			ProjectDescriptionReader reader = new ProjectDescriptionReader(getWorkspace());
			// Write out the project description file
			ensureDoesNotExistInFileSystem(location.toFile());
			InputStream stream = new ByteArrayInputStream(longProjectDescription.getBytes());
			createFileInFileSystem(location, stream);
			ProjectDescription projDesc = reader.read(location);
			ensureDoesNotExistInFileSystem(location.toFile());
			for (LinkDescription link : projDesc.getLinks().values()) {
				assertEquals("1.0." + link.getProjectRelativePath(), LONG_LOCATION_URI, link.getLocationURI());
			}
		} finally {
			Workspace.clear(location.toFile());
		}
	}

	/**
	 * Tests a project description with a very long URI location for linked resource.
	 */
	public void testLongProjectDescriptionURI() throws Throwable {
		String longProjectDescription = getLongDescriptionURI();
		IPath location = getRandomLocation();
		try {
			ProjectDescriptionReader reader = new ProjectDescriptionReader(ResourcesPlugin.getWorkspace());
			// Write out the project description file
			ensureDoesNotExistInFileSystem(location.toFile());
			InputStream stream = new ByteArrayInputStream(longProjectDescription.getBytes());
			createFileInFileSystem(location, stream);
			ProjectDescription projDesc = reader.read(location);
			ensureDoesNotExistInFileSystem(location.toFile());
			for (LinkDescription link : projDesc.getLinks().values()) {
				assertEquals("1.0." + link.getProjectRelativePath(), LONG_LOCATION_URI, link.getLocationURI());
			}
		} finally {
			Workspace.clear(location.toFile());
		}
	}

	public void testMultiLineCharFields() throws Throwable {
		String multiLineProjectDescription = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<projectDescription>\n" + "	<name>\n" + "      abc\n" + "   </name>\n" + "	<charset>\n" + "		ISO-8859-1\n" + "	</charset>\n" + "	<comment>This is the comment.</comment>\n" + "	<projects>\n" + "	   <project>\n" + "         org.eclipse.core.boot\n" + "      </project>\n" + "	</projects>\n" + "	<buildSpec>\n" + "		<buildCommand>\n" + "			<name>\n" + "              org.eclipse.jdt.core.javabuilder\n" + "           </name>\n" + "			<arguments>\n" + "              <key>thisIsTheKey</key>\n" + "              <value>thisIsTheValue</value>\n" + "			</arguments>\n" + "		</buildCommand>\n" + "	</buildSpec>\n" + "	<natures>\n" + "	   <nature>\n"
				+ "         org.eclipse.jdt.core.javanature\n" + "      </nature>\n" + "	</natures>\n" + "	<linkedResources>\n" + "		<link>\n" + "			<name>" + "newLink" + "</name>\n" + "			<type>\n" + "              2\n" + "           </type>\n" + "			<location>" + PATH_STRING + "</location>\n" + "		</link>\n" + "	</linkedResources>\n" + "</projectDescription>";

		String singleLineProjectDescription = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<projectDescription>\n" + "	<name>abc</name>\n" + "	<charset>ISO-8859-1</charset>\n" + "	<comment>This is the comment.</comment>\n" + "	<projects>\n" + "	   <project>org.eclipse.core.boot</project>\n" + "	</projects>\n" + "	<buildSpec>\n" + "		<buildCommand>\n" + "			<name>org.eclipse.jdt.core.javabuilder</name>\n" + "			<arguments>\n" + "              <key>thisIsTheKey</key>\n" + "              <value>thisIsTheValue</value>\n" + "			</arguments>\n" + "		</buildCommand>\n" + "	</buildSpec>\n" + "	<natures>\n" + "	   <nature>org.eclipse.jdt.core.javanature</nature>\n" + "	</natures>\n" + "	<linkedResources>\n" + "		<link>\n"
				+ "			<name>newLink</name>\n" + "			<type>2</type>\n" + "			<location>" + PATH_STRING + "</location>\n" + "		</link>\n" + "	</linkedResources>\n" + "</projectDescription>";

		IWorkspace workspace = getWorkspace();
		IPath root = workspace.getRoot().getLocation();
		IPath multiLocation = root.append("multiLineTest.txt");
		IPath singleLocation = root.append("singleLineTest.txt");
		ProjectDescriptionReader reader = new ProjectDescriptionReader(workspace);
		// Write out the project description file
		ensureDoesNotExistInFileSystem(multiLocation.toFile());
		ensureDoesNotExistInFileSystem(singleLocation.toFile());
		InputStream multiStream = new ByteArrayInputStream(multiLineProjectDescription.getBytes());
		InputStream singleStream = new ByteArrayInputStream(singleLineProjectDescription.getBytes());
		try {
			createFileInFileSystem(multiLocation, multiStream);
			createFileInFileSystem(singleLocation, singleStream);
			ProjectDescription multiDesc = reader.read(multiLocation);
			ProjectDescription singleDesc = reader.read(singleLocation);
			compareProjectDescriptions(1, multiDesc, singleDesc);
		} finally {
			Workspace.clear(multiLocation.toFile());
			Workspace.clear(singleLocation.toFile());
		}
	}

	public void testMultipleProjectDescriptions() throws Throwable {
		URL whereToLook = Platform.getBundle("org.eclipse.core.tests.resources").getEntry("MultipleProjectTestFiles/");
		String[] members = {"abc.project", "def.project", "org.apache.lucene.project", "org.eclipse.ant.core.project"};
		HashMap<String, ProjectDescription> baselines = buildBaselineDescriptors();
		ProjectDescriptionReader reader = new ProjectDescriptionReader(getWorkspace());

		for (int i = 0; i < members.length; i++) {
			URL currentURL = null;
			currentURL = new URL(whereToLook, members[i]);
			InputStream is = null;
			try {
				is = currentURL.openStream();
			} catch (IOException e) {
				fail("0.5");
			}
			InputSource in = new InputSource(is);
			ProjectDescription description = reader.read(in);

			compareProjectDescriptions(i + 1, description, baselines.get(members[i]));
		}
	}

	public void testProjectDescription() throws Throwable {

		IFileStore tempStore = getTempStore();
		URI location = tempStore.toURI();
		/* test write */
		ProjectDescription description = new ProjectDescription();
		description.setLocationURI(location);
		description.setName("MyProjectDescription");
		HashMap<String, String> args = new HashMap<>(3);
		args.put("ArgOne", "ARGH!");
		args.put("ArgTwo", "2 x ARGH!");
		args.put("NullArg", null);
		args.put("EmptyArg", "");
		ICommand[] commands = new ICommand[2];
		commands[0] = description.newCommand();
		commands[0].setBuilderName("MyCommand");
		commands[0].setArguments(args);
		commands[1] = description.newCommand();
		commands[1].setBuilderName("MyOtherCommand");
		commands[1].setArguments(args);
		description.setBuildSpec(commands);

		writeDescription(tempStore, description);

		/* test read */
		ProjectDescription description2 = readDescription(tempStore);
		assertTrue("1.1", description.getName().equals(description2.getName()));
		assertEquals("1.2", location, description.getLocationURI());

		ICommand[] commands2 = description2.getBuildSpec();
		assertEquals("2.00", 2, commands2.length);
		assertEquals("2.01", "MyCommand", commands2[0].getBuilderName());
		assertEquals("2.02", "ARGH!", commands2[0].getArguments().get("ArgOne"));
		assertEquals("2.03", "2 x ARGH!", commands2[0].getArguments().get("ArgTwo"));
		assertEquals("2.04", "", commands2[0].getArguments().get("NullArg"));
		assertEquals("2.05", "", commands2[0].getArguments().get("EmptyArg"));
		assertEquals("2.06", "MyOtherCommand", commands2[1].getBuilderName());
		assertEquals("2.07", "ARGH!", commands2[1].getArguments().get("ArgOne"));
		assertEquals("2.08", "2 x ARGH!", commands2[1].getArguments().get("ArgTwo"));
		assertEquals("2.09", "", commands2[0].getArguments().get("NullArg"));
		assertEquals("2.10", "", commands2[0].getArguments().get("EmptyArg"));
	}

	public void testProjectDescription2() throws Throwable {
		// Use ModelObject2 to read the project description

		/* initialize common objects */
		ModelObjectWriter writer = new ModelObjectWriter();
		ProjectDescriptionReader reader = new ProjectDescriptionReader(getWorkspace());
		IFileStore tempStore = getTempStore();
		URI location = tempStore.toURI();
		/* test write */
		ProjectDescription description = new ProjectDescription();
		description.setLocationURI(location);
		description.setName("MyProjectDescription");
		HashMap<String, String> args = new HashMap<>(3);
		args.put("ArgOne", "ARGH!");
		ICommand[] commands = new ICommand[1];
		commands[0] = description.newCommand();
		commands[0].setBuilderName("MyCommand");
		commands[0].setArguments(args);
		description.setBuildSpec(commands);
		String comment = "Now is the time for all good men to come to the aid of the party.  Now is the time for all good men to come to the aid of the party.  Now is the time for all good men to come to the aid of the party.";
		description.setComment(comment);
		IProject[] refProjects = new IProject[3];
		refProjects[0] = ResourcesPlugin.getWorkspace().getRoot().getProject("org.eclipse.core.runtime");
		refProjects[1] = ResourcesPlugin.getWorkspace().getRoot().getProject("org.eclipse.core.boot");
		refProjects[2] = ResourcesPlugin.getWorkspace().getRoot().getProject("org.eclipse.core.resources");
		description.setReferencedProjects(refProjects);

		try (OutputStream output = tempStore.openOutputStream(EFS.NONE, getMonitor())) {
			writer.write(description, output, System.lineSeparator());
		}

		/* test read */
		InputStream input = tempStore.openInputStream(EFS.NONE, getMonitor());
		ProjectDescription description2;
		try {
			InputSource in = new InputSource(input);
			description2 = reader.read(in);
		} finally {
			input.close();
		}
		assertTrue("1.1", description.getName().equals(description2.getName()));
		assertTrue("1.2", location.equals(description.getLocationURI()));

		ICommand[] commands2 = description2.getBuildSpec();
		assertEquals("2.00", 1, commands2.length);
		assertEquals("2.01", "MyCommand", commands2[0].getBuilderName());
		assertEquals("2.02", "ARGH!", commands2[0].getArguments().get("ArgOne"));

		assertTrue("3.0", description.getComment().equals(description2.getComment()));

		IProject[] ref = description.getReferencedProjects();
		IProject[] ref2 = description2.getReferencedProjects();
		assertEquals("4.0", 3, ref2.length);
		assertTrue("4.1", ref[0].getName().equals(ref2[0].getName()));
		assertTrue("4.2", ref[1].getName().equals(ref2[1].getName()));
		assertTrue("4.3", ref[2].getName().equals(ref2[2].getName()));
	}

	// see bug 274437
	public void testProjectDescription3() throws Throwable {
		IFileStore tempStore = getTempStore();
		URI location = tempStore.toURI();
		/* test write */
		ProjectDescription description = new ProjectDescription();
		description.setLocationURI(location);
		description.setName("MyProjectDescription");
		ICommand[] commands = new ICommand[1];
		commands[0] = description.newCommand();
		commands[0].setBuilderName("MyCommand");
		commands[0].setArguments(null);
		description.setBuildSpec(commands);

		writeDescription(tempStore, description);

		/* test read */
		ProjectDescription description2 = readDescription(tempStore);
		assertTrue("1.0", description.getName().equals(description2.getName()));
		assertEquals("2.0", location, description.getLocationURI());

		ICommand[] commands2 = description2.getBuildSpec();
		assertEquals("3.0", 1, commands2.length);
		assertEquals("4.0", "MyCommand", commands2[0].getBuilderName());
		assertEquals("5.0", 0, commands2[0].getArguments().size());
	}

	public void testProjectDescriptionWithSpaces() throws Throwable {

		IFileStore store = getTempStore();
		final Path path = new Path("link");
		URI location = store.toURI();
		URI locationWithSpaces = store.getChild("With some spaces").toURI();
		/* test write */
		ProjectDescription description = new ProjectDescription();
		description.setLocationURI(location);
		description.setName("MyProjectDescription");
		description.setLinkLocation(path, createLinkDescription(path, IResource.FOLDER, locationWithSpaces));

		writeDescription(store, description);

		/* test read */
		ProjectDescription description2 = readDescription(store);
		assertTrue("1.1", description.getName().equals(description2.getName()));
		assertEquals("1.2", location, description.getLocationURI());
		assertEquals("1.3", locationWithSpaces, description2.getLinkLocationURI(path));
	}

	protected URI uriFromPortableString(String pathString) {
		return Path.fromPortableString(pathString).toFile().toURI();
	}

	/**
	 * Writes a project description to a file store
	 * @param store
	 * @param description
	 * @throws IOException
	 * @throws CoreException
	 */
	private void writeDescription(IFileStore store, ProjectDescription description) throws IOException, CoreException {
		OutputStream output = null;
		try {
			output = store.openOutputStream(EFS.NONE, getMonitor());
			new ModelObjectWriter().write(description, output, System.lineSeparator());
		} finally {
			assertClose(output);
		}

	}

	// Regression for Bug 300669
	public void testProjectDescriptionWithFiltersAndNullProject() {
		String projectDescription = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<projectDescription>\n" + //
				"	<name>rome_dfw</name>\n" + //
				"		<comment></comment>\n" + //
				"	<projects>\n" + //
				"	</projects>\n" + //
				"	<linkedResources>\n" + //
				"		<link>\n" + //
				"			<name>OcdTargetPlugin</name>\n" + //
				"			<type>2</type>\n" + //
				"			<location>M:/lcruaud.dfw-main-validation/HSI_api/OcdTargetPlugin</location>\n" + //
				"		</link>\n" + //
				"	</linkedResources>\n" + //
				"	<filteredResources>\n" + //
				"		<filter>\n" + //
				"			<id>1264174785480</id>\n" + //
				"			<name></name>\n" + //
				"			<type>22</type>\n" + //
				"			<matcher>\n" + //
				"				<id>org.eclipse.ui.ide.patternFilterMatcher</id>\n" + //
				"				<arguments>*:*</arguments>\n" + //
				"			</matcher>\n" + //
				"		</filter>\n" + //
				"	</filteredResources>\n" + //
				"</projectDescription>\n";

		IPath root = getWorkspace().getRoot().getLocation();
		IPath location = root.append("ModelObjectReaderWriterTest.txt");
		ProjectDescriptionReader reader = new ProjectDescriptionReader(getWorkspace());
		// Write out the project description file
		ensureDoesNotExistInFileSystem(location.toFile());
		InputStream stream = new ByteArrayInputStream(projectDescription.getBytes());
		createFileInFileSystem(location, stream);
		try {
			ProjectDescription projDesc = reader.read(location);
			assertNotNull("1.0", projDesc);
		} catch (IOException e) {
			fail("1.1", e);
		} finally {
			Workspace.clear(location.toFile());
		}
	}
}
