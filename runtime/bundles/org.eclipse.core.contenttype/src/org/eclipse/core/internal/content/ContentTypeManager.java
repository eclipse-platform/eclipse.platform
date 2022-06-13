/*******************************************************************************
 *  Copyright (c) 2004, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mickael Istria (Red Hat Inc.) - Bug 485227
 *******************************************************************************/
package org.eclipse.core.internal.content;

import java.io.InputStream;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.content.*;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;

public class ContentTypeManager extends ContentTypeMatcher implements IContentTypeManager {
	private static class ContentTypeRegistryChangeListener implements IRegistryChangeListener {
		@Override
		public void registryChanged(IRegistryChangeEvent event) {
			// no changes related to the content type registry
			if (event.getExtensionDeltas(IContentConstants.RUNTIME_NAME, ContentTypeBuilder.PT_CONTENTTYPES).length == 0
					&& event.getExtensionDeltas(IContentConstants.CONTENT_NAME,
							ContentTypeBuilder.PT_CONTENTTYPES).length == 0)
				return;
			getInstance().invalidate();
		}
	}

	private static IRegistryChangeListener runtimeExtensionListener = new ContentTypeRegistryChangeListener();
	private static IRegistryChangeListener contentExtensionListener = new ContentTypeRegistryChangeListener();

	private static volatile ContentTypeManager instance;

	public static final int BLOCK_SIZE = 0x400;
	public static final String CONTENT_TYPE_PREF_NODE = IContentConstants.RUNTIME_NAME + IPath.SEPARATOR + "content-types"; //$NON-NLS-1$
	private static final String OPTION_DEBUG_CONTENT_TYPES = "org.eclipse.core.contenttype/debug"; //$NON-NLS-1$
	private ContentTypeCatalog catalog;
	private int catalogGeneration;

	/**
	 * The DebuggingHolder contains a single boolean with a
	 * {@link DebuggingHolder#DEBUGGING} field. By providing a class that wraps it,
	 * it will defer the lookup of this field until it is needed.
	 */
	static class DebuggingHolder {
		static final boolean DEBUGGING;
		static {
			boolean[] debugging = { false };
			ServiceCaller.callOnce(DebuggingHolder.class, DebugOptions.class,
					debugOptions -> {
						debugging[0] = debugOptions.getBooleanOption(OPTION_DEBUG_CONTENT_TYPES, false);
					});
			DEBUGGING = debugging[0];
		}
	}

	/**
	 * List of registered listeners (element type:
	 * <code>IContentTypeChangeListener</code>).
	 * These listeners are to be informed when
	 * something in a content type changes.
	 */
	protected final ListenerList<IContentTypeChangeListener> contentTypeListeners = new ListenerList<>();


	public void addRegistryChangeListener(IExtensionRegistry registry) {
		if (registry == null)
			return;
		// Different instances of listener required. See documentation of
		// IExtensionRegistry.addRegistryChangeListener(IRegistryChangeListener, String).
		registry.addRegistryChangeListener(runtimeExtensionListener, IContentConstants.RUNTIME_NAME);
		registry.addRegistryChangeListener(contentExtensionListener, IContentConstants.CONTENT_NAME);
	}

	/**
	 * Shuts down the platform's content type manager. After this call returns,
	 * the content type manager will be closed for business.
	 */
	public static void shutdown() {
		// there really is nothing left to do except null the instance.
		instance = null;
	}

	public void removeRegistryChangeListener(IExtensionRegistry registry) {
		if (registry == null)
			return;
		getInstance().invalidate();
		registry.removeRegistryChangeListener(runtimeExtensionListener);
		registry.removeRegistryChangeListener(contentExtensionListener);
	}

	/**
	 * Obtains this platform's content type manager. Lazyly creates and initializes
	 * the platform's content type manager.
	 *
	 *
	 * @return the content type manager
	 */
	public static ContentTypeManager getInstance() {
		if (instance == null) {
			synchronized (ContentTypeManager.class) {
				if (instance == null) {
					instance = new ContentTypeManager();
				}
			}
		}
		return instance;
	}

