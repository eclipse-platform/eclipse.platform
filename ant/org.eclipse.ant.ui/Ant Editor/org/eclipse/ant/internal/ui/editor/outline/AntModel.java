/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Nico Seessle - bug 51332
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.outline;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Main;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskAdapter;
import org.apache.tools.ant.UnknownElement;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.Property;
import org.eclipse.ant.core.Type;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.editor.model.AntDefiningTaskNode;
import org.eclipse.ant.internal.ui.editor.model.AntElementNode;
import org.eclipse.ant.internal.ui.editor.model.AntImportNode;
import org.eclipse.ant.internal.ui.editor.model.AntProjectNode;
import org.eclipse.ant.internal.ui.editor.model.AntPropertyNode;
import org.eclipse.ant.internal.ui.editor.model.AntTargetNode;
import org.eclipse.ant.internal.ui.editor.model.AntTaskNode;
import org.eclipse.ant.internal.ui.editor.model.IAntModelConstants;
import org.eclipse.ant.internal.ui.editor.utils.ProjectHelper;
import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

public class AntModel {

	private static ClassLoader fgClassLoader;
	private static int fgInstanceCount= 0;
	
	private XMLCore fCore;
	private IDocument fDocument;
	private IProblemRequestor fProblemRequestor;
	private LocationProvider fLocationProvider;

	private AntProjectNode fProjectNode;
	private AntTargetNode fCurrentTargetNode;
	private AntElementNode fLastNode;
	private AntElementNode fNodeBeingResolved;
	
	private AntTargetNode fIncrementalTarget= null;
	private boolean fReplaceHasOccurred= false;
	private int fRemoveLengthOfReplace= 0;
	private DirtyRegion fDirtyRegion= null;
	
	 /**
     * Stack of still open elements.
     * <P>
     * On top of the stack is the innermost element.
     */
	private Stack fStillOpenElements = new Stack();
	
	private Map fTaskToNode= new HashMap();

	private List fTaskNodes= new ArrayList();
	
	private Map fEntityNameToPath;

	private final Object fDirtyLock= new Object();
	private boolean fIsDirty= true;
	private IDocumentListener fListener;
	private File fEditedFile= null;	
	private AntEditorMarkerUpdater fMarkerUpdater= null;
	private Set fNamesOfOldDefiningNodes;
	
	private Preferences.IPropertyChangeListener fCorePropertyChangeListener= new Preferences.IPropertyChangeListener() {
		public void propertyChange(Preferences.PropertyChangeEvent event) {
			if (event.getProperty().equals(IAntCoreConstants.PREFERENCE_CLASSPATH_CHANGED)) {
				if (((Boolean)event.getNewValue()) == Boolean.TRUE) {
					reconcileForPropertyChange(true);		
				}
			}
		}
	};
	
