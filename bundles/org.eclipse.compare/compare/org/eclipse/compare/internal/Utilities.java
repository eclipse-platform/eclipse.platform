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
 *     Stefan Dirix (sdirix@eclipsesource.com) - Bug 473847: Minimum E4 Compatibility of Compare
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ICompareFilter;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.SharedDocumentAdapter;
import org.eclipse.compare.contentmergeviewer.IDocumentRange;
import org.eclipse.compare.internal.core.patch.HunkResult;
import org.eclipse.compare.internal.patch.PatchMessages;
import org.eclipse.compare.patch.IHunk;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * Convenience and utility methods.
 */
public class Utilities {
	private static final IPath ICONS_PATH= new Path("$nl$/icons/full/"); //$NON-NLS-1$

	public static IWorkbenchPartSite findSite(Control c) {
		while (c != null && !c.isDisposed()) {
			Object data= c.getData();
			if (data instanceof IWorkbenchPart)
				return ((IWorkbenchPart) data).getSite();
			c= c.getParent();
		}
		return null;
	}

	public static IActionBars findActionBars(Control c) {
		while (c != null && !c.isDisposed()) {
			Object data= c.getData();
			if (data instanceof CompareEditor)
				return ((CompareEditor) data).getActionBars();

			// PR 1GDVZV7: ITPVCM:WIN98 - CTRL + C does not work in Java source compare
			if (data instanceof IViewPart)
				return ((IViewPart) data).getViewSite().getActionBars();
			// end PR 1GDVZV7

			c= c.getParent();
		}
		return null;
	}

	public static void setEnableComposite(Composite composite, boolean enable) {
		Control[] children= composite.getChildren();
		for (Control c : children) {
			c.setEnabled(enable);
		}
	}

	public static boolean getBoolean(CompareConfiguration cc, String key, boolean dflt) {
		if (cc != null) {
			Object value= cc.getProperty(key);
			if (value instanceof Boolean)
				return ((Boolean) value).booleanValue();
		}
		return dflt;
	}

	/**
	 * Returns the active compare filters for the compare configuration
	 *
	 * @param cc
	 * @return the active compare filters
	 */
	public static ICompareFilter[] getCompareFilters(CompareConfiguration cc) {
		if (cc != null) {
			Object value = cc.getProperty(ChangeCompareFilterPropertyAction.COMPARE_FILTERS);
			if (value instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, ICompareFilter[]> filtersMap = (Map<String, ICompareFilter[]>) value;
				return filtersMap.values().toArray(new ICompareFilter[filtersMap.size()]);
			}
		}
		return new ICompareFilter[0];
	}

	public static void firePropertyChange(ListenerList<IPropertyChangeListener> listenerList, Object source, String property, Object old, Object newValue) {
		PropertyChangeEvent event= new PropertyChangeEvent(source, property, old, newValue);
		firePropertyChange(listenerList, event);
	}

