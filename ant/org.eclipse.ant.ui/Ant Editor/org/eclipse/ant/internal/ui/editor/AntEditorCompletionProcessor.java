/*******************************************************************************
 * Copyright (c) 2002, 2021 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH,
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug fixes
 *     John-Mason P. Shackelford (john-mason.shackelford@pearson.com) - bug 49383, 56299, 59024
 *     Brock Janiczak (brockj_eclipse@ihug.com.au ) - bug 78028, 78030
 *     Remy Chi Jian Suen - bug 277587
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.ExtensionPoint;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.MacroDef;
import org.apache.tools.ant.taskdefs.MacroDef.Attribute;
import org.apache.tools.ant.taskdefs.MacroDef.TemplateElement;
import org.apache.tools.ant.taskdefs.MacroInstance;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Reference;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.dtd.IAttribute;
import org.eclipse.ant.internal.ui.dtd.IDfm;
import org.eclipse.ant.internal.ui.dtd.IElement;
import org.eclipse.ant.internal.ui.dtd.ISchema;
import org.eclipse.ant.internal.ui.dtd.ParseError;
import org.eclipse.ant.internal.ui.dtd.Parser;
import org.eclipse.ant.internal.ui.editor.TaskDescriptionProvider.ProposalNode;
import org.eclipse.ant.internal.ui.editor.templates.AntContext;
import org.eclipse.ant.internal.ui.editor.templates.AntTemplateAccess;
import org.eclipse.ant.internal.ui.editor.templates.AntTemplateInformationControlCreator;
import org.eclipse.ant.internal.ui.editor.templates.AntTemplateProposal;
import org.eclipse.ant.internal.ui.editor.templates.BuildFileContextType;
import org.eclipse.ant.internal.ui.editor.templates.TargetContextType;
import org.eclipse.ant.internal.ui.editor.templates.TaskContextType;
import org.eclipse.ant.internal.ui.model.AntDefiningTaskNode;
import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.model.AntTargetNode;
import org.eclipse.ant.internal.ui.model.AntTaskNode;
import org.eclipse.ant.internal.ui.model.IAntElement;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The completion processor for the Ant Editor.
 */
public class AntEditorCompletionProcessor extends TemplateCompletionProcessor implements ICompletionListener {

	private final Comparator<ICompletionProposal> proposalComparator = new Comparator<>() {
		@Override
		public int compare(ICompletionProposal o1, ICompletionProposal o2) {

			int type1 = getProposalType(o1);
			int type2 = getProposalType(o2);
			if (type1 != type2) {
				if (type1 > type2) {
					return 1;
				}
				return -1;
			}
			String string1 = o1.getDisplayString();
			String string2 = o2.getDisplayString();
			return string1.compareToIgnoreCase(string2);
		}

		private int getProposalType(Object o) {
			if (o instanceof AntCompletionProposal) {
				return ((AntCompletionProposal) o).getType();
			}
			return AntCompletionProposal.TASK_PROPOSAL;
		}
	};

	protected final static int PROPOSAL_MODE_NONE = 0;
	protected final static int PROPOSAL_MODE_BUILDFILE = 1;
	protected final static int PROPOSAL_MODE_TASK_PROPOSAL = 2;
	protected final static int PROPOSAL_MODE_PROPERTY_PROPOSAL = 3;
	protected final static int PROPOSAL_MODE_ATTRIBUTE_PROPOSAL = 4;
	protected final static int PROPOSAL_MODE_TASK_PROPOSAL_CLOSING = 5;
	protected final static int PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL = 6;
	protected final static int PROPOSAL_MODE_NESTED_ELEMENT_PROPOSAL = 7;

	private final static ICompletionProposal[] NO_PROPOSALS = new ICompletionProposal[0];

	/**
	 * The fully qualified name of the {@link MacroInstance} class
	 *
	 * @since 3.5.500
	 */
	private static final String MACROINSTANCE_NAME = "org.apache.tools.ant.taskdefs.MacroInstance"; //$NON-NLS-1$

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
	 * The additional offset required from a required attribute to place the cursor for the current proposal
	 */
	private int additionalProposalOffset = -1;

	private static final String ANT_DTD_FILENAME = "/org/eclipse/ant/internal/ui/editor/ant1.6.2.dtd"; //$NON-NLS-1$

	/**
	 * The DTD.
	 */
	private static ISchema fgDtd;

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
	 * The set of characters that will trigger the activation of the completion proposal computation.
	 */
	private char[] autoActivationChars = null;

	private String errorMessage;

	protected AntModel antModel;

	/**
	 * The proposal mode for the current content assist
	 *
	 * @see #determineProposalMode(IDocument, int, String)
	 */
	private int currentProposalMode = -1;

	/**
	 * The prefix for the current content assist
	 */
	protected String currentPrefix = null;

	/**
	 * The current task string for content assist
	 *
	 * @see #determineProposalMode(IDocument, int, String)
	 */
	protected String currentTaskString = null;

	private boolean fTemplatesOnly = false;
	protected IContentAssistantExtension2 fContentAssistant;

	public AntEditorCompletionProcessor(AntModel model) {
		super();
		antModel = model;
	}

	/**
	 * Parses the dtd.
	 */
	private ISchema parseDtd() throws ParseError, IOException {
		try (InputStream stream = getClass().getResourceAsStream(ANT_DTD_FILENAME); Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			Parser parser = new Parser();
			ISchema schema = parser.parseDTD(reader, "project"); //$NON-NLS-1$
			return schema;
		}
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer refViewer, int documentOffset) {
		this.viewer = refViewer;
		try {
			if (fTemplatesOnly) {
				fContentAssistant.setStatusMessage(getIterationGestureMessage(AntEditorMessages.getString("AntEditorCompletionProcessor.0"))); //$NON-NLS-1$
				fContentAssistant.setEmptyMessage(AntEditorMessages.getString("AntEditorCompletionProcessor.60")); //$NON-NLS-1$
				ICompletionProposal[] templates = determineTemplateProposals(refViewer, documentOffset);
				Arrays.sort(templates, proposalComparator);
				return templates;
			}
			fContentAssistant.setStatusMessage(getIterationGestureMessage(AntEditorMessages.getString("AntEditorCompletionProcessor.61"))); //$NON-NLS-1$
			fContentAssistant.setEmptyMessage(AntEditorMessages.getString("AntEditorCompletionProcessor.62")); //$NON-NLS-1$
			ICompletionProposal[] matchingProposals = determineProposals();
			ICompletionProposal[] matchingTemplateProposals = determineTemplateProposals(refViewer, documentOffset);
			return mergeProposals(matchingProposals, matchingTemplateProposals);
		}
		finally {
			currentPrefix = null;
			currentProposalMode = -1;
			fTemplatesOnly = !fTemplatesOnly;
		}
	}

	protected ICompletionProposal[] determineTemplateProposals(ITextViewer refViewer, int documentOffset) {
		this.viewer = refViewer;
		String prefix = getCurrentPrefix();
		ICompletionProposal[] matchingTemplateProposals;
		if (prefix.length() == 0) {
			matchingTemplateProposals = determineTemplateProposalsForContext(documentOffset);
		} else {
			ICompletionProposal[] templateProposals = determineTemplateProposalsForContext(documentOffset);
			List<ICompletionProposal> templateProposalList = new ArrayList<>(templateProposals.length);
			for (ICompletionProposal templateProposal : templateProposals) {
				if (templateProposal.getDisplayString().toLowerCase().startsWith(prefix)) {
					templateProposalList.add(templateProposal);
				}
			}
			matchingTemplateProposals = templateProposalList.toArray(new ICompletionProposal[templateProposalList.size()]);
		}
		return matchingTemplateProposals;
	}

