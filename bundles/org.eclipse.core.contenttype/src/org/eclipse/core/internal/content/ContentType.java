/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
package org.eclipse.core.internal.content;

import java.io.*;
import java.util.*;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.content.*;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public final class ContentType implements IContentType, IContentTypeInfo {

	/* A placeholder for missing/invalid binary/text describers. */
	private static class InvalidDescriber implements IContentDescriber, ITextContentDescriber {
		@Override
		public int describe(InputStream contents, IContentDescription description) {
			return INVALID;
		}

		@Override
		public int describe(Reader contents, IContentDescription description) {
			return INVALID;
		}

		@Override
		public QualifiedName[] getSupportedOptions() {
			return new QualifiedName[0];
		}
	}

	final static byte ASSOCIATED_BY_EXTENSION = 2;
	final static byte ASSOCIATED_BY_NAME = 1;
	private static final String DESCRIBER_ELEMENT = "describer"; //$NON-NLS-1$
	private static ArrayList<FileSpec> EMPTY_LIST = new ArrayList<>(0);
	private static final Object INHERITED_DESCRIBER = "INHERITED DESCRIBER"; //$NON-NLS-1$

	private static final Object NO_DESCRIBER = "NO DESCRIBER"; //$NON-NLS-1$
	final static byte NOT_ASSOCIATED = 0;
	public final static String PREF_DEFAULT_CHARSET = "charset"; //$NON-NLS-1$
	public final static String PREF_FILE_EXTENSIONS = "file-extensions"; //$NON-NLS-1$
	public final static String PREF_FILE_NAMES = "file-names"; //$NON-NLS-1$
	/** @since 3.7 */
	public final static String PREF_FILE_PATTERNS = "file-patterns"; //$NON-NLS-1$
	/** @since 3.6 */
	public static final String PREF_USER_DEFINED = "userDefined"; //$NON-NLS-1$
	/** @since 3.6 */
	public static final String PREF_USER_DEFINED__SEPARATOR = ","; //$NON-NLS-1$
	/** @since 3.6 */
	public static final String PREF_USER_DEFINED__NAME = "name"; //$NON-NLS-1$
	/** @since 3.6 */
	public static final String PREF_USER_DEFINED__BASE_TYPE_ID = "baseTypeId"; //$NON-NLS-1$
	final static byte PRIORITY_HIGH = 1;
	final static byte PRIORITY_LOW = -1;
	final static byte PRIORITY_NORMAL = 0;
	final static int SPEC_PRE_DEFINED = IGNORE_PRE_DEFINED;
	final static int SPEC_USER_DEFINED = IGNORE_USER_DEFINED;
	final static byte STATUS_INVALID = 2;
	final static byte STATUS_UNKNOWN = 0;
	final static byte STATUS_VALID = 1;
	static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private String aliasTargetId;
	private String baseTypeId;
	private boolean builtInAssociations = false;
	private ContentTypeCatalog catalog;
	private IConfigurationElement contentTypeElement;
	private DefaultDescription defaultDescription;
	private Map<QualifiedName, String> defaultProperties;
	private Object describer;
	// we need a Cloneable list
	private ArrayList<FileSpec> fileSpecs = EMPTY_LIST;
	String id;
	private ContentTypeManager manager;
	private String name;
	private byte priority;
	private ContentType target;
	private String userCharset;
	private byte validation = STATUS_UNKNOWN;
	private ContentType baseType;
	// -1 means unknown
	private byte depth = -1;

	public static ContentType createContentType(ContentTypeCatalog catalog, String uniqueId, String name, byte priority,
			String[] fileExtensions, String[] fileNames, String[] filePatterns, String baseTypeId, String aliasTargetId,
			Map<QualifiedName, String> defaultProperties, IConfigurationElement contentTypeElement) {
		ContentType contentType = new ContentType(catalog.getManager());
		contentType.catalog = catalog;
		contentType.defaultDescription = new DefaultDescription(contentType);
		contentType.id = uniqueId;
		contentType.name = name;
		contentType.priority = priority;
		if ((fileExtensions != null && fileExtensions.length > 0) || (fileNames != null && fileNames.length > 0)
				|| (filePatterns != null && filePatterns.length > 0)) {
			contentType.builtInAssociations = true;
			contentType.fileSpecs = new ArrayList<>(fileExtensions.length + fileNames.length + filePatterns.length);
			for (String fileName : fileNames)
				contentType.internalAddFileSpec(fileName, FILE_NAME_SPEC | SPEC_PRE_DEFINED);
			for (String fileExtension : fileExtensions)
				contentType.internalAddFileSpec(fileExtension, FILE_EXTENSION_SPEC | SPEC_PRE_DEFINED);
			for (String fileExtension : filePatterns) {
				contentType.internalAddFileSpec(fileExtension, FILE_PATTERN_SPEC | SPEC_PRE_DEFINED);
			}
		}
		contentType.defaultProperties = defaultProperties;
		contentType.contentTypeElement = contentTypeElement;
		contentType.baseTypeId = baseTypeId;
		contentType.aliasTargetId = aliasTargetId;
		return contentType;
	}

	static FileSpec createFileSpec(String fileSpec, int type) {
		return new FileSpec(fileSpec, type);
	}

	static String getPreferenceKey(int flags) {
		if ((flags & FILE_EXTENSION_SPEC) != 0)
			return PREF_FILE_EXTENSIONS;
		if ((flags & FILE_NAME_SPEC) != 0)
			return PREF_FILE_NAMES;
		if ((flags & FILE_PATTERN_SPEC) != 0)
			return PREF_FILE_PATTERNS;
		throw new IllegalArgumentException("Unknown type: " + flags); //$NON-NLS-1$
	}

	private static String getValidationString(byte validation) {
		return validation == STATUS_VALID ? "VALID" : (validation == STATUS_INVALID ? "INVALID" : "UNKNOWN"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public static void log(String message, Throwable reason) {
		// don't log CoreExceptions again
		IStatus status = new Status(IStatus.ERROR, ContentMessages.OWNER_NAME, 0, message, reason instanceof CoreException ? null : reason);
		RuntimeLog.log(status);
	}

	public ContentType(ContentTypeManager manager) {
		this.manager = manager;
	}

	@Override
	public void addFileSpec(String fileSpec, int type) throws CoreException {
		Assert.isLegal(type == FILE_EXTENSION_SPEC || type == FILE_NAME_SPEC || type == FILE_PATTERN_SPEC,
				"Unknown type: " + type); //$NON-NLS-1$
		String[] userSet;
		synchronized (this) {
			if (!internalAddFileSpec(fileSpec, type | SPEC_USER_DEFINED))
				return;
			userSet = getFileSpecs(type | IGNORE_PRE_DEFINED);
		}
		// persist using preferences
		Preferences contentTypeNode = manager.getPreferences().node(id);
		String newValue = Util.toListString(userSet);
		// we are adding stuff, newValue must be non-null
		Assert.isNotNull(newValue);
		setPreference(contentTypeNode, getPreferenceKey(type), newValue);
		try {
			contentTypeNode.flush();
		} catch (BackingStoreException bse) {
			String message = NLS.bind(ContentMessages.content_errorSavingSettings, id);
			IStatus status = new Status(IStatus.ERROR, ContentMessages.OWNER_NAME, 0, message, bse);
			throw new CoreException(status);
		}
		// notify listeners
		manager.fireContentTypeChangeEvent(this);
	}

	int describe(IContentDescriber selectedDescriber, ILazySource contents, ContentDescription description) throws IOException {
		try {
			return contents.isText() ? ((ITextContentDescriber) selectedDescriber).describe((Reader) contents, description) : selectedDescriber.describe((InputStream) contents, description);
		} catch (RuntimeException re) {
			// describer seems to be buggy. just disable it (logging the reason)
			invalidateDescriber(re);
		} catch (Error e) {
			// describer got some serious problem. disable it (logging the reason) and throw the error again
			invalidateDescriber(e);
			throw e;
		} catch (LowLevelIOException llioe) {
			// throw the actual exception
			throw llioe.getActualException();
		} catch (IOException ioe) {
			// bugs 67841/ 62443  - non-low level IOException should be "ignored"
			if (ContentTypeManager.DebuggingHolder.DEBUGGING) {
				String message = NLS.bind(ContentMessages.content_errorReadingContents, id);
				ContentType.log(message, ioe);
			}
			// we don't know what the describer would say if the exception didn't occur
			return IContentDescriber.INDETERMINATE;
		} finally {
			contents.rewind();
		}
		return IContentDescriber.INVALID;
	}

	@Override
	public boolean equals(Object another) {
		if (another instanceof ContentType)
			return id.equals(((ContentType) another).id);
		if (another instanceof ContentTypeHandler)
			return id.equals(((ContentTypeHandler) another).id);
		return false;
	}

	public String getAliasTargetId() {
		return aliasTargetId;
	}

	@Override
	public IContentType getBaseType() {
		return baseType;
	}

	String getBaseTypeId() {
		return baseTypeId;
	}

	public ContentTypeCatalog getCatalog() {
		return catalog;
	}

	@Override
	public ContentType getContentType() {
		return this;
	}

	@Override
	public String getDefaultCharset() {
		return getDefaultProperty(IContentDescription.CHARSET);
	}

	@Override
	public IContentDescription getDefaultDescription() {
		return defaultDescription;
	}

	/**
	 * Returns the default value for the given property in this content type, or <code>null</code>.
	 */
	@Override
	public String getDefaultProperty(QualifiedName key) {
		String propertyValue = internalGetDefaultProperty(key);
		if ("".equals(propertyValue)) //$NON-NLS-1$
			return null;
		return propertyValue;
	}

	byte getDepth() {
		byte tmpDepth = depth;
		if (tmpDepth >= 0)
			return tmpDepth;
		// depth was never computed - do it now
		if (baseType == null)
			return depth = 0;
		return depth = (byte) (baseType == null ? 0 : (1 + baseType.getDepth()));
	}

	/**
	 * Public for tests only, should not be called by anyone else.
	 */
	public IContentDescriber getDescriber() {
		try {
			// thread safety
			Object tmpDescriber = describer;
			if (tmpDescriber != null) {
				if (INHERITED_DESCRIBER == tmpDescriber)
					return baseType.getDescriber();
				return (NO_DESCRIBER == tmpDescriber) ? null : (IContentDescriber) tmpDescriber;
			}
			final String describerValue = contentTypeElement != null
					? contentTypeElement.getAttribute(DESCRIBER_ELEMENT)
					: null;
			IConfigurationElement[] childrenDescribers = contentTypeElement != null
					? contentTypeElement.getChildren(DESCRIBER_ELEMENT)
					: new IConfigurationElement[0];
			if (describerValue != null || childrenDescribers.length > 0)
				try {
					if ("".equals(describerValue)) { //$NON-NLS-1$
						describer = NO_DESCRIBER;
						return null;
					}
					describer = tmpDescriber = contentTypeElement.createExecutableExtension(DESCRIBER_ELEMENT);
					return (IContentDescriber) tmpDescriber;
				} catch (CoreException ce) {
					// the content type definition was invalid. Ensure we don't
					// try again, and this content type does not accept any
					// contents
					return invalidateDescriber(ce);
				}
		} catch (InvalidRegistryObjectException e) {
			/*
			 * This should only happen if  an API call is made after the registry has changed and before
			 * the corresponding registry change event has been broadcast.
			 */
			// the configuration element is stale - need to rebuild the catalog
			manager.invalidate();
			// bad timing - next time the client asks for a describer, s/he will have better luck
			return null;
		}
		if (baseType == null) {
			describer = NO_DESCRIBER;
			return null;
		}
		// remember so we don't have to come all the way down here next time
		describer = INHERITED_DESCRIBER;
		return baseType.getDescriber();
	}

	@Override
	public IContentDescription getDescriptionFor(InputStream contents, QualifiedName[] options) throws IOException {
		return internalGetDescriptionFor(ContentTypeManager.readBuffer(contents), options);
	}

	@Override
	public IContentDescription getDescriptionFor(Reader contents, QualifiedName[] options) throws IOException {
		return internalGetDescriptionFor(ContentTypeManager.readBuffer(contents), options);
	}

	@Override
	public String[] getFileSpecs(int typeMask) {
		if (fileSpecs.isEmpty())
			return new String[0];
		// invert the last two bits so it is easier to compare
		typeMask ^= (IGNORE_PRE_DEFINED | IGNORE_USER_DEFINED);
		List<String> result = new ArrayList<>(fileSpecs.size());
		for (FileSpec spec : fileSpecs) {
			if ((spec.getType() & typeMask) == spec.getType())
				result.add(spec.getText());
		}
		return result.toArray(new String[result.size()]);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	byte getPriority() {
		return priority;
	}

	@Override
	public IContentTypeSettings getSettings(IScopeContext context) {
		if (context == null || context.equals(manager.getContext()))
			return this;
		return new ContentTypeSettings(this, context);
	}

	/*
	 * Returns the alias target, if one is found, or this object otherwise.
	 */
	ContentType getAliasTarget(boolean self) {
		return (self && target == null) ? this : target;
	}

	byte getValidation() {
		return validation;
	}

	boolean hasBuiltInAssociations() {
		return builtInAssociations;
	}

	boolean hasFileSpec(IScopeContext context, String text, int typeMask) {
		if (context.equals(manager.getContext()) || (typeMask & IGNORE_USER_DEFINED) != 0)
			return hasFileSpec(text, typeMask, false);
		String[] fileSpecs = ContentTypeSettings.getFileSpecs(context, id, typeMask);
		for (String fileSpec : fileSpecs)
			if (text.equalsIgnoreCase(fileSpec))
				return true;
		// no user defined association... try built-in
		return hasFileSpec(text, typeMask | IGNORE_PRE_DEFINED, false);
	}

	/**
	 * Returns whether this content type has the given file spec.
	 *
	 * @param text
	 *            the file spec string
	 * @param typeMask
	 *            FILE_NAME_SPEC or FILE_EXTENSION_SPEC or FILE_REGEXP_SPEC
	 * @param strict
	 * @return true if this file spec has already been added, false otherwise
	 */
	boolean hasFileSpec(String text, int typeMask, boolean strict) {
		if (fileSpecs.isEmpty())
			return false;
		for (FileSpec spec : fileSpecs) {
			if (spec.equals(text, typeMask, strict))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	/**
	 * Adds a user-defined or pre-defined file spec.
	 */
	boolean internalAddFileSpec(String fileSpec, int typeMask) {
		if (hasFileSpec(fileSpec, typeMask, false))
			return false;
		FileSpec newFileSpec = createFileSpec(fileSpec, typeMask);
		if ((typeMask & ContentType.SPEC_USER_DEFINED) == 0) {
			// plug-in defined - all that is left to be done is to add it to the list
			if (fileSpecs.isEmpty())
				fileSpecs = new ArrayList<>(3);
			fileSpecs.add(newFileSpec);
			return true;
		}
		// update file specs atomically so threads traversing the list of file specs don't have to synchronize
		@SuppressWarnings("unchecked")
		ArrayList<FileSpec> tmpFileSpecs = (ArrayList<FileSpec>) fileSpecs.clone();
		tmpFileSpecs.add(newFileSpec);
		catalog.associate(this, newFileSpec.getText(), newFileSpec.getType());
		// set the new file specs atomically
		fileSpecs = tmpFileSpecs;
		return true;
	}

	/**
	 * Returns the default value for a property, recursively if necessary.
	 */
	String internalGetDefaultProperty(QualifiedName key) {
		// a special case for charset - users can override
		if (userCharset != null && key.equals(IContentDescription.CHARSET))
			return userCharset;
		String defaultValue = basicGetDefaultProperty(key);
		if (defaultValue != null)
			return defaultValue;
		// not defined here, try base type
		return baseType == null ? null : baseType.internalGetDefaultProperty(key);
	}

	/**
	 * Returns the value of a built-in property defined for this content type.
	 */
	String basicGetDefaultProperty(QualifiedName key) {
		return defaultProperties == null ? null : defaultProperties.get(key);
	}

	BasicDescription internalGetDescriptionFor(ILazySource buffer, QualifiedName[] options) throws IOException {
		if (buffer == null)
			return defaultDescription;
		// use temporary local var to avoid sync'ing
		IContentDescriber tmpDescriber = this.getDescriber();
		// no describer - return default description
		if (tmpDescriber == null)
			return defaultDescription;
		if (buffer.isText() && !(tmpDescriber instanceof ITextContentDescriber))
			// it is an error to provide a Reader to a non-text content type
			throw new UnsupportedOperationException();
		ContentDescription description = new ContentDescription(options, this);
		if (describe(tmpDescriber, buffer, description) == IContentDescriber.INVALID)
			// the contents were actually invalid for the content type
			return null;
		// the describer didn't add any details, return default description
		if (!description.isSet())
			return defaultDescription;
		// description cannot be changed afterwards
		description.markImmutable();
		return description;
	}

	byte internalIsAssociatedWith(String fileName, IScopeContext context) {
		if (hasFileSpec(context, fileName, FILE_NAME_SPEC))
			return ASSOCIATED_BY_NAME;
		String fileExtension = ContentTypeManager.getFileExtension(fileName);
		if (hasFileSpec(context, fileExtension, FILE_EXTENSION_SPEC))
			return ASSOCIATED_BY_EXTENSION;
		// if does not have built-in file specs, delegate to parent (if any)
		if (!hasBuiltInAssociations() && baseType != null)
			return baseType.internalIsAssociatedWith(fileName, context);
		return NOT_ASSOCIATED;
	}

	boolean internalRemoveFileSpec(String fileSpec, int typeMask) {
		if (fileSpecs.isEmpty())
			return false;
		// we modify the list of file specs atomically so we don't interfere with threads doing traversals
		@SuppressWarnings("unchecked")
		ArrayList<FileSpec> tmpFileSpecs = (ArrayList<FileSpec>) fileSpecs.clone();
		for (Iterator<FileSpec> i = tmpFileSpecs.iterator(); i.hasNext();) {
			FileSpec spec = i.next();
			if ((spec.getType() == typeMask) && fileSpec.equals(spec.getText())) {
				i.remove();
				catalog.dissociate(this, spec.getText(), spec.getType());
				// update the list of file specs
				fileSpecs = tmpFileSpecs;
				return true;
			}
		}
		return false;
	}

	public IContentDescriber invalidateDescriber(Throwable reason) {
		String message = NLS.bind(ContentMessages.content_invalidContentDescriber, id);
		log(message, reason);
		return (IContentDescriber) (describer = new InvalidDescriber());
	}

	boolean isAlias() {
		return target != null;
	}

	@Override
	public boolean isAssociatedWith(String fileName) {
		return isAssociatedWith(fileName, manager.getContext());
	}

	@Override
	public boolean isAssociatedWith(String fileName, IScopeContext context) {
		return internalIsAssociatedWith(fileName, context) != NOT_ASSOCIATED;
	}

	@Override
	public boolean isKindOf(IContentType another) {
		if (another == null)
			return false;
		if (this == another)
			return true;
		return baseType != null && baseType.isKindOf(another);
	}

	boolean isValid() {
		return validation == STATUS_VALID;
	}

	void processPreferences(Preferences contentTypeNode) {
		// user set default charset
		this.userCharset = contentTypeNode.get(PREF_DEFAULT_CHARSET, null);
		// user set file names
		String userSetFileNames = contentTypeNode.get(PREF_FILE_NAMES, null);
		String[] fileNames = Util.parseItems(userSetFileNames);
		for (String fileName : fileNames)
			internalAddFileSpec(fileName, FILE_NAME_SPEC | SPEC_USER_DEFINED);
		// user set file extensions
		String userSetFileExtensions = contentTypeNode.get(PREF_FILE_EXTENSIONS, null);
		String[] fileExtensions = Util.parseItems(userSetFileExtensions);
		for (String fileExtension : fileExtensions)
			internalAddFileSpec(fileExtension, FILE_EXTENSION_SPEC | SPEC_USER_DEFINED);
		// user set file name regexp
		String userSetFileRegexp = contentTypeNode.get(PREF_FILE_PATTERNS, null);
		String[] fileRegexps = Util.parseItems(userSetFileRegexp);
		for (String fileRegexp : fileRegexps) {
			internalAddFileSpec(fileRegexp, FILE_PATTERN_SPEC | SPEC_USER_DEFINED);
		}
	}

	@Override
	public void removeFileSpec(String fileSpec, int type) throws CoreException {
		Assert.isLegal(type == FILE_EXTENSION_SPEC || type == FILE_NAME_SPEC || type == FILE_PATTERN_SPEC,
				"Unknown type: " + type); //$NON-NLS-1$
		synchronized (this) {
			if (!internalRemoveFileSpec(fileSpec, type | SPEC_USER_DEFINED))
				return;
		}
		// persist the change
		Preferences contentTypeNode = manager.getPreferences().node(id);
		final String[] userSet = getFileSpecs(type | IGNORE_PRE_DEFINED);
		String preferenceKey = getPreferenceKey(type);
		String newValue = Util.toListString(userSet);
		setPreference(contentTypeNode, preferenceKey, newValue);
		try {
			contentTypeNode.flush();
		} catch (BackingStoreException bse) {
			String message = NLS.bind(ContentMessages.content_errorSavingSettings, id);
			IStatus status = new Status(IStatus.ERROR, ContentMessages.OWNER_NAME, 0, message, bse);
			throw new CoreException(status);
		}
		// notify listeners
		manager.fireContentTypeChangeEvent(this);
	}

	void setAliasTarget(ContentType newTarget) {
		target = newTarget;
	}

	@Override
	public void setDefaultCharset(String newCharset) throws CoreException {
		synchronized (this) {
			// don't do anything if there is no actual change
			if (userCharset == null) {
				if (newCharset == null)
					return;
			} else if (userCharset.equals(newCharset))
				return;
			// apply change in memory
			userCharset = newCharset;
		}
		// persist the change
		Preferences contentTypeNode = manager.getPreferences().node(id);
		setPreference(contentTypeNode, PREF_DEFAULT_CHARSET, userCharset);
		try {
			contentTypeNode.flush();
		} catch (BackingStoreException bse) {
			String message = NLS.bind(ContentMessages.content_errorSavingSettings, id);
			IStatus status = new Status(IStatus.ERROR, ContentMessages.OWNER_NAME, 0, message, bse);
			throw new CoreException(status);
		}
		// notify listeners
		manager.fireContentTypeChangeEvent(this);
	}

	static void setPreference(Preferences node, String key, String value) {
		if (value == null)
			node.remove(key);
		else
			node.put(key, value);
	}

	void setValidation(byte validation) {
		this.validation = validation;
		if (ContentTypeManager.DebuggingHolder.DEBUGGING)
			ContentMessages.message("Validating " + this + ": " + getValidationString(validation)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public String toString() {
		return id;
	}

	void setBaseType(ContentType baseType) {
		this.baseType = baseType;
	}

	@Override
	public boolean isUserDefined() {
		return this.contentTypeElement == null;
	}

}