	/*
	 * Returns the extension for a file name (omitting the leading '.').
	 */
	static String getFileExtension(String fileName) {
		int dotPosition = fileName.lastIndexOf('.');
		return (dotPosition == -1 || dotPosition == fileName.length() - 1) ? "" : fileName.substring(dotPosition + 1); //$NON-NLS-1$
	}

	protected static ILazySource readBuffer(InputStream contents) {
		return new LazyInputStream(contents, BLOCK_SIZE);
	}

	protected static ILazySource readBuffer(Reader contents) {
		return new LazyReader(contents, BLOCK_SIZE);
	}

	public ContentTypeManager() {
		super(null, InstanceScope.INSTANCE);
		instance = this;
	}

	protected ContentTypeBuilder createBuilder(ContentTypeCatalog newCatalog) {
		return new ContentTypeBuilder(newCatalog);
	}

	@Override
	public IContentType[] getAllContentTypes() {
		ContentTypeCatalog currentCatalog = getCatalog();
		IContentType[] types = currentCatalog.getAllContentTypes();
		IContentType[] result = new IContentType[types.length];
		int generation = currentCatalog.getGeneration();
		for (int i = 0; i < result.length; i++)
			result[i] = new ContentTypeHandler((ContentType) types[i], generation);
		return result;
	}

	protected synchronized ContentTypeCatalog getCatalog() {
		if (catalog != null)
			// already has one
			return catalog;
		// create new catalog
		ContentTypeCatalog newCatalog = new ContentTypeCatalog(this, catalogGeneration++);
		// build catalog by parsing the extension registry
		ContentTypeBuilder builder = createBuilder(newCatalog);
		try {
			builder.buildCatalog(getContext());
			// only remember catalog if building it was successful
			catalog = newCatalog;
		} catch (InvalidRegistryObjectException e) {
			// the registry has stale objects... just don't remember the returned (incomplete) catalog
		}
		newCatalog.organize();
		return newCatalog;
	}

	@Override
	public IContentType getContentType(String contentTypeIdentifier) {
		ContentTypeCatalog currentCatalog = getCatalog();
		ContentType type = currentCatalog.getContentType(contentTypeIdentifier);
		return type == null ? null : new ContentTypeHandler(type, currentCatalog.getGeneration());
	}

	@Override
	public IContentTypeMatcher getMatcher(final ISelectionPolicy customPolicy, final IScopeContext context) {
		return new ContentTypeMatcher(customPolicy, context == null ? getContext() : context);
	}

	IEclipsePreferences getPreferences() {
		return getPreferences(getContext());
	}

	IEclipsePreferences getPreferences(IScopeContext context) {
		return context.getNode(CONTENT_TYPE_PREF_NODE);
	}

	/**
	 * Causes a new catalog to be built afresh next time an API call is made.
	 */
	synchronized void invalidate() {
		if (DebuggingHolder.DEBUGGING && catalog != null)
			ContentMessages.message("Registry discarded"); //$NON-NLS-1$
		catalog = null;
	}

	@Override
	public void addContentTypeChangeListener(IContentTypeChangeListener listener) {
		contentTypeListeners.add(listener);
	}

	@Override
	public void removeContentTypeChangeListener(IContentTypeChangeListener listener) {
		contentTypeListeners.remove(listener);
	}

