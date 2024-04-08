/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Carsten Pfeiffer <carsten.pfeiffer@gebit.de> - CompareUIPlugin.getCommonType() returns null if left or right side is not available - https://bugs.eclipse.org/311843
 *     Stefan Xenos <sxenos@gmail.com> (Google) - bug 448968 - Add diagnostic logging
 *     Stefan Dirix <sdirix@eclipsesource.com> - bug 473847: Minimum E4 Compatibility of Compare
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.IStreamMerger;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.core.CompareSettings;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IStructureCreator;
import org.eclipse.compare.structuremergeviewer.StructureDiffViewer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * The Compare UI plug-in defines the entry point to initiate a configurable
 * compare operation on arbitrary resources. The result of the compare
 * is opened into a compare editor where the details can be browsed and
 * edited in dynamically selected structure and content viewers.
 * <p>
 * The Compare UI provides a registry for content and structure compare viewers,
 * which is initialized from extensions contributed to extension points
 * declared by this plug-in.
 * <p>
 * This class is the plug-in runtime class for the
 * <code>"org.eclipse.compare"</code> plug-in.
 * </p>
 */
public final class CompareUIPlugin extends AbstractUIPlugin {

	static class CompareRegistry<T> {
		private final static String ID_ATTRIBUTE= "id"; //$NON-NLS-1$
		private final static String EXTENSIONS_ATTRIBUTE= "extensions"; //$NON-NLS-1$
		private final static String CONTENT_TYPE_ID_ATTRIBUTE= "contentTypeId"; //$NON-NLS-1$

		private HashMap<String, T> fIdMap;	// maps ids to data
		private HashMap<String, List<T>> fExtensionMap;	// multimap: maps extensions to list of data
		private HashMap<IContentType, List<T>> fContentTypeBindings; // multimap: maps content type bindings to list of data


		void register(IConfigurationElement element, T data) {
			String id= element.getAttribute(ID_ATTRIBUTE);
			if (id != null) {
				if (fIdMap == null)
					fIdMap= new HashMap<>();
				fIdMap.put(id, data);
			}

			String types= element.getAttribute(EXTENSIONS_ATTRIBUTE);
			if (types != null) {
				if (fExtensionMap == null)
					fExtensionMap= new HashMap<>();
				StringTokenizer tokenizer= new StringTokenizer(types, ","); //$NON-NLS-1$
				while (tokenizer.hasMoreElements()) {
					String extension= tokenizer.nextToken().trim();
					List<T> l = fExtensionMap.get(normalizeCase(extension));
					if (l == null)
						fExtensionMap.put(normalizeCase(extension),	l = new ArrayList<>());
					l.add(data);
				}
			}
		}

		void createBinding(IConfigurationElement element, String idAttributeName) {
			String type= element.getAttribute(CONTENT_TYPE_ID_ATTRIBUTE);
			String id= element.getAttribute(idAttributeName);
			if (id == null)
				logErrorMessage(Utilities.getFormattedString("CompareUIPlugin.targetIdAttributeMissing", idAttributeName)); //$NON-NLS-1$
			if (type != null && id != null && fIdMap != null) {
				T o= fIdMap.get(id);
				if (o != null) {
					IContentType ct= fgContentTypeManager.getContentType(type);
					if (ct != null) {
						if (fContentTypeBindings == null)
							fContentTypeBindings= new HashMap<>();
						List<T> l = fContentTypeBindings.get(ct);
						if (l == null)
							fContentTypeBindings.put(ct, l = new ArrayList<>());
						l.add(o);
					} else {
						logErrorMessage(Utilities.getFormattedString("CompareUIPlugin.contentTypeNotFound", type)); //$NON-NLS-1$
					}
				} else {
					logErrorMessage(Utilities.getFormattedString("CompareUIPlugin.targetNotFound", id)); //$NON-NLS-1$
				}
			}
		}

		T search(IContentType type) {
			List<T> list = searchAll(type);
			return list != null ? list.get(0) : null;
		}

		List<T> searchAll(IContentType type) {
			if (fContentTypeBindings != null) {
				for (; type != null; type= type.getBaseType()) {
					List<T> data= fContentTypeBindings.get(type);
					if (data != null)
						return data;
				}
			}
			return null;
		}

		T search(String extension) {
			List<T> list = searchAll(extension);
			return list != null ? list.get(0) : null;
		}

		List<T> searchAll(String extension) {
			if (fExtensionMap != null)
				return fExtensionMap.get(normalizeCase(extension));
			return null;
		}

		Collection<T> getAll() {
			return fIdMap == null ? Collections.emptySet() : fIdMap.values();
		}
	}

	/** Status code describing an internal error */
	public static final int INTERNAL_ERROR= 1;

	private static boolean NORMALIZE_CASE= true;

	public static final String PLUGIN_ID= "org.eclipse.compare"; //$NON-NLS-1$

	private static final String BINARY_TYPE= "binary"; //$NON-NLS-1$

	private static final String STREAM_MERGER_EXTENSION_POINT= "streamMergers"; //$NON-NLS-1$
	private static final String STREAM_MERGER= "streamMerger"; //$NON-NLS-1$
	private static final String STREAM_MERGER_ID_ATTRIBUTE= "streamMergerId"; //$NON-NLS-1$
	private static final String STRUCTURE_CREATOR_EXTENSION_POINT= "structureCreators"; //$NON-NLS-1$
	private static final String STRUCTURE_CREATOR= "structureCreator"; //$NON-NLS-1$
	private static final String STRUCTURE_CREATOR_ID_ATTRIBUTE= "structureCreatorId"; //$NON-NLS-1$

	private static final String VIEWER_TAG= "viewer"; //$NON-NLS-1$
	private static final String FILTER_TAG = "filter"; //$NON-NLS-1$
	private static final String STRUCTURE_MERGE_VIEWER_EXTENSION_POINT= "structureMergeViewers"; //$NON-NLS-1$
	private static final String STRUCTURE_MERGE_VIEWER_ID_ATTRIBUTE= "structureMergeViewerId"; //$NON-NLS-1$
	private static final String CONTENT_MERGE_VIEWER_EXTENSION_POINT= "contentMergeViewers"; //$NON-NLS-1$
	private static final String COMPARE_FILTER_EXTENTION_POINT = "compareFilters"; //$NON-NLS-1$
	private static final String COMPARE_FILTER_ID_ATTRIBUTE = "filterId"; //$NON-NLS-1$
	private static final String CONTENT_MERGE_VIEWER_ID_ATTRIBUTE= "contentMergeViewerId"; //$NON-NLS-1$
	private static final String CONTENT_VIEWER_EXTENSION_POINT= "contentViewers"; //$NON-NLS-1$
	private static final String CONTENT_VIEWER_ID_ATTRIBUTE= "contentViewerId"; //$NON-NLS-1$

