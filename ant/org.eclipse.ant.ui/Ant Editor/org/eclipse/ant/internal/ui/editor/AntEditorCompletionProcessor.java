/*******************************************************************************
 * Copyright (c) 2002, 2004 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH, 
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug fixes
 *     John-Mason P. Shackelford (john-mason.shackelford@pearson.com) - bug 49383, 58299, 59024
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.Available;
import org.apache.tools.ant.taskdefs.MacroDef;
import org.apache.tools.ant.taskdefs.MacroInstance;
import org.apache.tools.ant.taskdefs.Parallel;
import org.apache.tools.ant.taskdefs.PathConvert;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.taskdefs.Sequential;
import org.apache.tools.ant.taskdefs.UpToDate;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Reference;
import org.eclipse.ant.internal.ui.dtd.IAttribute;
import org.eclipse.ant.internal.ui.dtd.IDfm;
import org.eclipse.ant.internal.ui.dtd.IElement;
import org.eclipse.ant.internal.ui.dtd.ISchema;
import org.eclipse.ant.internal.ui.dtd.ParseError;
import org.eclipse.ant.internal.ui.dtd.Parser;
import org.eclipse.ant.internal.ui.editor.model.AntDefiningTaskNode;
import org.eclipse.ant.internal.ui.editor.model.AntElementNode;
import org.eclipse.ant.internal.ui.editor.model.AntProjectNode;
import org.eclipse.ant.internal.ui.editor.model.AntTargetNode;
import org.eclipse.ant.internal.ui.editor.model.AntTaskNode;
import org.eclipse.ant.internal.ui.editor.outline.AntModel;
import org.eclipse.ant.internal.ui.editor.templates.AntTemplateAccess;
import org.eclipse.ant.internal.ui.editor.templates.AntTemplateProposal;
import org.eclipse.ant.internal.ui.editor.templates.BuildFileContextType;
import org.eclipse.ant.internal.ui.editor.templates.TaskContextType;
import org.eclipse.ant.internal.ui.model.AntUIImages;
import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.IAntUIConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.FileEditorInput;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The completion processor for the Ant Editor.
 */
public class AntEditorCompletionProcessor  extends TemplateCompletionProcessor implements IContentAssistProcessor  {       
 
 	private Comparator proposalComparator= new Comparator() {
		public int compare(Object o1, Object o2) {
		    
			int type1= getProposalType(o1);
			int type2= getProposalType(o2);
			if (type1 != type2) {
				if (type1 > type2) {
					return 1;
				}  else {
					return -1;
				}
			}
			String string1 = ((ICompletionProposal)o1).getDisplayString();
			String string2 = ((ICompletionProposal)o2).getDisplayString();
			return string1.compareToIgnoreCase(string2);
		}
		private int getProposalType(Object o){
		    if(o instanceof AntCompletionProposal){
		        return ((AntCompletionProposal) o).getType();
		    } else {
		    	return AntCompletionProposal.TASK_PROPOSAL;    
		    }
		}
 	};
	
	protected final static int PROPOSAL_MODE_NONE = 0;
	protected final static int PROPOSAL_MODE_BUILDFILE = 1;
	protected final static int PROPOSAL_MODE_TASK_PROPOSAL = 2;
	protected final static int PROPOSAL_MODE_PROPERTY_PROPOSAL = 3;
	protected final static int PROPOSAL_MODE_ATTRIBUTE_PROPOSAL = 4;
	protected final static int PROPOSAL_MODE_TASK_PROPOSAL_CLOSING = 5;
	protected final static int PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL = 6;
	
	private final static ICompletionProposal[] NO_PROPOSALS= new ICompletionProposal[0];
	
    /**
     * The line where the cursor sits now.
     * <P>
     * The first line has index '1'.
     */
	protected int lineNumber = -1;

    /**
     * The startingColumn where the cursor sits now.
     * <P>
     * The first startingColumn has index '1'.
     */
	protected int columnNumber = -1;
    
	/**
	 * The additional offset required from a required attribute to
	 * place the cursor for the current proposal
	 */
	private int additionalProposalOffset = -1;
    
    private static final String ANT_DTD_FILENAME = "/ant1.6.0.dtd"; //$NON-NLS-1$

    /**
     * The dtd.
     */
	protected static ISchema dtd;

    /**
     * Cursor position, counted from the beginning of the document.
     * <P>
     * The first position has index '0'.
     */
	protected int cursorPosition = -1;
	
    /**
     * The text viewer.
     */
	private ITextViewer viewer;
	
	/**
	 * The set of characters that will trigger the activation of the
	 * completion proposal computation.
	 */
	private char[] autoActivationChars= null;
	
    /**
     * The provider for all task and attribute descriptions.
     */
	private TaskDescriptionProvider descriptionProvider = TaskDescriptionProvider.getDefault();
	
	private String errorMessage;
	
	protected AntModel antModel;
	
	/**
	 * The proposal mode for the current content assist
     * @see determineProposalMode(IDocument, int, String)
     */
	private int currentProposalMode= -1;
	
	/**
	 * The prefix for the current content assist
     */
	protected String currentPrefix= null;
	
	public AntEditorCompletionProcessor(AntModel model) {
		super();
		if(dtd == null) {
	        try {
	     	  	dtd = parseDtd();
	        } catch (IOException e) {
	        	AntUIPlugin.log(e);
	        } catch (ParseError e) {
				AntUIPlugin.log(e);
			}
		}
		antModel= model;
	}

    /**
     * Parses the dtd.
     */
    private ISchema parseDtd() throws ParseError, IOException {
        InputStream stream = getClass().getResourceAsStream(ANT_DTD_FILENAME);
        InputStreamReader reader = new InputStreamReader(stream, "UTF-8"); //$NON-NLS-1$
        Parser parser = new Parser();
        ISchema schema= parser.parseDTD(reader, "project"); //$NON-NLS-1$
        reader.close();
        return schema;
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer refViewer, int documentOffset) {
        this.viewer = refViewer;   
        ICompletionProposal[] matchingTemplateProposals = determineTemplateProposals(refViewer, documentOffset);
        ICompletionProposal[] merged= mergeProposals(determineProposals(), matchingTemplateProposals);
        currentPrefix= null;
        return merged;
    }
	
	private ICompletionProposal[] determineTemplateProposals(ITextViewer refViewer, int documentOffset) {
        String prefix = getCurrentPrefix();
        ICompletionProposal[] matchingTemplateProposals;
        if (prefix.length() == 0) {
            matchingTemplateProposals = super.computeCompletionProposals(refViewer, documentOffset);
        } else {
            ICompletionProposal[] templateProposals = super.computeCompletionProposals(refViewer, documentOffset);
            List templateProposalList = new ArrayList(templateProposals.length);
            for (int i = 0; i < templateProposals.length; i++) {
                if (templateProposals[i].getDisplayString().toLowerCase().startsWith(prefix)) {
                    templateProposalList.add(templateProposals[i]);
                }
            }
            matchingTemplateProposals = 
                (ICompletionProposal[]) templateProposalList.toArray(new ICompletionProposal[templateProposalList.size()]);
        }
		return matchingTemplateProposals;
	}

