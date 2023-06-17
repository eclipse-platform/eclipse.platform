/*******************************************************************************
 * Copyright (c) 2002, 2015 GEBIT Gesellschaft fuer EDV-Beratung
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
 *     John-Mason P. Shackelford (john-mason.shackelford@pearson.com) - bug 49445
 *     Ericsson AB, Hamdan Msheik - Bug 389564
 *     Ericsson AB, Julian Enoch - Bug 389564
 *     David North - Bug 475839
 *******************************************************************************/

package org.eclipse.ant.internal.ui.model;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.AntImageDescriptor;
import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
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
	 * 
	 * @see #getOffset()
	 */
	protected int fOffset = -1;

	/**
	 * The length of the corresponding source.
	 * 
	 * @see #getLength()
	 */
	protected int fLength = -1;

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
	protected List<IAntElement> fChildNodes = null;

	/**
	 * The (tag-)name of the element.
	 */
	protected String fName;

	/**
	 * Whether this element has been generated as part of an element hierarchy this has problems. This is the severity of the problem.
	 * 
	 * @see XMLProblem#NO_PROBLEM
	 * @see XMLProblem#SEVERITY_ERROR
	 * @see XMLProblem#SEVERITY_WARNING
	 * @see XMLProblem#SEVERITY_FATAL_ERROR
	 */
	private int fProblemSeverity = AntModelProblem.NO_PROBLEM;

	private String fProblemMessage = null;

	/**
	 * The absolute file system path of the file this element is defined within.
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
	private int fIndex = 0;

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

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public String getLabel() {
		return getName();
	}

	@Override
	public List<IAntElement> getChildNodes() {
		return fChildNodes;
	}

	@Override
	public IAntElement getParentNode() {
		return fParent;
	}

	@Override
	public AntProjectNode getProjectNode() {
		IAntElement projectParent = getParentNode();
		while (projectParent != null && !(projectParent instanceof AntProjectNode)) {
			projectParent = projectParent.getParentNode();
		}
		return (AntProjectNode) projectParent;
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
				fChildNodes = new ArrayList<>();
			}
			fChildNodes.add(childElement);
			childElement.setIndex(fChildNodes.size() - 1);
		}
	}

	private void setIndex(int index) {
		fIndex = index;
	}

	protected void setParent(AntElementNode node) {
		fParent = node;
	}

	/**
	 * Sets the absolute file system path of the file this element is defined within.
	 */
	public void setFilePath(String path) {
		if (path == null) {
			return;
		}
		URL url = null;
		try {
			url = new URL(path);
		}
		catch (MalformedURLException e) {
			fFilePath = path;
			return;
		}

		try {
			URL fileURL = FileLocator.toFileURL(url);
			// To be worked in 4.6 via Bug 476266
			if (IAntCoreConstants.FILE.equals(fileURL.getProtocol())) {
				fFilePath = IPath.fromOSString((URIUtil.toFile(URIUtil.toURI(fileURL))).getAbsolutePath()).toString();
			}
		}
		catch (URISyntaxException e) {
			AntUIPlugin.log(e);
		}
		catch (IOException e) {
			AntUIPlugin.log(e);
		}
	}

	/**
	 * Returns the absolute file system path of the file this element is defined within. Only relevant for nodes that are external
	 * 
	 * @see #isExternal()
	 */
	public String getFilePath() {
		return fFilePath;
	}

	@Override
	public int getOffset() {
		return fOffset;
	}

	@Override
	public void setOffset(int anOffset) {
		fOffset = anOffset;
	}

	@Override
	public int getLength() {
		return fLength;
	}

	@Override
	public void setLength(int aLength) {
		fLength = aLength;
		if (fProblem != null && fProblem instanceof AntModelProblem) {
			((AntModelProblem) fProblem).setLength(aLength);
		}
	}

	/**
	 * Returns a string representation of this element.
	 */
	@Override
	public String toString() {
		return "Ant Element Node: " + getLabel() + " Offset: " + getOffset() + " Length: " + getLength(); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public boolean isErrorNode() {
		return fProblemSeverity == AntModelProblem.SEVERITY_ERROR || fProblemSeverity == AntModelProblem.SEVERITY_FATAL_ERROR;
	}

	@Override
	public boolean isWarningNode() {
		return fProblemSeverity == AntModelProblem.SEVERITY_WARNING;
	}

	@Override
	public void setProblemSeverity(int severity) {
		fProblemSeverity = severity;
	}

	@Override
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
	 * Returns a unique string representation of this element. The format of the string is not specified.
	 * 
	 * @return the string representation
	 */
	@Override
	public String getElementPath() {
		if (fElementPath == null) {
			StringBuilder buffer = new StringBuilder();
			String buildFileName = getProjectNode().getBuildFileName();
			if (buildFileName != null) {
				buffer.append(buildFileName);
			}
			buffer.append(getParentNode() != null ? getParentNode().getElementPath() : IAntCoreConstants.EMPTY_STRING);
			buffer.append('/');
			buffer.append(getElementIdentifier());
			buffer.append('[');
			buffer.append(fIndex);
			buffer.append(']');

			fElementPath = buffer.toString();
		}
		return fElementPath;
	}

	private String getElementIdentifier() {
		if (fElementIdentifier == null) {
			StringBuffer buffer = escape(new StringBuffer(getName() != null ? getName() : IAntCoreConstants.EMPTY_STRING), '\\', "$/[]\\"); //$NON-NLS-1$
			buffer.append('$');
			buffer.append(escape(new StringBuffer(getLabel() != null ? getLabel() : IAntCoreConstants.EMPTY_STRING), '\\', "$/[]\\").toString()); //$NON-NLS-1$

			fElementIdentifier = buffer.toString();
		}
		return fElementIdentifier;
	}

	private StringBuffer escape(StringBuffer sb, char esc, String special) {
		for (int i = 0; i < sb.length(); i++) {
			if (special.indexOf(sb.charAt(i)) >= 0) {
				sb.insert(i++, esc);
			}
		}

		return sb;
	}

	@Override
	public boolean equals(Object o2) {
		Object o1 = this;

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

		AntElementNode e1 = (AntElementNode) o1;
		AntElementNode e2 = (AntElementNode) o2;

		return e1.getElementPath().equals(e2.getElementPath());
	}

	@Override
	public int hashCode() {
		return getElementPath().hashCode();
	}

	@Override
	public int getSelectionLength() {
		return fSelectionLength;
	}

	public void setSelectionLength(int selectionLength) {
		this.fSelectionLength = selectionLength;
	}

	@Override
	public AntElementNode getNode(int sourceOffset) {
		synchronized (this) {
			if (fChildNodes != null) {
				for (IAntElement node : fChildNodes) {
					AntElementNode containingNode = node.getNode(sourceOffset);
					if (containingNode != null) {
						return containingNode;
					}
				}
			}
		}
		if (fLength == -1 && fOffset <= sourceOffset && !isExternal()) { // this is still an open element
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
		if (fImportNode != null || isExternal()) {
			flags = flags | AntImageDescriptor.IMPORTED;
		}
		ImageDescriptor base = getBaseImageDescriptor();
		return AntUIImages.getImage(new AntImageDescriptor(base, flags));
	}

	protected ImageDescriptor getBaseImageDescriptor() {
		return AntUIImages.getImageDescriptor(IAntUIConstants.IMG_TASK_PROPOSAL);
	}

	protected IAntModel getAntModel() {
		IAntElement parentNode = getParentNode();
		while (!(parentNode instanceof AntProjectNode)) {
			parentNode = parentNode.getParentNode();
		}
		if (parentNode instanceof AntProjectNode) {
			return ((AntProjectNode) parentNode).getAntModel();
		}
		return null;
	}

	@Override
	public void setProblem(IProblem problem) {
		fProblem = problem;
	}

	/**
	 * @return
	 */
	public IProblem getProblem() {
		return fProblem;
	}

	protected void appendEntityName(StringBuffer displayName) {
		String path = getFilePath();

		if (getImportNode() != null) {
			displayName.append(MessageFormat.format(AntModelMessages.AntElementNode_9, new Object[] { getImportNode().getLabel() }));
		} else {
			String entityName = getAntModel().getEntityName(path);
			displayName.append(MessageFormat.format(AntModelMessages.AntElementNode_9, new Object[] { entityName }));
		}
	}

	@Override
	public AntElementNode getImportNode() {
		return fImportNode;
	}

	public void setImportNode(AntElementNode importNode) {
		fImportNode = importNode;
	}

	@Override
	public boolean hasChildren() {
		if (fChildNodes == null) {
			return false;
		}
		return !fChildNodes.isEmpty();
	}

	public void reset() {
		fChildNodes = null;
	}

	public void setExternalInfo(int line, int column) {
		fLine = line;
		fColumn = column;
	}

	public int[] getExternalInfo() {
		return new int[] { fLine, fColumn };
	}

	/**
	 * Return the resource that contains the definition of this Ant node.
	 * 
	 * @return The resource that contains the definition of this ant node or <code>null</code> if that resource could not be determined (a buildfile
	 *         that is external to the workspace).
	 */
	public IFile getIFile() {
		if (isExternal()) {
			return AntUtil.getFileForLocation(fFilePath, null);
		}
		return getBuildFileResource();
	}

	/**
	 * Return the resource that is the main build file for this Ant node.
	 * 
	 * @return The resource that is the main buildfile for this ant node or <code>null</code> if that resource could not be determined (a buildfile
	 *         that is external to the workspace).
	 */
	public IFile getBuildFileResource() {
		LocationProvider locationProvider = getAntModel().getLocationProvider();
		return locationProvider.getFile();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	/**
	 * Returns whether this node is a structural node that should be shown in the buildfile outline. For example, an AntCommentNode would return
	 * <code>false</code>
	 * 
	 * @return whether this node is a structural node that should be shown in the buildfile outline
	 */
	public boolean isStructuralNode() {
		return true;
	}

	@Override
	public boolean collapseProjection() {
		return false;
	}

	public void dispose() {
		getAntModel().dispose();
	}

	/**
	 * Returns the name or path of the element referenced at the offset within the declaration of this node or <code>null</code> if no element is
	 * referenced at the offset
	 * 
	 * @param offset
	 *            The offset within the declaration of this node
	 * @return <code>null</code> or the name or path of the referenced element
	 */
	public String getReferencedElement(int offset) {
		return null;
	}

	@Override
	public String getProblemMessage() {
		return fProblemMessage;
	}

	@Override
	public void setProblemMessage(String problemMessage) {
		fProblemMessage = problemMessage;
	}

	@Override
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
	 * Returns whether the supplied region can be considered as an area in this node containing a reference.
	 * 
	 * @param region
	 *            the area to consider for finding a reference
	 * @return whether a reference could exist in this node from the supplied region
	 */
	public boolean isRegionPotentialReference(IRegion region) {
		return region.getOffset() >= fOffset;
	}

	@Override
	public List<Integer> computeIdentifierOffsets(String identifier) {
		return null;
	}

	/**
	 * Returns whether the supplied region is from within this node's declaration identifier area
	 * 
	 * @param region
	 *            The region to check
	 * @return whether the region is from within this node and is the declaration of a reference.
	 */
	public boolean isFromDeclaration(IRegion region) {
		return false;
	}

	protected boolean checkReferenceRegion(IRegion region, String textToSearch, String attributeName) {
		int attributeOffset = textToSearch.indexOf(attributeName);
		while (attributeOffset > 0 && !Character.isWhitespace(textToSearch.charAt(attributeOffset - 1))) {
			attributeOffset = textToSearch.indexOf(attributeName, attributeOffset + 1);
		}
		if (attributeOffset != -1) {
			attributeOffset += attributeName.length();
			int attributeOffsetEnd = textToSearch.indexOf('"', attributeOffset);
			attributeOffsetEnd = textToSearch.indexOf('"', attributeOffsetEnd + 1);
			return region.getOffset() >= getOffset() + attributeOffset
					&& (region.getOffset() + region.getLength()) <= getOffset() + attributeOffsetEnd;
		}
		return false;
	}
}
