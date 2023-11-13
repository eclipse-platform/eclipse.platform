/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - derived implementation
 *     Martin Karpisek - bug 195840
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelperRepository;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.helper.AntXMLContext;
import org.apache.tools.ant.helper.ProjectHelper2;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JAXPUtils;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.model.IAntModel;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

/**
 * Derived from the original Ant ProjectHelper2 with help from the JAXPUtils class. This class provides parsing for using a String as a source and
 * provides handlers that will continue parsing to completion upon hitting errors.
 */
public class ProjectHelper extends ProjectHelper2 {

	/**
	 * helper for path -&gt; URI and URI -&gt; path conversions.
	 */
	private static FileUtils fu = null;

	/**
	 * The build file that is to be parsed. Must be set if parsing is to be successful.
	 */
	private File buildFile = null;

	private static String currentEntityName = null;
	private static String currentEntityPath = null;

	private static int currentImportStackSize = 1;

	/**
	 * The Ant Model
	 */
	private static IAntModel fgAntModel;

	/**
	 * The current Ant parsing context
	 */
	private static AntXMLContext fgAntContext;

	private static AntHandler elementHandler = new ElementHandler();
	private static AntHandler projectHandler = new ProjectHandler();
	private static AntHandler targetHandler = new TargetHandler();
	private static AntHandler mainHandler = new MainHandler();
	private static LexicalHandler lexicalHandler = new LexHandler();

	private static XMLReader fgXMLReader = null;
	/*
	 * Required to remember the project names (in case they are required for target-prefixing). The build file(s) are parsed anyway and hence the
	 * project name is added to this map when it gets parsed.
	 */
	private static Map<String, String> parsedProjectNames = null;

	public static class ElementHandler extends ProjectHelper2.ElementHandler {

		private UnknownElement task = null;
		private Task currentTask = null;

		private final Map<String, String> fNormalizedFileNames = new HashMap<>();

		@Override
		public AntHandler onStartChild(String uri, String tag, String qname, Attributes attrs, AntXMLContext context) {
			return ProjectHelper.elementHandler;
		}

		@Override
		public void onStartElement(String uri, String tag, String qname, Attributes attrs, AntXMLContext context) {
			try {
				RuntimeConfigurable wrapper = context.currentWrapper();
				currentTask = null;
				task = null;
				if (wrapper != null && (wrapper.getProxy() instanceof Task)) {
					currentTask = (Task) wrapper.getProxy();
				}
				onStartElement0(uri, tag, qname, attrs, context);

				Locator locator = context.getLocator();
				getAntModel().addTask(task, currentTask, attrs, locator.getLineNumber(), locator.getColumnNumber());

			}
			catch (BuildException be) {
				Locator locator = context.getLocator();
				getAntModel().addTask(task, currentTask, attrs, locator.getLineNumber(), locator.getColumnNumber());
				getAntModel().error(be);
			}
		}

		@Override
		public void onEndElement(String uri, String tag, AntXMLContext context) {
			super.onEndElement(uri, tag, context);

			Locator locator = context.getLocator();
			if (getAntModel().canGetTaskInfo()) {
				getAntModel().setCurrentElementLength(locator.getLineNumber(), locator.getColumnNumber());
			}
		}

