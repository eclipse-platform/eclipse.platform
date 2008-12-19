/*******************************************************************************
 * Copyright (c) 2002, 2008 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH, 
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug fixes
 *     John-Mason P. Shackelford (john-mason.shackelford@pearson.com) - bug 49445
 *******************************************************************************/

package org.eclipse.ant.internal.ui.model;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import com.ibm.icu.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ant.internal.ui.AntImageDescriptor;
import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.graphics.Image;

/**
 * General representation of an Ant buildfile element.
 * 
 */
public class AntElementNode implements IAdaptable, IAntElement {
    
	/**
	 * The offset of the corresponding source.
	 * @see #getOffset()
	 */
	protected int fOffset= -1;
	
	/**
	 * The length of the corresponding source.
	 * @see #getLength()
	 */
	protected int fLength= -1;
	
	/**
	 * The length of the source to select for this node
	 */
	protected int fSelectionLength;
	
    /**
     * The parent node.
     */
    protected AntElementNode fParent;
    
    /**
     * The import node that "imported" this element
     */
    private AntElementNode fImportNode;

    /**
     * The child nodes.
     */
    protected List fChildNodes= null;

    /**
     * The (tag-)name of the element.
     */
    protected String fName;

	/**
	 * Whether this element has been generated as part of an element hierarchy
	 * this has problems. This is the severity of the problem.
	 * @see XMLProblem#NO_PROBLEM
	 * @see XMLProblem#SEVERITY_ERROR
	 * @see XMLProblem#SEVERITY_WARNING
	 * @see XMLProblem#SEVERITY_FATAL_ERROR
	 */
	private int fProblemSeverity= AntModelProblem.NO_PROBLEM;
	
	private String fProblemMessage= null;
	
	/**
	 * The absolute file system path of the file this element is
	 * defined within.
	 */
	private String fFilePath;
	
	/**
	 * Whether this element has been generated from an external entity definition
	 */
	private boolean fIsExternal = false;
	
	/**
	 * The unique (in the corresponding element tree) path of this element.
	 */
	private String fElementPath;
	
	/**
	 * The (not necessarily unique) identifier of this element.
	 */
	private String fElementIdentifier;

	/**
	 * The problem associated with this node. May be <code>null</code>.
     */
	private IProblem fProblem;
	
	/**
	 * The unique index of this element in it's parents child collection
     */
	private int fIndex= 0;
	
	/**
	 * Only used when opening an import element to indicate the location in the imported file
	 */
	private int fLine;
	private int fColumn;

	/**
     * Creates an instance with the specified name.
     */
    public AntElementNode(String aName) {
       fName = aName;
    }
    
    public AntElementNode() {
    }
    
    /**
     * Returns the name.
     */
    public String getName() {
        return fName;
    }
    
    /**
     * Returns the label that is used for display.
     * <P>
     * The default implementation returns just the same as the method <code>getName()</code>.
     * Override this method in your own subclass for special elements in order to provide a
     * custom label.
     */
    public String getLabel() {
    	return getName();
    }
    
    /**
     * Returns the child nodes.
     */
    public List getChildNodes() {
        return fChildNodes;
    }

	/**
     * Returns the parent <code>AntElementNode</code>.
     * 
     * @return the parent or <code>null</code> if this element has no parent.
     */
    public AntElementNode getParentNode() {
        return fParent;
    } 
    
    public AntProjectNode getProjectNode() {
    	AntElementNode projectParent= getParentNode();
    	while (projectParent != null && !(projectParent instanceof AntProjectNode)) {
    		projectParent= projectParent.getParentNode();
    	}
    	return (AntProjectNode)projectParent;
    }
    
    /**
     * Adds the specified element as a child.
     * <P>
     * The specified element will have this assigned as its parent.
     */
    public void addChildNode(AntElementNode childElement) {
    	childElement.setParent(this);
        synchronized (this) {
            if (fChildNodes == null) {
                fChildNodes= new ArrayList();
            }
            fChildNodes.add(childElement);
            childElement.setIndex(fChildNodes.size() - 1);
        }
    }
    
	private void setIndex(int index) {
		fIndex= index;
	}

	protected void setParent(AntElementNode node) {
		fParent= node;
	}