	private Preferences.IPropertyChangeListener fUIPropertyChangeListener= new Preferences.IPropertyChangeListener() {
		public void propertyChange(Preferences.PropertyChangeEvent event) {
			String property= event.getProperty();
			if (property.equals(AntEditorPreferenceConstants.PROBLEM)) {
				AntUIPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fUIPropertyChangeListener);
				reconcileForPropertyChange(false);
				AntUIPlugin.getDefault().getPluginPreferences().setToDefault(AntEditorPreferenceConstants.PROBLEM);
				AntUIPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fUIPropertyChangeListener);
			} else if (property.equals(AntEditorPreferenceConstants.CODEASSIST_USER_DEFINED_TASKS)) {
				if (((Boolean)event.getNewValue()).booleanValue()) {
					reconcileForPropertyChange(false);		
				}
			}
		}
	};

	public AntModel(XMLCore core, IDocument document, IProblemRequestor problemRequestor, LocationProvider locationProvider) {
		fCore= core;
		fDocument= document;
		fProblemRequestor= problemRequestor;
		fMarkerUpdater= new AntEditorMarkerUpdater();
		fMarkerUpdater.setModel(this);
		fLocationProvider= locationProvider;
		AntCorePlugin.getPlugin().getPluginPreferences().addPropertyChangeListener(fCorePropertyChangeListener);
		AntUIPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fUIPropertyChangeListener);
		AntDefiningTaskNode.setJavaClassPath();
		fgInstanceCount++;
	}

	private void reconcileForPropertyChange(boolean classpathChanged) {
		if (classpathChanged) {
			fProjectNode= null; //need to reset tasks, types and properties
			fgClassLoader= null;
			AntDefiningTaskNode.setJavaClassPath();
		}
		fIsDirty= true;
		reconcile(null);
		fCore.notifyDocumentModelListeners(new DocumentModelChangeEvent(this, true));
		fMarkerUpdater.updateMarkers();
	}

	public void install() {
		fListener= new IDocumentListener() {
			public void documentAboutToBeChanged(DocumentEvent event) {
				synchronized (fDirtyLock) {
					fIsDirty= true;
				}
			}
			public void documentChanged(DocumentEvent event) {}
		};
		fDocument.addDocumentListener(fListener);
	}
	
	public void dispose() {		
		synchronized (this) {
			if (fDocument != null) {
				fDocument.removeDocumentListener(fListener);
			}
			fDocument= null;
			fCore= null;
			ProjectHelper.setAntModel(null);
		}
		
		AntCorePlugin.getPlugin().getPluginPreferences().removePropertyChangeListener(fCorePropertyChangeListener);
		AntUIPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fUIPropertyChangeListener);
		fgInstanceCount--;
		if (fgInstanceCount == 0) {
			fgClassLoader= null;
		}
		if (getProjectNode() != null) {
			//cleanup the introspection helpers that may have been generated
			IntrospectionHelper.getHelper(getProjectNode().getProject(), AntModel.class);
			getProjectNode().getProject().fireBuildFinished(null);
		}
	}
	
	public void reconcile(DirtyRegion region) {
		//TODO turn off incremental as it is deferred to post 3.0
		region= null; 
		fDirtyRegion= region;
		synchronized (fDirtyLock) {
			if (!fIsDirty) {
				return;
			}
			if (fReplaceHasOccurred && region != null) {
				//this is the removed part of a replace
				//the insert region will be along shortly
				fRemoveLengthOfReplace= region.getLength();
				fReplaceHasOccurred= false;
				return;
			}
			fIsDirty= false;
		}

		synchronized (this) {
			if (fCore == null) {
				// disposed
				return;
			}
			
			if (fDocument == null) {
				fProjectNode= null;
			} else {
				reset(region);
				parseDocument(fDocument, region);
				fRemoveLengthOfReplace= 0;
				fDirtyRegion= null;
				reconcileTaskAndTypes();
			} 
	
			fCore.notifyDocumentModelListeners(new DocumentModelChangeEvent(this));
		}
	}

	private void reset(DirtyRegion region) {
		//TODO this could be better for incremental parsing
		//cleaning up the task to node map (do when a target is reset)
		fCurrentTargetNode= null;
		
		if (region == null ) {
			fStillOpenElements= new Stack();
			fTaskToNode= new HashMap();
			fTaskNodes= new ArrayList();
			fNodeBeingResolved= null;
			fLastNode= null;
		}
	}

	public AntElementNode[] getRootElements() {
		reconcile(null);
		if (fProjectNode == null) {
			return new AntElementNode[0];
		} 
			return new AntElementNode[] {fProjectNode};
		}

	private void parseDocument(IDocument input, DirtyRegion region) {
		boolean parsed= true;
		if (input.getLength() == 0) {
			fProjectNode= null;
			parsed= false;
			return;
		}
		ClassLoader parsingClassLoader= getClassLoader();
		ClassLoader originalClassLoader= Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(parsingClassLoader);
		boolean incremental= false;
		Project project= null;
    	try {
    		String textToParse= null;
    		ProjectHelper projectHelper= null;
			if (region == null || fProjectNode == null) {  //full parse
				if (fProjectNode == null || !fProjectNode.hasChildren()) {
					fProjectNode= null;
					project = new AntModelProject();
					projectHelper= prepareForFullParse(project, parsingClassLoader);
					textToParse= input.get(); //the entire document
				} else {
					project= fProjectNode.getProject();
					projectHelper= (ProjectHelper)project.getReference("ant.projectHelper"); //$NON-NLS-1$
					textToParse= prepareForFullIncremental(input);
				}
			} else { //incremental
				project= fProjectNode.getProject();
				textToParse= prepareForIncrementalParse(project, region, input);
				if (textToParse == null) {
					parsed= false;
					return;
				}
				incremental= true;
				projectHelper= (ProjectHelper)project.getReference("ant.projectHelper"); //$NON-NLS-1$
			}
			beginReporting();
			Map references= project.getReferences();
			references.remove("ant.parsing.context"); //$NON-NLS-1$
			ProjectHelper.setAntModel(this);
			projectHelper.parse(project, textToParse);
			
    	} catch(BuildException e) {
			handleBuildException(e, null);
    	} finally {
    		Thread.currentThread().setContextClassLoader(originalClassLoader);
    		if (parsed) {
    			if (incremental) {
    	    		updateAfterIncrementalChange(region, true);
    	    	}
    			resolveBuildfile();
    			endReporting();
    			project.fireBuildFinished(null); //cleanup (IntrospectionHelper)
    			fIncrementalTarget= null;
    		}
    	}
	}
	
	private void updateAfterIncrementalChange(DirtyRegion region, boolean updateProjectLength) {
		if (fProjectNode == null) {
			return;
		}
		int editAdjustment= determineEditAdjustment(region);
		if (editAdjustment == 0) {
			return;
		}
		if (updateProjectLength) { //edit within the project 
			fProjectNode.setLength(fProjectNode.getLength() + editAdjustment);
		} else {
			fProjectNode.setOffset(fProjectNode.getOffset() + editAdjustment);
		}
		if ((fIncrementalTarget != null || !updateProjectLength) && fProjectNode.hasChildren()) {
			List children= fProjectNode.getChildNodes();
			int index= children.indexOf(fIncrementalTarget) + 1;
			updateNodesForIncrementalParse(editAdjustment, children, index);
		}
	}

	private void updateNodesForIncrementalParse(int editAdjustment, List children, int index) {
		AntElementNode node;
		for (int i = index; i < children.size(); i++) {
			node= (AntElementNode)children.get(i);
			node.setOffset(node.getOffset() + editAdjustment);
			if (node.hasChildren()) {
				updateNodesForIncrementalParse(editAdjustment, node.getChildNodes(), 0);
			}
		}
	}

	private ProjectHelper prepareForFullParse(Project project, ClassLoader parsingClassLoader) {
		initializeProject(project, parsingClassLoader);
    	// Ant's parsing facilities always works on a file, therefore we need
    	// to determine the actual location of the file. Though the file 
    	// contents will not be parsed. We parse the passed document string
    	File file = getEditedFile();
    	String filePath= ""; //$NON-NLS-1$
    	if (file != null) {
    		filePath= file.getAbsolutePath();
    	}
    	project.setUserProperty("ant.file", filePath); //$NON-NLS-1$
    	project.setUserProperty("ant.version", Main.getAntVersion()); //$NON-NLS-1$

		ProjectHelper projectHelper= new ProjectHelper(this);
		projectHelper.setBuildFile(file);
		project.addReference("ant.projectHelper", projectHelper); //$NON-NLS-1$
		return projectHelper;
	}
	
	private String prepareForIncrementalParse(Project project, DirtyRegion region, IDocument input) {
		String textToParse= null;
		AntElementNode node= fProjectNode.getNode(region.getOffset());
		if (node == null) {
			if (fProjectNode.getLength() > 0) {
				//outside of any element
				if (region.getOffset() < fProjectNode.getOffset()) {
					updateAfterIncrementalChange(region, false);
				}
				return null;
			}
			//nodes don't know their lengths due to parsing error --> full parse
			textToParse = prepareForFullIncremental(input);
			return textToParse;
		}
		
		while (node != null && !(node instanceof AntTargetNode)) {
			node= node.getParentNode();
		}
		if (node == null) { //no enclosing target node found
			if (region.getText() != null && region.getText().trim().length() == 0) {
				return null; //no need to parse for whitespace additions
			}
			textToParse= prepareForFullIncremental(input);
		} else {
			fIncrementalTarget= (AntTargetNode)node;
			if (fIncrementalTarget.hasChildren()) {
				Collection nodes= fTaskToNode.values();
				nodes.removeAll(fIncrementalTarget.getDescendents());
			}
			
			markHierarchy(node, XMLProblem.NO_PROBLEM);
			
			StringBuffer temp = createIncrementalContents(project);			
			fIncrementalTarget.reset();
			try {
				int editAdjustment = determineEditAdjustment(region) + 1;
				String targetString= input.get(node.getOffset() - 1, node.getLength() + editAdjustment);
				temp.append(targetString);
				temp.append("\n</project>"); //$NON-NLS-1$
				textToParse= temp.toString();
			} catch (BadLocationException e) {
				textToParse= input.get();
			}
		}
		return textToParse;
	}

	private String prepareForFullIncremental(IDocument input) {
		String textToParse=  input.get();
		fProjectNode.reset();
		fTaskToNode= new HashMap();
		fTaskNodes= new ArrayList();
		return textToParse;
	}

	private StringBuffer createIncrementalContents(Project project) {
		int offset= fIncrementalTarget.getOffset();
		int line= getLine(offset) - 1;
		
		StringBuffer temp= new StringBuffer("<project");//$NON-NLS-1$
		String defltTarget= project.getDefaultTarget();
		if (defltTarget != null) {
			temp.append(" default=\""); //$NON-NLS-1$
			temp.append(defltTarget);
			temp.append("\""); //$NON-NLS-1$
		}
		temp.append(">"); //$NON-NLS-1$
		while (line > 0) {
			temp.append("\n"); //$NON-NLS-1$
			line--;
		}
		return temp;
	}

	private int determineEditAdjustment(DirtyRegion region) {
		int editAdjustment= 0;
		if (region.getType().equals(DirtyRegion.INSERT)) {
			editAdjustment+= region.getLength() - fRemoveLengthOfReplace;
		} else {
			editAdjustment-= region.getLength();
		}
		return editAdjustment;
	}

	private void initializeProject(Project project, ClassLoader loader) {
		project.init();
		setProperties(project);
		setTasks(project, loader);
		setTypes(project, loader);
	}
	
	private void setTasks(Project project, ClassLoader loader) {
		List tasks = AntCorePlugin.getPlugin().getPreferences().getTasks();
		for (Iterator iterator = tasks.iterator(); iterator.hasNext();) {
			org.eclipse.ant.core.Task task = (org.eclipse.ant.core.Task) iterator.next();
			AntTypeDefinition def= new AntTypeDefinition();
			def.setName(task.getTaskName());
            def.setClassName(task.getClassName());
            def.setClassLoader(loader);
            def.setAdaptToClass(Task.class);
            def.setAdapterClass(TaskAdapter.class);
            ComponentHelper.getComponentHelper(project).addDataTypeDefinition(def);
		}
	}
		
	private void setTypes(Project project, ClassLoader loader) {
		List types = AntCorePlugin.getPlugin().getPreferences().getTypes();
		for (Iterator iterator = types.iterator(); iterator.hasNext();) {
			Type type = (Type) iterator.next();
			 AntTypeDefinition def = new AntTypeDefinition();
             def.setName(type.getTypeName());
             def.setClassName(type.getClassName());
             def.setClassLoader(loader);
             ComponentHelper.getComponentHelper(project).addDataTypeDefinition(def);
		}
	}
	
	private void setProperties(Project project) {
		setBuiltInProperties(project);
		setGlobalProperties(project);
		loadPropertyFiles(project);
	}
	
	/**
	 * Load all properties from the files 
	 */
	private void loadPropertyFiles(Project project) {
		String[] fileNames= AntCorePlugin.getPlugin().getPreferences().getCustomPropertyFiles();
		for (int i = 0; i < fileNames.length; i++) {
			String filename = fileNames[i];
           	File file= getFileRelativeToBaseDir(project, filename);
            Properties props = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                props.load(fis);
            } catch (IOException e) {
            	AntUIPlugin.log(e);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e){
                    }
                }
            }
          
            Enumeration propertyNames = props.propertyNames();
            while (propertyNames.hasMoreElements()) {
                String name = (String) propertyNames.nextElement();
                project.setUserProperty(name, props.getProperty(name));
            }
        }
	}
	
	private File getFileRelativeToBaseDir(Project project, String fileName) {
		IPath path= new Path(fileName);
		if (!path.isAbsolute()) {
			String base= project.getUserProperty("basedir"); //$NON-NLS-1$
			if (base != null) {
				File baseDir= new File(base);
				if (baseDir != null) {
					//relative to the base dir
					path= new Path(baseDir.getAbsolutePath());
				} 
			} else {
				//relative to the build file location
				path= new Path(getEditedFile().getAbsolutePath());
				path= path.removeLastSegments(1);
			}
			path= path.addTrailingSeparator();
			path= path.append(fileName);
		}
		
		return path.toFile();
	}

	private void setBuiltInProperties(Project project) {
		//note also see processAntHome for system properties that are set
		project.setUserProperty("ant.file", getEditedFile().getAbsolutePath()); //$NON-NLS-1$
		project.setUserProperty("ant.version", Main.getAntVersion()); //$NON-NLS-1$
	}
	
	private void setGlobalProperties(Project project) {
		List properties= AntCorePlugin.getPlugin().getPreferences().getProperties();
		if (properties != null) {
			for (Iterator iter = properties.iterator(); iter.hasNext();) {
				Property property = (Property) iter.next();
				String value= property.getValue();
				if (value != null) {
					project.setUserProperty(property.getName(), value);
				}
			}
		}
	}

	private void resolveBuildfile() {	
		Collection nodeCopy= new ArrayList(fTaskNodes.size());
		nodeCopy.addAll(fTaskNodes);
		Iterator iter= nodeCopy.iterator();
		while (iter.hasNext()) {
			AntTaskNode node = (AntTaskNode) iter.next();
			fNodeBeingResolved= node;
			if (node.configure(false)) {
				//resolve any new elements that may have been added
				resolveBuildfile();
			}
		}
		fNodeBeingResolved= null;
		
		checkTargets();
	}

	/**
	 * Check that we have a default target defined and that the 
	 * target dependencies exist. 
	 */
	private void checkTargets() {
		if (fProjectNode == null) {
			return;
		}
		String defaultTargetName= fProjectNode.getProject().getDefaultTarget();
		if (defaultTargetName == null || fProjectNode.getProject().getTargets().get(defaultTargetName) == null) {
			//no default target
			String message;
			if (defaultTargetName == null) {
				message= AntOutlineMessages.getString("AntModel.0"); //$NON-NLS-1$
			} else {
				message= MessageFormat.format(AntOutlineMessages.getString("AntModel.43"), new String[]{defaultTargetName}); //$NON-NLS-1$
			}
			IProblem problem= createProblem(message, fProjectNode.getOffset(), fProjectNode.getSelectionLength(), XMLProblem.SEVERITY_ERROR);
			acceptProblem(problem);
			markHierarchy(fProjectNode, XMLProblem.SEVERITY_ERROR);
		}
		if (!fProjectNode.hasChildren()) {
			return;
		}
		List children= fProjectNode.getChildNodes();
		Iterator iter= children.iterator();
		while (iter.hasNext()) {
			AntElementNode node = (AntElementNode) iter.next();
			AntElementNode originalNode= node;
			if (node instanceof AntTargetNode) {
				String missing= ((AntTargetNode)node).checkDependencies();
				if (missing != null) {
					String message= MessageFormat.format(AntOutlineMessages.getString("AntModel.44"), new String[]{missing}); //$NON-NLS-1$
					AntElementNode importNode= node.getImportNode();
					if (importNode != null) {
						node= importNode;
					}
					IProblem problem= createProblem(message, node.getOffset(), node.getSelectionLength(), XMLProblem.SEVERITY_ERROR);
					acceptProblem(problem);
					markHierarchy(originalNode, XMLProblem.SEVERITY_ERROR);
				}
			}
		}
		
	}

	public void handleBuildException(BuildException e, AntElementNode node, int severity) {
		try {
			if (node != null) {
				markHierarchy(node, severity);
			}
			Location location= e.getLocation();
			int line= 0;
			int originalOffset= 0;
			int nonWhitespaceOffset= 0; 
			int length= 0;
			if (location == Location.UNKNOWN_LOCATION && node != null) {
				nonWhitespaceOffset= node.getOffset();
				length= node.getLength();
			} else {
				line= location.getLineNumber();
				if (line == 0) {
					if (getProjectNode() != null) {
						length= getProjectNode().getSelectionLength();
						nonWhitespaceOffset= getProjectNode().getOffset();
						if (severity == XMLProblem.SEVERITY_ERROR) {
							getProjectNode().setProblemSeverity(XMLProblem.NO_PROBLEM);
						}
					} else {
						return;
					}
				} else {
					if (node == null) {
						originalOffset= getOffset(line, 1);
						nonWhitespaceOffset= originalOffset;
						try {
							nonWhitespaceOffset= getNonWhitespaceOffset(line, 1);
						} catch (BadLocationException be) {
						}
							length= getLastCharColumn(line) - (nonWhitespaceOffset - originalOffset);
						} else {
							nonWhitespaceOffset= node.getOffset();
							length= node.getLength();
					}
				}
			}
			notifyProblemRequestor(e, nonWhitespaceOffset, length, severity);
		} catch (BadLocationException e1) {
		}
	}

	public void handleBuildException(BuildException e, AntElementNode node) {
		handleBuildException(e, node, XMLProblem.SEVERITY_ERROR);
	}

	public File getEditedFile() {
		if (fLocationProvider != null && fEditedFile == null) {
        	fEditedFile= fLocationProvider.getLocation().toFile();
		}
		return fEditedFile;
    }

	private void markHierarchy(AntElementNode openElement, int severity) {
		while (openElement != null) {
			openElement.setProblemSeverity(severity);
			openElement= openElement.getParentNode();
		}
	}
	
	public LocationProvider getLocationProvider() {
		return fLocationProvider;
	}

	public void addTarget(Target newTarget, int line, int column) {
		if (fIncrementalTarget != null) {
			fCurrentTargetNode= fIncrementalTarget;
			fCurrentTargetNode.setTarget(newTarget);
			fStillOpenElements.push(fCurrentTargetNode);
		} else {
			AntTargetNode targetNode= new AntTargetNode(newTarget);
			fProjectNode.addChildNode(targetNode);
			fCurrentTargetNode= targetNode;
			fStillOpenElements.push(targetNode);
			computeOffset(targetNode, line, column);
			if (fNodeBeingResolved instanceof AntImportNode) {
				targetNode.setImportNode(fNodeBeingResolved);
			}
		}
	}
	
	public void addProject(Project project, int line, int column) {
		if (fIncrementalTarget != null) {
			return;
		}
		fProjectNode= new AntProjectNode((AntModelProject)project, this);
		fStillOpenElements.push(fProjectNode);
		computeOffset(fProjectNode, line, column);
	}

	public void addTask(Task newTask, Task parentTask, Attributes attributes, int line, int column) {
		AntTaskNode taskNode= null;
		if (parentTask == null) {
			taskNode= newTaskNode(newTask, attributes);
			if (fCurrentTargetNode == null) {
				fProjectNode.addChildNode(taskNode);
			} else {
				fCurrentTargetNode.addChildNode(taskNode);
				if (taskNode.isExternal()) {
					fCurrentTargetNode.setExternal(true);
					fCurrentTargetNode.setFilePath(taskNode.getFilePath());
			}
			}
		} else {
			taskNode= newNotWellKnownTaskNode(newTask, attributes);
			((AntTaskNode)fTaskToNode.get(parentTask)).addChildNode(taskNode);
		}
		fTaskToNode.put(newTask, taskNode);
		
		fStillOpenElements.push(taskNode);
		computeOffset(taskNode, line, column);
		if (fNodeBeingResolved instanceof AntImportNode) {
			taskNode.setImportNode(fNodeBeingResolved);
			//place the node in the collection right after the import node
			int index= fTaskNodes.indexOf(fNodeBeingResolved) + 1;
			fTaskNodes.add(index, taskNode);
		} else {
			fTaskNodes.add(taskNode);
		}
	}
	
	public void addEntity(String entityName, String entityPath) {
		if (fEntityNameToPath == null) {
			fEntityNameToPath= new HashMap();
		}
		fEntityNameToPath.put(entityName, entityPath);
	}

	private AntTaskNode newTaskNode(Task newTask, Attributes attributes) {
		AntTaskNode newNode= null;
		String taskName= newTask.getTaskName();
		if (isPropertySettingTask(taskName)) { //$NON-NLS-1$
			newNode= new AntPropertyNode(newTask, attributes);
		} else if (taskName.equalsIgnoreCase("import")) { //$NON-NLS-1$
			newNode= new AntImportNode(newTask, attributes);
		} else if (taskName.equalsIgnoreCase("macrodef")  //$NON-NLS-1$
        		|| taskName.equalsIgnoreCase("presetdef") //$NON-NLS-1$
				|| taskName.equalsIgnoreCase("typedef") //$NON-NLS-1$
				|| taskName.equalsIgnoreCase("taskdef")) { //$NON-NLS-1$
                    String name = attributes.getValue(IAntModelConstants.ATTR_NAME);
                    newNode= new AntDefiningTaskNode(newTask, name);
		} else if(taskName.equalsIgnoreCase("antcall")) { //$NON-NLS-1$
            newNode= new AntTaskNode(newTask, generateLabel(taskName, attributes, IAntModelConstants.ATTR_TARGET));
        } else if(taskName.equalsIgnoreCase("mkdir")) { //$NON-NLS-1$
            newNode= new AntTaskNode(newTask, generateLabel(taskName, attributes, IAntModelConstants.ATTR_DIR));
        } else if(taskName.equalsIgnoreCase("copy")) { //$NON-NLS-1$
        	 newNode= new AntTaskNode(newTask, generateLabel(taskName, attributes, IAntModelConstants.ATTR_DESTFILE));
        } else if(taskName.equalsIgnoreCase("tar")  //$NON-NLS-1$
        	|| taskName.equalsIgnoreCase("jar") //$NON-NLS-1$
        	|| taskName.equalsIgnoreCase("war") //$NON-NLS-1$
        	|| taskName.equalsIgnoreCase("zip")) { //$NON-NLS-1$
        	newNode= new AntTaskNode(newTask, generateLabel(newTask.getTaskName(), attributes, IAntModelConstants.ATTR_DESTFILE));
        } else if(taskName.equalsIgnoreCase("untar")  //$NON-NLS-1$
        	|| taskName.equalsIgnoreCase("unjar") //$NON-NLS-1$
        	|| taskName.equalsIgnoreCase("unwar") //$NON-NLS-1$
        	|| taskName.equalsIgnoreCase("gunzip") //$NON-NLS-1$
        	|| taskName.equalsIgnoreCase("bunzip2") //$NON-NLS-1$
        	|| taskName.equalsIgnoreCase("unzip")) { //$NON-NLS-1$
        	newNode= new AntTaskNode(newTask, generateLabel(newTask.getTaskName(), attributes, IAntModelConstants.ATTR_SRC));
        } else if(taskName.equalsIgnoreCase("gzip")  //$NON-NLS-1$
        		|| taskName.equalsIgnoreCase("bzip2")) { //$NON-NLS-1$
        	newNode= new AntTaskNode(newTask, generateLabel(newTask.getTaskName(), attributes, IAntModelConstants.ATTR_ZIPFILE));
        } else if(taskName.equalsIgnoreCase("exec")) { //$NON-NLS-1$
        	String label = "exec "; //$NON-NLS-1$
            String command = attributes.getValue(IAntModelConstants.ATTR_COMMAND);
            if(command != null) {
            	label += command;
            }
            command = attributes.getValue(IAntModelConstants.ATTR_EXECUTABLE);
            if(command != null) {
            	label += command;
            }
            newNode= new AntTaskNode(newTask, label);        
		} else if(taskName.equalsIgnoreCase("delete")) { //$NON-NLS-1$
			
        	String label = "delete "; //$NON-NLS-1$
            String file = attributes.getValue(IAntModelConstants.ATTR_FILE);
            if(file != null) {
            	label+= file;
            } else {
            	file = attributes.getValue(IAntModelConstants.ATTR_DIR);
            	if(file != null) {
            		label+= file;
            	}
            }
            newNode= new AntTaskNode(newTask, label);
        	
        } else if(taskName.equalsIgnoreCase("import")) { //$NON-NLS-1$
        	newNode= new AntTaskNode(newTask, generateLabel(taskName, attributes, IAntModelConstants.ATTR_FILE)); //$NON-NLS-1$
        } else {   
        	newNode = newNotWellKnownTaskNode(newTask, attributes);
        }
		String taskFileName= newTask.getLocation().getFileName();
		boolean external= isTaskExternal(taskFileName);
		newNode.setExternal(external);
		if (external) {
			newNode.setFilePath(taskFileName);
		}
		return newNode;
	}
            
	/**
	 * @param taskName the name of the task to check
	 * @return whether or not a task with this name sets properties
	 */
	public static boolean isPropertySettingTask(String taskName) {
		return taskName.equalsIgnoreCase("property") //$NON-NLS-1$
			|| taskName.equalsIgnoreCase("available") //$NON-NLS-1$
			|| taskName.equalsIgnoreCase("basename") //$NON-NLS-1$
			|| taskName.equalsIgnoreCase("condition") //$NON-NLS-1$
			|| taskName.equalsIgnoreCase("dirname") //$NON-NLS-1$
			|| taskName.equalsIgnoreCase("loadfile") //$NON-NLS-1$
			|| taskName.equalsIgnoreCase("pathconvert") //$NON-NLS-1$
			|| taskName.equalsIgnoreCase("uptodate") //$NON-NLS-1$
			|| taskName.equalsIgnoreCase("xmlproperty"); //$NON-NLS-1$
	}

	private boolean isTaskExternal(String taskFileName) {
		File taskFile= new File(taskFileName);
		return !taskFile.equals(getEditedFile());
	}

	private AntTaskNode newNotWellKnownTaskNode(Task newTask, Attributes attributes) {
		AntTaskNode newNode= new AntTaskNode(newTask);
		String id= attributes.getValue("id"); //$NON-NLS-1$
		if (id != null) {
			newNode.setId(id);
		}
		return newNode;
	}

	private String generateLabel(String taskName, Attributes attributes, String attributeName) {
		StringBuffer label = new StringBuffer(taskName);
        String srcFile = attributes.getValue(attributeName);
        if(srcFile != null) {
        	label.append(' ');
        	label.append(srcFile);
        }	
        return label.toString();
	}

	private void computeLength(AntElementNode element, int line, int column) {
		if (element.isExternal()) {
			element.setExternalInfo(line, column);
			return;
		}
		try {
			int length;
			int offset;
			if (column <= 0) {
				column= getLastCharColumn(line);
				String lineText= fDocument.get(fDocument.getLineOffset(line - 1), column);
				StringBuffer searchString= new StringBuffer("</"); //$NON-NLS-1$
				searchString.append(element.getName());
				searchString.append('>'); 
				int index= lineText.indexOf(searchString.toString());
				if (index == -1) {
					index= lineText.indexOf("/>"); //$NON-NLS-1$
					if (index == -1 ) {
						index= column; //set to the end of line 
					} else {
						index= index + 3;
					}
				} else {
					index= index + searchString.length() + 1;
				}
				offset= getOffset(line, index);
			} else {
				offset= getOffset(line, column);
			}
			
			length= offset - element.getOffset();
			element.setLength(length);
		} catch (BadLocationException e) {
			//ignore as the parser may be out of sync with the document during reconciliation
		}
	}
	
	private void computeOffset(AntElementNode element, int line, int column) {
		if (element.isExternal()) {
			return;
		}
		try {
			int offset;
			String prefix= "<"; //$NON-NLS-1$
			if (column <= 0) {
				offset= getOffset(line, 0);
				int lastCharColumn= getLastCharColumn(line);
				offset= computeOffsetUsingPrefix(element, line, offset, prefix, lastCharColumn);
			} else {
				offset= getOffset(line, column);
				offset= computeOffsetUsingPrefix(element, line, offset, prefix, column);
			}
 			
			element.setOffset(offset + 1);
			element.setSelectionLength(element.getName().length());
		} catch (BadLocationException e) {
			//ignore as the parser may be out of sync with the document during reconciliation
		}
	}
	
	private int computeOffsetUsingPrefix(AntElementNode element, int line, int offset, String prefix, int column) throws BadLocationException {
		String lineText= fDocument.get(fDocument.getLineOffset(line - 1), column);
		int lastIndex= lineText.indexOf(prefix + element.getName());
		if (lastIndex > -1) {
			offset= getOffset(line, lastIndex + 1);
		} else {
			return computeOffsetUsingPrefix(element, line - 1, offset, prefix, getLastCharColumn(line - 1));
		}
		return offset;
	}

	public int getOffset(int line, int column) throws BadLocationException {
		return fDocument.getLineOffset(line - 1) + column - 1;
	}
	
	private int getNonWhitespaceOffset(int line, int column) throws BadLocationException {
		int offset= fDocument.getLineOffset(line - 1) + column - 1;
		while(Character.isWhitespace(fDocument.getChar(offset))) {
			offset++;
		}
		return offset;
	}
	
	private int getLine(int offset) {
		try {
			return fDocument.getLineOfOffset(offset) + 1;
		} catch (BadLocationException be) {
			return -1;
		}
	}
	
	private int getLastCharColumn(int line) throws BadLocationException {
		String lineDelimiter= fDocument.getLineDelimiter(line - 1);
		int lineDelimiterLength= lineDelimiter != null ? lineDelimiter.length() : 0;
		return fDocument.getLineLength(line - 1) - lineDelimiterLength;
	}

	public void setCurrentElementLength(int lineNumber, int column) {
		fLastNode= (AntElementNode)fStillOpenElements.pop();
		if (fLastNode == fCurrentTargetNode) {
			fCurrentTargetNode= null; //the current target element has been closed
		}
		computeLength(fLastNode, lineNumber, column);
	}
	
	public void acceptProblem(IProblem problem) {
		if (fProblemRequestor != null) {
			fProblemRequestor.acceptProblem(problem);
			fMarkerUpdater.acceptProblem(problem);
		}
	}
	
	protected IFile getFile() {
		IPath location= fLocationProvider.getLocation();
		if (location == null) {
			return null;
		}
		IFile[] files= ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(location);
		if (files.length > 0) {
			return files[0];
		} 
		return null;
	}

	private void beginReporting() {
		if (fProblemRequestor != null) {
			fProblemRequestor.beginReporting();
			fMarkerUpdater.beginReporting();
		}
	}
	
	private void endReporting() {
		if (fProblemRequestor != null) {
			fProblemRequestor.endReporting();
		}
	}

	private IProblem createProblem(Exception exception, int offset, int length,  int severity) {
		return createProblem(exception.getMessage(), offset, length, severity);
	}
	
	private IProblem createProblem(String message, int offset, int length, int severity) {
		return new XMLProblem(message, severity, offset, length, getLine(offset));
	}

	protected void notifyProblemRequestor(Exception exception, AntElementNode element, int severity) {
		AntElementNode importNode= element.getImportNode();
		if (importNode != null) {
			element= importNode;
		}
		IProblem problem= createProblem(exception, element.getOffset(), element.getLength(), severity);
		acceptProblem(problem);
		element.associatedProblem(problem);
	}
	
	protected void notifyProblemRequestor(Exception exception, int offset, int length, int severity) {
		if (fProblemRequestor != null) {
			IProblem problem= createProblem(exception, offset, length, severity);
			acceptProblem(problem);
		}
	}

	public void warning(Exception exception) {
		notifyProblemRequestor(exception, (AntElementNode)fStillOpenElements.pop(), XMLProblem.SEVERITY_WARNING);
	}
	
	public void error(Exception exception) {
		handleError(exception, XMLProblem.SEVERITY_ERROR);
	}
	
	public void errorFromElementText(Exception exception, int start, int count) {
		AntElementNode node= fLastNode;
		if (node == null) {
			if (!fStillOpenElements.empty()) {
				node= (AntElementNode)fStillOpenElements.peek();
			} 
		}
		if (node == null) {
			return;
		}
		computeEndLocationForErrorNode(node, start, count);
		notifyProblemRequestor(exception, start, count, XMLProblem.SEVERITY_ERROR);
		markHierarchy(fLastNode, XMLProblem.SEVERITY_ERROR);
	} 
	
	public void errorFromElement(Exception exception, AntElementNode node, int lineNumber, int column) {
		if (node == null) {
			if (!fStillOpenElements.empty()) {
				node= (AntElementNode)fStillOpenElements.peek();
			} else {
				node= fLastNode;
			}
		}
		computeEndLocationForErrorNode(node, lineNumber, column);
		notifyProblemRequestor(exception, node, XMLProblem.SEVERITY_ERROR);
		markHierarchy(node, XMLProblem.SEVERITY_ERROR);
	}

	private AntElementNode createProblemElement(SAXParseException exception) {
		int lineNumber= exception.getLineNumber();
		StringBuffer message= new StringBuffer(exception.getMessage());
		if (lineNumber != -1){
			message.append(AntOutlineMessages.getString("AntModel.1") + lineNumber); //$NON-NLS-1$
		}

		AntElementNode errorNode= new AntElementNode(message.toString());
		errorNode.setFilePath(exception.getSystemId());
		errorNode.setProblemSeverity(XMLProblem.SEVERITY_ERROR);
		computeErrorLocation(errorNode, exception);
		return errorNode;
	}
	
	private void computeErrorLocation(AntElementNode element, SAXParseException exception) {
		if (element.isExternal()) {
			return;
		}
		
		int line= exception.getLineNumber();
		int startColumn= exception.getColumnNumber();
		computeEndLocationForErrorNode(element, line, startColumn);	
	}
	
	private void computeEndLocationForErrorNode(AntElementNode element, int line, int startColumn) {
		try {
			if (line <= 0) {
				line= 1;
			}
			int endColumn;
			if (startColumn <= 0) {
				if (element.getOffset() > -1) {
					startColumn= element.getOffset() + 1;
				} else {
					startColumn= 1;
				}
				endColumn= getLastCharColumn(line) + 1;
			} else {
				if (startColumn > 1) {
					--startColumn;
				}
				
				endColumn= startColumn;
				if (startColumn <= getLastCharColumn(line)) {
					++endColumn;
				}
			}
			
			int correction= 0;
			if (element.getOffset() == -1) {
				int originalOffset= getOffset(line, startColumn);
				int nonWhitespaceOffset= originalOffset; 
				try {
					nonWhitespaceOffset= getNonWhitespaceOffset(line, startColumn);
				} catch (BadLocationException be) {
				}
				element.setOffset(nonWhitespaceOffset);
				correction= nonWhitespaceOffset - originalOffset;
			}
			if (endColumn - startColumn == 0) {
				int offset= getOffset(line, startColumn);
				element.setLength(offset - element.getOffset() - correction);
			} else {
				element.setLength(endColumn - startColumn - correction);
			}
		} catch (BadLocationException e) {
			//ignore as the parser may be out of sync with the document during reconciliation
		}
	}

	private void handleError(Exception exception, int severity) {
		AntElementNode node= null;
		if (fStillOpenElements.isEmpty()) {
			if (exception instanceof SAXParseException) {
				node= createProblemElement((SAXParseException)exception);
			}
		} else {
			node= (AntElementNode)fStillOpenElements.peek();
		}
		if (node == null) {
			return;
		}
		markHierarchy(node, severity);
		
		if (exception instanceof SAXParseException) {
			SAXParseException parseException= (SAXParseException)exception;
			if (node.getOffset() == -1) { 
				computeEndLocationForErrorNode(node, parseException.getLineNumber() - 1, parseException.getColumnNumber());
			} else {
				int lineNumber= parseException.getLineNumber();
				int columnNumber= parseException.getColumnNumber();
				if (columnNumber == -1) {
					columnNumber= 1;
				}
				try {
					AntElementNode childNode= node.getNode(getNonWhitespaceOffset(lineNumber, columnNumber) + 1);
					if (childNode != null && childNode != node) {
						node= childNode;
						node.setProblemSeverity(severity);
					} else {
						node= createProblemElement(parseException);
					}
				} catch (BadLocationException be) {
					node= createProblemElement(parseException);
				}
			}
		}
		
		notifyProblemRequestor(exception, node, severity);
		
		if (node != null) {
			while (node.getParentNode() != null) {
				AntElementNode parentNode= node.getParentNode();
				if (parentNode.getLength() == -1) {
					parentNode.setLength(node.getOffset() - parentNode.getOffset() + node.getLength());
				}
				node= parentNode;
			}
		}
		
		if (fIncrementalTarget != null) { //update the targets length for the edit
			int editAdjustment= determineEditAdjustment(fDirtyRegion);
			fIncrementalTarget.setLength(fIncrementalTarget.getLength() + editAdjustment);
			AntElementNode startingNode= null;	
			while(fStillOpenElements.peek() != fIncrementalTarget) {
				startingNode= (AntElementNode)fStillOpenElements.pop();
				if (startingNode.getLength() > -1) {
					startingNode.setLength(startingNode.getLength() + editAdjustment);
				}
			}
			fStillOpenElements.pop(); //get rid of the incremental target
			if (startingNode != null && fIncrementalTarget.hasChildren()) {
				List children= fIncrementalTarget.getChildNodes();
				int index= children.indexOf(startingNode);
				updateNodesForIncrementalParse(editAdjustment, children, index);
			}
		}
	}
	
	public void fatalError(Exception exception) {
		handleError(exception, XMLProblem.SEVERITY_FATAL_ERROR);
	}
	
	public AntElementNode getOpenElement() {
		if (fStillOpenElements.isEmpty()) {
			return null;
		}
		return (AntElementNode)fStillOpenElements.peek();
	}

	
	public String getEntityPath(String entityName) {
		if (fEntityNameToPath != null) {
			return (String)fEntityNameToPath.get(entityName);
		} 
		return null;
	}
	
	public String getEntityName(String path) {
		if (fEntityNameToPath != null) {
			Iterator itr= fEntityNameToPath.keySet().iterator();
			String entityPath;
			String name;
			while (itr.hasNext()) {
				name= (String) itr.next();
				entityPath= (String) fEntityNameToPath.get(name);
				if (entityPath.equals(path)) {
					return name;
				}
			}
		} 
		return null;
	}
	
	public String getPropertyValue(String propertyName) {
		AntProjectNode projectNode= getProjectNode();
		if (projectNode == null) {
			return null;
		}
		return projectNode.getProject().getProperty(propertyName);
	}

	public Object getReferenceObject(String refId) {
		AntProjectNode projectNode= getProjectNode();
		if (projectNode == null) {
			return null;
		}
		try {
			Project project= projectNode.getProject();
			Object ref= project.getReference(refId);
			return ref;
			
		} catch (BuildException be) {
			handleBuildException(be, null);
		}
		return null;
	}

	public AntElementNode getReferenceNode(String text) {
		Object reference= getReferenceObject(text);
		if (reference == null) {
			return null;
		}
		
		Collection nodes= fTaskToNode.keySet();
		Iterator iter= nodes.iterator();
		while (iter.hasNext()) {
			Object original = iter.next();
			Object object= original;
			if (object instanceof UnknownElement) {
				UnknownElement element= (UnknownElement) object;
				object= element.getRealThing();
				if (object == null) {
					continue;
				}
			} 
			if (object == reference) {
				return (AntElementNode)fTaskToNode.get(original);
			}
		}
		return null;
	}
	
	private ClassLoader getClassLoader() {
		if (fgClassLoader == null) {
			fgClassLoader= AntCorePlugin.getPlugin().getNewClassLoader(true);
		}
		return fgClassLoader;
	}

	
	public String getTargetDescription(String targetRename) {
		AntProjectNode projectNode= getProjectNode();
		if (projectNode == null) {
			return null;
		}
		Project project= projectNode.getProject();
		Map targets= project.getTargets();
		Target target= (Target)targets.get(targetRename);
		if (target != null) {
			return target.getDescription();
		}
		return null;
	}
	
	public AntProjectNode getProjectNode(boolean doReconcile) {
		if (doReconcile) {
			reconcile(null);
		}
		return fProjectNode;
	}
	
	public AntProjectNode getProjectNode() {
		return getProjectNode(true);
	}
	
	public void setReplaceHasOccurred() {
		fReplaceHasOccurred= true;
	}
	
	public void updateMarkers() {
		reconcile(null);
		fMarkerUpdater.updateMarkers();
	}
	
	public AntElementNode getNode(int offset, boolean waitForReconcile) {
		if (getProjectNode(waitForReconcile) != null) {
			return getProjectNode(waitForReconcile).getNode(offset);
		}
		return null;
	}
	
	/**
	 * The Ant model has been reconciled for the first time with the contents displayed in the Ant editor.
	 * Since problem marker creation has been added after many buildfiles have been created (or if the file has been
	 * created outside of Eclipse) we need to update the markers to match the problems.
	 */
	public void updateForInitialReconcile() {
		fMarkerUpdater.updateMarkers();
	}

	/**
     * Provides the set of names of the defining nodes that existed from the previous
     * parse of the build file.
     */
	public void setNamesOfOldDefiningNodes(Set set) {
		fNamesOfOldDefiningNodes= set;
	}
	
	/**
     * Removes any type definitions that no longer exist in the buildfile
     */
	private void reconcileTaskAndTypes() {
		if (fNamesOfOldDefiningNodes == null) {
			return;
		}
		Iterator iter= fNamesOfOldDefiningNodes.iterator();
		while (iter.hasNext()) {
			String nodeLabel = (String) iter.next();
			if (fProjectNode.getDefininingTaskNode(nodeLabel) == null) {
				ComponentHelper helper= ComponentHelper.getComponentHelper(fProjectNode.getProject());
				helper.getAntTypeTable().remove(nodeLabel);
				iter.remove();
			}
		}
	}
}