		private void onStartElement0(String uri, String tag, String qname, Attributes attrs, AntXMLContext context) {

			RuntimeConfigurable parentWrapper = context.currentWrapper();
			Object parent = null;

			if (parentWrapper != null) {
				parent = parentWrapper.getProxy();
			}

			/*
			 * UnknownElement is used for tasks and data types - with delayed eval
			 */
			task = new UnknownElement(tag);
			task.setProject(context.getProject());
			task.setNamespace(uri);
			task.setQName(qname);
			task.setTaskType(org.apache.tools.ant.ProjectHelper.genComponentName(task.getNamespace(), tag));
			task.setTaskName(qname);

			Locator contextLocator = context.getLocator();
			String fileName = contextLocator.getSystemId();
			String normalizedFileName = fNormalizedFileNames.get(fileName);
			if (normalizedFileName == null) {
				if (fileName.startsWith(IAntCoreConstants.FILE_PROTOCOL)) {
					normalizedFileName = getFileUtils().fromURI(fileName);
					fNormalizedFileNames.put(fileName, normalizedFileName);
				} else {
					normalizedFileName = fileName;
				}
			}

			Target currentTarget = context.getCurrentTarget();
			Location location = new Location(normalizedFileName, contextLocator.getLineNumber(), contextLocator.getColumnNumber());
			task.setLocation(location);
			task.setOwningTarget(currentTarget);

			if (parent != null) {
				// Nested element
				((UnknownElement) parent).addChild(task);
			} else {
				// Task included in a target ( including the default one ).
				currentTarget.addTask(task);
			}

			// do not configure the id of an augment node, it is resolved
			// when it is configured
			if (!IAntCoreConstants.AUGMENT.equals(task.getQName())) {
				context.configureId(task, attrs);
			}

			// container.addTask(task);
			// This is a nop in UE: task.init();

			RuntimeConfigurable wrapper = new RuntimeConfigurable(task, task.getTaskName());

			for (int i = 0; i < attrs.getLength(); i++) {
				String attrUri = attrs.getURI(i);
				if (attrUri != null && attrUri.length() != 0 && !attrUri.equals(uri)) {
					continue; // Ignore attributes from unknown uris
				}
				String name = attrs.getLocalName(i);
				String value = attrs.getValue(i);
				// PR: Hack for ant-type value
				// an ant-type is a component name which can
				// be namespaced, need to extract the name
				// and convert from qualified name to uri/name
				if (name.equals("ant-type")) { //$NON-NLS-1$
					int index = value.indexOf(':');
					if (index != -1) {
						String prefix = value.substring(0, index);
						String mappedUri = context.getPrefixMapping(prefix);
						if (mappedUri == null) {
							throw new BuildException("Unable to find XML NS prefix " + prefix); //$NON-NLS-1$
						}
						value = org.apache.tools.ant.ProjectHelper.genComponentName(mappedUri, value.substring(index + 1));
					}
				}
				wrapper.setAttribute(name, value);
			}

			if (parentWrapper != null) {
				parentWrapper.addChild(wrapper);
			}

			context.pushWrapper(wrapper);
		}

		@Override
		public void characters(char[] buf, int start, int count, AntXMLContext context) {
			try {
				super.characters(buf, start, count, context);
			}
			catch (SAXParseException e) {
				ErrorHelper.handleErrorFromElementText(start, count, context, e);
			}
			catch (BuildException be) {
				ErrorHelper.handleErrorFromElementText(start, count, context, be);
			}
		}

		public void reset() {
			task = null;
			currentTask = null;
			fNormalizedFileNames.clear();
		}
	}

	public static class MainHandler extends ProjectHelper2.MainHandler {

		@Override
		public AntHandler onStartChild(String uri, String name, String qname, Attributes attrs, AntXMLContext context) throws SAXParseException {
			if (name.equals("project") //$NON-NLS-1$
					&& (uri.length() == 0 || uri.equals(ANT_CORE_URI))) {
				return ProjectHelper.projectHandler;
			}
			try {
				return super.onStartChild(uri, name, qname, attrs, context);
			}
			catch (SAXParseException e) {
				getAntModel().error(e);
				throw e;
			}
		}
	}

	/**
	 * Gets the associated project name by the absolute build-file path
	 *
	 * @param buildFile
	 *            The file
	 * @return The project name
	 */
	public static String getProjectNameOfBuildFile(IFile buildFile) {
		return parsedProjectNames.get(getBuildFileKey(buildFile));
	}

	/**
	 * Gets the associated project name by the absolute build-file path
	 *
	 * @param absolutePath
	 *            The file path as String
	 * @return The project name
	 */
	public static String getProjectNameOfBuildFile(String absolutePath) {
		return parsedProjectNames.get(absolutePath);
	}

	/**
	 * Builds the hash-map's build-file key
	 *
	 * @param buildFile
	 *            The build file
	 * @return The key as string
	 */
	private static String getBuildFileKey(IFile buildFile) {
		return buildFile.getLocation().toFile().getAbsolutePath();
	}