	private ICompletionProposal[] mergeProposals(ICompletionProposal[] proposals1, ICompletionProposal[] proposals2) {

        ICompletionProposal[] combinedProposals = new ICompletionProposal[proposals1.length + proposals2.length];
                
		System.arraycopy(proposals1, 0, combinedProposals, 0, proposals1.length);
		System.arraycopy(proposals2, 0, combinedProposals, proposals1.length, proposals2.length);		                

		Arrays.sort(combinedProposals, proposalComparator);
        return combinedProposals;
    }

    /**
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer refViewer, int documentOffset) {
		return new IContextInformation[0];
	}

	/**
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
        return autoActivationChars;
	}

	/**
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
        return null;
	}

	/**
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	/**
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
	
    /**
     * Returns the new determined proposals.
     */ 
	private ICompletionProposal[] determineProposals() {
		ITextSelection selection= (ITextSelection)viewer.getSelectionProvider().getSelection();
		cursorPosition = selection.getOffset() + selection.getLength();
        
        IDocument doc = viewer.getDocument();
        try {
            lineNumber = doc.getLineOfOffset(cursorPosition);
            columnNumber = cursorPosition - doc.getLineOffset(lineNumber);
        } catch (BadLocationException e) {
            AntUIPlugin.log(e);
        }
		
		String prefix = getCurrentPrefix();
		if (prefix == null || cursorPosition == -1) {
			AntUIPlugin.getStandardDisplay().beep();
			return NO_PROPOSALS;
		}
	
		ICompletionProposal[] proposals = getProposalsFromDocument(doc, prefix);
		return proposals;
		
	}

    /**
     * Returns the proposals for the specified document.
     */
    protected ICompletionProposal[] getProposalsFromDocument(IDocument document, String prefix) {
        String taskString = null;
		ICompletionProposal[] proposals= null;
		currentProposalMode= determineProposalMode(document, cursorPosition, prefix);
        switch (currentProposalMode) {
            case PROPOSAL_MODE_ATTRIBUTE_PROPOSAL:
                taskString = getTaskStringFromDocumentStringToPrefix(document.get().substring(0, cursorPosition-prefix.length()));
                proposals= getAttributeProposals(taskString, prefix);
                if (proposals.length == 0) {
                	errorMessage= AntEditorMessages.getString("AntEditorCompletionProcessor.28"); //$NON-NLS-1$
                }
                break;
            case PROPOSAL_MODE_TASK_PROPOSAL:
            	String parentName= getParentName(document, lineNumber, columnNumber);
            	if (parentName == null || parentName.length() == 0) { //outside of any parent element
            		 proposals= NO_PROPOSALS;
            		 currentProposalMode= PROPOSAL_MODE_NONE;
            	} else {
            		proposals= getTaskProposals(document, parentName, prefix);
            	}
            	if (proposals.length == 0) {
        			errorMessage= AntEditorMessages.getString("AntEditorCompletionProcessor.29"); //$NON-NLS-1$
        		}
				break;
            case PROPOSAL_MODE_BUILDFILE:
				proposals= getBuildFileProposals(document, prefix);
            	if (proposals.length == 0) {
				   errorMessage= AntEditorMessages.getString("AntEditorCompletionProcessor.29"); //$NON-NLS-1$
            	}
				break;
            case PROPOSAL_MODE_TASK_PROPOSAL_CLOSING:
                ICompletionProposal proposal= getClosingTaskProposal(getOpenElementName(), prefix, true);
            	if (proposal == null) {
				   errorMessage= AntEditorMessages.getString("AntEditorCompletionProcessor.30"); //$NON-NLS-1$
				   proposals= NO_PROPOSALS;
            	} else {
	            	proposals= new ICompletionProposal[]{proposal};
            	}
                break;
            case PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL:
            	String textToSearch= document.get().substring(0, cursorPosition-prefix.length());
                taskString = getTaskStringFromDocumentStringToPrefix(textToSearch);
                String attributeString = getAttributeStringFromDocumentStringToPrefix(textToSearch);
                if ("target".equalsIgnoreCase(taskString)) { //$NON-NLS-1$
                	proposals= getTargetAttributeValueProposals(document, textToSearch, prefix, attributeString);
                } else if ("refid".equalsIgnoreCase(attributeString)) { //$NON-NLS-1$
                	proposals= getReferencesValueProposals(prefix);

                } else {
                	proposals=getAttributeValueProposals(taskString, attributeString, prefix);
                }
				if (proposals.length == 0) {
				   errorMessage= AntEditorMessages.getString("AntEditorCompletionProcessor.31"); //$NON-NLS-1$
				}
				break;
            case PROPOSAL_MODE_PROPERTY_PROPOSAL:
				proposals= getPropertyProposals(document, prefix, cursorPosition);
            	if (proposals.length == 0) {
				   errorMessage= AntEditorMessages.getString("AntEditorCompletionProcessor.32"); //$NON-NLS-1$
            	}
				break;
			case PROPOSAL_MODE_NONE :
            default :
                proposals= NO_PROPOSALS;
				errorMessage= AntEditorMessages.getString("AntEditorCompletionProcessor.33"); //$NON-NLS-1$
        }
        
        if (proposals.length > 0) {
        	errorMessage= ""; //$NON-NLS-1$
        }
        return proposals;

    }
    
	private ICompletionProposal[] getReferencesValueProposals(String prefix) {
		Project project= antModel.getProjectNode().getProject();
		Map references= project.getReferences();
		if (references.isEmpty()) {
			return NO_PROPOSALS;
		}
		Set refIds= references.keySet();
		AntElementNode node= antModel.getNode(cursorPosition, false);
		while (node.getParentNode() instanceof AntTaskNode) {
			node= node.getParentNode();
		}
		String id= null;
		if (node instanceof AntTaskNode) {
			id= ((AntTaskNode)node).getId();
		}
		List proposals= new ArrayList(refIds.size());
		int i= 0;
		String refId;
		ICompletionProposal proposal;
		for (Iterator iter = refIds.iterator(); iter.hasNext(); i++) {
			refId= (String) iter.next();
			if (!refId.equals(id) && (prefix.length() == 0 || refId.toLowerCase().startsWith(prefix))) {
				proposal= new AntCompletionProposal(refId, cursorPosition - prefix.length(), prefix.length(), refId.length(), null, refId, null, AntCompletionProposal.TASK_PROPOSAL);
				proposals.add(proposal);
			}
		}
		return (ICompletionProposal[])proposals.toArray(new ICompletionProposal[proposals.size()]);      
	}

