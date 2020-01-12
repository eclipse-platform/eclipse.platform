/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.repo;

import java.util.*;

import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RepositoriesViewContentHandler extends DefaultHandler {

	public static final String REPOSITORIES_VIEW_TAG = "repositories-view"; //$NON-NLS-1$

	public static final String REPOSITORY_TAG = "repository"; //$NON-NLS-1$
	public static final String WORKING_SET_TAG = "working-set"; //$NON-NLS-1$
	public static final String CURRENT_WORKING_SET_TAG = "current-working-set"; //$NON-NLS-1$
	public static final String MODULE_TAG = "module"; //$NON-NLS-1$
	public static final String TAG_TAG = "tag"; //$NON-NLS-1$
	public static final String AUTO_REFRESH_FILE_TAG = "auto-refresh-file"; //$NON-NLS-1$
	public static final String DATE_TAGS_TAG = "date-tags"; //$NON-NLS-1$
	public static final String DATE_TAG_TAG = "date-tag"; //$NON-NLS-1$
	
	public static final String ID_ATTRIBUTE = "id"; //$NON-NLS-1$
	public static final String NAME_ATTRIBUTE = "name"; //$NON-NLS-1$
	public static final String PATH_ATTRIBUTE = "path"; //$NON-NLS-1$
	public static final String FULL_PATH_ATTRIBUTE = "full-path"; //$NON-NLS-1$
	public static final String TYPE_ATTRIBUTE = "type"; //$NON-NLS-1$
	public static final String READ_ID_ATTRIBUTE = "read-id"; //$NON-NLS-1$
	public static final String WRITE_ID_ATTRIBUTE = "write-id"; //$NON-NLS-1$
	public static final String LAST_ACCESS_TIME_ATTRIBUTE = "lastAcessTime"; //$NON-NLS-1$
	
	public static final String[] TAG_TYPES = {"head", "branch", "version", "date"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	public static final String DEFAULT_TAG_TYPE = "version"; //$NON-NLS-1$
	public static final String DEFINED_MODULE_TYPE = "defined"; //$NON-NLS-1$
	
	private RepositoryManager manager;
	private StringBuffer buffer = new StringBuffer();
	private Stack tagStack = new Stack();
	private RepositoryRoot currentRepositoryRoot;
	private String currentRemotePath;
	private List tags;
	private List dateTags;
	private List autoRefreshFiles;
	private boolean ignoreElements;

	private long lastAccessTime;

	public RepositoriesViewContentHandler(RepositoryManager manager) {
		this.manager = manager;
	}
	
	@Override
	public void characters(char[] chars, int startIndex, int length) throws SAXException {
		buffer.append(chars, startIndex, length);
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		
		String elementName = getElementName(namespaceURI, localName, qName);
		if (!elementName.equals(tagStack.peek())) {
			throw new SAXException(NLS.bind(CVSUIMessages.RepositoriesViewContentHandler_unmatchedTag, new String[] { elementName })); 
		}
		
		switch (elementName) {
		case REPOSITORIES_VIEW_TAG:
			// all done
			break;
		case REPOSITORY_TAG:
			if (!ignoreElements) {
				manager.add(currentRepositoryRoot);
			}
			currentRepositoryRoot = null;
			break;
		case WORKING_SET_TAG:
			// This tag is no longer used
			ignoreElements = false;
			break;
		case CURRENT_WORKING_SET_TAG:
			// This tag is no longer used
			ignoreElements = false;
			break;
		case MODULE_TAG:
			if (! ignoreElements && currentRepositoryRoot != null) {
				currentRepositoryRoot.addTags(currentRemotePath,
						(CVSTag[]) tags.toArray(new CVSTag[tags.size()]));
				if (lastAccessTime > 0)
					currentRepositoryRoot.setLastAccessedTime(currentRemotePath, lastAccessTime);
				currentRepositoryRoot.setAutoRefreshFiles(currentRemotePath,
						(String[]) autoRefreshFiles.toArray(new String[autoRefreshFiles.size()]));
			}
			break;
		case DATE_TAG_TAG:
			if (! ignoreElements && currentRepositoryRoot != null) {
				Iterator iter = dateTags.iterator();
				while(iter.hasNext()){
					CVSTag tag = (CVSTag)iter.next();
					currentRepositoryRoot.addDateTag(tag);
				}
			}
			break;
		default:
			break;
		}
		tagStack.pop();
	}
		
	@Override
	public void startElement(
			String namespaceURI,
			String localName,
			String qName,
			Attributes atts)
			throws SAXException {
		
		String elementName = getElementName(namespaceURI, localName, qName);
		switch (elementName) {
		case REPOSITORIES_VIEW_TAG:
			// just started
			break;
		case REPOSITORY_TAG: {
			String id = atts.getValue(ID_ATTRIBUTE);
			if (id == null) {
				throw new SAXException(NLS.bind(CVSUIMessages.RepositoriesViewContentHandler_missingAttribute,
						new String[] { REPOSITORY_TAG, ID_ATTRIBUTE }));
			}
			ICVSRepositoryLocation root;
			try {
				root = KnownRepositories.getInstance().getRepository(id);
				if (!KnownRepositories.getInstance().isKnownRepository(id)) {
					KnownRepositories.getInstance().addRepository(root, false);
				}
			} catch (CVSException e) {
				throw new SAXException(
						NLS.bind(CVSUIMessages.RepositoriesViewContentHandler_errorCreatingRoot, new String[] { id }),
						e);
			}
			currentRepositoryRoot = new RepositoryRoot(root);
			String name = atts.getValue(NAME_ATTRIBUTE);
			if (name != null) {
				currentRepositoryRoot.setName(name);
			}
			break;
		}
		case DATE_TAGS_TAG:
			//prepare to collect date tag
			dateTags = new ArrayList();
			break;
		case DATE_TAG_TAG: {
			String name = atts.getValue(NAME_ATTRIBUTE);
			if (name == null) {
				throw new SAXException(NLS.bind(CVSUIMessages.RepositoriesViewContentHandler_missingAttribute,
						new String[] { DATE_TAGS_TAG, NAME_ATTRIBUTE }));
			}
			dateTags.add(new CVSTag(name, CVSTag.DATE));
			break;
		}
		case WORKING_SET_TAG: {
			String name = atts.getValue(NAME_ATTRIBUTE);
			if (name == null) {
				throw new SAXException(NLS.bind(CVSUIMessages.RepositoriesViewContentHandler_missingAttribute,
						new String[] { WORKING_SET_TAG, NAME_ATTRIBUTE }));
			}
			// Ignore any elements until the corresponding end tag is reached
			ignoreElements = true;
			break;
		}
		case MODULE_TAG: {
			String path = atts.getValue(PATH_ATTRIBUTE);
			if (path == null) {
				throw new SAXException(NLS.bind(CVSUIMessages.RepositoriesViewContentHandler_missingAttribute,
						new String[] { MODULE_TAG, PATH_ATTRIBUTE }));
			}
			String type = atts.getValue(TYPE_ATTRIBUTE);
			if (type != null && type.equals(DEFINED_MODULE_TYPE)) {
				path = RepositoryRoot.asDefinedModulePath(path);
			}
			long cachedTime = 0;
			String cachedTimeString = atts.getValue(LAST_ACCESS_TIME_ATTRIBUTE);
			if (cachedTimeString != null) {
				try {
					Long time = Long.valueOf(cachedTimeString);
					cachedTime = time.longValue();
				} catch (NumberFormatException e) {
					// Ignore
				}
			}
			startModule(path, cachedTime);
			break;
		}
		case TAG_TAG: {
			String type = atts.getValue(TYPE_ATTRIBUTE);
			if (type == null) {
				type = DEFAULT_TAG_TYPE;
			}
			String name = atts.getValue(NAME_ATTRIBUTE);
			if (name == null) {
				throw new SAXException(NLS.bind(CVSUIMessages.RepositoriesViewContentHandler_missingAttribute,
						new String[] { TAG_TAG, NAME_ATTRIBUTE }));
			}
			tags.add(new CVSTag(name, getCVSTagType(type)));
			break;
		}
		case AUTO_REFRESH_FILE_TAG: {
			String path = atts.getValue(FULL_PATH_ATTRIBUTE);
			if (path == null) {
				// get the old path attribute format which was relative to the module
				path = atts.getValue(PATH_ATTRIBUTE);
				if (path == null) {
					throw new SAXException(NLS.bind(CVSUIMessages.RepositoriesViewContentHandler_missingAttribute,
							new String[] { AUTO_REFRESH_FILE_TAG, FULL_PATH_ATTRIBUTE }));
				}
				if (RepositoryRoot.isDefinedModuleName(currentRemotePath)) {
					path = null;
				} else {
					path = new Path(null, currentRemotePath).append(path).toString();
				}
			}
			if (path != null) {
				autoRefreshFiles.add(path);
			}
			break;
		}
		case CURRENT_WORKING_SET_TAG:
			// Ignore any elements until the corresponding end tag is reached
			ignoreElements = true;
			break;
		default:
			break;
		}
		// empty buffer
		buffer = new StringBuffer();
		tagStack.push(elementName);
	}

	private void startModule(String path, long cachedTime) {
		currentRemotePath = path;
		tags = new ArrayList();
		this.lastAccessTime = cachedTime;
		autoRefreshFiles = new ArrayList();
	}
	
	/**
	 * Method getCVSTagType.
	 * @param type
	 */
	public int getCVSTagType(String type) {
		for (int i = 0; i < TAG_TYPES.length; i++) {
			if (TAG_TYPES[i].equals(type))
				return i;
		}
		return CVSTag.VERSION;
	}
	
	/*
	 * Couldn't figure out from the SAX API exactly when localName vs. qName is used.
	 * However, the XML for project sets doesn't use namespaces so either of the two names
	 * is fine. Therefore, use whichever one is provided.
	 */
	private String getElementName(String namespaceURI, String localName, String qName) {
		if (localName != null && localName.length() > 0) {
			return localName;
		} else {
			return qName;
		}
	}
}