	/**
	 * Adds a parsed project-name to the property-holder (and initializes the collection if necessary)
	 *
	 * @param key
	 *            The key (file-path).
	 * @param projectName
	 *            The projectname to add.
	 */
	public static void storeParsedProjectName(String key, String projectName) {
		// init if required
		if (parsedProjectNames == null) {
			parsedProjectNames = new HashMap<>();
		}
		parsedProjectNames.put(key, projectName);
	}

	/**
	 * Clear the parsed project-holder to avoid potential memory leaks.
	 */
	public static void clearAdditionalPropertyHolders() {
		/*
		 * Currently only this "property-holder" is used. Extend if necessary.
		 */
		if (parsedProjectNames != null) {
			parsedProjectNames.clear();
			parsedProjectNames = null;
		}
	}

	public static class ProjectHandler extends ProjectHelper2.ProjectHandler {

		@Override
		public AntHandler onStartChild(String uri, String name, String qname, Attributes attrs, AntXMLContext context) {
			if ((name.equals("target") || name.equals("extension-point"))//$NON-NLS-1$ //$NON-NLS-2$
					&& (uri.length() == 0 || uri.equals(ANT_CORE_URI))) {
				return ProjectHelper.targetHandler;
			}
			return ProjectHelper.elementHandler;
		}

		@Override
		public void onEndElement(String uri, String tag, AntXMLContext context) {
			super.onEndElement(uri, tag, context);
			if (currentImportStackSize == 1) {
				Locator locator = context.getLocator();
				getAntModel().setCurrentElementLength(locator.getLineNumber(), locator.getColumnNumber());
			}
		}

		@Override
		public void onStartElement(String uri, String tag, String qname, Attributes attrs, AntXMLContext context) {
			try {
				super.onStartElement(uri, tag, qname, attrs, context);
				// add project-name to property holder (if no aliases are used, the project name is required for target prefixing)
				String currentProjectName = context.getCurrentProjectName();
				// just an additional check if the name is non-empty
				if (this.isCurrentProjectNameValid(currentProjectName)) {
					if (context.getBuildFile() != null) {
						storeParsedProjectName(context.getBuildFile().getAbsolutePath(), currentProjectName);
					} else if (context.getBuildFileURL() != null) {
						storeParsedProjectName(new File(context.getBuildFileURL().getPath()).getAbsolutePath(), currentProjectName);
					}
				}
			}
			catch (SAXParseException e) {
				getAntModel().error(e);
			}
			catch (BuildException be) {
				getAntModel().error(be);
			}
			if (context.getCurrentTarget() == null) {
				// exception occurred creating the project
				context.getProject().addTarget(IAntCoreConstants.EMPTY_STRING, context.getImplicitTarget());
				context.setCurrentTarget(context.getImplicitTarget());
			}
			if (currentImportStackSize == 1) {
				Locator locator = context.getLocator();
				getAntModel().addProject(context.getProject(), locator.getLineNumber(), locator.getColumnNumber());
			}
		}

		/**
		 * Checks if the parsed value is a valid project name (i.e. is non-empty: It doesn't make sense to store empty project-names)
		 *
		 * @param currentProjectName
		 *            The current project name
		 * @return If the given project name is valid or not.
		 */
		private boolean isCurrentProjectNameValid(String currentProjectName) {
			return currentProjectName != null && !currentProjectName.isEmpty();
		}

		@Override
		public void characters(char[] buf, int start, int count, AntXMLContext context) {
			try {
				super.characters(buf, start, count, context);
			}
			catch (SAXParseException e) {
				ErrorHelper.handleErrorFromElementText(start, count, context, e);
			}
			catch (BuildException be) {
				ErrorHelper.handleErrorFromElementText(start, count, context, be);
			}
		}
	}

	public static class TargetHandler extends ProjectHelper2.TargetHandler {
		@Override
		public AntHandler onStartChild(String uri, String name, String qname, Attributes attrs, AntXMLContext context) {
			return ProjectHelper.elementHandler;
		}