	/**
	 * Sets the absolute file system path of the file this element is defined
	 * within.
	 */
	public void setFilePath(String path) {
		if (path == null) {
			return;
		}
		URL url= null;
		try {		
			url= new URL(path);
		} catch (MalformedURLException e) {		
			fFilePath= path;
			return;
		}
		fFilePath = new Path(new File(url.getPath()).getAbsolutePath()).toString();
	}
	
	/**
	 * Returns the absolute file system path of the file this element is defined
     * within. Only relevant for nodes that are external
     * @see #isExternal()
     */
	public String getFilePath() {
		return fFilePath;
	}

    /**
	 * Returns the 0-based index of the first character of the source code for this element,
	 * relative to the source buffer in which this element is contained.
	 * 
	 * @return the 0-based index of the first character of the source code for this element,
	 * relative to the source buffer in which this element is contained
	 */
	public int getOffset() {
		return fOffset;
	}
	
	/**
	 * Sets the offset.
	 * 
	 * @see #getOffset()
	 */
	public void setOffset(int anOffset) {
		fOffset = anOffset;
	}
	
	/**
	 * Returns the number of characters of the source code for this element,
	 * relative to the source buffer in which this element is contained.
	 * 
	 * @return the number of characters of the source code for this element,
	 * relative to the source buffer in which this element is contained
	 */
    public int getLength() {
        return fLength;
    }

	/**
	 * Sets the length.
	 * 
	 * @see #getLength()
	 */
	public void setLength(int aLength) {
		fLength = aLength;
		if (fProblem != null && fProblem instanceof AntModelProblem) {
			((AntModelProblem)fProblem).setLength(aLength);
		}
	}

	/**
	 * Returns a string representation of this element.
	 */
	public String toString() {
		return "Ant Element Node: " + getLabel() + " Offset: " + getOffset() + " Length: " + getLength();  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}
	
	/**
	 * Returns whether this element has been generated as part of an element
	 * hierarchy that has error(s) associated with it
	 */
	public boolean isErrorNode() {
		return fProblemSeverity == AntModelProblem.SEVERITY_ERROR || fProblemSeverity == AntModelProblem.SEVERITY_FATAL_ERROR;
	}
	
	/**
	 * Returns whether this element has been generated as part of an element
	 * hierarchy that has warning(s) associated with it
	 */
	public boolean isWarningNode() {
		return fProblemSeverity == AntModelProblem.SEVERITY_WARNING;
	}
	
	/**
	 * Sets whether this element has been generated as part of an element
	 * hierarchy that has problems. The severity of the problem is provided.
	 */
	public void setProblemSeverity(int severity) {
		fProblemSeverity= severity;
	}
	
	/**
	 * Returns whether this xml element is defined in an external entity.
	 * 
	 * @return boolean
	 */
	public boolean isExternal() {
		return fIsExternal;
	}

	/**
	 * Sets whether this xml element is defined in an external entity.
	 */
	public void setExternal(boolean isExternal) {
		fIsExternal = isExternal;
	}
	
    /**
     * Returns a unique string representation of this element. The format of
     * the string is not specified.
     *
     * @return the string representation
     */
	public String getElementPath() {
		if (fElementPath == null) {
			StringBuffer buffer= new StringBuffer();
			String buildFileName= getProjectNode().getBuildFileName();
			if (buildFileName != null) {
				buffer.append(buildFileName);
			}
			buffer.append(getParentNode() != null ? getParentNode().getElementPath() : ""); //$NON-NLS-1$
			buffer.append('/');
			buffer.append(getElementIdentifier());
			buffer.append('[');
			buffer.append(fIndex);
			buffer.append(']');
			
			fElementPath= buffer.toString();
		}
		return fElementPath;
	}

	private String getElementIdentifier() {
		if (fElementIdentifier == null) {
			StringBuffer buffer= escape(new StringBuffer(getName() != null ? getName() : ""), '\\', "$/[]\\"); //$NON-NLS-1$ //$NON-NLS-2$
			buffer.append('$');
			buffer.append(escape(new StringBuffer(getLabel() != null ? getLabel() : ""), '\\', "$/[]\\").toString()); //$NON-NLS-1$ //$NON-NLS-2$
			
			fElementIdentifier= buffer.toString();
		}
		return fElementIdentifier;
	}