	protected ICompletionProposal[] getTargetAttributeValueProposals(IDocument document, String textToSearch, String prefix, String attributeName) {
		if (attributeName.equalsIgnoreCase("depends")) { //$NON-NLS-1$
			return getDependsValueProposals(document, prefix);
		} else if (attributeName.equalsIgnoreCase("if") || attributeName.equalsIgnoreCase("unless")) { //$NON-NLS-1$ //$NON-NLS-2$
			if (!textToSearch.trim().endsWith(",")) { //$NON-NLS-1$
				return getPropertyProposals(document, prefix, cursorPosition);
			}
		}
		
		return NO_PROPOSALS;
	}

	private ICompletionProposal[] getDependsValueProposals(IDocument document, String prefix) {
		List possibleDependencies = new ArrayList();
		String currentTargetName= getEnclosingTargetName(document, lineNumber, columnNumber);
		if(currentTargetName == null) {
			return NO_PROPOSALS;
		}
			
		Map targets= getTargets();
		Set targetNames= targets.keySet();
		Iterator itr= targetNames.iterator();
		Enumeration dependencies= null;
		while (itr.hasNext()) {
			String targetName = (String) itr.next();
			if (targetName.equals(currentTargetName)) {
				Target currentTarget= (Target)targets.get(targetName);
				dependencies= currentTarget.getDependencies();
				continue;
			}
			if (targetName.toLowerCase().startsWith(prefix) && targetName.length() > 0){
				possibleDependencies.add(targetName);
			}
		}
		
		if (dependencies != null) {
			while (dependencies.hasMoreElements()) {
				possibleDependencies.remove(dependencies.nextElement());
			}
		}
		
		ICompletionProposal[] proposals= new ICompletionProposal[possibleDependencies.size()];
		int i= 0;
		for (Iterator iter = possibleDependencies.iterator(); iter.hasNext(); i++) {
			String targetName = (String) iter.next();
			ICompletionProposal proposal = new AntCompletionProposal(targetName, cursorPosition - prefix.length(), prefix.length(), targetName.length(), null, targetName, null, AntCompletionProposal.TASK_PROPOSAL);
			proposals[i]= proposal;
		}
		return proposals;
	}

	/**
     * Returns all possible attributes for the specified task.
     * 
     * @param taskName the name of the task for that the attribute shall be 
     * completed
     * @param prefix prefix, that all proposals should start with. The prefix
     * may be an empty string.
     */
    protected ICompletionProposal[] getAttributeProposals(String taskName, String prefix) {
        List proposals = new ArrayList();
        IElement element = dtd.getElement(taskName);
        if (element != null) {
        	Iterator keys = element.getAttributes().keySet().iterator();
        	while (keys.hasNext()) {
        		String attrName = (String) keys.next();
        		if (prefix.length() == 0 || attrName.toLowerCase().startsWith(prefix)) {
        			IAttribute dtdAttributes = (IAttribute) element.getAttributes().get(attrName);
					String replacementString = attrName+"=\"\""; //$NON-NLS-1$
					String displayString = attrName;
					String[] items = dtdAttributes.getEnum();
					if (items != null) {					        			
                        if(items.length > 1) {
                            displayString += " - ("; //$NON-NLS-1$
                        }
                        for (int i = 0; i < items.length; i++) {
                            displayString += items[i];
                            if(i+1 < items.length) {
                                displayString += " | "; //$NON-NLS-1$
                            } else {
                                displayString += ")"; //$NON-NLS-1$
                            }
                        }
                    }
                    
                    addAttributeProposal(taskName, prefix, proposals, attrName, replacementString, displayString, true);
                }       
            }
        } else { //possibly a user defined task or type
        	Class taskClass= getTaskClass(taskName);
        	if (taskClass != null) {
        		if (taskClass == MacroInstance.class) {
        			addMacroDefProposals(taskName, prefix, proposals);
                	
        		} else {
	        		IntrospectionHelper helper= getIntrospectionHelper(taskClass);
	        		if (helper != null) {
		        		Enumeration attributes= helper.getAttributes();
			        	while (attributes.hasMoreElements()) {
							String attribute = (String) attributes.nextElement();
							if (prefix.length() == 0 || attribute.toLowerCase().startsWith(prefix)) {
								String replacementString = attribute + "=\"\""; //$NON-NLS-1$
								addAttributeProposal(taskName, prefix, proposals, attribute, replacementString, attribute, false);
							}
						}
	        		}
        		}
        	}
        }
        return (ICompletionProposal[])proposals.toArray(new ICompletionProposal[proposals.size()]);
    }
    
    private IntrospectionHelper getIntrospectionHelper(Class taskClass) {
    	IntrospectionHelper helper= null;
    	try {
    		helper= IntrospectionHelper.getHelper(antModel.getProjectNode().getProject(), taskClass);
    	} catch (NoClassDefFoundError e) {
			AntUIPlugin.log(MessageFormat.format(AntEditorMessages.getString("AntEditorCompletionProcessor.0"), new String[]{taskClass.getName()}), e); //$NON-NLS-1$
		}
    	return helper;
    }

    private void addMacroDefProposals(String taskName, String prefix, List proposals) {
		AntProjectNode projectNode= antModel.getProjectNode();
		AntDefiningTaskNode node= projectNode.getDefininingTaskNode(taskName);
		Object task= node.getRealTask();
		if (task instanceof MacroDef) {
			List attributes= ((MacroDef)task).getAttributes();
			Iterator itr= attributes.iterator();
			while (itr.hasNext()) {
				MacroDef.Attribute attribute = (MacroDef.Attribute) itr.next();
				String attributeName= attribute.getName();
				if (prefix.length() == 0 || attributeName.toLowerCase().startsWith(prefix)) {
					String replacementString = attributeName + "=\"\""; //$NON-NLS-1$
					String proposalInfo = null;
					
					String description = attribute.getDescription();
					if(description != null) {
					    proposalInfo= description;
					}
					String deflt = attribute.getDefault();
					if(deflt != null && deflt.length() > 0) {
						proposalInfo= (proposalInfo == null ?  "<BR><BR>" : (proposalInfo += "<BR><BR>")); //$NON-NLS-1$ //$NON-NLS-2$
						proposalInfo+= MessageFormat.format(AntEditorMessages.getString("AntEditorCompletionProcessor.59"), new String[]{deflt}); //$NON-NLS-1$
					}
					
					ICompletionProposal proposal = new AntCompletionProposal(replacementString, cursorPosition - prefix.length(), prefix.length(), attributeName.length() + 2, null, attributeName, proposalInfo, AntCompletionProposal.TASK_PROPOSAL);
					proposals.add(proposal);
				}
			}
		}
	}

