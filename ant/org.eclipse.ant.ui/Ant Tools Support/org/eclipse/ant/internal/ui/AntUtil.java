/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.util.FileUtils;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.internal.core.AntCoreUtil;
import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.ant.internal.ui.launchConfigurations.AntHomeClasspathEntry;
import org.eclipse.ant.internal.ui.launchConfigurations.IAntLaunchConfigurationConstants;
import org.eclipse.ant.internal.ui.launchConfigurations.TaskLinkManager;
import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.model.AntTargetNode;
import org.eclipse.ant.internal.ui.model.IAntModel;
import org.eclipse.ant.internal.ui.model.LocationProvider;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry2;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsUtil;
import org.eclipse.ui.externaltools.internal.model.ExternalToolBuilder;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.ibm.icu.text.MessageFormat;

/**
 * General utility class dealing with Ant build files
 */
public final class AntUtil {
	public static final String ATTRIBUTE_SEPARATOR = ","; //$NON-NLS-1$;
	public static final char ANT_CLASSPATH_DELIMITER= '*';
	public static final String ANT_HOME_CLASSPATH_PLACEHOLDER= "G"; //$NON-NLS-1$
	public static final String ANT_GLOBAL_USER_CLASSPATH_PLACEHOLDER= "UG"; //$NON-NLS-1$
	
	private static String fgBrowserId;
	
	/**
	 * No instances allowed
	 */
	private AntUtil() {
		super();
	}
	
