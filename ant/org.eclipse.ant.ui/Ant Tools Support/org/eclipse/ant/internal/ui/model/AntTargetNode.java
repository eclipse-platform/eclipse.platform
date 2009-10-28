/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.model;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.Target;
import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRegion;

public class AntTargetNode extends AntElementNode {

	private Target fTarget= null;
	private String fLabel= null;
	
	public AntTargetNode(Target target) {
		super("target"); //$NON-NLS-1$
		fTarget= target;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#getLabel()
	 */
	public String getLabel() {
	    if (fLabel == null) {
	        StringBuffer displayName= new StringBuffer(getTargetName());
	        if (isDefaultTarget()) {
	            displayName.append(AntModelMessages.AntTargetNode_2);
	        }
	        if (isExternal()) {
	            appendEntityName(displayName);
	        }
		
			fLabel= displayName.toString();
	    }
	    return fLabel;
	}
	
	public Target getTarget() {
		return fTarget;
	}
	
	public boolean isDefaultTarget() {
		String targetName= fTarget.getName();
		if (targetName == null) {
			return false;
		}
		return targetName.equals(fTarget.getProject().getDefaultTarget());
	}
	
	/**
	 * Returns whether this target is an internal target. Internal
	 * targets are targets which has no description or starts with hyphen ('-') character. The default target
	 * is never considered internal.
	 * @return whether the given target is an internal target
	 */
	public boolean isInternal() {
		Target target= getTarget();
		return (target.getDescription() == null || getTargetName().startsWith("-")) && !isDefaultTarget(); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.model.AntElementNode#getBaseImageDescriptor()
	 */
	protected ImageDescriptor getBaseImageDescriptor() {
		ImageDescriptor base= null;
		if (isDefaultTarget()) {
			base = AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_DEFAULT_TARGET);
		} else if (isInternal()) {
			base = AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_TARGET_INTERNAL);
		} else {
			base = AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_TARGET);
		}
		return base;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.model.AntElementNode#reset()
	 */
	public void reset() {
		super.reset();
		 Map currentTargets = fTarget.getProject().getTargets();
		 if (currentTargets.get(fTarget.getName()) != null) {
		 	currentTargets.remove(fTarget.getName());
		 }
	}