	private static final String CONTENT_TYPE_BINDING= "contentTypeBinding"; //$NON-NLS-1$


	private static final String COMPARE_EDITOR= PLUGIN_ID + ".CompareEditor"; //$NON-NLS-1$

	private static final String STRUCTUREVIEWER_ALIASES_PREFERENCE_NAME= "StructureViewerAliases";	//$NON-NLS-1$

	// content type
	private static final IContentTypeManager fgContentTypeManager= Platform.getContentTypeManager();

	public static final int NO_DIFFERENCE = 10000;

	/**
	 * The plugin singleton.
	 */
	private static CompareUIPlugin fgComparePlugin;

	/** Maps type to icons */
	private static Map<String, Image> fgImages= new Hashtable<>(10);
	/** Maps type to ImageDescriptors */
	private static Map<String, ImageDescriptor> fgImageDescriptors= new Hashtable<>(10);
	/** Maps ImageDescriptors to Images */
	private static Map<ImageDescriptor, Image> fgImages2= new Hashtable<>(10);

	private static List<Image> fgDisposeOnShutdownImages= new ArrayList<>();

	private ResourceBundle fResourceBundle;

	private boolean fRegistriesInitialized;
	private final CompareRegistry<StreamMergerDescriptor> fStreamMergers= new CompareRegistry<>();
	private final CompareRegistry<StructureCreatorDescriptor> fStructureCreators= new CompareRegistry<>();
	private final CompareRegistry<ViewerDescriptor> fStructureMergeViewers= new CompareRegistry<>();
	private final CompareRegistry<ViewerDescriptor> fContentViewers= new CompareRegistry<>();
	private final CompareRegistry<ViewerDescriptor> fContentMergeViewers= new CompareRegistry<>();
	private final CompareRegistry<CompareFilterDescriptor> fCompareFilters = new CompareRegistry<>();

	private Map<String, String> fStructureViewerAliases;
	private CompareResourceFilter fFilter;
	private IPropertyChangeListener fPropertyChangeListener;

	private ServiceRegistration<DebugOptionsListener> debugRegistration;

	/**
	 * Creates the <code>CompareUIPlugin</code> object and registers all
	 * structure creators, content merge viewers, and structure merge viewers
	 * contributed to this plug-in's extension points.
	 * <p>
	 * Note that instances of plug-in runtime classes are automatically created
	 * by the platform in the course of plug-in activation.
	 */
	public CompareUIPlugin() {
		super();
		Assert.isTrue(fgComparePlugin == null);
		fgComparePlugin= this;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		Hashtable<String, String> properties = new Hashtable<>(2);
		properties.put(DebugOptions.LISTENER_SYMBOLICNAME, PLUGIN_ID);
		debugRegistration = context.registerService(DebugOptionsListener.class, Policy.DEBUG_OPTIONS_LISTENER,
				properties);

		CompareSettings.getDefault().setCappingDisabled(
				getPreferenceStore().getBoolean(
						ComparePreferencePage.CAPPING_DISABLED));
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		IPreferenceStore ps= getPreferenceStore();
		rememberAliases(ps);
		if (fPropertyChangeListener != null) {
			ps.removePropertyChangeListener(fPropertyChangeListener);
			fPropertyChangeListener= null;
		}

		super.stop(context);

		if (fgDisposeOnShutdownImages != null) {
			for (Image img : fgDisposeOnShutdownImages) {
				if (!img.isDisposed())
					img.dispose();
			}
			fgImages= null;
		}

		if (debugRegistration != null) {
			debugRegistration.unregister();
			debugRegistration = null;
		}
	}

	/**
	 * Returns the singleton instance of this plug-in runtime class.
	 *
	 * @return the compare plug-in instance
	 */
	public static CompareUIPlugin getDefault() {
		return fgComparePlugin;
	}

	/**
	 * Returns this plug-in's resource bundle.
	 *
	 * @return the plugin's resource bundle
	 */
	public ResourceBundle getResourceBundle() {
		if (fResourceBundle == null)
			fResourceBundle= Platform.getResourceBundle(getBundle());
		return fResourceBundle;
	}

	/**
	 * Returns this plug-in's unique identifier.
	 *
	 * @return the plugin's unique identifier
	 */
	public static String getPluginId() {
		return getDefault().getBundle().getSymbolicName();
	}

	private void initializeRegistries() {
		if (!fRegistriesInitialized) {
			registerExtensions();
			fRegistriesInitialized= true;
		}
	}