	/**
	 * Returns a single-string of the strings for storage.
	 * 
	 * @param strings the array of strings
	 * @return a single-string representation of the strings or
	 * <code>null</code> if the array is empty.
	 */
	public static String combineStrings(String[] strings) {
		if (strings.length == 0)
			return null;

		if (strings.length == 1)
			return strings[0];

		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < strings.length - 1; i++) {
			buf.append(strings[i]);
			buf.append(ATTRIBUTE_SEPARATOR);
		}
		buf.append(strings[strings.length - 1]);
		return buf.toString();
	}

	/**
	 * Returns an array of targets to be run, or <code>null</code> if none are
	 * specified (indicating the default target or implicit target should be run).
	 *
	 * @param configuration launch configuration
	 * @return array of target names, or <code>null</code>
	 * @throws CoreException if unable to access the associated attribute
	 */
	public static String[] getTargetNames(ILaunchConfiguration configuration) throws CoreException {
        String attribute= null;
        if (IAntLaunchConfigurationConstants.ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE.equals(configuration.getType().getIdentifier())) {
            attribute= getTargetNamesForAntBuilder(configuration);
        }
        if (attribute == null) {
            attribute = configuration.getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, (String) null);
            if (attribute == null) {
                return null;
            } 
        }
		
		return AntUtil.parseRunTargets(attribute);
	}
	
	private static String getTargetNamesForAntBuilder(ILaunchConfiguration configuration) throws CoreException {
        String buildType= ExternalToolBuilder.getBuildType();
        String targets= null;
        if (IExternalToolConstants.BUILD_TYPE_AUTO.equals(buildType)) {
            targets= configuration.getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_AUTO_TARGETS, (String)null);
        } else if (IExternalToolConstants.BUILD_TYPE_CLEAN.equals(buildType)) {
            targets = configuration.getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_CLEAN_TARGETS, (String) null);
        } else if (IExternalToolConstants.BUILD_TYPE_FULL.equals(buildType)) {
            targets = configuration.getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_AFTER_CLEAN_TARGETS, (String) null);
        } else if (IExternalToolConstants.BUILD_TYPE_INCREMENTAL.equals(buildType)) {
            targets = configuration.getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_MANUAL_TARGETS, (String) null);
        }
       
        return targets;
    }

    /**
	 * Returns a map of properties to be defined for the build, or
	 * <code>null</code> if none are specified (indicating no additional
	 * properties specified for the build).
	 *
	 * @param configuration launch configuration
	 * @return map of properties (name --> value), or <code>null</code>
	 * @throws CoreException if unable to access the associated attribute
	 */
	public static Map getProperties(ILaunchConfiguration configuration) throws CoreException {
		Map map = configuration.getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_PROPERTIES, (Map) null);
		return map;
	}
	
	/**
	 * Returns a String specifying the Ant home to use for the build.
	 *
	 * @param configuration launch configuration
	 * @return String specifying Ant home to use or <code>null</code>
	 * @throws CoreException if unable to access the associated attribute
	 */
	public static String getAntHome(ILaunchConfiguration configuration) throws CoreException {
		IRuntimeClasspathEntry[] entries = JavaRuntime.computeUnresolvedRuntimeClasspath(configuration);
		for (int i = 0; i < entries.length; i++) {
			IRuntimeClasspathEntry entry = entries[i];
			if (entry.getType() == IRuntimeClasspathEntry.OTHER) {
				IRuntimeClasspathEntry2 entry2 = (IRuntimeClasspathEntry2)entry;
				if (entry2.getTypeId().equals(AntHomeClasspathEntry.TYPE_ID)) {
					return ((AntHomeClasspathEntry)entry2).getAntHome();
				}
			}
		}
		return null;
	}

	/**
	 * Returns an array of property files to be used for the build, or
	 * <code>null</code> if none are specified (indicating no additional
	 * property files specified for the build).
	 *
	 * @param configuration launch configuration
	 * @return array of property file names, or <code>null</code>
	 * @throws CoreException if unable to access the associated attribute
	 */
	public static String[] getPropertyFiles(ILaunchConfiguration configuration) throws CoreException {
		String attribute = configuration.getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_PROPERTY_FILES, (String) null);
		if (attribute == null) {
			return null;
		}
		String[] propertyFiles= AntUtil.parseString(attribute, ","); //$NON-NLS-1$
		for (int i = 0; i < propertyFiles.length; i++) {
			String propertyFile = propertyFiles[i];
			propertyFile= expandVariableString(propertyFile, AntUIModelMessages.AntUtil_6);
			propertyFiles[i]= propertyFile;
		}
		return propertyFiles;
	}
	
	public static AntTargetNode[] getTargets(String path, ILaunchConfiguration config) throws CoreException {
		File buildfile= getBuildFile(path);
		if (buildfile == null) {
		    return null;
		}
		URL[] urls= getCustomClasspath(config);
		//no lexical, no position, no task
		IAntModel model= getAntModel(buildfile, urls, false, false, false);
		try {
			model.setProperties(getAllProperties(config));
		} catch (CoreException ex){
		}
		model.setPropertyFiles(getPropertyFiles(config));
		AntProjectNode project= model.getProjectNode(); //forces a reconcile
		model.dispose();
		return getTargets(project);
	}
	
    private static Map getAllProperties(ILaunchConfiguration config) throws CoreException {
        String allArgs = config.getAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, (String) null);
		Map properties= new HashMap();
		if (allArgs != null) {
			String[] arguments = ExternalToolsUtil.parseStringIntoList(allArgs);
			// filter arguments to avoid resolving variables that will prompt the user
			List filtered = new ArrayList();
			Pattern pattern = Pattern.compile("\\$\\{.*_prompt.*\\}"); //$NON-NLS-1$
			IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
			for (int i = 0; i < arguments.length; i++) {
				String arg = arguments[i];
				if (arg.startsWith("-D")) { //$NON-NLS-1$
					if (!pattern.matcher(arg).find()) {
						filtered.add(manager.performStringSubstitution(arg, false));
					}
				}
			}
		    AntCoreUtil.processMinusDProperties(filtered, properties);
		}
		Map configProperties= getProperties(config);
		if (configProperties != null) {
		    Iterator keys= configProperties.keySet().iterator();
		    while (keys.hasNext()) {
		        String name = (String) keys.next();
		        if (properties.get(name) == null) {
		            properties.put(name, configProperties.get(name));
		        }
		    }
		}
		return properties;
    }

    private static AntTargetNode[] getTargets(AntProjectNode project) {
        if (project == null || !project.hasChildren()) {
		    return null;
		}
		List targets= new ArrayList();
		Iterator possibleTargets= project.getChildNodes().iterator();
		while (possibleTargets.hasNext()) {
			AntElementNode node= (AntElementNode)possibleTargets.next();
			if (node instanceof AntTargetNode) {
				targets.add(node);
			}
		}
		if (targets.size() == 0) {
		    return null;
		}
		return (AntTargetNode[])targets.toArray(new AntTargetNode[targets.size()]);
    }

    public static AntTargetNode[] getTargets(String path) {
		File buildfile= getBuildFile(path);
		if (buildfile == null) {
		    return null;
		}
		//tasks and position info but no lexical info
		IAntModel model= getAntModel(buildfile, null, false, true, true);
		AntProjectNode project= model.getProjectNode();
		if (project == null) {
			model.dispose();
			return null;
		}
		AntTargetNode[] targets= getTargets(project);
        if (targets == null) {
            Hashtable antTargets= project.getProject().getTargets();
            Target implicitTarget= (Target) antTargets.get(""); //$NON-NLS-1$
            if (implicitTarget != null) {
                AntTargetNode implicitTargetNode= new AntTargetNode(implicitTarget);
                project.addChildNode(implicitTargetNode);
                return new AntTargetNode[] {implicitTargetNode};
            }
        }
        return targets;
	}
	
	public static IAntModel getAntModel(String buildFilePath, boolean needsLexicalResolution, boolean needsPositionResolution, boolean needsTaskResolution) {
	    IAntModel model= getAntModel(getBuildFile(buildFilePath), null, needsLexicalResolution, needsPositionResolution, needsTaskResolution);
	    return model;   
	}
	
	/**
	 * Return a buildfile from the specified location.
	 * If there isn't one return null.
	 */
	private static File getBuildFile(String path) {
		File buildFile = new File(path);
		if (!buildFile.isFile() || !buildFile.exists()) { 
			return null;
		}

		return buildFile;
	}
	
	private static IAntModel getAntModel(final File buildFile, URL[] urls, boolean needsLexical, boolean needsPosition, boolean needsTask) {
	    if (buildFile == null || !buildFile.exists()) {
	        return null;
	    }
		IDocument doc= getDocument(buildFile);
		if (doc == null) {
			return null;
		}
		final IFile file= getFileForLocation(buildFile.getAbsolutePath(), null);
		LocationProvider provider= new LocationProvider(null) {
		    /* (non-Javadoc)
		     * @see org.eclipse.ant.internal.ui.model.LocationProvider#getFile()
		     */
		    public IFile getFile() {
		        return file;
		    }
			/* (non-Javadoc)
			 * @see org.eclipse.ant.internal.ui.model.LocationProvider#getLocation()
			 */
			public IPath getLocation() {
			    if (file == null) {
			        return new Path(buildFile.getAbsolutePath());   
			    } 
			    return file.getLocation();
			}
		};
		IAntModel model= new AntModel(doc, null, provider, needsLexical, needsPosition, needsTask);
		
		if (urls != null) {
		    model.setClassLoader(AntCorePlugin.getPlugin().getNewClassLoader(urls));
		}
		return model;
	}
	
	private static IDocument getDocument(File buildFile) {
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		IPath location= new Path(buildFile.getAbsolutePath());
		boolean connected= false;
		try {
			ITextFileBuffer buffer= manager.getTextFileBuffer(location, LocationKind.NORMALIZE);
			if (buffer == null) {
				//no existing file buffer..create one
				manager.connect(location, LocationKind.NORMALIZE, new NullProgressMonitor());
				connected= true;
				buffer= manager.getTextFileBuffer(location, LocationKind.NORMALIZE);
				if (buffer == null) {
					return null;
				}
			}
			
			return buffer.getDocument();
		} catch (CoreException ce) {
			AntUIPlugin.log(ce.getStatus());
			return null;
		} finally {
			if (connected) {
				try {
					manager.disconnect(location, LocationKind.NORMALIZE, new NullProgressMonitor());
				} catch (CoreException e) {
					AntUIPlugin.log(e.getStatus());
				}
			}
		}
	}
	
	/**
	 * Returns the list of URLs that define the custom classpath for the Ant
	 * build, or <code>null</code> if the global classpath is to be used.
	 *
	 * @param config launch configuration
	 * @return a list of <code>URL</code>
	 *
	 * @throws CoreException if file does not exist, IO problems, or invalid format.
	 */
	public static URL[] getCustomClasspath(ILaunchConfiguration config) throws CoreException {
		boolean useDefault = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, true);
		if (useDefault) {
			return null;
		}
		IRuntimeClasspathEntry[] unresolved = JavaRuntime.computeUnresolvedRuntimeClasspath(config);
		// don't consider bootpath entries
		List userEntries = new ArrayList(unresolved.length);
		for (int i = 0; i < unresolved.length; i++) {
			IRuntimeClasspathEntry entry = unresolved[i];
			if (entry.getClasspathProperty() == IRuntimeClasspathEntry.USER_CLASSES) {
				userEntries.add(entry);
			}
		}
		IRuntimeClasspathEntry[] entries = JavaRuntime.resolveRuntimeClasspath((IRuntimeClasspathEntry[])userEntries.toArray(new IRuntimeClasspathEntry[userEntries.size()]), config);
		URL[] urls = new URL[entries.length];
		for (int i = 0; i < entries.length; i++) {
			IRuntimeClasspathEntry entry = entries[i];
			try {
				urls[i] = new URL("file:"+entry.getLocation()); //$NON-NLS-1$
			} catch (MalformedURLException e) {
				throw new CoreException(new Status(IStatus.ERROR, AntUIPlugin.getUniqueIdentifier(), AntUIPlugin.INTERNAL_ERROR, AntUIModelMessages.AntUtil_7, e));
			}
		}
		return urls;		
	}

	private static String expandVariableString(String variableString, String invalidMessage) throws CoreException {
		String expandedString = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(variableString);
		if (expandedString == null || expandedString.length() == 0) {
			String msg = MessageFormat.format(invalidMessage, new String[] {variableString});
			throw new CoreException(new Status(IStatus.ERROR, IAntUIConstants.PLUGIN_ID, 0, msg, null));
		} 
		
		return expandedString;
	}
	
	/**
	 * Returns the list of target names to run
	 * 
	 * @param extraAttibuteValue the external tool's extra attribute value
	 * 		for the run targets key.
	 * @return a list of target names
	 */
	public static String[] parseRunTargets(String extraAttibuteValue) {
		return parseString(extraAttibuteValue, ATTRIBUTE_SEPARATOR);
	}
	
	/**
	 * Returns the list of Strings that were delimiter separated.
	 * 
	 * @param delimString the String to be tokenized based on the delimiter
	 * @return a list of Strings
	 */
	public static String[] parseString(String delimString, String delim) {
		if (delimString == null) {
			return new String[0];
		}
		
		// Need to handle case where separator character is
		// actually part of the target name!
		StringTokenizer tokenizer = new StringTokenizer(delimString, delim);
		String[] results = new String[tokenizer.countTokens()];
		for (int i = 0; i < results.length; i++) {
			results[i] = tokenizer.nextToken();
		}
		
		return results;
	}
	
	/**
	 * Returns an IFile with the given fully qualified path (relative to the workspace root).
	 * The returned IFile may or may not exist.
	 */
	public static IFile getFile(String fullPath) {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		return root.getFile(new Path(fullPath));
	}

	public static IHyperlink getLocationLink(String path, File buildFileParent) {
		path = path.trim();
		if (path.length() == 0) {
			return null;
		}
		if (path.startsWith("file:")) { //$NON-NLS-1$
			// remove "file:"
			path= path.substring(5, path.length());
		}
		// format is file:F:L: where F is file path, and L is line number
		int index = path.lastIndexOf(':');
		if (index == -1) {
			//incorrect format
			return null;
		}
		if (index == path.length() - 1) {
			// remove trailing ':'
			path = path.substring(0, index);
			index = path.lastIndexOf(':');
		}
		// split file and line number
		String fileName = path.substring(0, index);
		try {
			String lineNumber = path.substring(index + 1);
			int line = Integer.parseInt(lineNumber);
			IFile file = getFileForLocation(fileName, buildFileParent);
			if (file != null) {
				return new FileLink(file, null, -1, -1, line);
			}
		} catch (NumberFormatException e) {
		}
		
		return null;
	}

	/**
	 * Returns the workspace file associated with the given path in the
	 * local file system, or <code>null</code> if none.
	 * If the path happens to be a relative path, then the path is interpreted as
	 * relative to the specified parent file.
	 * 
	 * Attempts to handle linked files; the first found linked file with the correct
	 * path is returned.
	 *   
	 * @param path
	 * @param buildFileParent
	 * @return file or <code>null</code>
	 * @see org.eclipse.core.resources.IWorkspaceRoot#findFilesForLocation(IPath)
	 */
	public static IFile getFileForLocation(String path, File buildFileParent) {
		if (path == null) {
			return null;
		}
		IPath filePath= new Path(path);
		IFile file = null;
		IFile[] files= ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(filePath);
		if (files.length > 0) {
			file= files[0];
		}
		if (file == null) {
			//relative path
			File relativeFile= null;
			try {
				//this call is ok if buildFileParent is null
				relativeFile= FileUtils.getFileUtils().resolveFile(buildFileParent, path);
				filePath= new Path(relativeFile.getAbsolutePath());
				files= ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(filePath);
				if (files.length > 0) {
					file= files[0];
				} else {
					return null;
				}
			} catch (BuildException be) {
				return null;
			}
		}
		
		if (file.exists()) {
			return file;
		} 
		File ioFile= file.getLocation().toFile();
		if (ioFile.exists()) {//needs to handle case insensitivity on WINOS
			try {
				files= ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(new Path(ioFile.getCanonicalPath()));
				if (files.length > 0) {
					return files[0];
				}
			} catch (IOException e) {
			}			
		}
			
		return null;
	}

	/**
	 * Migrates the classpath in the given configuration from the old format
	 * to the new format. The old format is not preserved. Instead, the default
	 * classpath will be used. However, ANT_HOME settings are preserved.
	 * 
	 * @param configuration a configuration to migrate
	 * @throws CoreException if unable to migrate
	 * @since 3.0
	 */
	public static void migrateToNewClasspathFormat(ILaunchConfiguration configuration) throws CoreException {
		String oldClasspath = configuration.getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_CUSTOM_CLASSPATH, (String)null);
		String oldAntHome = configuration.getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_HOME, (String)null);
		String provider = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, (String)null);
		if (oldClasspath != null || oldAntHome != null || provider == null) {
			ILaunchConfigurationWorkingCopy workingCopy = null;
			if (configuration.isWorkingCopy()) {
				workingCopy = (ILaunchConfigurationWorkingCopy) configuration;
			} else {
				workingCopy = configuration.getWorkingCopy();
			}
			workingCopy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_CUSTOM_CLASSPATH, (String)null);
			workingCopy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_HOME, (String)null);
			workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, "org.eclipse.ant.ui.AntClasspathProvider"); //$NON-NLS-1$
			workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, true);
			if (oldAntHome != null) {
				IRuntimeClasspathEntry[] entries = JavaRuntime.computeUnresolvedRuntimeClasspath(workingCopy);
				List mementos = new ArrayList(entries.length);
				for (int i = 0; i < entries.length; i++) {
					IRuntimeClasspathEntry entry = entries[i];
					if (entry.getType() == IRuntimeClasspathEntry.OTHER) {
						IRuntimeClasspathEntry2 entry2 = (IRuntimeClasspathEntry2) entry;
						if (entry2.getTypeId().equals(AntHomeClasspathEntry.TYPE_ID)) {
							AntHomeClasspathEntry homeEntry = new AntHomeClasspathEntry(oldAntHome);
							mementos.add(homeEntry.getMemento());
							continue;
						}
					}
					mementos.add(entry.getMemento());
				}
				workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, false);
				workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, mementos);
			}
			workingCopy.doSave();
		}
	}

    private static int getOffset(int line, int column, ITextEditor editor) {
    	IDocumentProvider provider= editor.getDocumentProvider();
    	IEditorInput input= editor.getEditorInput();
    	try {
    		provider.connect(input);
    	} catch (CoreException e) {
    		return -1;
    	}
    	try {
    		IDocument document= provider.getDocument(input);
    		if (document != null) {
    			if (column > -1) {
    				 //column marks the length..adjust to 0 index and to be within the element's source range
    				return document.getLineOffset(line - 1) + column - 1 - 2;
    			} 
    			return document.getLineOffset(line - 1);
    		}
    	} catch (BadLocationException e) {
    	} finally {
    		provider.disconnect(input);
    	}
    	return -1;
    }

    /**
     * Opens the given editor on the buildfile of the provided node and selects that node in the editor.
     *
     * @param page the page to open the editor in
     * @param editorDescriptor the editor descriptor, or <code>null</code> for the system editor
     * @param node the node from the buildfile to open and then select in the editor
     */
    public static void openInEditor(IWorkbenchPage page, IEditorDescriptor editorDescriptor, AntElementNode node) {
    	IEditorPart editorPart= null;
    	IFile fileResource = node.getIFile();
    	try {
    		if (editorDescriptor == null) {
    			editorPart= page.openEditor(new FileEditorInput(fileResource), IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
    		} else {
    			editorPart= page.openEditor(new FileEditorInput(fileResource), editorDescriptor.getId());
    		}
    	} catch (PartInitException e) {
    		AntUIPlugin.log(MessageFormat.format(AntUIModelMessages.AntUtil_0, new String[]{fileResource.getLocation().toOSString()}), e);
    	}
    	
    	if (editorPart instanceof AntEditor) {
    	    AntEditor editor= (AntEditor)editorPart;
    		if (node.getImportNode() != null) {	
    			AntModel model= editor.getAntModel();
    			AntProjectNode project= model.getProjectNode();
    			if (project == null) {
    				return;
    			}
    			int[] info= node.getExternalInfo();
    			int offset= getOffset(info[0], info[1], editor);
    			node= project.getNode(offset);
    		}
    		editor.setSelection(node, true);
    	}
    }

    /**
     * Opens an editor on the buildfile of the provided node and selects that node in the editor.
     * 
     * @param page the page to open the editor in
     * @param node the node from the buildfile to open and then select in the editor
     */
    public static void openInEditor(IWorkbenchPage page, AntElementNode node) {
    	IFile file= node.getIFile();
    	IEditorDescriptor editorDesc;
        try {
            editorDesc = IDE.getEditorDescriptor(file);
        } catch (PartInitException e) {
           return;
        }
        openInEditor(page, editorDesc, node);
    }
    
    /**
     * Opens an external browser on the provided <code>urlString</code>
     * @param urlString The url to open
     * @param shell the shell
     * @param errorDialogTitle the title of any error dialog
     */
    public static void openBrowser(final String urlString, final Shell shell, final String errorDialogTitle) {
    	shell.getDisplay().syncExec(new Runnable() {
    		public void run() {
    			IWorkbenchBrowserSupport support= PlatformUI.getWorkbench().getBrowserSupport();
    			try {
    				IWebBrowser browser= support.createBrowser(fgBrowserId);
    				fgBrowserId= browser.getId();
    				browser.openURL(new URL(urlString));
    				return;
    			} catch (PartInitException e) {
    				AntUIPlugin.log(e.getStatus());
    			} catch (MalformedURLException e) {
    				AntUIPlugin.log(e);
				}
    			
    			String platform= SWT.getPlatform();
    			boolean succeeded= true;
    			if ("motif".equals(platform) || "gtk".equals(platform)) { //$NON-NLS-1$ //$NON-NLS-2$
    				Program program= Program.findProgram("html"); //$NON-NLS-1$
    				if (program == null) {
    					program= Program.findProgram("htm"); //$NON-NLS-1$
    				}
    				if (program != null) {
    					succeeded= program.execute(urlString.toString());
    				}
    			} else {
    				succeeded= Program.launch(urlString.toString());
    			}
    			if (!succeeded) {
    				MessageDialog.openInformation(shell, errorDialogTitle, AntUIModelMessages.AntUtil_1);
    			}
    		}
    	});
	}
    
    public static boolean isSeparateJREAntBuild(ILaunchConfiguration configuration) {
    	boolean separateJRE= true;
		try {
			//always null for same JRE
			separateJRE = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String)null) != null;
		} catch (CoreException e) {
			AntUIPlugin.log(AntUIModelMessages.AntUtil_2, e);
		}
    	
		return separateJRE;
    }
    
    public static void linkBuildFailedMessage(String message, IProcess process) {
        String fileName = null;
        String lineNumber = ""; //$NON-NLS-1$
        int fileStart = 0;
        int index = message.indexOf("xml"); //$NON-NLS-1$
        if (index > 0) {
            int numberStart= index + 4;
            int numberEnd= message.indexOf(':', numberStart);
            int fileEnd = index + 3;
            if (numberStart > 0 && fileEnd > 0) {
                fileName = message.substring(fileStart, fileEnd).trim();
                if (numberEnd > 0) {
                    lineNumber = message.substring(numberStart, numberEnd).trim();
                }
            }
        }
        
        if (fileName != null) {
            int num = -1;
            try {
                num = Integer.parseInt(lineNumber);
            } catch (NumberFormatException e) {
            }
            IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(new Path(fileName));
            IFile file= null;
            if (files.length > 0) {
                file= files[0];
            }
            if (file != null && file.exists()) {
                FileLink link = new FileLink(file, null, -1, -1, num);
                TaskLinkManager.addTaskHyperlink(process, link, new Region(0, message.length()), message);
            }
        }
    }
}