	private void addAttributeProposal(String taskName, String prefix, List proposals, String attrName, String replacementString, String displayString, boolean lookupDescription) {
    	
		String proposalInfo = null;
		if (lookupDescription) {
			String required = descriptionProvider.getRequiredAttributeForTaskAttribute(taskName, attrName);
			if(required != null && required.length() > 0) {
			    proposalInfo = AntEditorMessages.getString("AntEditorCompletionProcessor.Required___4") + required; //$NON-NLS-1$
			    proposalInfo += "<BR><BR>"; //$NON-NLS-1$
			}
			String description = descriptionProvider.getDescriptionForTaskAttribute(taskName, attrName);
			if(description != null) {
			    proposalInfo = (proposalInfo == null ? "" : proposalInfo); //$NON-NLS-1$
			    proposalInfo += description;
			}
		}
		
		ICompletionProposal proposal = new AntCompletionProposal(replacementString, cursorPosition - prefix.length(), prefix.length(), attrName.length()+2, null, displayString, proposalInfo, AntCompletionProposal.TASK_PROPOSAL);
		proposals.add(proposal);
	}

	/**
     * Returns all possible values for the specified attribute of the specified 
     * task.
     * 
     * @param aTaskName the name of the task that the specified attribute 
     * belongs to.
     * 
     * @param anAttributeName the name of the attribute for that the value
     * shall be completed
     * 
     * @param prefix the prefix that all proposals should start with. The prefix
     * may be an empty string.
     */
    private ICompletionProposal[] getAttributeValueProposals(String taskName, String attributeName, String prefix) {
        List proposals = new ArrayList();
        IElement taskElement = dtd.getElement(taskName);
        if (taskElement != null) {
        	IAttribute attribute = (IAttribute) taskElement.getAttributes().get(attributeName);
        	if (attribute != null) {
        		String[] items = attribute.getEnum();
        		if (items != null) {
					String item;
                    for (int i = 0; i < items.length; i++) {
                        item= items[i];
                        if(prefix.length() ==0 || item.toLowerCase().startsWith(prefix)) { 
                            proposals.add(
                            	new AntCompletionProposal(item, cursorPosition - prefix.length(), prefix.length(), item.length(), null, item, null, AntCompletionProposal.TASK_PROPOSAL));
                        }
                    }
        		}
            }
        } else { //possibly a user defined task or type
        	Class taskClass= getTaskClass(taskName);
        	if (taskClass != null) {
        		IntrospectionHelper helper= getIntrospectionHelper(taskClass);
        		if (helper != null) {
	        		Enumeration attributes= helper.getAttributes();
		        	while (attributes.hasMoreElements()) {
						String attribute= (String) attributes.nextElement();
						if (attribute.equals(attributeName)) {
							Class attributeType= helper.getAttributeType(attribute);
							addAttributeValueProposalsForAttributeType(attributeType, prefix, proposals);
							break;
						}
					}
        		}
        	}
        }
        return (ICompletionProposal[])proposals.toArray(new ICompletionProposal[proposals.size()]);
    }

	private void addAttributeValueProposalsForAttributeType(Class attributeType, String prefix, List proposals) {
		if (attributeType == Boolean.TYPE && prefix.length() <= 5) {
			addBooleanAttributeValueProposals(prefix, proposals);
		} else if (EnumeratedAttribute.class.isAssignableFrom(attributeType)) {
			try {
				addEnumeratedAttributeValueProposals(attributeType, prefix, proposals);
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			}
		} else if (Reference.class == attributeType) {
			proposals.addAll(Arrays.asList(getReferencesValueProposals(prefix)));
		}
	}
	
	private void addEnumeratedAttributeValueProposals(Class type, String prefix, List proposals) throws InstantiationException, IllegalAccessException { 
		EnumeratedAttribute ea= (EnumeratedAttribute) type.newInstance();
		String[] values = ea.getValues();
		String enumerated;
		for (int i = 0; i < values.length; i++) {
			enumerated= values[i].toLowerCase();
			if (prefix.length() == 0 || enumerated.startsWith(prefix)) {
				proposals.add(new AntCompletionProposal(enumerated, cursorPosition - prefix.length(), prefix.length(), enumerated.length(), null, enumerated, null, AntCompletionProposal.TASK_PROPOSAL));
			}
		}
	}