		@Override
		public void onStartElement(String uri, String tag, String qname, Attributes attrs, AntXMLContext context) {
			try {
				super.onStartElement(uri, tag, qname, attrs, context);
				Target newTarget = context.getCurrentTarget();
				Locator locator = context.getLocator();
				getAntModel().addTarget(newTarget, locator.getLineNumber(), locator.getColumnNumber());
			}
			catch (SAXParseException e) {
				handleErrorInTarget(context, e);
			}
			catch (BuildException be) {
				handleErrorInTarget(context, be);
			}
		}

		private void handleErrorInTarget(AntXMLContext context, Exception e) {
			Target newTarget = context.getCurrentTarget();
			Locator locator = context.getLocator();
			getAntModel().addTarget(newTarget, locator.getLineNumber(), locator.getColumnNumber());
			getAntModel().errorFromElement(e, null, locator.getLineNumber(), locator.getColumnNumber());
		}

		@Override
		public void onEndElement(String uri, String tag, AntXMLContext context) {
			super.onEndElement(uri, tag, context);
			Locator locator = context.getLocator();
			getAntModel().setCurrentElementLength(locator.getLineNumber(), locator.getColumnNumber());
		}

		@Override
		public void characters(char[] buf, int start, int count, AntXMLContext context) {
			try {
				super.characters(buf, start, count, context);
			}
			catch (SAXParseException e) {
				ErrorHelper.handleErrorFromElementText(start, count, context, e);
			}
			catch (BuildException be) {
				ErrorHelper.handleErrorFromElementText(start, count, context, be);
			}
		}
	}

	public static class RootHandler extends ProjectHelper2.RootHandler {

		public RootHandler(AntXMLContext context, AntHandler rootHandler) {
			super(context, rootHandler);
		}

		@Override
		public void error(SAXParseException e) {
			getAntModel().error(e);
		}

		@Override
		public void fatalError(SAXParseException e) {
			getAntModel().fatalError(e);
		}

		@Override
		public void warning(SAXParseException e) {
			getAntModel().warning(e);
		}

		@Override
		public InputSource resolveEntity(String publicId, String systemId) {
			InputSource source = super.resolveEntity(publicId, systemId);
			if (source != null) {
				String path = getFileUtils().fromURI(source.getSystemId());
				if (currentEntityName == null) {
					currentEntityPath = path;
				} else {
					getAntModel().addEntity(currentEntityName, path);
					currentEntityName = null;
				}
			}
			return source;
		}

		@Override
		public void startPrefixMapping(String prefix, String uri) {

			super.startPrefixMapping(prefix, uri);
			getAntModel().addPrefixMapping(prefix, uri);
		}
	}

	private static class ErrorHelper {
		public static void handleErrorFromElementText(int start, int count, AntXMLContext context, Exception e) {
			Locator locator = context.getLocator();
			int columnNumber = locator.getColumnNumber();
			if (columnNumber > -1) {
				int offset = start;
				try {
					offset = getAntModel().getOffset(locator.getLineNumber(), 1);
				}
				catch (BadLocationException e1) {
					// do nothing
				}
				getAntModel().errorFromElementText(e, offset, locator.getColumnNumber());
			} else {
				getAntModel().errorFromElementText(e, start, count);
			}
		}
	}

	private static class LexHandler implements LexicalHandler {
		@Override
		public void endCDATA() throws SAXException {
			// do nothing
		}

		@Override
		public void endDTD() throws SAXException {
			if (getAntModel().canGetLexicalInfo()) {
				AntXMLContext context = getContext();
				Locator locator = context.getLocator();
				getAntModel().setCurrentElementLength(locator.getLineNumber(), locator.getColumnNumber());
			}
		}

		@Override
		public void startCDATA() throws SAXException {
			// do nothing
		}

		@Override
		public void comment(char[] ch, int start, int length) throws SAXException {
			if (getAntModel().canGetLexicalInfo()) {
				AntXMLContext context = getContext();
				Locator locator = context.getLocator();
				if (locator != null) {
					getAntModel().addComment(locator.getLineNumber(), locator.getColumnNumber(), length);
				}
			}
		}

