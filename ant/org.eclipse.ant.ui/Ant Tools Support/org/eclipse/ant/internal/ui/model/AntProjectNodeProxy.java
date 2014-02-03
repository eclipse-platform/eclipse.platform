/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.model;

import java.util.Collections;
import java.util.List;

import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;


public class AntProjectNodeProxy extends AntProjectNode {
	
	private String fBuildFileName;
	private String fDefaultTargetName;
	private boolean fParsed= false;

	/**
	 * Creates a new project node with the given name and the given build file
	 * name.
	 * 
	 * @param name the project's name or <code>null</code> if the project's
	 * name is not known. If this value is <code>null</code>, the file will be
	 * parsed the first time a value is requested that requires it.
	 * @param buildFileName
	 */
	public AntProjectNodeProxy(String name, String buildFileName) {
		super(null, null);
		fName= name;
		fBuildFileName= buildFileName;
	}
	
	/**
	 * Creates a new project node on the given build file.
	 */
	public AntProjectNodeProxy(String buildFileName) {
		this(null, buildFileName);
	}
	
	public void parseBuildFile(boolean force) {
		if (fParsed && !force) {
			return;
		}
		fChildNodes= null;
		fParsed= true;
		AntTargetNode[] nodes = null;
		IPath buildFilePath= AntUtil.getFile(getBuildFileName()).getLocation();
		if (buildFilePath == null) {
			setProblemSeverity(AntModelProblem.SEVERITY_ERROR);
			setProblemMessage(AntModelMessages.AntProjectNodeProxy_0);
			return;
		}
		nodes = AntUtil.getTargets(buildFilePath.toString());
		
		if (nodes == null || nodes.length < 1) {
			setProblemSeverity(AntModelProblem.SEVERITY_ERROR);
			setProblemMessage(AntModelMessages.AntProjectNodeProxy_1);
			return;
		}
		
		AntProjectNode projectNode = nodes[0].getProjectNode();
        if (nodes[0].getTargetName().length() != 0) {
            //not just the implicit target
            for (int i = 0; i < nodes.length; i++) {
                addChildNode(nodes[i]);
            }
        }
		
		fModel= projectNode.getAntModel();
		fProject= (AntModelProject)projectNode.getProject();
		fLabel= null;
		fName= null;
	}
	
	public void parseBuildFile() {
		parseBuildFile(false);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.model.AntProjectNode#getDescription()
	 */
	public String getDescription() {
		if (fProject != null) {
			return super.getDescription();
		}
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#getLabel()
	 */
	public String getLabel() {
	    if (fName != null) {
	        return fName;
	    }
		if (fProject == null) {
			parseBuildFile();
		}
		StringBuffer name= new StringBuffer(super.getLabel());
		AntProjectNode realNode= getRealNode();
		if (realNode != null && realNode.getProblemMessage() != null) {
			name.append(" <"); //$NON-NLS-1$
			name.append(getRealNode().getProblemMessage());
			name.append('>');
		}
		fName= name.toString();
		return fName;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#getChildNodes()
	 */
	public List getChildNodes() {
		if (fProject == null) {
			parseBuildFile();
		}
		List children= super.getChildNodes();
		if (children == null) {
		    return Collections.EMPTY_LIST;
		}
		return children;
	}
	
	public String getBuildFileName() {
		return fBuildFileName;
	}

	public void setDefaultTargetName(String defaultTarget) {
		fDefaultTargetName= defaultTarget;
	}
	
	public String getDefaultTargetName() {
		if (fProject == null) {
			return fDefaultTargetName;
		}
		return super.getDefaultTargetName();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#hasChildren()
	 */
	public boolean hasChildren() {
		return true;
	}
    
    /* (non-Javadoc)
     * @see org.eclipse.ant.internal.ui.model.AntElementNode#dispose()
     */
    public void dispose() {
        if (fProject != null) {
            super.dispose();
        }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ant.internal.ui.model.AntElementNode#isErrorNode()
     */
    public boolean isErrorNode() {
        if (fProject == null) {
            return super.isErrorNode();
        } 
        return getRealNode().isErrorNode();
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ant.internal.ui.model.AntElementNode#getProblemMessage()
     */
    public String getProblemMessage() {
		if (isErrorNode()) {
			return getBuildFileName();
		}
    	return null;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ant.internal.ui.model.AntElementNode#isWarningNode()
     */
    public boolean isWarningNode() {
        if (fProject == null) {
            return super.isWarningNode();
        } 
        return getRealNode().isWarningNode();
    }
    
	private AntProjectNode getRealNode() {
		if (fModel != null) {
			return fModel.getProjectNode();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#getAntModel()
	 */
	protected IAntModel getAntModel() {
		if (fProject == null) {
			parseBuildFile();
		}
		return super.getAntModel();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#getLength()
	 */
	public int getLength() {
		if (fProject == null) {
			parseBuildFile();
		}
		AntProjectNode realNode= getRealNode();
		if (realNode == null) {
			return -1;
		}
		return realNode.getLength();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#getOffset()
	 */
	public int getOffset() {
		if (fProject == null) {
			parseBuildFile();
		}
		AntProjectNode realNode= getRealNode();
		if (realNode == null) {
			return -1;
		}
		return realNode.getOffset();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#getSelectionLength()
	 */
	public int getSelectionLength() {
		if (fProject == null) {
			parseBuildFile();
		}
		AntProjectNode realNode= getRealNode();
		if (realNode == null) {
			return -1;
		}
		return realNode.getSelectionLength();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#getBuildFileResource()
	 */
	public IFile getBuildFileResource() {
		if (fProject == null) {
			if (fBuildFileName != null) {
				return AntUtil.getFile(fBuildFileName);
			}
		}
		return super.getBuildFileResource();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
        return getLabel();
	}
}