	// essentially a copy of super.computeCompletionProposals but we need to have both context types work
	// for target (task and target) context type in a backwards compatible way
	private ICompletionProposal[] determineTemplateProposalsForContext(int offset) {
		ITextSelection selection = (ITextSelection) viewer.getSelectionProvider().getSelection();

		// adjust offset to end of normalized selection
		int newoffset = offset;
		if (selection.getOffset() == newoffset) {
			newoffset = selection.getOffset() + selection.getLength();
		}

		String prefix = extractPrefix(viewer, newoffset);
		Region region = new Region(newoffset - prefix.length(), prefix.length());
		TemplateContext context = createContext(viewer, region);
		if (context == null) {
			return new ICompletionProposal[0];
		}

		context.setVariable("selection", selection.getText()); // name of the selection variables {line, word}_selection //$NON-NLS-1$

		Template[] templates;
		String contextTypeId = context.getContextType().getId();
		boolean isTargetContextType = contextTypeId.equals(TargetContextType.TARGET_CONTEXT_TYPE);
		if (isTargetContextType) {
			Template[] tasks = AntTemplateAccess.getDefault().getTemplateStore().getTemplates(TaskContextType.TASK_CONTEXT_TYPE);
			Template[] targets = getTemplates(contextTypeId);
			templates = new Template[tasks.length + targets.length];
			System.arraycopy(tasks, 0, templates, 0, tasks.length);
			System.arraycopy(targets, 0, templates, tasks.length, targets.length);
		} else {
			templates = getTemplates(contextTypeId);
		}

		List<ICompletionProposal> matches = new ArrayList<>();
		for (Template template : templates) {
			try {
				context.getContextType().validate(template.getPattern());
			}
			catch (TemplateException e) {
				continue;
			}
			if (template.matches(prefix, contextTypeId) || (isTargetContextType && template.matches(prefix, TaskContextType.TASK_CONTEXT_TYPE))) {
				matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));
			}
		}

		Collections.sort(matches, proposalComparator);

		return matches.toArray(new ICompletionProposal[matches.size()]);
	}

	private ICompletionProposal[] mergeProposals(ICompletionProposal[] proposals1, ICompletionProposal[] proposals2) {

		ICompletionProposal[] combinedProposals = new ICompletionProposal[proposals1.length + proposals2.length];

		System.arraycopy(proposals1, 0, combinedProposals, 0, proposals1.length);
		System.arraycopy(proposals2, 0, combinedProposals, proposals1.length, proposals2.length);

		Arrays.sort(combinedProposals, proposalComparator);
		return combinedProposals;
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer refViewer, int documentOffset) {
		return new IContextInformation[0];
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return autoActivationChars;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Returns the new determined proposals.
	 */
	private ICompletionProposal[] determineProposals() {
		ITextSelection selection = (ITextSelection) viewer.getSelectionProvider().getSelection();
		cursorPosition = selection.getOffset() + selection.getLength();

		IDocument doc = viewer.getDocument();
		try {
			lineNumber = doc.getLineOfOffset(cursorPosition);
			columnNumber = cursorPosition - doc.getLineOffset(lineNumber);
		}
		catch (BadLocationException e) {
			AntUIPlugin.log(e);
		}

		String prefix = getCurrentPrefix();
		if (prefix == null || cursorPosition == -1) {
			return NO_PROPOSALS;
		}

		ICompletionProposal[] proposals = getProposalsFromDocument(doc, prefix);
		currentTaskString = null;
		return proposals;
	}

	/**
	 * Returns the proposals for the specified document.
	 */
	protected ICompletionProposal[] getProposalsFromDocument(IDocument document, String prefix) {
		ICompletionProposal[] proposals = null;
		currentProposalMode = determineProposalMode(document, cursorPosition, prefix);
		switch (currentProposalMode) {
			case PROPOSAL_MODE_ATTRIBUTE_PROPOSAL:
				proposals = getAttributeProposals(currentTaskString, prefix);
				if (proposals.length == 0) {
					errorMessage = AntEditorMessages.getString("AntEditorCompletionProcessor.28"); //$NON-NLS-1$
				}
				break;
			case PROPOSAL_MODE_TASK_PROPOSAL:
				String parentName = getParentName(document, lineNumber, columnNumber);
				if (parentName == null || parentName.length() == 0) { // outside of any parent element
					proposals = NO_PROPOSALS;
					currentProposalMode = PROPOSAL_MODE_NONE;
				} else {
					proposals = getTaskProposals(document, parentName, prefix);
				}
				if (proposals.length == 0) {
					errorMessage = AntEditorMessages.getString("AntEditorCompletionProcessor.29"); //$NON-NLS-1$
				}
				break;
			case PROPOSAL_MODE_BUILDFILE:
				proposals = getBuildFileProposals(document, prefix);
				if (proposals.length == 0) {
					errorMessage = AntEditorMessages.getString("AntEditorCompletionProcessor.29"); //$NON-NLS-1$
				}
				break;
			case PROPOSAL_MODE_TASK_PROPOSAL_CLOSING:
				ICompletionProposal proposal = getClosingTaskProposal(getOpenElementName(), prefix, true);
				if (proposal == null) {
					errorMessage = AntEditorMessages.getString("AntEditorCompletionProcessor.30"); //$NON-NLS-1$
					proposals = NO_PROPOSALS;
				} else {
					proposals = new ICompletionProposal[] { proposal };
				}
				break;
			case PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL:
				String textToSearch = document.get().substring(0, cursorPosition - prefix.length());
				String attributeString = getAttributeStringFromDocumentStringToPrefix(textToSearch);
				if ("target".equalsIgnoreCase(currentTaskString) || "extension-point".equalsIgnoreCase(currentTaskString)) { //$NON-NLS-1$ //$NON-NLS-2$
					proposals = getTargetAttributeValueProposals(document, textToSearch, prefix, attributeString);
				} else if ("antcall".equalsIgnoreCase(currentTaskString)) { //$NON-NLS-1$
					proposals = getAntCallAttributeValueProposals(document, prefix, attributeString);
				} else if ("project".equalsIgnoreCase(currentTaskString)) { //$NON-NLS-1$
					proposals = getProjectAttributeValueProposals(prefix, attributeString);
				} else if ("refid".equalsIgnoreCase(attributeString) || "classpathref".equalsIgnoreCase(attributeString) //$NON-NLS-1$//$NON-NLS-2$
						|| "sourcepathref".equalsIgnoreCase(attributeString) || "bootpathref".equalsIgnoreCase(attributeString)) { //$NON-NLS-1$ //$NON-NLS-2$
					proposals = getReferencesValueProposals(prefix);
				} else {
					proposals = getAttributeValueProposals(currentTaskString, attributeString, prefix);
				}
				if (proposals.length == 0) {
					errorMessage = AntEditorMessages.getString("AntEditorCompletionProcessor.31"); //$NON-NLS-1$
				}
				break;
			case PROPOSAL_MODE_PROPERTY_PROPOSAL:
				proposals = getPropertyProposals(document, prefix, cursorPosition);
				if (proposals.length == 0) {
					errorMessage = AntEditorMessages.getString("AntEditorCompletionProcessor.32"); //$NON-NLS-1$
				}
				break;
			case PROPOSAL_MODE_NONE:
			default:
				proposals = NO_PROPOSALS;
				errorMessage = AntEditorMessages.getString("AntEditorCompletionProcessor.33"); //$NON-NLS-1$
		}

		if (proposals.length > 0) {
			errorMessage = IAntCoreConstants.EMPTY_STRING;
		}
		return proposals;

	}

	private ICompletionProposal[] getProjectAttributeValueProposals(String prefix, String attributeName) {
		if (attributeName.equalsIgnoreCase(IAntCoreConstants.DEFAULT)) {
			return getDefaultValueProposals(prefix);
		}

		return NO_PROPOSALS;
	}

	private ICompletionProposal[] getDefaultValueProposals(String prefix) {
		Map<String, Target> targets = getTargets();
		List<AntCompletionProposal> defaultProposals = new ArrayList<>(targets.size());
		Iterator<Target> itr = targets.values().iterator();
		Target target;
		String targetName;
		while (itr.hasNext()) {
			target = itr.next();
			targetName = target.getName();
			if (targetName.toLowerCase().startsWith(prefix) && targetName.length() > 0) {
				defaultProposals.add(new AntCompletionProposal(targetName, cursorPosition
						- prefix.length(), prefix.length(), targetName.length(), getTargetImage(targetName), targetName, target.getDescription(), AntCompletionProposal.TASK_PROPOSAL));
			}
		}
		ICompletionProposal[] proposals = new ICompletionProposal[defaultProposals.size()];
		return defaultProposals.toArray(proposals);
	}

	private ICompletionProposal[] getReferencesValueProposals(String prefix) {
		Project project = antModel.getProjectNode().getProject();
		Map<String, Object> references = project.getReferences();
		if (references.isEmpty()) {
			return NO_PROPOSALS;
		}

		IAntElement node = antModel.getNode(cursorPosition, false);
		if (node == null) {
			return NO_PROPOSALS;
		}
		while (node.getParentNode() instanceof AntTaskNode) {
			node = node.getParentNode();
		}
		String id = null;
		if (node instanceof AntTaskNode) {
			id = ((AntTaskNode) node).getId();
		}
		Set<String> refIds = references.keySet();
		List<ICompletionProposal> proposals = new ArrayList<>(refIds.size());
		String refId;
		ICompletionProposal proposal;
		int prefixLength = prefix.length();
		int replacementOffset = cursorPosition - prefixLength;
		Iterator<String> iter = refIds.iterator();
		while (iter.hasNext()) {
			refId = iter.next();
			if (!refId.equals(id) && (prefixLength == 0 || refId.toLowerCase().startsWith(prefix))) {
				proposal = new AntCompletionProposal(refId, replacementOffset, prefixLength, refId.length(), null, refId, null, AntCompletionProposal.TASK_PROPOSAL);
				proposals.add(proposal);
			}
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	protected ICompletionProposal[] getTargetAttributeValueProposals(IDocument document, String textToSearch, String prefix, String attributeName) {
		if (attributeName.equalsIgnoreCase("depends")) { //$NON-NLS-1$
			return getDependsValueProposals(document, prefix);
		} else if (attributeName.equalsIgnoreCase("if") || attributeName.equalsIgnoreCase("unless")) { //$NON-NLS-1$ //$NON-NLS-2$
			if (!textToSearch.trim().endsWith(",")) { //$NON-NLS-1$
				return getPropertyProposals(document, prefix, cursorPosition);
			}
		} else if (attributeName.equalsIgnoreCase("extensionOf")) {//$NON-NLS-1$
			return getExtensionOfValueProposals(prefix);
		}

		return NO_PROPOSALS;
	}

	private ICompletionProposal[] getExtensionOfValueProposals(String prefix) {
		List<ICompletionProposal> extensions = new ArrayList<>();
		Map<String, Target> targets = getTargets();
		Set<String> targetNames = targets.keySet();
		for (String targetName : targetNames) {
			Target currentTarget = targets.get(targetName);
			if (currentTarget instanceof ExtensionPoint) {
				extensions.add(new AntCompletionProposal(targetName, cursorPosition
						- prefix.length(), prefix.length(), targetName.length(), getTargetImage(targetName), targetName, targets.get(targetName).getDescription(), AntCompletionProposal.TASK_PROPOSAL));
			}

		}
		return extensions.toArray(new ICompletionProposal[extensions.size()]);
	}

	protected ICompletionProposal[] getAntCallAttributeValueProposals(IDocument document, String prefix, String attributeName) {
		if (attributeName.equalsIgnoreCase("target")) { //$NON-NLS-1$
			return getTargetProposals(document, prefix);
		}

		return NO_PROPOSALS;
	}

	private ICompletionProposal[] getTargetProposals(IDocument document, String prefix) {
		String currentTargetName = getEnclosingTargetName(document, lineNumber, columnNumber);
		if (currentTargetName == null) {
			return NO_PROPOSALS;
		}

		Map<String, Target> targets = getTargets();
		Set<String> targetNames = targets.keySet();
		List<ICompletionProposal> proposals = new ArrayList<>(targets.size() - 2); // current target and implicit target
		for (String targetName : targetNames) {
			if (targetName.equals(currentTargetName)) {
				continue;
			}
			if (targetName.toLowerCase().startsWith(prefix) && targetName.length() > 0) {
				proposals.add(new AntCompletionProposal(targetName, cursorPosition
						- prefix.length(), prefix.length(), targetName.length(), getTargetImage(targetName), targetName, targets.get(targetName).getDescription(), AntCompletionProposal.TASK_PROPOSAL));
			}
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	/**
	 * Retrieves the representative image of a target of the given name. If the target cannot be found, <code>null</code> will be returned.
	 *
	 * @param targetName
	 *            the target's name
	 * @return an image suitable for representing the target, or <code>null</code> if the target cannot be found
	 * @since 3.6
	 */
	private Image getTargetImage(String targetName) {
		AntTargetNode targetNode = antModel.getTargetNode(targetName);
		if (targetNode == null) {
			return null;
		} else if (targetNode.isInternal()) {
			return AntUIImages.getImage(IAntUIConstants.IMG_ANT_TARGET_INTERNAL);
		} else if (targetNode.isDefaultTarget()) {
			return AntUIImages.getImage(IAntUIConstants.IMG_ANT_DEFAULT_TARGET);
		} else {
			return AntUIImages.getImage(IAntUIConstants.IMG_ANT_TARGET);
		}
	}

	private ICompletionProposal[] getDependsValueProposals(IDocument document, String prefix) {
		List<String> possibleDependencies = new ArrayList<>();
		String currentTargetName = getEnclosingTargetName(document, lineNumber, columnNumber);
		if (currentTargetName == null) {
			return NO_PROPOSALS;
		}

		Map<String, Target> targets = getTargets();
		Set<String> targetNames = targets.keySet();
		Enumeration<String> dependencies = null;
		for (String targetName : targetNames) {
			if (targetName.equals(currentTargetName)) {
				Target currentTarget = targets.get(targetName);
				dependencies = currentTarget.getDependencies();
				continue;
			}
			if (targetName.toLowerCase().startsWith(prefix) && targetName.length() > 0) {
				possibleDependencies.add(targetName);
			}
		}
		if (dependencies != null) {
			while (dependencies.hasMoreElements()) {
				possibleDependencies.remove(dependencies.nextElement());
			}
		}
		ArrayList<ICompletionProposal> proposals = new ArrayList<>(possibleDependencies.size());
		for (String targetName : possibleDependencies) {
			ICompletionProposal proposal = new AntCompletionProposal(targetName, cursorPosition
					- prefix.length(), prefix.length(), targetName.length(), getTargetImage(targetName), targetName, targets.get(targetName).getDescription(), AntCompletionProposal.TASK_PROPOSAL);
			proposals.add(proposal);
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	/**
	 * Returns all possible attributes for the specified task.
	 *
	 * @param taskName
	 *            the name of the task for that the attribute shall be completed
	 * @param prefix
	 *            prefix, that all proposals should start with. The prefix may be an empty string.
	 */
	protected ICompletionProposal[] getAttributeProposals(String taskName, String prefix) {
		List<ICompletionProposal> proposals = new ArrayList<>();
		IElement element = getDtd().getElement(taskName);
		if (element != null) {
			for (String attrName : element.getAttributes().keySet()) {
				if (prefix.length() == 0 || attrName.toLowerCase().startsWith(prefix)) {
					IAttribute dtdAttributes = element.getAttributes().get(attrName);
					String replacementString = attrName + "=\"\""; //$NON-NLS-1$
					String displayString = attrName;
					String[] items = dtdAttributes.getEnum();
					if (items != null) {
						if (items.length > 1) {
							displayString += " - ("; //$NON-NLS-1$
						}
						for (int i = 0; i < items.length; i++) {
							displayString += items[i];
							if (i + 1 < items.length) {
								displayString += " | "; //$NON-NLS-1$
							} else {
								displayString += ")"; //$NON-NLS-1$
							}
						}
					}

					addAttributeProposal(taskName, prefix, proposals, attrName, replacementString, displayString, true);
				}
			}
		} else { // possibly a user defined task or type
			AntTypeDefinition taskClass = getTaskClass(taskName);
			if (taskClass != null) {
				if (MACROINSTANCE_NAME.equals(taskClass.getClassName())) {
					addMacroDefAttributeProposals(taskName, prefix, proposals);
				} else {
					IntrospectionHelper helper = getIntrospectionHelper(taskClass);
					if (helper != null) {
						addAttributeProposals(helper, taskName, prefix, proposals);
					}
				}
			} else { // nested user defined element
				AntTypeDefinition nestedType = getNestedType();
				if (nestedType != null) {
					IntrospectionHelper helper = getIntrospectionHelper(nestedType);
					if (helper != null) {
						addAttributeProposals(helper, taskName, prefix, proposals);
					}
				}
			}
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	private void addAttributeProposals(IntrospectionHelper helper, String taskName, String prefix, List<ICompletionProposal> proposals) {
		Enumeration<String> attributes = helper.getAttributes();
		while (attributes.hasMoreElements()) {
			String attribute = attributes.nextElement();
			if (prefix.length() == 0 || attribute.toLowerCase().startsWith(prefix)) {
				String replacementString = attribute + "=\"\""; //$NON-NLS-1$
				addAttributeProposal(taskName, prefix, proposals, attribute, replacementString, attribute, false);
			}
		}
	}

	private AntTypeDefinition getNestedType() {
		AntElementNode currentNode = antModel.getNode(cursorPosition, false);
		if (currentNode == null) {
			return null;
		}
		IAntElement parent = currentNode.getParentNode();
		if (parent instanceof AntTaskNode) {
			String parentName = parent.getName();
			if (hasNestedElements(parentName)) {
				AntTypeDefinition taskClass = getTaskClass(parentName);
				if (taskClass != null) {
					IntrospectionHelper helper = getIntrospectionHelper(taskClass);
					if (helper != null) {
						try {
							Class<?> nestedType = helper.getElementType(currentNode.getName());
							// TODO probably a much better way to find the definition of a nested type than this
							AntTypeDefinition def = new AntTypeDefinition();
							def.setClass(nestedType);
							def.setClassName(nestedType.getName());
							return def;
						}
						catch (BuildException be) {
							// do nothing
						}
					}
				}
			}
		}
		return null;
	}

	private IntrospectionHelper getIntrospectionHelper(AntTypeDefinition taskClass) {
		Project p = antModel.getProjectNode().getProject();
		Class<?> clazz = taskClass.getExposedClass(p);
		if (clazz != null) {
			IntrospectionHelper helper = null;
			try {
				helper = IntrospectionHelper.getHelper(antModel.getProjectNode().getProject(), clazz);
			}
			catch (NoClassDefFoundError e) {
				// ignore as a task may require additional classpath components
			}
			return helper;
		}
		return null;
	}

	private void addMacroDefAttributeProposals(String taskName, String prefix, List<ICompletionProposal> proposals) {
		currentProposalMode = PROPOSAL_MODE_ATTRIBUTE_PROPOSAL;
		AntDefiningTaskNode node = antModel.getDefininingTaskNode(taskName);
		Object task = node.getRealTask();
		if (!(task instanceof MacroDef)) {
			return;
		}
		List<Attribute> attributes = ((MacroDef) task).getAttributes();
		for (Attribute attribute : attributes) {
			String attributeName = attribute.getName();
			if (!(prefix.length() == 0 || attributeName.toLowerCase().startsWith(prefix))) {
				continue;
			}
			String replacementString = attributeName + "=\"\""; //$NON-NLS-1$
			String proposalInfo = null;

			String description = attribute.getDescription();
			if (description != null) {
				proposalInfo = description;
			}
			String deflt = attribute.getDefault();
			if (deflt != null && deflt.length() > 0) {
				proposalInfo = (proposalInfo == null ? "<BR><BR>" : (proposalInfo += "<BR><BR>")); //$NON-NLS-1$ //$NON-NLS-2$
				proposalInfo += MessageFormat.format(AntEditorMessages.getString("AntEditorCompletionProcessor.59"), new Object[] { deflt }); //$NON-NLS-1$
			}

			ICompletionProposal proposal = new AntCompletionProposal(replacementString, cursorPosition
					- prefix.length(), prefix.length(), attributeName.length()
							+ 2, null, attributeName, proposalInfo, AntCompletionProposal.TASK_PROPOSAL);
			proposals.add(proposal);
		}
	}

	private void addMacroDefElementProposals(String taskName, String prefix, List<ICompletionProposal> proposals) {
		AntDefiningTaskNode node = antModel.getDefininingTaskNode(taskName);
		Object task = node.getRealTask();
		if (!(task instanceof MacroDef)) {
			return;
		}
		Map<String, TemplateElement> elements = ((MacroDef) task).getElements();
		int prefixLength = prefix.length();
		int replacementOffset = cursorPosition - prefixLength;
		for (String elementName : elements.keySet()) {
			if (!(prefixLength == 0 || elementName.toLowerCase().startsWith(prefix))) {
				continue;
			}
			MacroDef.TemplateElement element = elements.get(elementName);
			String replacementString = MessageFormat.format("<{0}>\n</{1}>", new Object[] { elementName, elementName }); //$NON-NLS-1$
			String proposalInfo = null;

			String description = element.getDescription();
			if (description != null) {
				proposalInfo = description;
			}
			proposalInfo = (proposalInfo == null ? "<BR><BR>" : (proposalInfo += "<BR><BR>")); //$NON-NLS-1$ //$NON-NLS-2$

			if (element.isOptional()) {
				proposalInfo += AntEditorMessages.getString("AntEditorCompletionProcessor.1"); //$NON-NLS-1$
			} else {
				proposalInfo += AntEditorMessages.getString("AntEditorCompletionProcessor.2"); //$NON-NLS-1$
			}

			ICompletionProposal proposal = new AntCompletionProposal(replacementString, replacementOffset, prefixLength, elementName.length()
					+ 2, null, elementName, proposalInfo, AntCompletionProposal.TASK_PROPOSAL);
			proposals.add(proposal);
		}
	}

	private void addAttributeProposal(String taskName, String prefix, List<ICompletionProposal> proposals, String attrName, String replacementString, String displayString, boolean lookupDescription) {

		String proposalInfo = null;
		if (lookupDescription) {
			String required = getDescriptionProvider().getRequiredAttributeForTaskAttribute(taskName, attrName);
			if (required != null && required.length() > 0) {
				proposalInfo = AntEditorMessages.getString("AntEditorCompletionProcessor.Required___4") + required; //$NON-NLS-1$
				proposalInfo += "<BR><BR>"; //$NON-NLS-1$
			}
			String description = getDescriptionProvider().getDescriptionForTaskAttribute(taskName, attrName);
			if (description != null) {
				proposalInfo = (proposalInfo == null ? IAntCoreConstants.EMPTY_STRING : proposalInfo);
				proposalInfo += description;
			}
		}

		ICompletionProposal proposal = new AntCompletionProposal(replacementString, cursorPosition
				- prefix.length(), prefix.length(), attrName.length() + 2, null, displayString, proposalInfo, AntCompletionProposal.TASK_PROPOSAL);
		proposals.add(proposal);
	}

	/**
	 * Returns all possible values for the specified attribute of the specified task.
	 *
	 * @param taskName
	 *            the name of the task that the specified attribute belongs to.
	 *
	 * @param attributeName
	 *            the name of the attribute for that the value shall be completed
	 *
	 * @param prefix
	 *            the prefix that all proposals should start with. The prefix may be an empty string.
	 */
	private ICompletionProposal[] getAttributeValueProposals(String taskName, String attributeName, String prefix) {
		List<ICompletionProposal> proposals = new ArrayList<>();
		IElement taskElement = getDtd().getElement(taskName);
		if (taskElement != null) {
			IAttribute attribute = taskElement.getAttributes().get(attributeName);
			if (attribute != null) {
				String[] items = attribute.getEnum();
				if (items != null) {
					for (String item : items) {
						if (prefix.length() == 0 || item.toLowerCase().startsWith(prefix)) {
							proposals.add(new AntCompletionProposal(item, cursorPosition
									- prefix.length(), prefix.length(), item.length(), null, item, null, AntCompletionProposal.TASK_PROPOSAL));
						}
					}
				}
			}
		} else { // possibly a user defined task or type
			AntTypeDefinition taskClass = getTaskClass(taskName);
			if (taskClass != null) {
				IntrospectionHelper helper = getIntrospectionHelper(taskClass);
				if (helper != null) {
					addAttributeValueProposals(helper, attributeName, prefix, proposals);
				}
			} else { // nested user defined element
				AntTypeDefinition nestedType = getNestedType();
				if (nestedType != null) {
					IntrospectionHelper helper = getIntrospectionHelper(nestedType);
					if (helper != null) {
						addAttributeValueProposals(helper, attributeName, prefix, proposals);
					}
				}
			}
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	private void addAttributeValueProposals(IntrospectionHelper helper, String attributeName, String prefix, List<ICompletionProposal> proposals) {
		Enumeration<String> attributes = helper.getAttributes();
		while (attributes.hasMoreElements()) {
			String attribute = attributes.nextElement();
			if (attribute.equals(attributeName)) {
				Class<?> attributeType = helper.getAttributeType(attribute);
				addAttributeValueProposalsForAttributeType(attributeType, prefix, proposals);
				break;
			}
		}
	}

	private void addAttributeValueProposalsForAttributeType(Class<?> attributeType, String prefix, List<ICompletionProposal> proposals) {
		if ((attributeType == Boolean.TYPE || attributeType == Boolean.class) && prefix.length() <= 5) {
			addBooleanAttributeValueProposals(prefix, proposals);
		} else if (EnumeratedAttribute.class.isAssignableFrom(attributeType)) {
			try {
				addEnumeratedAttributeValueProposals(attributeType, prefix, proposals);
			}
			catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
					| SecurityException e) {
				// do nothing
			}
		} else if (Reference.class == attributeType) {
			proposals.addAll(Arrays.asList(getReferencesValueProposals(prefix)));
		}
	}

	private void addEnumeratedAttributeValueProposals(Class<?> type, String prefix, List<ICompletionProposal> proposals) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		EnumeratedAttribute ea = (EnumeratedAttribute) type.getConstructor().newInstance();
		String enumerated;
		for (String value : ea.getValues()) {
			enumerated = value.toLowerCase();
			if (prefix.length() == 0 || enumerated.startsWith(prefix)) {
				proposals.add(new AntCompletionProposal(enumerated, cursorPosition
						- prefix.length(), prefix.length(), enumerated.length(), null, enumerated, null, AntCompletionProposal.TASK_PROPOSAL));
			}
		}
	}

	private void addBooleanAttributeValueProposals(String prefix, List<ICompletionProposal> proposals) {
		String booleanAssist;
		for (String booleanValue : new String[] { "true", "false", "on", "off", "yes", "no" }) { //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			booleanAssist = booleanValue.toLowerCase();
			if (prefix.length() == 0 || booleanAssist.startsWith(prefix)) {
				proposals.add(new AntCompletionProposal(booleanAssist, cursorPosition
						- prefix.length(), prefix.length(), booleanAssist.length(), null, booleanAssist, null, AntCompletionProposal.TASK_PROPOSAL));
			}
		}
	}

	/**
	 * Returns all possible properties for the specified prefix.
	 * <P>
	 * Note that the completion mode must be property mode, otherwise it is not safe to call this method.
	 */
	protected ICompletionProposal[] getPropertyProposals(IDocument document, String prefix, int aCursorPosition) {
		List<ICompletionProposal> proposals = new ArrayList<>();
		Map<String, ICompletionProposal> displayStringToProposals = new HashMap<>();
		Map<String, Object> properties = findPropertiesFromDocument();

		Image image = AntUIImages.getImage(IAntUIConstants.IMG_PROPERTY);
		// Determine replacement length and offset
		// String from beginning to the beginning of the prefix
		int replacementLength = prefix.length();
		int replacementOffset = 0;
		String text = document.get();
		String stringToPrefix = text.substring(0, aCursorPosition - prefix.length());
		// Property proposal
		String lastTwoCharacters = stringToPrefix.substring(stringToPrefix.length() - 2, stringToPrefix.length());
		boolean appendBraces = true;
		if (lastTwoCharacters.equals("${")) { //$NON-NLS-1$
			replacementLength += 2;
			replacementOffset = aCursorPosition - prefix.length() - 2;
		} else if (lastTwoCharacters.endsWith("$")) { //$NON-NLS-1$
			replacementLength += 1;
			replacementOffset = aCursorPosition - prefix.length() - 1;
		} else {
			// support for property proposals for the if/unless attributes of targets
			replacementOffset = aCursorPosition - prefix.length();
			appendBraces = false;
		}

		if (text.length() > aCursorPosition && text.charAt(aCursorPosition) == '}') {
			replacementLength += 1;
		}
		String propertyName;
		for (Iterator<String> i = properties.keySet().iterator(); i.hasNext();) {
			propertyName = i.next();
			if (prefix.length() == 0 || propertyName.toLowerCase().startsWith(prefix)) {
				String additionalPropertyInfo = (String) properties.get(propertyName);

				StringBuilder replacementString = new StringBuilder();
				if (appendBraces) {
					replacementString.append("${"); //$NON-NLS-1$
				}
				replacementString.append(propertyName);
				if (appendBraces) {
					replacementString.append('}');
				}

				if (displayStringToProposals.get(propertyName) == null) {
					ICompletionProposal proposal = new AntCompletionProposal(replacementString.toString(), replacementOffset, replacementLength, replacementString.length(), image, propertyName, additionalPropertyInfo, AntCompletionProposal.PROPERTY_PROPOSAL);
					proposals.add(proposal);
					displayStringToProposals.put(propertyName, proposal);
				}
			}
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	/**
	 * Returns all possible proposals for the specified parent name.
	 * <P>
	 * No completions will be returned if <code>parentName</code> is not known.
	 *
	 * @param document
	 *            the entire document
	 * @param parentName
	 *            name of the parent (surrounding) element.
	 * @param prefix
	 *            the prefix that all proposals should start with. The prefix may be an empty string.
	 */
	protected ICompletionProposal[] getTaskProposals(IDocument document, String parentName, String prefix) {
		List<ICompletionProposal> proposals = new ArrayList<>(250);
		ICompletionProposal proposal;
		if (areTasksOrTypesValidChildren(parentName)) {
			// use the definitions in the project as that includes more than what is defined in the DTD
			Project project = antModel.getProjectNode().getProject();
			Map<String, AntTypeDefinition> tasksAndTypes = ComponentHelper.getComponentHelper(project).getAntTypeTable();
			createProposals(document, prefix, proposals, tasksAndTypes);
			if (parentName.equals("project")) { //$NON-NLS-1$
				if ("target".startsWith(prefix)) { //$NON-NLS-1$
					proposals.add(newCompletionProposal(document, prefix, "target")); //$NON-NLS-1$
				}
				if ("extension-point".startsWith(prefix)) { //$NON-NLS-1$
					proposals.add(newCompletionProposal(document, prefix, "extension-point")); //$NON-NLS-1$
				}
			}
		} else {
			IElement parent = getDtd().getElement(parentName);
			if (parent != null) {
				IDfm dfm = parent.getDfm();
				String[] accepts = dfm.getAccepts();
				if (accepts.length == 0) {
					currentProposalMode = PROPOSAL_MODE_NONE;
				}
				String elementName;
				for (String accept : accepts) {
					elementName = accept;
					if (prefix.length() == 0 || elementName.toLowerCase().startsWith(prefix)) {
						proposal = newCompletionProposal(document, prefix, elementName);
						proposals.add(proposal);
					}
				}
			} else {
				// a nested element of a user defined task/type?
				AntTypeDefinition taskClass = getTaskClass(parentName);
				if (taskClass != null) {
					if (MACROINSTANCE_NAME.equals(taskClass.getClassName())) {
						currentProposalMode = PROPOSAL_MODE_ATTRIBUTE_PROPOSAL;
						addMacroDefElementProposals(parentName, prefix, proposals);
					} else {
						currentProposalMode = PROPOSAL_MODE_NESTED_ELEMENT_PROPOSAL;
						IntrospectionHelper helper = getIntrospectionHelper(taskClass);
						if (helper != null) {
							Enumeration<String> nested = helper.getNestedElements();
							String nestedElement;
							while (nested.hasMoreElements()) {
								nestedElement = nested.nextElement();
								if (prefix.length() == 0 || nestedElement.toLowerCase().startsWith(prefix)) {
									proposal = newCompletionProposal(document, prefix, nestedElement);
									proposals.add(proposal);
								}
							}
						}
					}
				}
			}
		}

		proposal = getClosingTaskProposal(getOpenElementName(), prefix, false);
		if (proposal != null) {
			proposals.add(proposal);
		}

		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	private boolean areTasksOrTypesValidChildren(String parentName) {
		return parentName == "project" || parentName == "target" || parentName == "sequential" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				|| parentName == "presetdef" || parentName == "parallel" || parentName == "daemons" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				|| parentName == "extension-point"; //$NON-NLS-1$
	}

	/**
	 * Returns proposals that define the structure of a build file.
	 *
	 * Note that template proposals which define the structure of a build file are handled by {@link #determineTemplateProposals(ITextViewer, int)}
	 * which limits proposals by context type.
	 *
	 * @param document
	 *            the entire document
	 * @param prefix
	 *            the prefix that all proposals should start with. The prefix may be an empty string.
	 */
	protected ICompletionProposal[] getBuildFileProposals(IDocument document, String prefix) {
		String rootElementName = "project"; //$NON-NLS-1$
		IElement rootElement = getDtd().getElement(rootElementName);
		if (rootElement != null && rootElementName.toLowerCase().startsWith(prefix)) {
			ICompletionProposal proposal = newCompletionProposal(document, prefix, rootElementName);
			return new ICompletionProposal[] { proposal };
		}

		return NO_PROPOSALS;
	}

	private void createProposals(IDocument document, String prefix, List<ICompletionProposal> proposals, Map<String, AntTypeDefinition> tasks) {
		Iterator<String> keys = tasks.keySet().iterator();
		ICompletionProposal proposal;
		String key;
		while (keys.hasNext()) {
			key = antModel.getUserNamespaceCorrectName(keys.next());
			if (prefix.length() == 0 || key.toLowerCase().startsWith(prefix)) {
				proposal = newCompletionProposal(document, prefix, key);
				proposals.add(proposal);
			}
		}
	}

	private ICompletionProposal newCompletionProposal(IDocument document, String aPrefix, String elementName) {
		additionalProposalOffset = 0;
		Image proposalImage = AntUIImages.getImage(IAntUIConstants.IMG_TASK_PROPOSAL);
		String proposalInfo = getDescriptionProvider().getDescriptionForTask(elementName);
		boolean hasNestedElements = hasNestedElements(elementName);
		String replacementString = getTaskProposalReplacementString(elementName, hasNestedElements);
		int replacementOffset = cursorPosition - aPrefix.length();
		int replacementLength = aPrefix.length();
		if (replacementOffset > 0 && document.get().charAt(replacementOffset - 1) == '<') {
			replacementOffset--;
			replacementLength++;
		}
		int proposalCursorPosition;
		if (hasNestedElements) {
			proposalCursorPosition = elementName.length() + 2 + additionalProposalOffset;
		} else {
			if (additionalProposalOffset > 0) {
				additionalProposalOffset += 2; // <antstructure output="|"/>
			} else {
				additionalProposalOffset += 1; // <arg|/>
			}
			proposalCursorPosition = elementName.length() + additionalProposalOffset;
		}
		return new AntCompletionProposal(replacementString, replacementOffset, replacementLength, proposalCursorPosition, proposalImage, elementName, proposalInfo, AntCompletionProposal.TASK_PROPOSAL);
	}

	/**
	 * Returns the one possible completion for the specified unclosed task .
	 *
	 * @param openElementName
	 *            the task that hasn't been closed last
	 * @param prefix
	 *            The prefix that the one possible proposal should start with. The prefix may be an empty string.
	 * @return the proposal or <code>null</code> if no closing proposal available
	 */
	private ICompletionProposal getClosingTaskProposal(String openElementName, String prefix, boolean closingMode) {
		char previousChar = getPreviousChar();
		ICompletionProposal proposal = null;
		if (openElementName != null) {
			if (prefix.length() == 0 || openElementName.toLowerCase().startsWith(prefix)) {
				StringBuilder replaceString = new StringBuilder();
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
				StringBuilder displayString = new StringBuilder("</"); //$NON-NLS-1$
				displayString.append(openElementName);
				displayString.append('>');
				proposal = new AntCompletionProposal(replaceString.toString(), cursorPosition
						- prefix.length(), prefix.length(), replaceString.length(), null, displayString.toString(), AntEditorMessages.getString("AntEditorCompletionProcessor.39"), AntCompletionProposal.TAG_CLOSING_PROPOSAL); //$NON-NLS-1$
			}
		}

		return proposal;
	}

	protected char getPreviousChar() {
		ITextSelection selection = (ITextSelection) viewer.getSelectionProvider().getSelection();
		int offset = selection.getOffset();
		char previousChar = '?';
		try {
			previousChar = viewer.getDocument().getChar(offset - 1);
		}
		catch (BadLocationException e) {
			// do nothing
		}
		return previousChar;
	}

	/**
	 * Returns the replacement string for the specified task name.
	 */
	private String getTaskProposalReplacementString(String aTaskName, boolean hasNested) {
		StringBuffer replacement = new StringBuffer("<"); //$NON-NLS-1$
		replacement.append(aTaskName);
		ProposalNode task = getDescriptionProvider().getTaskNode(aTaskName);
		if (task != null) {
			appendRequiredAttributes(replacement, task);
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

	private void appendRequiredAttributes(StringBuffer replacement, ProposalNode task) {
		if (task.nodes != null) {
			boolean requiredAdded = false;
			Entry<String, ProposalNode> entry = null;
			for (Iterator<Entry<String, ProposalNode>> i = task.nodes.entrySet().iterator(); i.hasNext();) {
				entry = i.next();
				String name = entry.getKey();
				ProposalNode att = entry.getValue();
				if ("yes".equalsIgnoreCase(att.required)) { //$NON-NLS-1$
					replacement.append(' ');
					replacement.append(name);
					replacement.append("=\"\""); //$NON-NLS-1$
					if (!requiredAdded) {
						additionalProposalOffset = name.length() + 2;
						requiredAdded = true;
					}
				}
			}
		}
	}

	/**
	 * Returns whether the named element supports nested elements.
	 */
	private boolean hasNestedElements(String elementName) {
		IElement element = getDtd().getElement(elementName);
		if (element != null) {
			return !element.isEmpty();
		}
		AntTypeDefinition taskClass = getTaskClass(elementName);
		if (taskClass != null) {
			IntrospectionHelper helper = getIntrospectionHelper(taskClass);
			if (helper != null) {
				Enumeration<String> nested = helper.getNestedElements();
				return nested.hasMoreElements();
			}
		}
		return false;
	}

	/**
	 * Finds a direct child element with <code>aChildElementName</code> of <code>anElement</code>.
	 * <P>
	 * The child will not be searched for in the whole hierarchy but only in the hierarchy step below.
	 *
	 * @return the found child or <code>null</code> if not found.
	 */
	protected Element findChildElementNamedOf(Element anElement, String aChildElementName) {
		NodeList nodeList = anElement.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node childNode = nodeList.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				if (childNode.getNodeName().equals(aChildElementName)) {
					return (Element) childNode;
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
		ITextSelection selection = (ITextSelection) viewer.getSelectionProvider().getSelection();
		IDocument doc = viewer.getDocument();
		return getPrefixFromDocument(doc.get(), selection.getOffset() + selection.getLength()).toLowerCase();
	}

	/**
	 * Returns the prefix in the specified document text with respect to the specified offset.
	 *
	 * @param aDocumentText
	 *            the whole content of the edited file as String
	 * @param anOffset
	 *            the cursor position
	 */
	protected String getPrefixFromDocument(String aDocumentText, int anOffset) {
		if (currentPrefix != null) {
			return currentPrefix;
		}
		int startOfWordToken = anOffset;

		char token = 'a';
		if (startOfWordToken > 0) {
			token = aDocumentText.charAt(startOfWordToken - 1);
		}

		while (startOfWordToken > 0 && (Character.isJavaIdentifierPart(token) || '.' == token || '-' == token || ';' == token) && !('$' == token)) {
			startOfWordToken--;
			if (startOfWordToken == 0) {
				break; // word goes right to the beginning of the doc
			}
			token = aDocumentText.charAt(startOfWordToken - 1);
		}

		if (startOfWordToken != anOffset) {
			currentPrefix = aDocumentText.substring(startOfWordToken, anOffset).toLowerCase();
		} else {
			currentPrefix = IAntCoreConstants.EMPTY_STRING;
		}
		return currentPrefix;
	}

	/**
	 * Returns the current proposal mode.
	 */
	protected int determineProposalMode(IDocument document, int aCursorPosition, String aPrefix) {
		if (currentProposalMode != -1) {
			return currentProposalMode;
		}
		if (document.getLength() == 0 || (document.getLength() == 1 && document.get().equals("<"))) { //$NON-NLS-1$
			return PROPOSAL_MODE_BUILDFILE;
		}

		// String from beginning of document to the beginning of the prefix
		String text = document.get();
		String stringToPrefix = text.substring(0, aCursorPosition - aPrefix.length());
		if (stringToPrefix.length() == 0) {
			return PROPOSAL_MODE_BUILDFILE;
		}
		// Is trimmable from behind
		String trimmedString = stringToPrefix.trim();
		if (antModel != null && antModel.getProjectNode() == null) {
			currentTaskString = getTaskStringFromDocumentStringToPrefix(trimmedString);
			if ("project".equals(currentTaskString)) { //$NON-NLS-1$
				return PROPOSAL_MODE_ATTRIBUTE_PROPOSAL;
			}
			return PROPOSAL_MODE_BUILDFILE;
		}

		char lastChar = 0;
		if (trimmedString.length() > 0) {
			lastChar = trimmedString.charAt(trimmedString.length() - 1);
		} else {
			return PROPOSAL_MODE_TASK_PROPOSAL;
		}
		if (stringToPrefix.charAt(stringToPrefix.length() - 1) != lastChar && lastChar != '>' && lastChar != ',') {
			/*
			 * Substring must be trimmable from behind in case of attribute proposal because a space or a new line must be used as delimiter between
			 * task name and attribute or attribute and attribute. Example: '<property id="bla" name="hups"'
			 */

			// Attribute proposal
			if (lastChar != '>' && lastChar != '<') {
				currentTaskString = getTaskStringFromDocumentStringToPrefix(trimmedString);
				if (currentTaskString != null && isKnownElement(currentTaskString)) {
					return PROPOSAL_MODE_ATTRIBUTE_PROPOSAL;
				}
			}
		} else if (stringToPrefix.charAt(stringToPrefix.length() - 1) == '"' || trimmedString.charAt(trimmedString.length() - 1) == ',') {
			// Attribute value proposal
			currentTaskString = getTaskStringFromDocumentStringToPrefix(trimmedString);
			if (currentTaskString != null && isKnownElement(currentTaskString)) {
				return PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL;
			}
		} else { // Possibly a Task proposal
			int spaceIndex = stringToPrefix.lastIndexOf(' ');
			int lessThanIndex = stringToPrefix.lastIndexOf('<');
			int greaterThanIndex = stringToPrefix.lastIndexOf('>');
			// Task proposal
			if (greaterThanIndex < lessThanIndex) {
				// we are inside an open element
				if (lastChar == '$') {
					return PROPOSAL_MODE_PROPERTY_PROPOSAL;
				}
				if (lessThanIndex > spaceIndex) {
					int slashIndex = stringToPrefix.lastIndexOf('/');
					if (slashIndex == lessThanIndex + 1) {
						return PROPOSAL_MODE_TASK_PROPOSAL_CLOSING; // ... </
					}
					return PROPOSAL_MODE_TASK_PROPOSAL;
				}
			}
			if (lessThanIndex < greaterThanIndex) {
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
		if (stringToPrefix.length() >= 2) {
			String lastTwoChars = stringToPrefix.substring(stringToPrefix.length() - 2, stringToPrefix.length());
			if (lastTwoChars.equals("${") || //$NON-NLS-1$
					stringToPrefix.charAt(stringToPrefix.length() - 1) == '$') {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the last occurring task string in the specified string.
	 * <P>
	 * The returned string must not necessarily be a valid Ant task string. This can be tested with the method <code>inNamedTaskKnown(String)</code>
	 * after invoking this method.
	 *
	 * @param aDocumentStringToPrefix
	 *            the String that contains the whole string of the currently edited file from the beginning up to the prefix for code completion.
	 *            Example: {@literal '<project default="name"><property '}.
	 *
	 * @return the extracted task string or <code>null</code> if no string could be extracted.
	 */
	private String getTaskStringFromDocumentStringToPrefix(String aDocumentStringToPrefix) {

		int lessThanIndex = aDocumentStringToPrefix.lastIndexOf('<');

		if (lessThanIndex > -1) {
			String taskString = aDocumentStringToPrefix.trim();
			taskString = taskString.substring(lessThanIndex + 1, taskString.length());
			int index = taskString.indexOf(' ');
			if (index > 0) {
				taskString = taskString.substring(0, index);
			}
			index = taskString.indexOf('\n');
			if (index > 0) {
				taskString = taskString.substring(0, index);
			}
			index = taskString.indexOf('\r');
			if (index > 0) {
				taskString = taskString.substring(0, index);
			}
			return taskString;
		}

		return null;
	}

	/**
	 * Returns the last occurring attribute string in the specified string. <code>null</code> is returned if no attribute string is available.
	 * <P>
	 * Calling this method is only safe if the current proposal mode is really <code>PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL</code>.
	 */
	public static String getAttributeStringFromDocumentStringToPrefix(String docStringToPrefix) {
		int index = docStringToPrefix.lastIndexOf('=');
		if (index == -1) {
			return null;
		}
		String subString = docStringToPrefix.substring(0, index);
		subString = subString.trim();

		index = subString.lastIndexOf(' ');
		if (index > 0) {
			subString = subString.substring(index + 1, subString.length());
		}
		index = subString.lastIndexOf('\n');
		if (index > 0) {
			subString = subString.substring(index + 1, subString.length());
		}
		index = subString.lastIndexOf('\r');
		if (index > 0) {
			subString = subString.substring(index + 1, subString.length());
		}
		return trimBeginning(subString);
	}

	private static String trimBeginning(String toBeTrimmed) {
		int i = 0;
		while ((i != toBeTrimmed.length()) && Character.isWhitespace(toBeTrimmed.charAt(i))) {
			i++;
		}
		return toBeTrimmed.substring(i);
	}

	/**
	 * Returns whether the specified element name is known
	 */
	protected boolean isKnownElement(String elementName) {
		if (elementName.equals("target") || elementName.equals("project") || elementName.equals("extension-point")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return true;
		}
		AntProjectNode node = antModel.getProjectNode();
		if (node != null) {
			Project antProject = node.getProject();
			ComponentHelper helper = ComponentHelper.getComponentHelper(antProject);
			if (helper.getDefinition(elementName) != null) {
				return true;
			}
			if (helper.getDefinition(antModel.getNamespaceCorrectName(elementName)) != null) {
				return true;
			}
			// not everything is a task or type (nested elements)
			if (getDtd().getElement(elementName) != null) {
				return true;
			}

			if (getNestedType() != null) {
				return true;
			}
		}

		return false;
	}

	private AntTypeDefinition getTaskClass(String taskName) {
		AntTypeDefinition clss = null;
		AntProjectNode node = antModel.getProjectNode();
		if (node != null) {
			Project antProject = node.getProject();
			Map<String, AntTypeDefinition> tasksAndTypes = ComponentHelper.getComponentHelper(antProject).getAntTypeTable();
			clss = tasksAndTypes.get(taskName);
			if (clss == null) {
				clss = tasksAndTypes.get(antModel.getNamespaceCorrectName(taskName));
			}
		}
		return clss;
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
		AntProjectNode project = antModel.getProjectNode();
		if (project == null) {
			return null;
		}
		int offset = getOffset(document, aLineNumber, aColumnNumber);
		if (offset == -1) {
			return null;
		}
		IAntElement node = project.getNode(offset);
		if (node == null) {
			node = antModel.getOpenElement();
		}
		if (node == null) {
			return IAntCoreConstants.EMPTY_STRING;
		} else if (node instanceof AntTaskNode) {
			String name = node.getName();
			if (offset <= node.getOffset() + name.length() - 1) {
				// not really the enclosing node as the offset is within the name of the node
				node = node.getParentNode();
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
	 * Return the properties as defined in the entire buildfile
	 *
	 * @return a map with all the found properties
	 */
	private Map<String, Object> findPropertiesFromDocument() {
		Project project = antModel.getProjectNode().getProject();
		return project.getProperties();
	}

	private Map<String, Target> getTargets() {
		Project project = antModel.getProjectNode().getProject();
		return project.getTargets();
	}

	protected File getEditedFile() {
		IWorkbenchPage page = AntUIPlugin.getActivePage();
		if (page == null) {
			return null;
		}
		IEditorPart editor = page.getActiveEditor();
		if (editor == null) {
			return null;
		}
		FileEditorInput input = (FileEditorInput) editor.getEditorInput();
		String projectPath = input.getFile().getProject().getLocation().toFile().getAbsolutePath();
		String projectRelativeFilePath = input.getFile().getFullPath().removeFirstSegments(1).makeRelative().toString();
		return new File(projectPath + File.separator + projectRelativeFilePath);
	}

	private String getOpenElementName() {
		AntElementNode node = antModel.getOpenElement();
		if (node == null) {
			return null;
		}
		return node.getName();
	}

	/**
	 * Finds the enclosing target in respect to the cursor position and returns its name
	 *
	 * @return the name of the enclosing target or <code>null</code> if not found or the element is not contained in a target.
	 */
	private String getEnclosingTargetName(IDocument document, int aLineNumber, int aColumnNumber) {

		AntProjectNode project = antModel.getProjectNode();
		int offset = getOffset(document, aLineNumber, aColumnNumber);
		if (offset == -1) {
			return null;
		}
		IAntElement node = project.getNode(offset);
		if (node instanceof AntTaskNode) {
			node = node.getParentNode();
			if (!(node instanceof AntTargetNode)) {
				// top level task
				node = null;
			}
		} else if (node instanceof AntProjectNode) {
			node = null;
		}
		String targetName = null;
		if (node == null || (targetName = ((AntTargetNode) node).getTarget().getName()) == null || targetName.length() == 0) {
			return null;
		}
		return targetName;
	}

	private int getOffset(IDocument document, int line, int column) {
		try {
			return document.getLineOffset(line) + column - 1;
		}
		catch (BadLocationException e) {
			return -1;
		}
	}

	/**
	 * Sets this processor's set of characters triggering the activation of the completion proposal computation.
	 *
	 * @param activationSet
	 *            the activation set
	 */
	public void setCompletionProposalAutoActivationCharacters(char[] activationSet) {
		autoActivationChars = activationSet;
	}

	@Override
	protected String extractPrefix(ITextViewer textViewer, int offset) {
		return getPrefixFromDocument(textViewer.getDocument().get(), offset);
	}

	/**
	 * Cut out angular brackets for relevance sorting, since the template name does not contain the brackets.
	 */
	@Override
	protected int getRelevance(Template template, String prefix) {
		String newprefix = prefix;
		if (newprefix.startsWith("<")) {//$NON-NLS-1$
			newprefix = prefix.substring(1);
		}
		if (template.getName().startsWith(newprefix)) {
			return 90;
		}
		return 0;
	}

	@Override
	protected Template[] getTemplates(String contextTypeId) {
		return AntTemplateAccess.getDefault().getTemplateStore().getTemplates(contextTypeId);
	}

	@Override
	protected TemplateContextType getContextType(ITextViewer textViewer, IRegion region) {
		switch (determineProposalMode(textViewer.getDocument(), cursorPosition, getCurrentPrefix())) {
			case PROPOSAL_MODE_TASK_PROPOSAL:
				if (getEnclosingTargetName(textViewer.getDocument(), lineNumber, columnNumber) == null) {
					return AntTemplateAccess.getDefault().getContextTypeRegistry().getContextType(TargetContextType.TARGET_CONTEXT_TYPE);
				}
				return AntTemplateAccess.getDefault().getContextTypeRegistry().getContextType(TaskContextType.TASK_CONTEXT_TYPE);
			case PROPOSAL_MODE_BUILDFILE:
				return AntTemplateAccess.getDefault().getContextTypeRegistry().getContextType(BuildFileContextType.BUILDFILE_CONTEXT_TYPE);
			case PROPOSAL_MODE_NONE:
			case PROPOSAL_MODE_ATTRIBUTE_PROPOSAL:
			case PROPOSAL_MODE_TASK_PROPOSAL_CLOSING:
			case PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL:
			case PROPOSAL_MODE_PROPERTY_PROPOSAL:
			case PROPOSAL_MODE_NESTED_ELEMENT_PROPOSAL:
			default:
				return null;
		}
	}

	@Override
	protected Image getImage(Template template) {
		return AntUIImages.getImage(IAntUIConstants.IMG_TEMPLATE_PROPOSAL);
	}

	@Override
	protected TemplateContext createContext(ITextViewer contextViewer, IRegion region) {
		TemplateContextType contextType = getContextType(contextViewer, region);
		if (contextType != null) {
			Point selection = contextViewer.getSelectedRange();
			Position position;
			if (selection.y > 0) {
				position = new Position(selection.x, selection.y);
			} else {
				position = new Position(region.getOffset(), region.getLength());
			}

			IDocument document = contextViewer.getDocument();
			return new AntContext(contextType, document, antModel, position);
		}
		return null;
	}

	@Override
	protected ICompletionProposal createProposal(Template template, TemplateContext context, IRegion region, int relevance) {
		AntTemplateProposal proposal = new AntTemplateProposal(template, context, region, getImage(template), relevance);
		proposal.setInformationControlCreator(new AntTemplateInformationControlCreator());
		return proposal;
	}

	protected ISchema getDtd() {
		if (fgDtd == null) {
			IRunnableWithProgress runnable = monitor -> {
				try {
					fgDtd = parseDtd();
				}
				catch (IOException e1) {
					AntUIPlugin.log(e1);
				}
				catch (ParseError e2) {
					AntUIPlugin.log(e2);
				}
			};

			IProgressService service = PlatformUI.getWorkbench().getProgressService();
			try {
				service.busyCursorWhile(runnable);
			}
			catch (InvocationTargetException e) {
				// do nothing
			}
			catch (InterruptedException e) {
				// do nothing
			}
		}
		return fgDtd;
	}

	/**
	 * The provider for all task and attribute descriptions.
	 */
	private TaskDescriptionProvider getDescriptionProvider() {
		return TaskDescriptionProvider.getDefault();
	}

	protected static void resetCodeCompletionDataStructures() {
		fgDtd = null;
		TaskDescriptionProvider.reset();
	}

	@Override
	public void assistSessionStarted(ContentAssistEvent event) {
		IContentAssistant assistant = event.assistant;
		if (assistant instanceof IContentAssistantExtension2) {
			fContentAssistant = (IContentAssistantExtension2) assistant;
		}
	}

	@Override
	public void assistSessionEnded(ContentAssistEvent event) {
		fContentAssistant = null;
		fTemplatesOnly = false;
	}

	@Override
	public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
		// do nothing
	}

	private String getIterationGestureMessage(String showMessage) {
		final IBindingService bindingSvc = PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		TriggerSequence[] triggers = bindingSvc.getActiveBindingsFor(getContentAssistCommand());
		String message;
		if (triggers.length > 0) {
			message = MessageFormat.format(AntEditorMessages.getString("AntEditorCompletionProcessor.63"), new Object[] { triggers[0].format(), //$NON-NLS-1$
					showMessage });
		} else {
			message = MessageFormat.format(AntEditorMessages.getString("AntEditorCompletionProcessor.64"), new Object[] { showMessage }); //$NON-NLS-1$
		}
		return message;
	}

	private ParameterizedCommand getContentAssistCommand() {
		final ICommandService commandSvc = PlatformUI.getWorkbench().getAdapter(ICommandService.class);
		final Command command = commandSvc.getCommand(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		ParameterizedCommand pCmd = new ParameterizedCommand(command, null);
		return pCmd;
	}
}