	public void fireContentTypeChangeEvent(IContentType type) {
		IContentType eventObject = type;
		if (type instanceof ContentType) {
			eventObject = new ContentTypeHandler((ContentType) type, ((ContentType) type).getCatalog().getGeneration());
		} else {
			eventObject = type;
		}
		for (final IContentTypeChangeListener listener : this.contentTypeListeners) {
			final ContentTypeChangeEvent event = new ContentTypeChangeEvent(eventObject);
			ISafeRunnable job = new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					// already logged in SafeRunner#run()
				}

				@Override
				public void run() throws Exception {
					listener.contentTypeChanged(event);
				}
			};
			SafeRunner.run(job);
		}
	}

	@Override
	public IContentDescription getSpecificDescription(BasicDescription description) {
		// this is the platform content type manager, no specificities
		return description;
	}

	@Override
	public final void removeContentType(String contentTypeIdentifier) throws CoreException {
		if (contentTypeIdentifier == null) {
			return;
		}
		IContentType contentType = getContentType(contentTypeIdentifier);
		if (contentType == null) {
			return;
		}
		if (!contentType.isUserDefined()) {
			throw new IllegalArgumentException("Can only delete content-types defined by users."); //$NON-NLS-1$
		}
		getCatalog().removeContentType(contentType.getId());
		// Remove preferences for this content type.
		List<String> userDefinedIds = new ArrayList<>(Arrays.asList(getUserDefinedContentTypeIds()));
		userDefinedIds.remove(contentType.getId());
		getContext().getNode(ContentType.PREF_USER_DEFINED).put(ContentType.PREF_USER_DEFINED,
				userDefinedIds.stream().collect(Collectors.joining(ContentType.PREF_USER_DEFINED__SEPARATOR)));
		try {
			getContext().getNode(ContentType.PREF_USER_DEFINED).flush();
		} catch (BackingStoreException e) {
			String message = NLS.bind(ContentMessages.content_errorSavingSettings, contentType.getId());
			IStatus status = new Status(IStatus.ERROR, ContentMessages.OWNER_NAME, 0, message, e);
			throw new CoreException(status);
		}
		getCatalog().organize();
		fireContentTypeChangeEvent(contentType);
	}

	@Override
	public final IContentType addContentType(String id, String name, IContentType baseType) throws CoreException {
		if (id == null) {
			throw new IllegalArgumentException("Content-type 'id' mustn't be null");//$NON-NLS-1$
		}
		if (id.contains(ContentType.PREF_USER_DEFINED__SEPARATOR)) {
			throw new IllegalArgumentException(
					"Content-Type id mustn't contain '" + ContentType.PREF_USER_DEFINED__SEPARATOR + '\''); //$NON-NLS-1$
		}
		if (getContentType(id) != null) {
			throw new IllegalArgumentException("Content-type '" + id + "' already exists.");//$NON-NLS-1$ //$NON-NLS-2$
		}
		ContentType contentType = ContentType.createContentType(getCatalog(), id, name, (byte) 0, new String[0],
				new String[0], new String[0], baseType != null ? baseType.getId() : null, null, null, null);
		getCatalog().addContentType(contentType);
		// Add preferences for this content type.
		String currentUserDefined = getContext().getNode(ContentType.PREF_USER_DEFINED)
				.get(ContentType.PREF_USER_DEFINED, ContentType.EMPTY_STRING);
		if (!currentUserDefined.isEmpty()) {
			currentUserDefined += ContentType.PREF_USER_DEFINED__SEPARATOR;
		}
		getContext().getNode(ContentType.PREF_USER_DEFINED).put(ContentType.PREF_USER_DEFINED, currentUserDefined + id);
		contentType.setValidation(ContentType.STATUS_UNKNOWN);
		IEclipsePreferences contextTypeNode = getContext().getNode(contentType.getId());
		contextTypeNode.put(ContentType.PREF_USER_DEFINED__NAME, name);
		if (baseType != null) {
			contextTypeNode.put(ContentType.PREF_USER_DEFINED__BASE_TYPE_ID, baseType.getId());
		}
		try {
			getContext().getNode(ContentType.PREF_USER_DEFINED).flush();
			contextTypeNode.flush();
		} catch (BackingStoreException e) {
			String message = NLS.bind(ContentMessages.content_errorSavingSettings, id);
			IStatus status = new Status(IStatus.ERROR, ContentMessages.OWNER_NAME, 0, message, e);
			throw new CoreException(status);
		}
		getCatalog().organize();
		fireContentTypeChangeEvent(contentType);
		return contentType;
	}

	private String[] getUserDefinedContentTypeIds() {
		return getUserDefinedContentTypeIds(getContext());
	}

	static String[] getUserDefinedContentTypeIds(IScopeContext context) {
		String ids = context.getNode(ContentType.PREF_USER_DEFINED)
				.get(ContentType.PREF_USER_DEFINED, ContentType.EMPTY_STRING);
		if (ids.isEmpty()) {
			return new String[0];
		}
		return ids.split(ContentType.PREF_USER_DEFINED__SEPARATOR);
	}
}