	public static void firePropertyChange(final ListenerList<IPropertyChangeListener> listenerList, final PropertyChangeEvent event) {
		if (listenerList == null || listenerList.isEmpty())
			return;
		// Legacy listeners may expect to get notified in the UI thread
		Runnable runnable = () -> {
			for (IPropertyChangeListener listener : listenerList) {
				SafeRunner.run(() -> listener.propertyChange(event));
			}
		};
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(runnable);
		} else {
			runnable.run();
		}
	}

	public static boolean okToUse(Widget widget) {
		return widget != null && !widget.isDisposed();
	}

	private static ArrayList<IResource> internalGetResources(ISelection selection, Class<? extends IResource> type) {
		ArrayList<IResource> tmp= new ArrayList<>();
		if (selection instanceof IStructuredSelection) {
			Object[] s= ((IStructuredSelection) selection).toArray();

			for (Object o : s) {
				IResource resource= null;
				if (type.isInstance(o)) {
					resource= (IResource) o;
				} else if (o instanceof ResourceMapping) {
					try {
						ResourceTraversal[] travs= ((ResourceMapping)o).getTraversals(ResourceMappingContext.LOCAL_CONTEXT, null);
						if (travs != null) {
							for (ResourceTraversal trav : travs) {
								IResource[] resources = trav.getResources();
								for (IResource r : resources) {
									if (type.isInstance(r) && r.isAccessible()) {
										tmp.add(r);
									}
								}
							}
						}
					} catch (CoreException ex) {
						CompareUIPlugin.log(ex);
					}
				} else if (o instanceof IAdaptable) {
					IAdaptable a= (IAdaptable) o;
					Object adapter= a.getAdapter(IResource.class);
					if (type.isInstance(adapter))
						resource= (IResource) adapter;
				}

				if (resource != null && resource.isAccessible())
					tmp.add(resource);
			}
		}
		return tmp;
	}

	/*
	 * Convenience method: extract all accessible <code>IResources</code> from given selection.
	 * Never returns null.
	 */
	public static IResource[] getResources(ISelection selection) {
		ArrayList<IResource> tmp= internalGetResources(selection, IResource.class);
		return tmp.toArray(new IResource[tmp.size()]);
	}

	/*
	 * Convenience method: extract all accessible <code>IFiles</code> from given selection.
	 * Never returns null.
	 */
	public static IFile[] getFiles(ISelection selection) {
		ArrayList<IResource> tmp= internalGetResources(selection, IFile.class);
		return tmp.toArray(new IFile[tmp.size()]);
	}

	public static byte[] readBytes(InputStream in) {
		ByteArrayOutputStream bos= new ByteArrayOutputStream();
		try {
			while (true) {
				int c= in.read();
				if (c == -1)
					break;
				bos.write(c);
			}

		} catch (IOException ex) {
			return null;

		} finally {
			Utilities.close(in);
			try {
				bos.close();
			} catch (IOException x) {
				// silently ignored
			}
		}

		return bos.toByteArray();
	}

	public static IPath getIconPath(Display display) {
		return ICONS_PATH;
	}

	/*
	 * Initializes the given Action from a ResourceBundle.
	 */
	public static void initAction(IAction a, ResourceBundle bundle, String prefix) {
		String labelKey= "label"; //$NON-NLS-1$
		String tooltipKey= "tooltip"; //$NON-NLS-1$
		String imageKey= "image"; //$NON-NLS-1$
		String descriptionKey= "description"; //$NON-NLS-1$

		if (prefix != null && prefix.length() > 0) {
			labelKey= prefix + labelKey;
			tooltipKey= prefix + tooltipKey;
			imageKey= prefix + imageKey;
			descriptionKey= prefix + descriptionKey;
		}

		a.setText(getString(bundle, labelKey, labelKey));
		a.setToolTipText(getString(bundle, tooltipKey, null));
		a.setDescription(getString(bundle, descriptionKey, null));

		String relPath= getString(bundle, imageKey, null);
		if (relPath != null && relPath.trim().length() > 0) {
			String dPath;
			String ePath;

			if (relPath.contains("/")) { //$NON-NLS-1$
				String path= relPath.substring(1);
				dPath= 'd' + path;
				ePath= 'e' + path;
			} else {
				dPath= "dlcl16/" + relPath; //$NON-NLS-1$
				ePath= "elcl16/" + relPath; //$NON-NLS-1$
			}

			ImageDescriptor id= CompareUIPlugin.getImageDescriptor(dPath);	// we set the disabled image first (see PR 1GDDE87)
			if (id != null)
				a.setDisabledImageDescriptor(id);
			id= CompareUIPlugin.getImageDescriptor(ePath);
			if (id != null) {
				a.setImageDescriptor(id);
				a.setHoverImageDescriptor(id);
			}
		}
	}

	public static void initToggleAction(IAction a, ResourceBundle bundle, String prefix, boolean checked) {
		String tooltip= null;
		if (checked) {
			tooltip= getString(bundle, prefix + "tooltip.checked", null);	//$NON-NLS-1$
		} else {
			tooltip= getString(bundle, prefix + "tooltip.unchecked", null);	//$NON-NLS-1$
		}
		if (tooltip == null)
			tooltip= getString(bundle, prefix + "tooltip", null);	//$NON-NLS-1$

		if (tooltip != null)
			a.setToolTipText(tooltip);

		String description= null;
		if (checked) {
			description= getString(bundle, prefix + "description.checked", null);	//$NON-NLS-1$
		} else {
			description= getString(bundle, prefix + "description.unchecked", null);	//$NON-NLS-1$
		}
		if (description == null)
			description= getString(bundle, prefix + "description", null);	//$NON-NLS-1$

		if (description != null)
			a.setDescription(description);
	}

	public static String getString(ResourceBundle bundle, String key, String dfltValue) {
		if (bundle != null) {
			try {
				return bundle.getString(key);
			} catch (MissingResourceException x) {
				// fall through
			}
		}
		return dfltValue;
	}

	public static String getFormattedString(ResourceBundle bundle, String key, String arg) {
		if (bundle != null) {
			try {
				return MessageFormat.format(bundle.getString(key), arg);
			} catch (MissingResourceException x) {
				CompareUIPlugin.log(x);
			}
		}
		return "!" + key + "!";	//$NON-NLS-2$ //$NON-NLS-1$
	}

	public static String getString(String key) {
		try {
			return CompareUI.getResourceBundle().getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";	//$NON-NLS-2$ //$NON-NLS-1$
		}
	}

	public static String getFormattedString(String key, String arg) {
		try {
			return MessageFormat.format(CompareUI.getResourceBundle().getString(key), arg);
		} catch (MissingResourceException e) {
			return "!" + key + "!";	//$NON-NLS-2$ //$NON-NLS-1$
		}
	}

	public static String getFormattedString(String key, String arg0, String arg1) {
		try {
			return MessageFormat.format(CompareUI.getResourceBundle().getString(key), arg0, arg1);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
	}

	public static String getString(ResourceBundle bundle, String key) {
		return getString(bundle, key, key);
	}

	public static int getInteger(ResourceBundle bundle, String key, int dfltValue) {
		if (bundle != null) {
			try {
				String s= bundle.getString(key);
				if (s != null)
					return Integer.parseInt(s);
			} catch (NumberFormatException x) {
				CompareUIPlugin.log(x);
			} catch (MissingResourceException x) {
				// Silently ignore Exception
			}
		}
		return dfltValue;
	}

	/**
	 * Answers <code>true</code> if the given selection contains resources that don't
	 * have overlapping paths and <code>false</code> otherwise.
	 */
	/*
	public static boolean isSelectionNonOverlapping() throws TeamException {
		IResource[] resources = getSelectedResources();
		// allow operation for non-overlapping resource selections
		if(resources.length>0) {
			List validPaths = new ArrayList(2);
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];

				// only allow cvs resources to be selected
				if(RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId()) == null) {
					return false;
				}

				// check if this resource overlaps other selections
				IPath resourceFullPath = resource.getFullPath();
				if(!validPaths.isEmpty()) {
					for (Iterator it = validPaths.iterator(); it.hasNext();) {
						IPath path = (IPath) it.next();
						if(path.isPrefixOf(resourceFullPath) ||
							resourceFullPath.isPrefixOf(path)) {
							return false;
						}
					}
				}
				validPaths.add(resourceFullPath);

				// ensure that resources are managed
				ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
				if(cvsResource.isFolder()) {
					if( ! ((ICVSFolder)cvsResource).isCVSFolder()) return false;
				} else {
					if( ! cvsResource.isManaged()) return false;
				}
			}
			return true;
		}
		return false;
	}
	*/

	/* validate edit utilities */

	/**
	 * Status constant indicating that an validateEdit call has changed the
	 * content of a file on disk.
	 */
	private static final int VALIDATE_EDIT_PROBLEM= 10004;

	/**
	 * Constant used to indicate that tests are being run.
	 */
	public static boolean RUNNING_TESTS = false;

	/**
	 * Constant used while testing the indicate that changes should be flushed
	 * when the compare input changes and a viewer is dirty.
	 */
	public static boolean TESTING_FLUSH_ON_COMPARE_INPUT_CHANGE = false;

	/*
	 * Makes the given resources committable. Committable means that all
	 * resources are writeable and that the content of the resources hasn't
	 * changed by calling <code>validateEdit</code> for a given file on
	 * <tt>IWorkspace</tt>.
	 *
	 * @param resources the resources to be checked
	 * @param shell the Shell passed to <code>validateEdit</code> as a context
	 * @return returns <code>true</code> if all resources are committable, <code>false</code> otherwise
	 *
	 * @see org.eclipse.core.resources.IWorkspace#validateEdit(org.eclipse.core.resources.IFile[], java.lang.Object)
	 */
	public static boolean validateResource(IResource resource, Shell shell, String title) {
		return validateResources(new IResource[] { resource }, shell, title);
	}

	/*
	 * Makes the given resources committable. Committable means that all
	 * resources are writeable and that the content of the resources hasn't
	 * changed by calling <code>validateEdit</code> for a given file on
	 * <tt>IWorkspace</tt>.
	 *
	 * @param resources the resources to be checked
	 * @param shell the Shell passed to <code>validateEdit</code> as a context
	 * @return returns <code>true</code> if all resources are committable, <code>false</code> otherwise
	 *
	 * @see org.eclipse.core.resources.IWorkspace#validateEdit(org.eclipse.core.resources.IFile[], java.lang.Object)
	 */
	public static boolean validateResources(List<IResource> resources, Shell shell, String title) {
		IResource r[]= resources.toArray(new IResource[resources.size()]);
		return validateResources(r, shell, title);
	}

	/*
	 * Makes the given resources committable. Committable means that all
	 * resources are writeable and that the content of the resources hasn't
	 * changed by calling <code>validateEdit</code> for a given file on
	 * <tt>IWorkspace</tt>.
	 *
	 * @param resources the resources to be checked
	 * @param shell the Shell passed to <code>validateEdit</code> as a context
	 * @return returns <code>true</code> if all resources are committable, <code>false</code> otherwise
	 *
	 * @see org.eclipse.core.resources.IWorkspace#validateEdit(org.eclipse.core.resources.IFile[], java.lang.Object)
	 */
	public static boolean validateResources(IResource[] resources, Shell shell, String title) {
		// get all readonly files
		List<IResource> readOnlyFiles= getReadonlyFiles(resources);
		if (readOnlyFiles.isEmpty())
			return true;

		// get timestamps of readonly files before validateEdit
		Map<IFile, Long> oldTimeStamps= createModificationStampMap(readOnlyFiles);

		IFile[] files= readOnlyFiles.toArray(new IFile[readOnlyFiles.size()]);
		IStatus status= ResourcesPlugin.getWorkspace().validateEdit(files, shell);
		if (!status.isOK()) {
			String message= getString("ValidateEdit.error.unable_to_perform"); //$NON-NLS-1$
			displayError(shell, title, status, message);
			return false;
		}

		IStatus modified= null;
		Map<IFile, Long> newTimeStamps= createModificationStampMap(readOnlyFiles);
		for (Map.Entry<IFile, Long> entry : newTimeStamps.entrySet()) {
			IFile file = entry.getKey();
			Long newTimeStamp = entry.getValue();
			if (file.isReadOnly()) {
				IStatus error = new Status(IStatus.ERROR,
								CompareUIPlugin.getPluginId(),
								VALIDATE_EDIT_PROBLEM,
								getFormattedString("ValidateEdit.error.stillReadonly", file.getFullPath().toString()), //$NON-NLS-1$
								null);
				modified= addStatus(modified, error);
			} else if (!oldTimeStamps.get(file).equals(newTimeStamp)) {
				IStatus error = new Status(IStatus.ERROR,
								CompareUIPlugin.getPluginId(),
								VALIDATE_EDIT_PROBLEM,
								getFormattedString("ValidateEdit.error.fileModified", file.getFullPath().toString()), //$NON-NLS-1$
								null);
				modified= addStatus(modified, error);
			}
		}
		if (modified != null) {
			String message= getString("ValidateEdit.error.unable_to_perform"); //$NON-NLS-1$
			displayError(shell, title, modified, message);
			return false;
		}
		return true;
	}

	private static void displayError(final Shell shell, final String title, final IStatus status, final String message) {
		if (Display.getCurrent() != null) {
			ErrorDialog.openError(shell, title, message, status);
		} else {
			Display.getDefault().syncExec(() -> ErrorDialog.openError(shell, title, message, status));
		}
	}

	private static List<IResource> getReadonlyFiles(IResource[] resources) {
		List<IResource> readOnlyFiles= new ArrayList<>();
		for (IResource resource : resources) {
			ResourceAttributes resourceAttributes= resource.getResourceAttributes();
			if (resource.getType() == IResource.FILE && resourceAttributes != null && resourceAttributes.isReadOnly())
				readOnlyFiles.add(resource);
		}
		return readOnlyFiles;
	}

	private static Map<IFile, Long> createModificationStampMap(List<IResource> files) {
		Map<IFile, Long> map= new HashMap<>();
		for (IResource file : files) {
			map.put((IFile) file, file.getModificationStamp());
		}
		return map;
	}

	private static IStatus addStatus(IStatus status, IStatus entry) {
		if (status == null)
			return entry;

		if (status.isMultiStatus()) {
			((MultiStatus)status).add(entry);
			return status;
		}

		MultiStatus result= new MultiStatus(CompareUIPlugin.getPluginId(),
				VALIDATE_EDIT_PROBLEM,
				getString("ValidateEdit.error.unable_to_perform"), null); //$NON-NLS-1$
		result.add(status);
		result.add(entry);
		return result;
	}

	// encoding

	public static String readString(IStreamContentAccessor sca, String encoding) throws CoreException {
		String s = null;
		try {
			try {
				s= Utilities.readString(sca.getContents(), encoding);
			} catch (UnsupportedEncodingException e) {
				if (!encoding.equals(ResourcesPlugin.getEncoding())) {
					s = Utilities.readString(sca.getContents(), ResourcesPlugin.getEncoding());
				}
			}
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, CompareUIPlugin.PLUGIN_ID, 0, e.getMessage(), e));
		}
		return s;
	}

	/*
	 * Returns null if an error occurred.
	 */
	public static String readString(InputStream is, String encoding) throws IOException {
		return readString(is, encoding, -1, null);
	}

	public static String readString(InputStream is, String encoding, int length, IProgressMonitor monitor) throws IOException {
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.setWorkRemaining(length);
		if (is == null)
			return null;
		BufferedReader reader= null;
		try {
			StringBuilder buffer= new StringBuilder();
			char[] part= new char[2048];
			int read= 0;
			reader= new BufferedReader(new InputStreamReader(is, encoding));
			while ((read= reader.read(part)) != -1) {
				buffer.append(part, 0, read);
				progress.worked(2048);
				if (progress.isCanceled())
					throw new OperationCanceledException();
			}

			return buffer.toString();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ex) {
					// silently ignored
				}
			}
		}
	}

	public static String getCharset(Object resource) {
		if (resource instanceof IEncodedStorage) {
			try {
				return ((IEncodedStorage)resource).getCharset();
			} catch (CoreException ex) {
				CompareUIPlugin.log(ex);
			}
		}
		return ResourcesPlugin.getEncoding();
	}

	public static byte[] getBytes(String s, String encoding) {
		byte[] bytes= null;
		if (s != null) {
			try {
				bytes= s.getBytes(encoding);
			} catch (UnsupportedEncodingException e) {
				bytes= s.getBytes();
			}
		}
		return bytes;
	}

	public static String readString(IStreamContentAccessor sa) throws CoreException {
		String encoding= null;
		if (sa instanceof IEncodedStreamContentAccessor)
			encoding= ((IEncodedStreamContentAccessor)sa).getCharset();
		if (encoding == null)
			encoding= ResourcesPlugin.getEncoding();
		return Utilities.readString(sa, encoding);
	}

	public static void close(InputStream is) {
		if (is != null) {
			try {
				is.close();
			} catch (IOException ex) {
				// silently ignored
			}
		}
	}

	public static IResource getFirstResource(ISelection selection) {
		IResource[] resources = getResources(selection);
		if (resources.length > 0)
			return resources[0];
		return null;
	}

	public static ITypedElement getLeg(char type, Object input) {
		if (input instanceof ICompareInput) {
			switch (type) {
			case MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR:
				return ((ICompareInput)input).getAncestor();
			case MergeViewerContentProvider.LEFT_CONTRIBUTOR:
				return ((ICompareInput)input).getLeft();
			case MergeViewerContentProvider.RIGHT_CONTRIBUTOR:
				return ((ICompareInput)input).getRight();
			default:
				break;
			}
		}
		return null;
	}

	public static IDocument getDocument(char type, Object element, boolean isUsingDefaultContentProvider, boolean canHaveSharedDocument) {
		ITypedElement te= getLeg(type, element);
		if (te == null)
			return null;
		if (te instanceof IDocument)
			return (IDocument) te;
		if (te instanceof IDocumentRange)
			return ((IDocumentRange) te).getDocument();

		if (isUsingDefaultContentProvider && canHaveSharedDocument) {
			ISharedDocumentAdapter sda = Adapters.adapt(te, ISharedDocumentAdapter.class);
			if (sda != null) {
				IEditorInput input= sda.getDocumentKey(te);
				if (input != null) {
					IDocumentProvider provider = SharedDocumentAdapter.getDocumentProvider(input);
					if (provider != null)
						return provider.getDocument(input);
				}
			}
		}

		if (te instanceof IStreamContentAccessor)
			return DocumentManager.get(te);

		return null;
	}

	/**
	 * Return whether either the left or right sides of the given input
	 * represents a hunk. A hunk is a portion of a file.
	 * @param input the compare input
	 * @return whether the left or right side of the input represents a hunk
	 */
	public static boolean isHunk(Object input) {
		if (input != null && input instanceof DiffNode){
			ITypedElement right = ((DiffNode) input).getRight();
			if (Adapters.adapt(right, IHunk.class) != null)
				return true;
			ITypedElement left = ((DiffNode) input).getLeft();
			if (Adapters.adapt(left, IHunk.class) != null)
				return true;
		}
		return false;
	}

	public static boolean isHunkOk(Object input) {
		if (input != null && input instanceof DiffNode){
			ITypedElement right = ((DiffNode) input).getRight();
			HunkResult element = Adapters.adapt(right, HunkResult.class);
			if (element != null) {
				return element.isOK();
			}
			ITypedElement left = ((DiffNode) input).getLeft();
			element = Adapters.adapt(left, HunkResult.class);
			if (element != null)
				return element.isOK();
		}
		return false;
	}

	public static void schedule(Job job, IWorkbenchSite site) {
		if (site != null) {
			IWorkbenchSiteProgressService siteProgress = site.getAdapter(IWorkbenchSiteProgressService.class);
			if (siteProgress != null) {
				siteProgress.schedule(job, 0, true /* use half-busy cursor */);
				return;
			}
		}
		job.schedule();
	}

	public static void runInUIThread(final Runnable runnable) {
		if (Display.getCurrent() != null) {
			BusyIndicator.showWhile(Display.getCurrent(), runnable);
		} else {
			Display.getDefault().syncExec(() -> BusyIndicator.showWhile(Display.getCurrent(), runnable));
		}
	}

	/**
	 * @param connection a connection for which the timeout is set
	 * @param timeout an int that specifies the connect timeout value in milliseconds
	 * @return whether the timeout has been successfully set
	 */
	public static boolean setReadTimeout(URLConnection connection, int timeout) {
		Method[] methods = connection.getClass().getMethods();
		for (Method method : methods) {
			if (method.getName().equals("setReadTimeout")) { //$NON-NLS-1$
				try {
					method.invoke(connection, new Object[] {Integer.valueOf(timeout)});
					return true;
				} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
					// ignore
				}
			}
		}
		return false;
	}

	/**
	 * Loads content of file under <code>url</code> displaying progress on given
	 * context.
	 *
	 * @param url
	 * @param context
	 * @return the content of file under given URL, or <code>null</code> if URL
	 *         could not be loaded
	 * @throws InvocationTargetException
	 *             thrown on errors while URL loading
	 * @throws OperationCanceledException
	 * @throws InterruptedException
	 */
	public static String getURLContents(final URL url, IRunnableContext context)
			throws InvocationTargetException, OperationCanceledException,
			InterruptedException {
		final String[] result = new String[1];
		context.run(true, true, monitor -> {
			SubMonitor progress = SubMonitor.convert(monitor,
					PatchMessages.InputPatchPage_URLConnecting, 100);
			try {
				URLConnection connection = url.openConnection();
				progress.worked(10);
				if (monitor.isCanceled())
					throw new OperationCanceledException();
				setReadTimeout(connection, 60 * 1000);
				progress.setTaskName(PatchMessages.InputPatchPage_URLFetchingContent);
				String enc = connection.getContentEncoding();
				if (enc == null)
					enc = ResourcesPlugin.getEncoding();
				result[0] = Utilities.readString(
						connection.getInputStream(), enc,
						connection.getContentLength(),
						progress.newChild(90));
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			}
		});
		return result[0];
	}

	/**
	 * Applies the compare filters to the lines of text taken from the specified
	 * contributors
	 *
	 * @param thisLine
	 * @param thisContributor
	 * @param otherLine
	 * @param otherContributor
	 * @param filters
	 *            may be null
	 * @return returns the result of applying the filters to the line from the
	 *         contributor
	 */
	public static String applyCompareFilters(String thisLine,
			char thisContributor, String otherLine, char otherContributor,
			ICompareFilter[] filters) {
		IRegion[][] ignoredRegions = new IRegion[filters.length][];

		HashMap<String, Comparable<?>> input = new HashMap<>(4);
		input.put(ICompareFilter.THIS_LINE, thisLine);
		input.put(ICompareFilter.THIS_CONTRIBUTOR, thisContributor);
		input.put(ICompareFilter.OTHER_LINE, otherLine);
		input.put(ICompareFilter.OTHER_CONTRIBUTOR, otherContributor);
		for (int i = 0; i < filters.length; i++) {
			ignoredRegions[i] = filters[i].getFilteredRegions(input);
		}

		boolean[] ignored = new boolean[thisLine.length()];
		for (IRegion[] regions : ignoredRegions) {
			if (regions != null) {
				for (IRegion region : regions) {
					if (region != null) {
						for (int l = 0; l < region.getLength(); l++) {
							ignored[region.getOffset() + l] = true;
						}
					}
				}
			}
		}
		StringBuilder buffer = new StringBuilder(thisLine.length());
		for (int i = 0; i < ignored.length; i++) {
			if (!ignored[i]) {
				buffer.append(thisLine.charAt(i));
			}
		}
		return buffer.toString();
	}

	/**
	 * Executes the given runnable. Uses the {@link org.eclipse.ui.progress.IProgressService IProgressService}
	 * if available.
	 *
	 * @param runnable
	 *            The {@link IRunnableWithProgress} to execute.
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	public static void executeRunnable(IRunnableWithProgress runnable) throws InvocationTargetException,
			InterruptedException {
		executeRunnable(runnable, true, true);
	}

	/**
	 * Executes the given runnable. Uses the {@link org.eclipse.ui.progress.IProgressService IProgressService}
	 * if available.
	 *
	 * @param runnable
	 *            The {@link IRunnableWithProgress} to execute.
	 * @param fork indicates whether to run within a separate thread.
	 * @param cancelable indicates whether the operation shall be cancelable
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	public static void executeRunnable(IRunnableWithProgress runnable, boolean fork, boolean cancelable) throws InvocationTargetException,
			InterruptedException {
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getProgressService().run(fork, cancelable, runnable);
		} else {
			runnable.run(new NullProgressMonitor());
		}
	}

	/**
	 * Sets the menu image for the given {@link Item}. Uses the workbench shared image if available, otherwise
	 * creates a new image and adds a dispose listener.
	 *
	 * @param item
	 *            The {@link Item} for which the menu image is to be set.
	 */
	public static void setMenuImage(final Item item) {
		final Image image;
		if (PlatformUI.isWorkbenchRunning()) {
			image = PlatformUI.getWorkbench().getSharedImages().getImage(
			/* IWorkbenchGraphicConstants */"IMG_LCL_VIEW_MENU"); //$NON-NLS-1$
		} else {
			image = CompareUIPlugin.getImageDescriptor("elcl16/view_menu.png").createImage(); //$NON-NLS-1$
			item.addDisposeListener(e -> {
				Image img = item.getImage();
				if ((img != null) && (!img.isDisposed())) {
					img.dispose();
				}
			});
		}
		item.setImage(image);
	}
}