	/**
	 * Registers all stream mergers, structure creators, content merge viewers, and structure merge viewers
	 * that are found in the XML plugin files.
	 */
	private void registerExtensions() {
		IExtensionRegistry registry= Platform.getExtensionRegistry();

		// collect all IStreamMergers
		IConfigurationElement[] elements= registry.getConfigurationElementsFor(PLUGIN_ID, STREAM_MERGER_EXTENSION_POINT);
		for (IConfigurationElement element : elements) {
			if (STREAM_MERGER.equals(element.getName()))
				fStreamMergers.register(element, new StreamMergerDescriptor(element));
		}
		for (IConfigurationElement element : elements) {
			if (CONTENT_TYPE_BINDING.equals(element.getName()))
				fStreamMergers.createBinding(element, STREAM_MERGER_ID_ATTRIBUTE);
		}

		// collect all IStructureCreators
		elements= registry.getConfigurationElementsFor(PLUGIN_ID, STRUCTURE_CREATOR_EXTENSION_POINT);
		for (IConfigurationElement element : elements) {
			String name= element.getName();
			if (!CONTENT_TYPE_BINDING.equals(name)) {
				if (!STRUCTURE_CREATOR.equals(name))
					logErrorMessage(Utilities.getFormattedString("CompareUIPlugin.unexpectedTag", name, STRUCTURE_CREATOR)); //$NON-NLS-1$
				fStructureCreators.register(element, new StructureCreatorDescriptor(element));
			}
		}
		for (IConfigurationElement element : elements) {
			if (CONTENT_TYPE_BINDING.equals(element.getName()))
				fStructureCreators.createBinding(element, STRUCTURE_CREATOR_ID_ATTRIBUTE);
		}

		// collect all viewers which define the structure merge viewer extension point
		elements= registry.getConfigurationElementsFor(PLUGIN_ID, STRUCTURE_MERGE_VIEWER_EXTENSION_POINT);
		for (IConfigurationElement element : elements) {
			String name= element.getName();
			if (!CONTENT_TYPE_BINDING.equals(name)) {
				if (!VIEWER_TAG.equals(name))
					logErrorMessage(Utilities.getFormattedString("CompareUIPlugin.unexpectedTag", name, VIEWER_TAG)); //$NON-NLS-1$
				fStructureMergeViewers.register(element, new ViewerDescriptor(element));
			}
		}
		for (IConfigurationElement element : elements) {
			if (CONTENT_TYPE_BINDING.equals(element.getName()))
				fStructureMergeViewers.createBinding(element, STRUCTURE_MERGE_VIEWER_ID_ATTRIBUTE);
		}

		// collect all viewers which define the content merge viewer extension point
		elements= registry.getConfigurationElementsFor(PLUGIN_ID, CONTENT_MERGE_VIEWER_EXTENSION_POINT);
		for (IConfigurationElement element : elements) {
			String name= element.getName();
			if (!CONTENT_TYPE_BINDING.equals(name)) {
				if (!VIEWER_TAG.equals(name))
					logErrorMessage(Utilities.getFormattedString("CompareUIPlugin.unexpectedTag", name, VIEWER_TAG)); //$NON-NLS-1$
				fContentMergeViewers.register(element, new ViewerDescriptor(element));
			}
		}
		for (IConfigurationElement element : elements) {
			if (CONTENT_TYPE_BINDING.equals(element.getName()))
				fContentMergeViewers.createBinding(element, CONTENT_MERGE_VIEWER_ID_ATTRIBUTE);
		}

		// collect all extensions that define the compare filter extension point
		elements = registry.getConfigurationElementsFor(PLUGIN_ID,
				COMPARE_FILTER_EXTENTION_POINT);
		for (IConfigurationElement element : elements) {
			String name = element.getName();
			if (!CONTENT_TYPE_BINDING.equals(name)) {
				if (!FILTER_TAG.equals(name))
					logErrorMessage(Utilities.getFormattedString(
							"CompareUIPlugin.unexpectedTag", name, FILTER_TAG)); //$NON-NLS-1$
				fCompareFilters.register(element, new CompareFilterDescriptor(element));
			}
		}
		for (IConfigurationElement element : elements) {
			if (CONTENT_TYPE_BINDING.equals(element.getName()))
				fCompareFilters.createBinding(element,
						COMPARE_FILTER_ID_ATTRIBUTE);
		}

		// collect all viewers which define the content viewer extension point
		elements= registry.getConfigurationElementsFor(PLUGIN_ID, CONTENT_VIEWER_EXTENSION_POINT);
		for (IConfigurationElement element : elements) {
			String name= element.getName();
			if (!CONTENT_TYPE_BINDING.equals(name)) {
				if (!VIEWER_TAG.equals(name))
					logErrorMessage(Utilities.getFormattedString("CompareUIPlugin.unexpectedTag", name, VIEWER_TAG)); //$NON-NLS-1$
				fContentViewers.register(element, new ViewerDescriptor(element));
			}
		}
		for (IConfigurationElement element : elements) {
			if (CONTENT_TYPE_BINDING.equals(element.getName()))
				fContentViewers.createBinding(element, CONTENT_VIEWER_ID_ATTRIBUTE);
		}
	}