	private void addBooleanAttributeValueProposals(String prefix, List proposals) {
		String[] booleanValues = new String[]{"true","false","on","off","yes","no"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		String booleanAssist;
		for (int i = 0; i < booleanValues.length; i++) {
			booleanAssist= booleanValues[i].toLowerCase();
			if (prefix.length() == 0 || booleanAssist.startsWith(prefix)) {
				proposals.add(new AntCompletionProposal(booleanAssist, cursorPosition -prefix.length(),
						prefix.length(), booleanAssist.length(), null, booleanAssist,
						null, AntCompletionProposal.TASK_PROPOSAL));
			}
		}	    
	}

	/**
     * Returns all possible properties for the specified prefix.
     * <P>
     * Note that the completion mode must be property mode, otherwise it is not
     * safe to call this method.
     */
    protected ICompletionProposal[] getPropertyProposals(IDocument document, String prefix, int aCursorPosition) {
        List proposals = new ArrayList();
        Map displayStringToProposals= new HashMap();
        Map properties = findPropertiesFromDocument(document);
		String propertyName;
		Image image = AntUIImages.getImage(IAntUIConstants.IMG_PROPERTY);
		// Determine replacement length and offset
	    // String from beginning to the beginning of the prefix
	   int replacementLength = prefix.length();
	   int replacementOffset = 0;
	   String text= document.get();
	   String stringToPrefix = text.substring(0, aCursorPosition - prefix.length());
	   // Property proposal
	   String lastTwoCharacters = stringToPrefix.substring(stringToPrefix.length()-2, stringToPrefix.length());
	   boolean appendBraces= true;
	   if(lastTwoCharacters.equals("${")) { //$NON-NLS-1$
		   replacementLength += 2;
		   replacementOffset = aCursorPosition - prefix.length() - 2;
	   } else if(lastTwoCharacters.endsWith("$")) { //$NON-NLS-1$
		   replacementLength += 1;
		   replacementOffset = aCursorPosition - prefix.length() - 1;                
	   } else {
			//support for property proposals for the if/unless attributes of targets
	   		replacementOffset= aCursorPosition - prefix.length();
	   		appendBraces= false;
	   }
	   
	   if(text.length() > aCursorPosition && text.charAt(aCursorPosition) == '}') {
		   replacementLength += 1;
	   }
        for(Iterator i=properties.keySet().iterator(); i.hasNext(); ) {
            propertyName= (String)i.next();
            if(prefix.length() == 0 || propertyName.toLowerCase().startsWith(prefix)) {
                String additionalPropertyInfo = (String)properties.get(propertyName);
                
                StringBuffer replacementString = new StringBuffer();
                if (appendBraces) {
                	replacementString.append("${"); //$NON-NLS-1$
                }
                replacementString.append(propertyName);
                if (appendBraces) {
                	replacementString.append('}');
                }
                
				if (displayStringToProposals.get(propertyName) == null) {
                	ICompletionProposal proposal = 
		                new AntCompletionProposal(
		                    replacementString.toString(), replacementOffset, replacementLength, 
		                    replacementString.length(), image, propertyName,
		                    additionalPropertyInfo, AntCompletionProposal.PROPERTY_PROPOSAL);
					proposals.add(proposal);
					displayStringToProposals.put(propertyName, proposal);
				}
            }
        }      
		return (ICompletionProposal[])proposals.toArray(new ICompletionProposal[proposals.size()]);          
    }


    /**
     * Returns all possible proposals for the specified parent name.
     * <P>
     * No completions will be returned if <code>parentName</code> is 
     * not known.
     * 
     * @param document the entire document 
     * @param parentName name of the parent (surrounding) element.
     * @param prefix the prefix that all proposals should start with. The prefix
     * may be an empty string.
     */
    protected ICompletionProposal[] getTaskProposals(IDocument document, String parentName, String prefix) {
       List proposals = new ArrayList(250);
       ICompletionProposal proposal;
       if (areTasksOrTypesValidChildren(parentName)) {
        	//use the definitions in the project as that includes more than what is defined in the DTD
			Project project= antModel.getProjectNode().getProject();
			Map tasksAndTypes= ComponentHelper.getComponentHelper(project).getAntTypeTable();
			createProposals(document, prefix, proposals, tasksAndTypes);
			if (parentName.equals("project") && "target".startsWith(prefix)) { //$NON-NLS-1$ //$NON-NLS-2$
				proposals.add(newCompletionProposal(document, prefix, "target")); //$NON-NLS-1$
			}
		} else {
			IElement parent = dtd.getElement(parentName);
			if (parent != null) {
				IDfm dfm = parent.getDfm();
				String[] accepts = dfm.getAccepts();
				String elementName;
				for (int i = 0; i < accepts.length; i++) {
					elementName = accepts[i];
					if(prefix.length() == 0 || elementName.toLowerCase().startsWith(prefix)) {
						proposal = newCompletionProposal(document, prefix, elementName);
						proposals.add(proposal);
					}
				}
			} else {
				//a nested element of a user defined task/type?
				Class taskClass= getTaskClass(parentName);
	        	if (taskClass != null) {
	        		IntrospectionHelper helper= getIntrospectionHelper(taskClass);
	        		if (helper != null) {
		        		Enumeration nested= helper.getNestedElements();
		        		String nestedElement;
			        	while (nested.hasMoreElements()) {
							nestedElement = (String) nested.nextElement();
							if (prefix.length() == 0 || nestedElement.toLowerCase().startsWith(prefix)) {
								proposal = newCompletionProposal(document, prefix, nestedElement);
								proposals.add(proposal);
							}
				        }
	        		}
	        	}
			}
        }
        
        proposal= getClosingTaskProposal(getOpenElementName(), prefix, false);
        if (proposal != null) {
        	proposals.add(proposal);
        }
        
        return (ICompletionProposal[])proposals.toArray(new ICompletionProposal[proposals.size()]);
   }
    
    private boolean areTasksOrTypesValidChildren(String parentName) {
		return parentName == "project" || parentName == "target" || parentName == "sequential" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			|| parentName == "presetdef" || parentName == "parallel" || parentName == "daemons"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/** 
    * Returns all possible proposals that define the structure of a buildfile.
    * 
    * @param document the entire document 
    * @param prefix the prefix that all proposals should start with. The prefix
    * may be an empty string.
    */
   protected ICompletionProposal[] getBuildFileProposals(IDocument document, String prefix) {
       String rootElementName= "project"; //$NON-NLS-1$
       IElement rootElement = dtd.getElement(rootElementName);
       if (rootElement != null && rootElementName.toLowerCase().startsWith(prefix)) {
       		ICompletionProposal proposal = newCompletionProposal(document, prefix, rootElementName);
			return new ICompletionProposal[] {proposal};
		} else {
			return NO_PROPOSALS;
		}
   } 

    private void createProposals(IDocument document, String prefix, List proposals, Map tasks) {
		Iterator keys= tasks.keySet().iterator();
		ICompletionProposal proposal;
		String key;
		while (keys.hasNext()) {
			key= (String) keys.next();
			if (prefix.length() == 0 || key.toLowerCase().startsWith(prefix)) {
				proposal = newCompletionProposal(document, prefix, key);
				proposals.add(proposal);
			}
		}
	}
    
    private ICompletionProposal newCompletionProposal(IDocument document, String aPrefix, String elementName) {
		additionalProposalOffset= 0;
		Image proposalImage = AntUIImages.getImage(IAntUIConstants.IMG_TASK_PROPOSAL);
		String proposalInfo = descriptionProvider.getDescriptionForTask(elementName);
		boolean hasNestedElements= hasNestedElements(elementName);
		String replacementString = getTaskProposalReplacementString(elementName, hasNestedElements);
		int replacementOffset = cursorPosition - aPrefix.length();
		int replacementLength = aPrefix.length();
		if (replacementOffset > 0 && document.get().charAt(replacementOffset - 1) == '<') {
			replacementOffset--;
			replacementLength++;
		}
		int proposalCursorPosition;
		if (hasNestedElements) {
			proposalCursorPosition= elementName.length() + 2 + additionalProposalOffset;
		} else {
			if (additionalProposalOffset > 0) {
				additionalProposalOffset+=2; //<antstructure output="|"/>
			} else {
				additionalProposalOffset+=1; //<arg|/>
			}
			proposalCursorPosition= elementName.length() + additionalProposalOffset;
		}
		return new AntCompletionProposal(replacementString, replacementOffset, 
			replacementLength, proposalCursorPosition, proposalImage, elementName, proposalInfo, AntCompletionProposal.TASK_PROPOSAL);
	}

	/**
     * Returns the one possible completion for the specified unclosed task .
     * 
     * @param openElementName the task that hasn't been closed 
     * last
     * @param prefix The prefix that the one possible proposal should start 
     * with. The prefix may be an empty string.
     * @return the proposal or <code>null</code> if no closing proposal available
     */
    private ICompletionProposal getClosingTaskProposal(String openElementName, String prefix, boolean closingMode) {
    	char previousChar = getPreviousChar();
		ICompletionProposal proposal= null;
        if(openElementName != null) {
            if(prefix.length() == 0 || openElementName.toLowerCase().startsWith(prefix)) {
                StringBuffer replaceString = new StringBuffer();
                if (!closingMode) {
                	if (previousChar != '/') {
	                	if (previousChar != '<') {
	                		replaceString.append('<');
	                	}
	                	replaceString.append('/');
                	}
                }
                replaceString.append(openElementName);
                replaceString.append('>');
                StringBuffer displayString= new StringBuffer("</"); //$NON-NLS-1$
                displayString.append(openElementName);
                displayString.append('>');
                proposal= new AntCompletionProposal(replaceString.toString(), cursorPosition - prefix.length(), prefix.length(), replaceString.length(), null, displayString.toString(), AntEditorMessages.getString("AntEditorCompletionProcessor.39"), AntCompletionProposal.TAG_CLOSING_PROPOSAL); //$NON-NLS-1$
    		}
        }

        return proposal;
    }

	protected char getPreviousChar() {
		ITextSelection selection = (ITextSelection)viewer.getSelectionProvider().getSelection();
    	int offset= selection.getOffset();
    	char previousChar= '?';
    	try {
			previousChar= viewer.getDocument().getChar(offset-1);
		} catch (BadLocationException e) {
			
		}
		return previousChar;
	}

    /**
     * Returns the replacement string for the specified task name.
     */
    private String getTaskProposalReplacementString(String aTaskName, boolean hasNested) {
        StringBuffer replacement = new StringBuffer("<"); //$NON-NLS-1$
        replacement.append(aTaskName); 
        Node attributeNode= descriptionProvider.getAttributesNode(aTaskName);
		
        if (attributeNode != null) {
			appendRequiredAttributes(replacement, attributeNode);
        } else if ("project".equals(aTaskName)){ //$NON-NLS-1$
        	replacement.append(" default=\"\""); //$NON-NLS-1$
			additionalProposalOffset= 9;
        }
        
        if (hasNested) {
        	replacement.append("></"); //$NON-NLS-1$
            replacement.append(aTaskName);
            replacement.append('>');
        } else {
        	replacement.append("/>"); //$NON-NLS-1$
        }
        return replacement.toString();               
    }

    private void appendRequiredAttributes(StringBuffer replacement, Node attributeNode) {
		boolean requiredAdded= false;
		NodeList attributes= attributeNode.getChildNodes();
		String required;
		Node attribute;
		for (int i = 0; i < attributes.getLength(); i++) {
			attribute = attributes.item(i);
			required= descriptionProvider.getRequiredOfNode(attribute);
			if (required.equalsIgnoreCase("yes")) { //$NON-NLS-1$
				String attributeName= descriptionProvider.getTaskAttributeName(attribute);
				replacement.append(' ');
				replacement.append(attributeName);
				replacement.append("=\"\""); //$NON-NLS-1$
				if (!requiredAdded){
					additionalProposalOffset= attributeName.length() + 2;
					requiredAdded= true;
				}	
			}
		}
	}

	/**
     * Returns whether the named element supports nested elements.
     */
    private boolean hasNestedElements(String elementName) {
        IElement element = dtd.getElement(elementName);
        if (element != null) {
        	return !element.isEmpty();
        } else {
        	Class taskClass= getTaskClass(elementName);
        	if (taskClass != null) {
        		IntrospectionHelper helper= getIntrospectionHelper(taskClass);
        		if (helper != null) {
        			Enumeration nested= helper.getNestedElements();
        			return nested.hasMoreElements();
        		}
    		}
        }
        return false;
    }
    
    /**
     * Finds a direct child element with <code>aChildElementName</code> of 
     * <code>anElement</code>.
     * <P>
     * The child will not be searched for in the whole hierarchy but only in
     * the hierarchy step below.
     * 
     * @return the found child or <code>null</code> if not found.
     */
    protected Element findChildElementNamedOf(Element anElement, String aChildElementName) {
        NodeList nodeList = anElement.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(i);
            if(childNode.getNodeType() == Node.ELEMENT_NODE) {
                if(childNode.getNodeName().equals(aChildElementName)) {
                    return (Element)childNode;
                }
            }   
        }
        return null;
    }

	/**
     * Determines the current prefix that should be used for completion.
     */
	private String getCurrentPrefix() {
		if (currentPrefix != null) {
        	return currentPrefix;
        }
		ITextSelection selection = (ITextSelection)viewer.getSelectionProvider().getSelection();
		IDocument doc = viewer.getDocument();
        return getPrefixFromDocument(doc.get(), selection.getOffset() + selection.getLength()).toLowerCase();
	}


    /**
     * Returns the prefix in the specified document text with respect to the 
     * specified offset.
     * 
     * @param aDocumentText the whole content of the edited file as String
     * @param anOffset the cursor position
     */
    protected String getPrefixFromDocument(String aDocumentText, int anOffset) {
        if (currentPrefix != null) {
        	return currentPrefix;
        }
        int startOfWordToken = anOffset;
        
        char token= 'a';
        if (startOfWordToken > 0) {
			token= aDocumentText.charAt(startOfWordToken - 1);
        }
        
        while (startOfWordToken > 0 
                && (Character.isJavaIdentifierPart(token) 
                    || '.' == token
					|| '-' == token
        			|| ';' == token)
                && !('$' == token)) {
            startOfWordToken--;
            if (startOfWordToken == 0) {
            	break; //word goes right to the beginning of the doc
            }
			token= aDocumentText.charAt(startOfWordToken - 1);
        }
        
        if (startOfWordToken != anOffset) {
            currentPrefix= aDocumentText.substring(startOfWordToken, anOffset).toLowerCase();
        } else {
            currentPrefix= ""; //$NON-NLS-1$
        }
        return currentPrefix;
    }
 
 
    /**
     * Returns the current proposal mode.
     */
    protected int determineProposalMode(IDocument document, int aCursorPosition, String aPrefix) {

    	if (document.getLength() == 0) {
    		return PROPOSAL_MODE_BUILDFILE;
    	}
    	
        // String from beginning of document to the beginning of the prefix
    	String text= document.get();
        String stringToPrefix = text.substring(0, aCursorPosition - aPrefix.length());
        if (stringToPrefix.length() == 0) {
        	return PROPOSAL_MODE_BUILDFILE;
        }
        
        // Is trimmable from behind
        String trimmedString = stringToPrefix.trim();
        char lastChar = 0;
        if(trimmedString.length() > 0) {
	        lastChar = trimmedString.charAt(trimmedString.length()-1);
        } else {
        	return PROPOSAL_MODE_TASK_PROPOSAL;
        }
        if(stringToPrefix.charAt(stringToPrefix.length()-1) != lastChar && lastChar != '>' && lastChar != ',') {
            /*
             * Substring must be trimmable from behind in case of attribute 
             * proposal because a space or a new line must be used as delimiter 
             * between task name and attribute or attribute and attribute.
             * Example: '<property id="bla" name="hups"'
             */
             
            // Attribute proposal
            if(lastChar != '>' && lastChar != '<') {
                String taskString= getTaskStringFromDocumentStringToPrefix(trimmedString);
                if(taskString != null && isKnownElement(taskString)) {
                    return PROPOSAL_MODE_ATTRIBUTE_PROPOSAL;
                }
            }                
        } else if(stringToPrefix.charAt(stringToPrefix.length()-1) == '"' || trimmedString.charAt(trimmedString.length()-1) == ',') {
			// Attribute value proposal
            String taskString= getTaskStringFromDocumentStringToPrefix(trimmedString);
            if (taskString != null && isKnownElement(taskString)) {
                return PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL;
            }
        } else {  // Possibly a Task proposal
            int spaceIndex = stringToPrefix.lastIndexOf(' ');
            int lessThanIndex = stringToPrefix.lastIndexOf('<');
            int greaterThanIndex = stringToPrefix.lastIndexOf('>');
            
            // Task proposal
            if(lessThanIndex > spaceIndex && greaterThanIndex < lessThanIndex) {
                int slashIndex = stringToPrefix.lastIndexOf('/');
                if(slashIndex == lessThanIndex +1) {
                    return PROPOSAL_MODE_TASK_PROPOSAL_CLOSING; // ... </
                }
                return PROPOSAL_MODE_TASK_PROPOSAL;
            }
            if(lessThanIndex < greaterThanIndex) {
            	if (isPropertyProposalMode(stringToPrefix)) {
				   return PROPOSAL_MODE_PROPERTY_PROPOSAL;
			   }
               return PROPOSAL_MODE_TASK_PROPOSAL;
            }
        }

        // Property proposal
		if (isPropertyProposalMode(stringToPrefix)) {
			return PROPOSAL_MODE_PROPERTY_PROPOSAL;
        }
        	            
        return PROPOSAL_MODE_NONE;
    }


	private boolean isPropertyProposalMode(String stringToPrefix) {
		if(stringToPrefix.length() >= 2) {
			String lastTwoChars = stringToPrefix.substring(stringToPrefix.length()-2, stringToPrefix.length());
			if(lastTwoChars.equals("${") || //$NON-NLS-1$
				stringToPrefix.charAt(stringToPrefix.length()-1) == '$') {
					return true;
			}
		}
		return false;
	}
    /**
     * Returns the last occuring task string in the specified string.
     * <P>
     * The returned string must not necessarily be a valid Ant task string.
     * This can be tested with the method <code>inNamedTaskKnown(String)</code>
     * after invoking this method.
     * 
     * @param aDocumentStringToPrefix the String that contains the whole string
     * of the currently edited file from the beginning up to the prefix for code
     * completion. Example: '<project default="name"><property '.
     * 
     * @return the extracted task string or <code>null</code> if no string could
     * be extracted.
     */
    private String getTaskStringFromDocumentStringToPrefix(String aDocumentStringToPrefix) {
            
        int lessThanIndex = aDocumentStringToPrefix.lastIndexOf('<');

        if(lessThanIndex > -1) {
            String taskString = aDocumentStringToPrefix.trim();
            taskString = taskString.substring(lessThanIndex+1, taskString.length());
            int index = taskString.indexOf(' ');
            if(index > 0) {
                taskString = taskString.substring(0, index);
            }
            index = taskString.indexOf('\n');
            if(index > 0) {
                taskString = taskString.substring(0, index);
            }
            index = taskString.indexOf('\r');
            if(index > 0) {
                taskString = taskString.substring(0, index);
            }
            return taskString;
        }
        
        return null;
    }
    

    /**
     * Returns the last occuring attribute string in the specified string.
     * <P>
     * Calling this method is only safe if the current proposal mode is really
     * <code>PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL</code>.
     */
    private String getAttributeStringFromDocumentStringToPrefix(String docStringToPrefix) {
        int index = docStringToPrefix.lastIndexOf('=');
        String subString = docStringToPrefix.substring(0, index);
        subString = subString.trim();
        
        index = subString.lastIndexOf(' ');
        if(index > 0) {
            subString = subString.substring(index+1, subString.length());
        }
        index = subString.lastIndexOf('\n');
        if(index > 0) {
            subString = subString.substring(index+1, subString.length());
        }
        index = subString.lastIndexOf('\r');
        if(index > 0) {
            subString = subString.substring(index+1, subString.length());
        }
        return subString;
    }


    /**
     * Returns whether the specified element name is known
     */
    protected boolean isKnownElement(String elementName) {
    	if (elementName.equals("target") || elementName.equals("project")) { //$NON-NLS-1$ //$NON-NLS-2$
    		return true;
    	} else {
    		AntProjectNode node= antModel.getProjectNode();
        	if (node != null) {
        		Project antProject= node.getProject();
        		return ComponentHelper.getComponentHelper(antProject).getDefinition(elementName) != null; 
        	}
    	}
        return false;
    }

    private Class getTaskClass(String taskName) {
    	AntProjectNode node= antModel.getProjectNode();
    	if (node != null) {
    		Project antProject= node.getProject();
    		Map tasksAndTypes= ComponentHelper.getComponentHelper(antProject).getAntTypeTable();
    		return (Class)tasksAndTypes.get(taskName);
    	}
    	return null;
    }

    /**
     * Finds the parent task element in respect to the cursor position.
     * 
     * @return the parent task element or <code>null</code> if not found.
     */
    protected String getParentName(IDocument document, int aLineNumber, int aColumnNumber) {
    	if (document.getLength() == 0) {
    		return null;
    	}
    	AntProjectNode project= antModel.getProjectNode();
    	if (project == null) {
    		return null;
    	}
    	int offset= getOffset(document, aLineNumber, aColumnNumber);
    	if (offset == -1) {
    		return null;
    	}
    	AntElementNode node= project.getNode(offset);
    	if (node == null) {
    		node= antModel.getOpenElement();
    	}
    	if (node == null) {
    		return ""; //$NON-NLS-1$
    	} else if (node instanceof AntTaskNode) {
    		String name= node.getName();
    		if (offset <= node.getOffset() + name.length() - 1) {
    			//not really the enclosing node as the offset is within the name of the node
    			node= node.getParentNode();
    		} else {
    			return name;
    		}
    	}
    	if (node instanceof AntTaskNode) {
    		return node.getName();
    	} else if (node instanceof AntTargetNode) {
    		return "target"; //$NON-NLS-1$
    	} else {
    		return "project"; //$NON-NLS-1$
    	}
    }

    /**
     * Parses the actually edited file as far as possible.
     * <P>
     * We use the parsing facilities of the ant plug-in here.
     * 
     * @return a map with all the found properties
     */
    private Map findPropertiesFromDocument(IDocument document) {
		/*
		 * What is implemented here:
		 * - Retrieve the project from the Ant model
		 * - Determine the enclosing target
		 * - Determine the dependency Vector for the target
		 * - Work through the dependency Vector and execute the
		 *   Property relevant tasks.
		 */

    	Project project= antModel.getProjectNode().getProject();
        
        // Determine the parent
    	String targetName = getEnclosingTargetName(document, lineNumber, columnNumber);
         
        if(targetName == null) {
        	return project.getProperties();
        }
        List sortedTargets = null;
        try {
        	sortedTargets= project.topoSort(targetName, project.getTargets());
        } catch (BuildException be) {
			return project.getProperties();
        }

        int curidx = 0;
        Target curtarget;

        do {
            curtarget = (Target) sortedTargets.get(curidx++);
			Task[] tasks = curtarget.getTasks();
			
			for (int i = 0; i < tasks.length; i++) {
				Task task = tasks[i];                

				if (task instanceof UnknownElement) {
					try {
						task.maybeConfigure();
					} catch (BuildException be) {
						continue;
					}
					task= ((UnknownElement)task).getTask();
				}
				
				// sequential
				if(task instanceof Sequential) {
					// (T)
				}
				
				// parallel
				if(task instanceof Parallel) {
					// (T)
				}
				
				// waitfor (@since Ant 1.5)
	//			if(tempTask instanceof WaitFor) { 
	//				// (T)
	//			}
				
				if(task instanceof Property 
					|| task instanceof PathConvert
					|| task instanceof Available
					|| task instanceof UpToDate
					|| task instanceof Condition) {
					try {
						task.perform();
					} catch (BuildException be) {
						
					}
				}
			
			
				// Ant 1.5
//				if(tempTask instanceof LoadFile) {
//				
//				}
			
				// Ant 1.5
//				if(tempTask instanceof XmlProperty) {
//				
//				}
			
				// Ant 1.5
//				if(tempTask instanceof Basename) {
//				
//				}
			
				
				// Ant 1.5
//				if(tempTask instanceof Dirname) {
//				
//				}
			
				// Ant 1.5
//				if(tempTask instanceof LoadProperties) {
//				
//				}
            }
        } while (!curtarget.getName().equals(targetName));

        return project.getProperties();
    }
    
    private Map getTargets() {
    	Project project = antModel.getProjectNode().getProject();  
    	return project.getTargets();
    }

    protected File getEditedFile() {
    	IWorkbenchPage page= AntUIPlugin.getActivePage();
    	if (page == null) {
    		return null;
    	}
		IEditorPart editor= page.getActiveEditor();
		if (editor == null) {
			return null;
		}
        FileEditorInput input = (FileEditorInput) editor.getEditorInput();
        String projectPath = input.getFile().getProject().getLocation().toFile().getAbsolutePath();
        String  projectRelativeFilePath = input.getFile().getFullPath().removeFirstSegments(1).makeRelative().toString();
        return new File(projectPath + File.separator + projectRelativeFilePath);
    }
    
    private String getOpenElementName() {
    	AntElementNode node= antModel.getOpenElement();
    	if (node == null) {
    		return null;
    	}
    	return node.getName();
    }

    /**
     * Finds the enclosing target in respect to the cursor position and returns its name  
     * 
     * @return the name of the enclosing target or <code>null</code> if not found 
	 * or the element is not contained in a target.
     */
 	private String getEnclosingTargetName(IDocument document, int aLineNumber, int aColumnNumber) {

       AntProjectNode project= antModel.getProjectNode();
       int offset= getOffset(document, aLineNumber, aColumnNumber);
       if(offset == -1) {
       		return null;
       }
       AntElementNode node= project.getNode(offset);
       if (node instanceof AntTaskNode) {
       		node= node.getParentNode();
       		if (!(node instanceof AntTargetNode)) {
       			//top level task
       			node= null;
       		}
       } else if (node instanceof AntProjectNode) {
       		node= null;
       }
       String targetName = null;
       if(node == null 
       		|| (targetName = ((AntTargetNode)node).getTarget().getName()) == null
			|| targetName.length() == 0) {
       		return null;
       }
       return targetName;
 	}
 	
 	private int getOffset(IDocument document, int line, int column) {
 		try {
			return document.getLineOffset(line ) + column - 1;
		} catch (BadLocationException e) {
			return -1;
		}
 	}
	
	/**
	 * Sets this processor's set of characters triggering the activation of the
	 * completion proposal computation.
	 * 
	 * @param activationSet the activation set
	 */
	public void setCompletionProposalAutoActivationCharacters(char[] activationSet) {
		autoActivationChars= activationSet;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#extractPrefix(org.eclipse.jface.text.ITextViewer, int)
	 */
	protected String extractPrefix(ITextViewer textViewer, int offset) {
		return getPrefixFromDocument(textViewer.getDocument().get(), offset);
	}

	/**
	 * Cut out angular brackets for relevance sorting, since the template name
	 * does not contain the brackets.
	 */
	protected int getRelevance(Template template, String prefix) {
		if (prefix.startsWith("<")) //$NON-NLS-1$
			prefix= prefix.substring(1);
		if (template.getName().startsWith(prefix))
			return 90; 
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getTemplates(java.lang.String)
	 */
	protected Template[] getTemplates(String contextTypeId) {
		return AntTemplateAccess.getDefault().getTemplateStore().getTemplates();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getContextType(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 */
	protected TemplateContextType getContextType(ITextViewer textViewer, IRegion region) {
		 switch (currentProposalMode) {
            case PROPOSAL_MODE_TASK_PROPOSAL:
            	return AntTemplateAccess.getDefault().getContextTypeRegistry().getContextType(TaskContextType.TASK_CONTEXT_TYPE);
            case PROPOSAL_MODE_BUILDFILE:
            	return AntTemplateAccess.getDefault().getContextTypeRegistry().getContextType(BuildFileContextType.BUILDFILE_CONTEXT_TYPE);
			case PROPOSAL_MODE_NONE :
            case PROPOSAL_MODE_ATTRIBUTE_PROPOSAL:
            case PROPOSAL_MODE_TASK_PROPOSAL_CLOSING:
            case PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL:
            case PROPOSAL_MODE_PROPERTY_PROPOSAL:
            default :
            	return null;
        }
	}

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getImage(org.eclipse.jface.text.templates.Template)
     */
    protected Image getImage(Template template) {
        return AntUIImages.getImage(IAntUIConstants.IMG_TEMPLATE_PROPOSAL);
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#createContext(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 */
//	protected TemplateContext createContext(ITextViewer contextViewer, IRegion region) {
//		ContextType contextType= getContextType(contextViewer, region);
//		if (contextType != null) {
//			IDocument document= contextViewer.getDocument();
//			return new AntContext(contextType, document, antModel, region.getOffset(), region.getLength());
//		}
//		return null;
//	}
    
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#createProposal(org.eclipse.jface.text.templates.Template, org.eclipse.jface.text.templates.TemplateContext, org.eclipse.jface.text.Region, int)
	 */
	protected ICompletionProposal createProposal(Template template,TemplateContext context, Region region, int relevance) {
		return new AntTemplateProposal(template, context, region, getImage(template), relevance);
	}
}