	private StringBuffer escape(StringBuffer sb, char esc, String special) {
		for (int i= 0; i < sb.length(); i++) {
			if (special.indexOf(sb.charAt(i)) >= 0) {
				sb.insert(i++, esc);
			}
		}

		return sb;
	}

//	private int getElementIndexOf(AntElementNode child) {
//		if (getChildNodes() == null) {
//			return -1;
//		}
//		
//		int result= -1;
//		
//		Iterator iter= getChildNodes().iterator();
//		AntElementNode current= null;
//		while (current != child && iter.hasNext()) {
//			current= (AntElementNode) iter.next();
//			if (child.getElementIdentifier().equals(current.getElementIdentifier()))
//				result++;
//		}
//		
//		if (current != child) {
//			return -1;
//		}
//		
//		return result;
//	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o2) {
		Object o1= this;
		
		if (o1 == o2) {
			return true;
		}
		if (o2 == null) {
			return false;
		}
		if (!(o1 instanceof AntElementNode || o2 instanceof AntElementNode)) {
			return o2.equals(o1);
		}
		if (!(o1 instanceof AntElementNode && o2 instanceof AntElementNode)) {
			return false;
		}
		
		AntElementNode e1= (AntElementNode) o1;
		AntElementNode e2= (AntElementNode) o2;
	