		@Override
		public void endEntity(String name) throws SAXException {
			// do nothing
		}

		@Override
		public void startEntity(String name) throws SAXException {
			if (currentEntityPath == null) {
				currentEntityName = name;
			} else {
				getAntModel().addEntity(name, currentEntityPath);
				currentEntityPath = null;
			}
		}

		@Override
		public void startDTD(String name, String publicId, String systemId) throws SAXException {
			if (getAntModel().canGetLexicalInfo()) {
				AntXMLContext context = getContext();
				Locator locator = context.getLocator();
				getAntModel().addDTD(name, locator.getLineNumber(), locator.getColumnNumber());
			}
		}
	}

	public ProjectHelper(IAntModel model) {
		setAntModel(model);
	}

	/**
	 * Constructor
	 * <p>
	 * This constructor is only to be used by the {@link ProjectHelperRepository} when loading instances of registered helpers.
	 * </p>
	 *
	 * @since 3.7
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public ProjectHelper() {
	}

	/**
	 * Parses the project file, configuring the project as it goes.
	 *
	 * @param project
	 *            the current project
	 * @param source
	 *            the XML source or a {@link File}
	 * @param handler
	 *            the root handler to use (contains the current context)
	 * @exception BuildException
	 *                if the configuration is invalid or cannot be read
	 */
	@Override
	public void parse(Project project, Object source, ProjectHelper2.RootHandler handler) throws BuildException {
		if (!(source instanceof String) && !(source instanceof File)) {
			// this should only occur with a source URL and that should not be possible currently
			// as Antlib hard codes using ProjectHelper2 (bug 152793)
			super.parse(project, source, handler);
			return;
		}
		AntXMLContext context = (AntXMLContext) project.getReference("ant.parsing.context"); //$NON-NLS-1$
		// switch to using "our" handler so parsing will continue on hitting errors.
		handler = new RootHandler(context, mainHandler);
		try (InputStream stream = createInputStream(source)) {
			InputSource inputSource = stream == null ? null : new InputSource(stream);

			/**
			 * SAX 2 style parser used to parse the given file.
			 */
			// We cannot use the JAXPUtils support here as the underlying parser factory is cached and
			// will not reflect classpath changes that effect which XML parser will be returned.
			// see bug 59764
			// XMLReader parser = JAXPUtils.getNamespaceXMLReader();
			XMLReader parser = getNamespaceXMLReader();
			if (parser == null) {
				throw new BuildException(ProjectHelperMessages.ProjectHelper_0);
			}
			String uri = null;
			if (buildFile != null) {
				uri = getFileUtils().toURI(buildFile.getAbsolutePath());
			}

			if (uri != null) {
				inputSource.setSystemId(uri);
			}

			context.setBuildFile(buildFile);

			parser.setContentHandler(handler);
			parser.setEntityResolver(handler);
			parser.setErrorHandler(handler);
			parser.setDTDHandler(handler);
			parser.setProperty("http://xml.org/sax/properties/lexical-handler", lexicalHandler); //$NON-NLS-1$

			parser.parse(inputSource);
		}
		catch (SAXParseException exc) {
			getAntModel().fatalError(exc);
		}
		catch (SAXException exc) {
			// ignore as we will be parsing incomplete source
		}
		catch (FileNotFoundException exc) {
			throw new BuildException(exc);
		}
		catch (UnsupportedEncodingException exc) {
			throw new BuildException(exc);
		}
		catch (IOException exc) {
			throw new BuildException(exc);
		}
	}

	private InputStream createInputStream(Object source) throws FileNotFoundException, UnsupportedEncodingException {
		if ((source instanceof File)) {
			buildFile = (File) source;
			buildFile = getFileUtils().normalize(buildFile.getAbsolutePath());
			return new FileInputStream(buildFile);
		} else if (source instanceof String) {
			IAntModel model = getAntModel();
			String encoding = IAntCoreConstants.UTF_8;
			if (model != null) {
				encoding = model.getEncoding();
			}
			return new ByteArrayInputStream(((String) source).getBytes(encoding));
		}
		return null;
	}