	/**
	 * Returns the name of a missing dependency or <code>null</code> if all
	 * dependencies exist in the project.
	 */
	public String checkDependencies() {
		Enumeration dependencies= fTarget.getDependencies();
		while (dependencies.hasMoreElements()) {
			String dependency = (String) dependencies.nextElement();
			 if (fTarget.getProject().getTargets().get(dependency) == null) {
			 	return dependency;
			 }
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#collapseProjection()
	 */
	public boolean collapseProjection() {
		IPreferenceStore store= AntUIPlugin.getDefault().getPreferenceStore();		
		if (store.getBoolean(AntEditorPreferenceConstants.EDITOR_FOLDING_TARGETS)) {
			return true;
		}
		return false;
	}

    public String getTargetName() {
        String targetName= fTarget.getName();
		if (targetName == null) {
			targetName= "target"; //$NON-NLS-1$
			setProblemSeverity(AntModelProblem.SEVERITY_ERROR);
		}
		return targetName;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#containsOccurrence(java.lang.String)
	 */
	public boolean containsOccurrence(String identifier) {
		if (getTargetName().equals(identifier)) {
			return true;
		}
		Enumeration dependencies= fTarget.getDependencies();
		while (dependencies.hasMoreElements()) {
			String dependency = (String) dependencies.nextElement();
			 if (dependency.equals(identifier)) {
			 	return true;
			 }
		}
        //looking for properties
		if (identifier.startsWith("${") && identifier.endsWith("}")) { //$NON-NLS-1$ //$NON-NLS-2$
		    String ifString= fTarget.getIf();
		    if (ifString != null && ifString.endsWith(identifier.substring(2, identifier.length() - 1))) {
		        return true;
		    }
		    String unlessString= fTarget.getUnless();
		    if (unlessString != null && unlessString.endsWith(identifier.substring(2, identifier.length() - 1))) {
		        return true;
		    }
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#getOccurrencesIdentifier()
	 */
	public String getOccurrencesIdentifier() {
		return getTargetName();
	}
    
    public List computeIdentifierOffsets(String identifier) {
        String textToSearch= getAntModel().getText(getOffset(), getLength());
        if (textToSearch == null || textToSearch.length() == 0 || identifier.length() == 0) {
        	return null;
        }
        List results= new ArrayList();
        if (getTargetName().equals(identifier)) {
            int nameOffset= textToSearch.indexOf("name"); //$NON-NLS-1$
            nameOffset= textToSearch.indexOf(identifier, nameOffset);
            results.add(new Integer(getOffset() + nameOffset));
        } else {
        	String ifString= fTarget.getIf();
    		if (ifString != null && ifString.endsWith(identifier)) {
    			int ifOffset= textToSearch.indexOf("if"); //$NON-NLS-1$
                ifOffset= textToSearch.indexOf(identifier, ifOffset);
                results.add(new Integer(getOffset() + ifOffset));
    		} else {
    			String unlessString= fTarget.getUnless();
    			if (unlessString != null && unlessString.endsWith(identifier)) {
    				int unlessOffset= textToSearch.indexOf("unless"); //$NON-NLS-1$
    	            unlessOffset= textToSearch.indexOf(identifier, unlessOffset);
    	            results.add(new Integer(getOffset() + unlessOffset));
    			} else {	
			        int dependsOffset= textToSearch.indexOf("depends"); //$NON-NLS-1$
					while (dependsOffset > 0 && !Character.isWhitespace(textToSearch.charAt(dependsOffset - 1))) {
						dependsOffset= textToSearch.indexOf("depends", dependsOffset + 1); //$NON-NLS-1$
					}
			        if (dependsOffset != -1) {
						dependsOffset+= 7;
			            int dependsOffsetEnd= textToSearch.indexOf('"', dependsOffset);
			            dependsOffsetEnd= textToSearch.indexOf('"', dependsOffsetEnd+1);
			            while (dependsOffset < dependsOffsetEnd) {
			                dependsOffset= textToSearch.indexOf(identifier, dependsOffset);
			                if (dependsOffset == -1 || dependsOffset > dependsOffsetEnd) {
			                    break;
			                }
			                char delimiter= textToSearch.charAt(dependsOffset - 1);
			                if (delimiter == ',' || delimiter == '"' || delimiter == ' ') {
			                	results.add(new Integer(getOffset() + dependsOffset));
			                }
			            	dependsOffset+= identifier.length();
			            }
			        }
    			}
    		}
        }
        return results;
    }
    
    /* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#isRegionPotentialReference(org.eclipse.jface.text.IRegion)
	 */
	public boolean isRegionPotentialReference(IRegion region) {
		boolean superOK= super.isRegionPotentialReference(region);
		if (!superOK) {
			return false;
		}
		
		String textToSearch= getAntModel().getText(getOffset(), getLength());
		if (textToSearch == null) {
			return false;
		}
		if (checkReferenceRegion(region, textToSearch, "depends")) { //$NON-NLS-1$
			return true;
		} else if (checkReferenceRegion(region, textToSearch, "name")) { //$NON-NLS-1$
			return true;
		} else if (checkReferenceRegion(region, textToSearch, "if")) { //$NON-NLS-1$
			return true;
		} 
		return checkReferenceRegion(region, textToSearch, "unless"); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.ant.internal.ui.model.AntElementNode#isFromDeclaration(org.eclipse.jface.text.IRegion)
     */
    public boolean isFromDeclaration(IRegion region) {
    	 String textToSearch= getAntModel().getText(getOffset(), getLength());
         if (textToSearch == null || textToSearch.length() == 0) {
         	return false;
         }
         return checkReferenceRegion(region, textToSearch, "name"); //$NON-NLS-1$
	}
}