	public static IWorkbench getActiveWorkbench() {
		return PlatformUI.getWorkbench();
	}

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		IWorkbench workbench= getActiveWorkbench();
		if (workbench == null)
			return null;
		return workbench.getActiveWorkbenchWindow();
	}

	/**
	 * Returns the active workbench page or <code>null</code> if
	 * no active workbench page can be determined.
	 *
	 * @return the active workbench page or <code>null</code> if
	 * 	no active workbench page can be determined
	 */
	private static IWorkbenchPage getActivePage() {
		IWorkbenchWindow window= getActiveWorkbenchWindow();
		if (window == null)
			return null;
		return window.getActivePage();
	}

	/**
	 * If the workbench is running returns the SWT Shell of the active workbench window or <code>null</code> if
	 * no workbench window is active.
	 *
	 * If the workbench is not running, returns the shell of the default display.
	 *
	 * @return If the workbench is running, returns the SWT Shell of the active workbench window, or <code>null</code> if
	 * 	no workbench window is active. Otherwise returns the shell of the default display.
	 */
	public static Shell getShell() {
		if(PlatformUI.isWorkbenchRunning()){
			IWorkbenchWindow window = getActiveWorkbenchWindow();
			if (window == null)
				return null;
			return window.getShell();
		}

		return Display.getDefault().getActiveShell();
	}

	/**
	 * Registers the given image for being disposed when this plug-in is shutdown.
	 *
	 * @param image the image to register for disposal
	 */
	public static void disposeOnShutdown(Image image) {
		if (image != null)
			fgDisposeOnShutdownImages.add(image);
	}

	/**
	 * Performs the comparison described by the given input and opens a compare
	 * editor on the result.
	 *
	 * @param input
	 *            the input on which to open the compare editor
	 * @param page
	 *            the workbench page on which to create a new compare editor
	 * @param editor
	 *            if not null the input is opened in this editor
	 * @param activate
	 *            if <code>true</code> the editor will be activated
	 * @see IWorkbenchPage#openEditor(org.eclipse.ui.IEditorInput, String,
	 *      boolean)
	 * @see CompareEditorInput
	 */
	public void openCompareEditor(final CompareEditorInput input,
			final IWorkbenchPage page, final IReusableEditor editor,
			final boolean activate) {
		CompareConfiguration configuration = input.getCompareConfiguration();
		if (configuration != null) {
			IPreferenceStore ps= configuration.getPreferenceStore();
			if (ps != null)
				configuration.setProperty(
						CompareConfiguration.USE_OUTLINE_VIEW,
						Boolean.valueOf(ps.getBoolean(ComparePreferencePage.USE_OUTLINE_VIEW)));
		}
		if (input.canRunAsJob()) {
			openEditorInBackground(input, page, editor, activate);
		} else {
			if (compareResultOK(input, null)) {
				internalOpenEditor(input, page, editor, activate);
			}
		}
	}

	private void openEditorInBackground(final CompareEditorInput input,
			final IWorkbenchPage page, final IReusableEditor editor,
			final boolean activate) {
		internalOpenEditor(input, page, editor, activate);
	}

	private void internalOpenEditor(final CompareEditorInput input,
			final IWorkbenchPage wp, final IReusableEditor editor,
			final boolean activate) {
		Runnable runnable = () -> {
			if (editor != null && !editor.getSite().getShell().isDisposed()) {	// reuse the given editor
				editor.setInput(input);
				return;
			}

			IWorkbenchPage page = wp;
			if (page == null)
				page= getActivePage();
			if (page != null) {
				// open new CompareEditor on page
				try {
					page.openEditor(input, COMPARE_EDITOR, activate);
				} catch (PartInitException e) {
					MessageDialog.openError(getShell(), Utilities.getString("CompareUIPlugin.openEditorError"), e.getMessage()); //$NON-NLS-1$
				}
			} else {
				MessageDialog.openError(getShell(),
						Utilities.getString("CompareUIPlugin.openEditorError"), //$NON-NLS-1$
						Utilities.getString("CompareUIPlugin.noActiveWorkbenchPage")); //$NON-NLS-1$
			}
		};
		syncExec(runnable);
	}

	/**
	 * Performs the comparison described by the given input and opens a
	 * compare dialog on the result.
	 *
	 * @param input the input on which to open the compare editor
	 * @see CompareEditorInput
	 */
	public void openCompareDialog(final CompareEditorInput input) {
		// We don't ever open dialogs in the background
		if (compareResultOK(input, null)) {
			internalOpenDialog(input);
		}
	}

	public IStatus prepareInput(CompareEditorInput input, IProgressMonitor monitor) {
		try {
			input.run(monitor);
			String message= input.getMessage();
			if (message != null) {
				return new Status(IStatus.ERROR, CompareUIPlugin.PLUGIN_ID, 0, message, null);
			}
			if (input.getCompareResult() == null) {
				return new Status(IStatus.ERROR, CompareUIPlugin.PLUGIN_ID, NO_DIFFERENCE, Utilities.getString("CompareUIPlugin.noDifferences"), null); //$NON-NLS-1$
			}
			return Status.OK_STATUS;
		} catch (InterruptedException e) {
			throw new OperationCanceledException();
		} catch (InvocationTargetException e) {
			return new Status(IStatus.ERROR, CompareUIPlugin.PLUGIN_ID, 0, Utilities.getString("CompareUIPlugin.compareFailed"), e.getTargetException()); //$NON-NLS-1$
		}
	}

	/*
	 * @return <code>true</code> if compare result is OK to show, <code>false</code> otherwise
	 */
	public boolean compareResultOK(CompareEditorInput input, IRunnableContext context) {
		final Shell shell= getShell();
		try {
			// run operation in context if possible
			if (context != null) {
				context.run(true, true, input);
			} else {
				Utilities.executeRunnable(input);
			}

			String message= input.getMessage();
			if (message != null) {
				MessageDialog.openError(shell, Utilities.getString("CompareUIPlugin.compareFailed"), message); //$NON-NLS-1$
				return false;
			}

			if (input.getCompareResult() == null) {
				MessageDialog.openInformation(shell, Utilities.getString("CompareUIPlugin.dialogTitle"), Utilities.getString("CompareUIPlugin.noDifferences")); //$NON-NLS-2$ //$NON-NLS-1$
				return false;
			}

			return true;
		} catch (InterruptedException x) {
			// canceled by user
		} catch (InvocationTargetException x) {
			MessageDialog.openError(shell, Utilities.getString("CompareUIPlugin.compareFailed"), x.getTargetException().getMessage()); //$NON-NLS-1$
		}
		return false;
	}

	/*
	 * Registers an image for the given type.
	 */
	private static void registerImage(String type, Image image, boolean dispose) {
		fgImages.put(normalizeCase(type), image);
		if (image != null && dispose) {
			fgDisposeOnShutdownImages.add(image);
		}
	}

	/**
	 * Registers an image descriptor for the given type.
	 *
	 * @param type the type
	 * @param descriptor the image descriptor
	 */
	public static void registerImageDescriptor(String type, ImageDescriptor descriptor) {
		fgImageDescriptors.put(normalizeCase(type), descriptor);
	}

	public static ImageDescriptor getImageDescriptor(String relativePath) {
		if (fgComparePlugin == null)
			return null;
		IPath path= Utilities.getIconPath(null).append(relativePath);
		URL url= FileLocator.find(fgComparePlugin.getBundle(), path, null);
		if (url == null)
			return null;
		return ImageDescriptor.createFromURL(url);
	}

	/**
	 * Returns a shared image for the given type, or a generic image if none
	 * has been registered for the given type.
	 * <p>
	 * Note: Images returned from this method will be automatically disposed
	 * of when this plug-in shuts down. Callers must not dispose of these
	 * images themselves.
	 * </p>
	 *
	 * @param type the type
	 * @return the image
	 */
	public static Image getImage(String type) {

		type= normalizeCase(type);

		boolean dispose= false;
		Image image= null;
		if (type != null)
			image= fgImages.get(type);
		if (image == null) {
			ImageDescriptor id= fgImageDescriptors.get(type);
			if (id != null) {
				image= id.createImage();
				dispose= true;
			}

			if (image == null) {
				if (fgComparePlugin != null) {
					if (ITypedElement.FOLDER_TYPE.equals(type)) {
						image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
						//image= SharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER);
					} else {
						image= createWorkbenchImage(type);
						dispose= true;
					}
				} else {
					id= fgImageDescriptors.get(normalizeCase("file")); //$NON-NLS-1$
					image= id.createImage();
					dispose= true;
				}
			}
			if (image != null)
				registerImage(type, image, dispose);
		}
		return image;
	}

	/**
	 * Returns a shared image for the given adaptable.
	 * This convenience method queries the given adaptable
	 * for its <code>IWorkbenchAdapter.getImageDescriptor</code>, which it
	 * uses to create an image if it does not already have one.
	 * <p>
	 * Note: Images returned from this method will be automatically disposed
	 * of when this plug-in shuts down. Callers must not dispose of these
	 * images themselves.
	 * </p>
	 *
	 * @param adaptable the adaptable for which to find an image
	 * @return an image
	 */
	public static Image getImage(IAdaptable adaptable) {
		if (adaptable != null) {
			IWorkbenchAdapter o= Adapters.adapt(adaptable, IWorkbenchAdapter.class);
			if (o == null) {
				return null;
			}
			ImageDescriptor id= o.getImageDescriptor(adaptable);
			if (id != null) {
				Image image= fgImages2.get(id);
				if (image == null) {
					image= id.createImage();
					try {
						fgImages2.put(id, image);
					} catch (NullPointerException e) {
						// NeedWork
					}
					fgDisposeOnShutdownImages.add(image);

				}
				return image;
			}
		}
		return null;
	}

	private static Image createWorkbenchImage(String type) {
		IEditorRegistry er= PlatformUI.getWorkbench().getEditorRegistry();
		ImageDescriptor id= er.getImageDescriptor("foo." + type); //$NON-NLS-1$
		return id.createImage();
	}

	/**
	 * Returns an structure creator descriptor for the given type.
	 *
	 * @param type the type for which to find a descriptor
	 * @return a descriptor for the given type, or <code>null</code> if no
	 *   descriptor has been registered
	 */
	public StructureCreatorDescriptor getStructureCreator(String type) {
		initializeRegistries();
		return fStructureCreators.search(type);
	}

	/**
	 * Returns a stream merger for the given type.
	 *
	 * @param type the type for which to find a stream merger
	 * @return a stream merger for the given type, or <code>null</code> if no
	 *   stream merger has been registered
	 */
	public IStreamMerger createStreamMerger(String type) {
		initializeRegistries();
		StreamMergerDescriptor descriptor= fStreamMergers.search(type);
		if (descriptor != null)
			return descriptor.createStreamMerger();
		return null;
	}

	/**
	 * Returns a stream merger for the given content type.
	 *
	 * @param type the type for which to find a stream merger
	 * @return a stream merger for the given type, or <code>null</code> if no
	 *   stream merger has been registered
	 */
	public IStreamMerger createStreamMerger(IContentType type) {
		initializeRegistries();
		StreamMergerDescriptor descriptor= fStreamMergers.search(type);
		if (descriptor != null)
			return descriptor.createStreamMerger();
		return null;
	}

	public ViewerDescriptor[] findStructureViewerDescriptor(Viewer oldViewer,
			ICompareInput input, CompareConfiguration configuration) {
		// we don't show the structure of additions or deletions
		if ((input == null) || input == null || input.getLeft() == null || input.getRight() == null)
			return null;

		Set<ViewerDescriptor> result = new LinkedHashSet<>();

		// content type search
		IContentType ctype= getCommonType(input);
		if (ctype != null) {
			initializeRegistries();
			List<ViewerDescriptor> list = fStructureMergeViewers.searchAll(ctype);
			if (list != null)
				result.addAll(list);
		}

		// old style search
		String[] types= getTypes(input);
		String type= null;
		if (isHomogenous(types)) {
			type= normalizeCase(types[0]);
			initializeRegistries();
			List<ViewerDescriptor> list = fStructureMergeViewers.searchAll(type);
			if (list != null)
				result.addAll(list);
			String alias= getStructureViewerAlias(type);
			if (alias != null) {
				list = fStructureMergeViewers.searchAll(alias);
				if (list != null)
					result.addAll(list);
			}
		}

		return result.size() > 0 ? result.toArray(new ViewerDescriptor[0]) : null;
	}

	/**
	 * Returns a structure compare viewer based on an old viewer and an input object.
	 * If the old viewer is suitable for showing the input, the old viewer
	 * is returned. Otherwise, the input's type is used to find a viewer descriptor in the registry
	 * which in turn is used to create a structure compare viewer under the given parent composite.
	 * If no viewer descriptor can be found <code>null</code> is returned.
	 *
	 * @param oldViewer a new viewer is only created if this old viewer cannot show the given input
	 * @param input the input object for which to find a structure viewer
	 * @param parent the SWT parent composite under which the new viewer is created
	 * @param configuration a configuration which is passed to a newly created viewer
	 * @return the compare viewer which is suitable for the given input object or <code>null</code>
	 */
	public Viewer findStructureViewer(Viewer oldViewer, ICompareInput input, Composite parent,
				CompareConfiguration configuration) {
		ViewerDescriptor[] descriptors = findStructureViewerDescriptor(oldViewer, input, configuration);
		if (descriptors == null || descriptors.length == 0) {
			// we didn't found any viewer so far.
			// now we try to find a structure creator for the generic StructureDiffViewer
			IContentType ctype= getCommonType(input);

			String[] types= getTypes(input);
			String type= null;
			if (isHomogenous(types)) {
				type= normalizeCase(types[0]);
			}

			initializeRegistries();
			StructureCreatorDescriptor scc= fStructureCreators.search(ctype);	// search for content type
			if (scc == null && type != null)
				scc= getStructureCreator(type);	// search for old-style type scheme
			if (scc != null) {
				IStructureCreator sc= scc.createStructureCreator();
				if (sc != null) {
					StructureDiffViewer sdv= new StructureDiffViewer(parent, configuration);
					sdv.setStructureCreator(sc);
					return sdv;
				}
			}
			return null;
		}
		return getViewer(descriptors[0], oldViewer, parent, configuration);
	}

	public CompareFilterDescriptor[] findCompareFilters(Object in) {
		Collection<Object> contentTypes = getContentTypes(in);
		if (contentTypes == null) {
			return new CompareFilterDescriptor[0];
		}
		Set<CompareFilterDescriptor> result = new LinkedHashSet<>();
		for (Object ct : contentTypes) {
			if (ct instanceof IContentType) {
				List<CompareFilterDescriptor> list = fCompareFilters.searchAll((IContentType) ct);
				if (list != null)
					result.addAll(list);
			} else if (ct instanceof String) {
				List<CompareFilterDescriptor> list = fCompareFilters.searchAll((String) ct);
				if (list != null)
					result.addAll(list);
			}
		}

		ArrayList<CompareFilterDescriptor> list = new ArrayList<>(result);
		Collections.sort(list, Comparator.comparing(CompareFilterDescriptor::getFilterId));

		return result.toArray(new CompareFilterDescriptor[result.size()]);
	}

	private Collection<Object> getContentTypes(Object in) {
		Set<Object> result = new LinkedHashSet<>();
		if (in instanceof IStreamContentAccessor) {
			String type = ITypedElement.TEXT_TYPE;

			if (in instanceof ITypedElement) {
				ITypedElement tin = (ITypedElement) in;

				IContentType ct = getContentType(tin);
				if (ct != null) {
					result.add(ct);
				}

				String ty = tin.getType();
				if (ty != null)
					type = ty;
				result.add(type);
			}
			return result;
		}

		if (!(in instanceof ICompareInput))
			return null;

		ICompareInput input = (ICompareInput) in;

		IContentType ctype = getCommonType(input);
		if (ctype != null) {
			result.add(ctype);
		}

		String[] types = getTypes(input);
		String type = null;
		if (isHomogenous(types))
			type = types[0];

		if (ITypedElement.FOLDER_TYPE.equals(type))
			return null;

		if (type == null) {
			int n = 0;
			for (String t : types) {
				if (!ITypedElement.UNKNOWN_TYPE.equals(t)) {
					n++;
					if (type == null) {
						type = t; // remember the first known type
					}
				}
			}
			if (n > 1) // don't use the type if there were more than one
				type = null;
		}

		if (type != null) {
			result.add(type);
		}

		// fallback
		String leftType = guessType(input.getLeft());
		String rightType = guessType(input.getRight());

		if (leftType != null || rightType != null) {
			boolean right_text = ITypedElement.TEXT_TYPE.equals(rightType);
			boolean left_text = ITypedElement.TEXT_TYPE.equals(leftType);
			if ((rightType != null && !right_text)
					|| (leftType != null && !left_text)) {
				result.add(BINARY_TYPE);
			}
			result.add(ITypedElement.TEXT_TYPE);
		}
		return result;
	}

	public ViewerDescriptor[] findContentViewerDescriptor(Viewer oldViewer, Object in, CompareConfiguration cc) {
		LinkedHashSet<ViewerDescriptor> result = new LinkedHashSet<>();
		if (in instanceof IStreamContentAccessor) {
			String type= ITypedElement.TEXT_TYPE;

			if (in instanceof ITypedElement) {
				ITypedElement tin= (ITypedElement) in;

				IContentType ct= getContentType(tin);
				if (ct != null) {
					initializeRegistries();
					List<ViewerDescriptor> list = fContentViewers.searchAll(ct);
					if (list != null)
						result.addAll(list);
				}

				String ty= tin.getType();
				if (ty != null)
					type= ty;
			}

			initializeRegistries();
			List<ViewerDescriptor> list = fContentViewers.searchAll(type);
			if (list != null)
				result.addAll(list);
			// fallback
			result.add(fContentViewers.search(Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT)));
			return result.toArray(new ViewerDescriptor[0]);
		}

		if (!(in instanceof ICompareInput))
			return null;

		ICompareInput input= (ICompareInput) in;
		String name = input.getName();

		IContentType ctype = getCommonType(input);
		if (ctype != null) {
			initializeRegistries();
			List<ViewerDescriptor> list = fContentMergeViewers.searchAll(ctype);
			if (list != null) {
				result.addAll(list);
			}
			// Add a hint for the viewers which content type we have detected
			cc.setProperty(CompareConfiguration.CONTENT_TYPE, ctype.getId());
		}

		String[] types= getTypes(input);
		String type= null;
		if (isHomogenous(types))
			type= types[0];

		if (ITypedElement.FOLDER_TYPE.equals(type))
			return null;

		if (type == null) {
			int n= 0;
			for (String t : types) {
				if (!ITypedElement.UNKNOWN_TYPE.equals(t)) {
					n++;
					if (type == null) {
						type = t; // remember the first known type
					}
				}
			}
			if (n > 1)	// don't use the type if there were more than one
				type= null;
		}

		if (type != null) {
			initializeRegistries();
			List<ViewerDescriptor> list = fContentMergeViewers.searchAll(type);
			if (list != null)
				result.addAll(list);
		}

		Set<ViewerDescriptor> editorLinkedDescriptors = findEditorLinkedDescriptors(name, ctype, false);
		result.addAll(editorLinkedDescriptors);

		// fallback
		String leftType= guessType(input.getLeft());
		String rightType= guessType(input.getRight());

		if (leftType != null || rightType != null) {
			boolean right_text = ITypedElement.TEXT_TYPE.equals(rightType);
			boolean left_text = ITypedElement.TEXT_TYPE.equals(leftType);
			initializeRegistries();
			if ((rightType != null && !right_text)
					|| (leftType != null && !left_text)) {
				List<ViewerDescriptor> list = fContentMergeViewers.searchAll(BINARY_TYPE);
				if (list != null)
					result.addAll(list);
			}
			List<ViewerDescriptor> list = fContentMergeViewers.searchAll(ITypedElement.TEXT_TYPE);
			if (list != null) {
				result.addAll(list);
			}
		}

		ensureTextIsLast(result);
		return result.isEmpty() ? null : result.toArray(new ViewerDescriptor[0]);
	}

	/**
	 * Modifies given set to move "fallback" text descriptor to be the last one.
	 * This is needed because we want more specific descriptors used first by default.
	 */
	private static void ensureTextIsLast(LinkedHashSet<ViewerDescriptor> result) {
		if (result.size() > 1) {
			ViewerDescriptor first = result.iterator().next();
			if (TextMergeViewerCreator.class.getName().equals(first.getViewerClass())) {
				result.remove(first);
				result.add(first);
			}
		}
	}

	/**
	 * @param fileName      possible file name for content in compare editor, may be
	 *                      null
	 * @param contentType   possible content type for content in compare editor, may
	 *                      be null
	 * @param firstIsEnough stop searching once first match is found
	 * @return set of descriptors which could be found for given content type via
	 *         "linked" editor
	 */
	Set<ViewerDescriptor> findEditorLinkedDescriptors(String fileName, IContentType contentType,
			boolean firstIsEnough) {
		if (fileName == null) {
			if (contentType == null) {
				contentType = fgContentTypeManager.findContentTypeFor(fileName);
			} else {
				return Collections.emptySet();
			}
		}

		LinkedHashSet<ViewerDescriptor> viewers = fContentMergeViewers.getAll().stream()
				.filter(vd -> vd.getLinkedEditorId() != null).collect(Collectors.toCollection(LinkedHashSet::new));
		if (viewers.isEmpty()) {
			return Collections.emptySet();
		}

		IEditorRegistry editorReg = PlatformUI.getWorkbench().getEditorRegistry();
		LinkedHashSet<ViewerDescriptor> result = new LinkedHashSet<>();
		IEditorDescriptor[] editors = editorReg.getEditors(fileName, contentType);
		for (IEditorDescriptor ed : editors) {
			addLinkedEditorContentTypes(viewers, firstIsEnough, ed.getId(), result);
			if (firstIsEnough && !result.isEmpty()) {
				return result;
			}
		}
		return result;
	}

	private void addLinkedEditorContentTypes(LinkedHashSet<ViewerDescriptor> viewers, boolean firstIsEnough,
			String editorId, Set<ViewerDescriptor> result) {
		Stream<ViewerDescriptor> stream = viewers.stream().filter(vd -> editorId.equals(vd.getLinkedEditorId()));
		if (firstIsEnough) {
			Optional<ViewerDescriptor> first = stream.findFirst();
			if (first.isPresent()) {
				result.add(first.get());
			}
		} else {
			stream.collect(Collectors.toCollection(() -> result));
		}
	}

	/**
	 * Returns a content compare viewer based on an old viewer and an input object.
	 * If the old viewer is suitable for showing the input the old viewer
	 * is returned. Otherwise the input's type is used to find a viewer descriptor in the registry
	 * which in turn is used to create a content compare viewer under the given parent composite.
	 * If no viewer descriptor can be found <code>null</code> is returned.
	 *
	 * @param oldViewer a new viewer is only created if this old viewer cannot show the given input
	 * @param in the input object for which to find a content viewer
	 * @param parent the SWT parent composite under which the new viewer is created
	 * @param cc a configuration which is passed to a newly created viewer
	 * @return the compare viewer which is suitable for the given input object or <code>null</code>
	 */
	public Viewer findContentViewer(Viewer oldViewer, Object in,
			Composite parent, CompareConfiguration cc) {
		ViewerDescriptor[] descriptors = findContentViewerDescriptor(oldViewer, in, cc);
		return getViewer(descriptors != null ? descriptors[0] : null, oldViewer, parent, cc);
	}

	private static Viewer getViewer(Object descriptor, Viewer oldViewer, Composite parent, CompareConfiguration cc) {
		if (descriptor instanceof IViewerDescriptor)
			return ((IViewerDescriptor)descriptor).createViewer(oldViewer, parent, cc);
		return null;
	}

	private static String[] getTypes(ICompareInput input) {
		ITypedElement ancestor= input.getAncestor();
		ITypedElement left= input.getLeft();
		ITypedElement right= input.getRight();

		ArrayList<String> tmp= new ArrayList<>();
		if (ancestor != null) {
			String type= ancestor.getType();
			if (type != null)
				tmp.add(normalizeCase(type));
		}
		if (left != null) {
			String type= left.getType();
			if (type != null)
				tmp.add(normalizeCase(type));
		}
		if (right != null) {
			String type= right.getType();
			if (type != null)
				tmp.add(normalizeCase(type));
		}
		return tmp.toArray(new String[tmp.size()]);
	}

	private static IContentType getContentType(ITypedElement element) {
		if (element == null)
			return null;
		String name= element.getName();
		IContentType ct= null;
		if (element instanceof IResourceProvider) {
			IResource resource= ((IResourceProvider)element).getResource();
			if (resource instanceof IFile) {
				try {
					IContentDescription contentDesc= ((IFile)resource).getContentDescription();
					return contentDesc != null ? contentDesc.getContentType() : null;
				} catch (CoreException e) {
					//$FALL-THROUGH$
				}
			}
		}
		if (element instanceof IStreamContentAccessor) {
			IStreamContentAccessor isa= (IStreamContentAccessor) element;
			try {
				InputStream is= isa.getContents();
				if (is != null) {
					try (InputStream bis = new BufferedInputStream(is)) {
						ct= fgContentTypeManager.findContentTypeFor(is, name);
					} catch (IOException e) {
						// silently ignored
					}
				}
			} catch (CoreException e1) {
				// silently ignored
			}
		}
		if (ct == null)
			ct= fgContentTypeManager.findContentTypeFor(name);
		return ct;
	}

	/*
	 * Returns true if the given types are homogeneous.
	 */
	private static boolean isHomogenous(String[] types) {
		switch (types.length) {
		case 1:
			return true;
		case 2:
			return types[0].equals(types[1]);
		case 3:
			return types[0].equals(types[1]) && types[1].equals(types[2]);
		default:
			return false;
		}
	}

	/*
	 * Returns the most specific content type that is common to the given inputs or null.
	 */
	private static IContentType getCommonType(ICompareInput input) {
		ITypedElement ancestor= input.getAncestor();
		ITypedElement left= input.getLeft();
		ITypedElement right= input.getRight();

		int n= 0;
		IContentType[] types= new IContentType[3];
		IContentType type= null;

		if (ancestor != null) {
			type= getContentType(ancestor);
			if (type != null)
				types[n++]= type;
		}
		type= getContentType(left);
		if (type != null)
			types[n++]= type;
		type= getContentType(right);
		if (type != null)
			types[n++]= type;

		IContentType result= null;
		IContentType[] s0, s1, s2;
		switch (n) {
		case 0:
			return null;
		case 1:
			return types[0];
		case 2:
			if (types[0].equals(types[1]))
				return types[0];
			s0= toFullPath(types[0]);
			s1= toFullPath(types[1]);
			for (int i= 0; i < Math.min(s0.length, s1.length); i++) {
				if (!s0[i].equals(s1[i]))
					break;
				result= s0[i];
			}
			return result;
		case 3:
			if (types[0].equals(types[1]) && types[1].equals(types[2]))
				return types[0];
			s0= toFullPath(types[0]);
			s1= toFullPath(types[1]);
			s2= toFullPath(types[2]);
			for (int i= 0; i < Math.min(Math.min(s0.length, s1.length), s2.length); i++) {
				if (!s0[i].equals(s1[i]) || !s1[i].equals(s2[i]))
					break;
				result= s0[i];
			}
			return result;
		default:
			return null;
		}
	}

	private static IContentType[] toFullPath(IContentType ct) {
		List<IContentType> l= new ArrayList<>();
		for (; ct != null; ct= ct.getBaseType())
			l.add(0, ct);
		return l.toArray(new IContentType[l.size()]);
	}

	/*
	 * Guesses the file type of the given input.
	 * Returns ITypedElement.TEXT_TYPE if none of the first 10 lines is longer than 1000 bytes.
	 * Returns ITypedElement.UNKNOWN_TYPE otherwise.
	 * Returns <code>null</code> if the input isn't an <code>IStreamContentAccessor</code>.
	 */
	private static String guessType(ITypedElement input) {
		if (input instanceof IStreamContentAccessor) {
			IStreamContentAccessor sca= (IStreamContentAccessor) input;
			InputStream is= null;
			try {
				is= sca.getContents();
				if (is == null)
					return null;
				int lineLength= 0;
				int lines= 0;
				while (lines < 10) {
					int c= is.read();
					if (c == -1)	// EOF
						break;
					if (c == '\n' || c == '\r') { // reset line length
						lineLength= 0;
						lines++;
					} else
						lineLength++;
					if (lineLength > 1000)
						return ITypedElement.UNKNOWN_TYPE;
				}
				return ITypedElement.TEXT_TYPE;
			} catch (CoreException | IOException ex) {
				// be silent and return UNKNOWN_TYPE
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException ex) {
						// silently ignored
					}
				}
			}
			return ITypedElement.UNKNOWN_TYPE;
		}
		return null;
	}

	private static String normalizeCase(String s) {
		if (NORMALIZE_CASE && s != null)
			return s.toUpperCase();
		return s;
	}

	//---- alias management

	private String getStructureViewerAlias(String type) {
		return getStructureViewerAliases().get(type);
	}

	public void addStructureViewerAlias(String type, String alias) {
		getStructureViewerAliases().put(normalizeCase(alias), normalizeCase(type));
	}

	private Map<String, String> getStructureViewerAliases() {
		if (fStructureViewerAliases == null) {
			fStructureViewerAliases= new Hashtable<>(10);
			String aliases= getPreferenceStore().getString(STRUCTUREVIEWER_ALIASES_PREFERENCE_NAME);
			if (aliases != null && aliases.length() > 0) {
				StringTokenizer st= new StringTokenizer(aliases, " ");	//$NON-NLS-1$
				while (st.hasMoreTokens()) {
					String pair= st.nextToken();
					int pos= pair.indexOf('.');
					if (pos > 0) {
						String key= pair.substring(0, pos);
						String alias= pair.substring(pos+1);
						fStructureViewerAliases.put(key, alias);
					}
				}
			}
		}
		return fStructureViewerAliases;
	}

	public void removeAllStructureViewerAliases(String type) {
		if (fStructureViewerAliases == null)
			return;
		String t= normalizeCase(type);
		Set<Entry<String, String>> entrySet= fStructureViewerAliases.entrySet();
		for (Iterator<Map.Entry<String, String>> iter= entrySet.iterator(); iter.hasNext(); ) {
			Map.Entry<String, String> entry= iter.next();
			if (entry.getValue().equals(t))
				iter.remove();
		}
	}

	/*
	 * Converts the aliases into a single string before they are stored
	 * in the preference store.
	 * The format is:
	 * <key> '.' <alias> ' ' <key> '.' <alias> ...
	 */
	private void rememberAliases(IPreferenceStore ps) {
		if (fStructureViewerAliases == null)
			return;
		StringBuilder buffer= new StringBuilder();
		for (Map.Entry<String, String> entry : fStructureViewerAliases.entrySet()) {
			String key= entry.getKey();
			String alias= entry.getValue();
			buffer.append(key);
			buffer.append('.');
			buffer.append(alias);
			buffer.append(' ');
		}
		ps.setValue(STRUCTUREVIEWER_ALIASES_PREFERENCE_NAME, buffer.toString());
	}

	//---- filters

	public boolean filter(String name, boolean isFolder, boolean isArchive) {
		if (fFilter == null) {
			fFilter= new CompareResourceFilter();
			final IPreferenceStore ps= getPreferenceStore();
			fFilter.setFilters(ps.getString(ComparePreferencePage.PATH_FILTER));
			fPropertyChangeListener= event -> {
				if (ComparePreferencePage.PATH_FILTER.equals(event.getProperty()))
					fFilter.setFilters(ps.getString(ComparePreferencePage.PATH_FILTER));
			};
			ps.addPropertyChangeListener(fPropertyChangeListener);
		}
		return fFilter.filter(name, isFolder, isArchive);
	}

	private void internalOpenDialog(final CompareEditorInput input) {
		syncExec(() -> {
			Shell shell;
			if (PlatformUI.isWorkbenchRunning()) {
				shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
			} else {
				shell = Display.getDefault().getActiveShell();
			}
			CompareDialog dialog = new CompareDialog(shell, input);
			dialog.open();
		});
	}

	private void syncExec(Runnable runnable) {
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(runnable);
		} else {
			runnable.run();
		}
	}

	//---- more utilities

	protected void handleNoDifference() {
		Runnable runnable = () -> MessageDialog.openInformation(getShell(), Utilities.getString("CompareUIPlugin.dialogTitle"), Utilities.getString("CompareUIPlugin.noDifferences")); //$NON-NLS-1$ //$NON-NLS-2$
		syncExec(runnable);
	}

	/**
	 * Returns an array of all editors that have an unsaved content. If the identical content is
	 * presented in more than one editor, only one of those editor parts is part of the result.
	 *
	 * @return an array of all dirty editor parts.
	 */
	public static IEditorPart[] getDirtyEditors() {
		Set<IEditorInput> inputs= new HashSet<>();
		List<IEditorPart> result= new ArrayList<>(0);
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
		for (IWorkbenchWindow window : windows) {
			IWorkbenchPage[] pages = window.getPages();
			for (IWorkbenchPage page : pages) {
				IEditorPart[] editors = page.getDirtyEditors();
				for (IEditorPart ep : editors) {
					IEditorInput input= ep.getEditorInput();
					if (!inputs.contains(input)) {
						inputs.add(input);
						result.add(ep);
					}
				}
			}
		}
		return result.toArray(new IEditorPart[result.size()]);
	}

	public static void logErrorMessage(String message) {
		if (message == null)
			message= ""; //$NON-NLS-1$
		log(new Status(IStatus.ERROR, getPluginId(), INTERNAL_ERROR, message, null));
	}

	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), INTERNAL_ERROR, CompareMessages.ComparePlugin_internal_error, e));
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	String findContentTypeNameOrType(ICompareInput input, ViewerDescriptor vd, CompareConfiguration cc) {
		IContentType ctype= getCommonType(input);
		if (ctype != null) {
			initializeRegistries();
			List<ViewerDescriptor> list = fContentMergeViewers.searchAll(ctype);
			if (list != null)
				if (list.contains(vd))
					return ctype.getName();
		}

		String[] types= getTypes(input);
		String type= null;
		if (isHomogenous(types))
			type= types[0];

		if (ITypedElement.FOLDER_TYPE.equals(type))
			return null;

		if (type == null) {
			int n= 0;
			for (String t : types) {
				if (!ITypedElement.UNKNOWN_TYPE.equals(t)) {
					n++;
					if (type == null) {
						type = t; // remember the first known type
					}
				}
			}
			if (n > 1)	// don't use the type if there were more than one
				type= null;
		}

		if (type != null) {
			initializeRegistries();
			List<ViewerDescriptor> list = fContentMergeViewers.searchAll(type);
			if (list != null)
				if (list.contains(vd))
					return type;
		}

		Set<ViewerDescriptor> editorLinkedDescriptors = findEditorLinkedDescriptors(input.getName(), ctype, true);
		if (!editorLinkedDescriptors.isEmpty()) {
			return type;
		}

		// fallback
		String leftType= guessType(input.getLeft());
		String rightType= guessType(input.getRight());

		if (leftType != null || rightType != null) {
			boolean right_text = ITypedElement.TEXT_TYPE.equals(rightType);
			boolean left_text = ITypedElement.TEXT_TYPE.equals(leftType);
			initializeRegistries();
			if ((rightType != null && !right_text)
					|| (leftType != null && !left_text)) {
				List<ViewerDescriptor> list = fContentMergeViewers.searchAll(BINARY_TYPE);
				if (list != null)
					if (list.contains(vd))
						return type;
			}
			List<ViewerDescriptor> list = fContentMergeViewers.searchAll(ITypedElement.TEXT_TYPE);
			if (list != null)
				if (list.contains(vd))
					return type;
		}
		return null;
	}

	String findStructureTypeNameOrType(ICompareInput input, ViewerDescriptor vd, CompareConfiguration cc) {
		// We don't show the structure of additions or deletions
		if ((input == null) || input == null || input.getLeft() == null || input.getRight() == null)
			return null;

		// Content type search
		IContentType ctype= getCommonType(input);
		if (ctype != null) {
			initializeRegistries();
			List<ViewerDescriptor> list = fStructureMergeViewers.searchAll(ctype);
			if (list != null)
				if (list.contains(vd))
					return ctype.getName();
		}

		// Old style search
		String[] types= getTypes(input);
		String type= null;
		if (isHomogenous(types)) {
			type= normalizeCase(types[0]);
			initializeRegistries();
			List<ViewerDescriptor> list = fStructureMergeViewers.searchAll(type);
			if (list != null)
				if (list.contains(vd))
					return type;
			String alias= getStructureViewerAlias(type);
			if (alias != null) {
				list = fStructureMergeViewers.searchAll(alias);
				if (list != null)
					if (list.contains(vd))
						return alias;
			}
		}

		return null;
	}
}