	/**
	 * Sets the buildfile that is about to be parsed or <code>null</code> if parsing has completed.
	 *
	 * @param file
	 *            The buildfile about to be parsed
	 */
	public void setBuildFile(File file) {
		buildFile = file;
		currentImportStackSize = 1;
	}

	/*
	 * We override this method from ProjectHelper2 as we do not want to execute the implicit target or any other target for that matter as it could
	 * hang Eclipse. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=50795 for more details.
	 */
	@Override
	public void parse(Project project, Object source) throws BuildException {
		AntXMLContext context = (AntXMLContext) project.getReference("ant.parsing.context"); //$NON-NLS-1$
		if (context == null) {
			getImportStack().removeAllElements();
			context = new AntXMLContext(project);
			project.addReference("ant.parsing.context", context); //$NON-NLS-1$
			project.addReference("ant.targets", context.getTargets()); //$NON-NLS-1$
			fgAntContext = context;
		}
		getImportStack().addElement(source);
		currentImportStackSize = getImportStack().size();
		if (getImportStack().size() > 1) {
			// we are in an imported file.
			context.setIgnoreProjectTag(true);
			Target currentTarget = context.getCurrentTarget();
			Target currentImplicit = context.getImplicitTarget();
			Map<String, Target> currentTargets = context.getCurrentTargets();

			try {
				Target newCurrent = new Target();
				newCurrent.setProject(project);
				newCurrent.setName(IAntCoreConstants.EMPTY_STRING);
				context.setCurrentTarget(newCurrent);
				context.setCurrentTargets(new HashMap<>());
				context.setImplicitTarget(newCurrent);
				parse(project, source, new RootHandler(context, mainHandler));
			}
			finally {
				context.setCurrentTarget(currentTarget);
				context.setImplicitTarget(currentImplicit);
				context.setCurrentTargets(currentTargets);
			}
		} else {
			// top level file
			context.setCurrentTargets(new HashMap<>());
			parse(project, source, new RootHandler(context, mainHandler));
		}
	}

	public static void reset() {
		fgXMLReader = null;
		fu = null;
	}

	public static void setAntModel(IAntModel antModel) {
		fgAntModel = antModel;
		((ProjectHelper.ElementHandler) elementHandler).reset();
		fu = null;
		fgAntContext = null;
	}

	public static IAntModel getAntModel() {
		return fgAntModel;
	}

	public static AntXMLContext getContext() {
		return fgAntContext;
	}

	private static FileUtils getFileUtils() {
		if (fu == null) {
			fu = FileUtils.getFileUtils();
		}
		return fu;
	}

	/**
	 * Returns a newly created SAX 2 XMLReader, which is namespace aware
	 *
	 * @return a SAX 2 XMLReader.
	 * @since Ant 1.6 from org.apache.tools.ant.util.JAXPUtils
	 */
	private XMLReader getNamespaceXMLReader() throws BuildException {
		if (fgXMLReader == null) {
			try {
				fgXMLReader = newSAXParser(getNSParserFactory()).getXMLReader();
			}
			catch (SAXException e) {
				// do nothing
			}
		}
		return fgXMLReader;
	}

	/**
	 * Returns the parser factory to use to create namespace aware parsers.
	 *
	 * @return a SAXParserFactory to use which supports manufacture of namespace aware parsers
	 *
	 * @since Ant 1.6 from org.apache.tools.ant.util.JAXPUtils
	 */
	private SAXParserFactory getNSParserFactory() throws BuildException {

		SAXParserFactory nsParserFactory = JAXPUtils.newParserFactory();
		nsParserFactory.setNamespaceAware(true);

		return nsParserFactory;
	}

	/**
	 * @return a new SAXParser instance as helper for getParser and getXMLReader.
	 *
	 * @since Ant 1.5 from org.apache.tools.ant.util.JAXPUtils
	 */
	private SAXParser newSAXParser(SAXParserFactory factory) {
		try {
			return factory.newSAXParser();
		}
		catch (ParserConfigurationException e) {
			// do nothing
		}
		catch (SAXException e) {
			// do nothing
		}
		return null;
	}
}