		return e1.getElementPath().equals(e2.getElementPath());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getElementPath().hashCode();
	}

	/**
	 * Returns the length of source to select for this node.
	 * @return the length of source to select
	 */
	public int getSelectionLength() {
		return fSelectionLength;
	}
	
	public void setSelectionLength(int selectionLength) {
		this.fSelectionLength= selectionLength;
	}
	
	/**
	 * Returns the node with the narrowest source range that contains the offset.
	 * It may be this node or one of its children or <code>null</code> if the offset is not in the source range of this node.
	 * @param sourceOffset The source offset
	 * @return the node that includes the offset in its source range or <code>null</code>
	 */
	public AntElementNode getNode(int sourceOffset) {
        synchronized (this) {
            if (fChildNodes != null) {
    			for (Iterator iter = fChildNodes.iterator(); iter.hasNext(); ) {
    				AntElementNode node = (AntElementNode) iter.next();
    				AntElementNode containingNode= node.getNode(sourceOffset);
    				if (containingNode != null) {
    					return containingNode;
    				}
    			}
            }
		}
		if (fLength == -1 && fOffset <= sourceOffset && !isExternal()) { //this is still an open element
			return this;
		}
		if (fOffset <= sourceOffset && sourceOffset <= (fOffset + fLength - 2)) {
			return this;
		}
		
		return null;
	}
	
	public Image getImage() {
		int flags = 0;
		
		if (isErrorNode()) {
			flags = flags | AntImageDescriptor.HAS_ERRORS;
		} else if (isWarningNode()) {
			flags = flags | AntImageDescriptor.HAS_WARNINGS;
		}
		if(fImportNode != null || isExternal()){
			flags = flags | AntImageDescriptor.IMPORTED;
		}
		ImageDescriptor base= getBaseImageDescriptor();
		return AntUIImages.getImage(new AntImageDescriptor(base, flags));			
	}

	protected ImageDescriptor getBaseImageDescriptor() {
		return AntUIImages.getImageDescriptor(IAntUIConstants.IMG_TASK_PROPOSAL);
	}
	
	protected IAntModel getAntModel() {
		AntElementNode parentNode= getParentNode();
		while (!(parentNode instanceof AntProjectNode)) {
			parentNode= parentNode.getParentNode();
		}
		return parentNode.getAntModel();
	}

	/**
	 * Sets the problem associated with this element
	 * @param problem The problem associated with this element.
	 */
	public void associatedProblem(IProblem problem) {
		fProblem= problem;
	}
	
	public IProblem getProblem() {
	    return fProblem;
	}

	protected void appendEntityName(StringBuffer displayName) {
		String path= getFilePath();
		
		if (getImportNode() != null) {
			displayName.append(MessageFormat.format(AntModelMessages.AntElementNode_9, new String[]{getImportNode().getLabel()}));
		} else {
			String entityName= getAntModel().getEntityName(path);
			displayName.append(MessageFormat.format(AntModelMessages.AntElementNode_9, new String[]{entityName}));
		}
	}
	
	public AntElementNode getImportNode() {
		return fImportNode;
	}
	
	public void setImportNode(AntElementNode importNode) {
		fImportNode = importNode;
	}
	
	public boolean hasChildren() {
		if (fChildNodes == null) {
			return false;
		}
		return !fChildNodes.isEmpty();
	}

	public void reset() {
		fChildNodes= null;
	}

	public void setExternalInfo(int line, int column) {
		fLine= line;
		fColumn= column;
	}
	
	public int[] getExternalInfo() {
		return new int[] {fLine, fColumn};
	}
	
	/**
     * Return the resource that contains the definition of this
     * Ant node.
     * @return The resource that contains the definition of this ant node or <code>null</code>
     * if that resource could not be determined (a buildfile that is external to the workspace).
     */
	public IFile getIFile() {
		if (isExternal()) {
			return AntUtil.getFileForLocation(fFilePath, null);
		} 
		return getBuildFileResource();
	}
	
	/**
     * Return the resource that is the main build file for this
     * Ant node.
     * @return The resource that is the main buildfile for this ant node or <code>null</code>
     * if that resource could not be determined (a buildfile that is external to the workspace).
     */
	public IFile getBuildFileResource() {
		LocationProvider locationProvider= getAntModel().getLocationProvider();
		return locationProvider.getFile();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}
	
	/**
	 * Returns whether this node is a structural node that should be shown in the buildfile outline.
	 * For example, an AntCommentNode would return <code>false</code>
	 * 
	 * @return whether this node is a structural node that should be shown in the buildfile outline
	 */
	public boolean isStructuralNode() {
		return true;
	}
	
	/**
	 * Returns whether to collapse the code folding projection (region) represented by this node. 
	 * @return whether the user preference is set to collapse the code folding projection (region)
	 *  represented by this node
	 */
	public boolean collapseProjection() {
		return false;
	}
	
	public void dispose() {
        getAntModel().dispose();
    }
    
    /**
     * Returns the name or path of the element referenced at the offset within the declaration of this node or
     * <code>null</code> if no element is referenced at the offset
     * @param offset The offset within the declaration of this node
     * @return <code>null</code> or the name or path of the referenced element
     */
    public String getReferencedElement(int offset) {
		return null;
	}
   
    public String getProblemMessage() {
        return fProblemMessage;
    }
   
    public void setProblemMessage(String problemMessage) {
        fProblemMessage = problemMessage;
    }

	/**
	 * Returns whether this node contains a reference to the supplied identifier
	 * 
	 * @param identifier
	 * @return whether this node contains a reference to the supplied identifier
	 */
	public boolean containsOccurrence(String identifier) {
		return false;
	}
	
	/**
	 * Returns the identifier to use for matching occurrences in the Ant editor.
	 * 
	 * @return the occurrences identifier for this node
	 */
	public String getOccurrencesIdentifier() {
		return getLabel();
	}

	/**
	 * Returns whether the supplied region can be considered as an area in this node containing
	 * a reference.
	 * 
	 * @param region the area to consider for finding a reference
	 * @return whether a reference could exist in this node from the supplied region 
	 */
	public boolean isRegionPotentialReference(IRegion region) {
		return region.getOffset() >= fOffset;
	}

    public List computeIdentifierOffsets(String identifier) {
        return null;
    }
    
	/**
	 * Returns whether the supplied region is from within this node's 
	 * declaration identifier area
	 * @param region The region to check
	 * @return whether the region is from within this node and is
	 * 			the declaration of a reference.
	 */
	public boolean isFromDeclaration(IRegion region) {
		return false;
	}

	protected boolean checkReferenceRegion(IRegion region, String textToSearch, String attributeName) {
		int attributeOffset= textToSearch.indexOf(attributeName);
		while (attributeOffset > 0 && !Character.isWhitespace(textToSearch.charAt(attributeOffset - 1))) {
			attributeOffset= textToSearch.indexOf(attributeName, attributeOffset + 1);
		}
		if (attributeOffset != -1) {
			attributeOffset+= attributeName.length();
			int attributeOffsetEnd = textToSearch.indexOf('"', attributeOffset);
			attributeOffsetEnd = textToSearch.indexOf('"', attributeOffsetEnd + 1);
			return region.getOffset() >= getOffset() + attributeOffset && (region.getOffset() + region.getLength()) <= getOffset() + attributeOffsetEnd;
		}
		return false;
	}
}