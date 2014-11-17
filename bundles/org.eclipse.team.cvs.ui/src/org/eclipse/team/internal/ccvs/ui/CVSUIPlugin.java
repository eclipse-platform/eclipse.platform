/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Philippe Ombredanne - bug 84808
 *     William Mitsuda (wmitsuda@gmail.com) - Bug 153879 [Wizards] configurable size of cvs commit comment history
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 161536 Warn user when committing resources with problem markers
 *******************************************************************************/

package org.eclipse.team.internal.ccvs.ui;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.console.CVSOutputConsole;
import org.eclipse.team.internal.ccvs.ui.model.CVSAdapterFactory;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryRoot;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.ui.*;
import org.eclipse.ui.*;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * UI Plugin for CVS provider-specific workbench functionality.
 */
public class CVSUIPlugin extends AbstractUIPlugin {
	/**
	 * The id of the CVS plug-in
	 */
	public static final String ID = "org.eclipse.team.cvs.ui"; //$NON-NLS-1$
	public static final String DECORATOR_ID = "org.eclipse.team.cvs.ui.decorator"; //$NON-NLS-1$
	
	/**
	 * Property constant indicating the decorator configuration has changed. 
	 */
	public static final String P_DECORATORS_CHANGED = CVSUIPlugin.ID  + ".P_DECORATORS_CHANGED";	 //$NON-NLS-1$

	private ServiceRegistration debugRegistration;
	private Hashtable imageDescriptors = new Hashtable(20);
	private static List propertyChangeListeners = new ArrayList(5);
	
	/**
	 * The singleton plug-in instance
	 */
	private static CVSUIPlugin plugin;
	
	/**
	 * The CVS console
	 */
	private CVSOutputConsole console;
	
	/**
	 * The repository manager
	 */
	private RepositoryManager repositoryManager;

	/**
	 * CVSUIPlugin constructor
	 * 
	 * @param descriptor  the plugin descriptor
	 */
	public CVSUIPlugin() {
		super();
		plugin = this;
	}
	
	/**
	 * Returns the standard display to be used. The method first checks, if
	 * the thread calling this method has an associated display. If so, this
	 * display is returned. Otherwise the method returns the default display.
	 */
	public static Display getStandardDisplay() {
		Display display= Display.getCurrent();
		if (display == null) {
			display= Display.getDefault();
		}
		return display;		
	}
	
	/**
	 * Creates an image and places it in the image registry.
	 */
	protected void createImageDescriptor(String id) {
		URL url = FileLocator.find(CVSUIPlugin.getPlugin().getBundle(), new Path(ICVSUIConstants.ICON_PATH + id), null);
		ImageDescriptor desc = ImageDescriptor.createFromURL(url);
		imageDescriptors.put(id, desc);
	}
	
	/**
	 * Returns the active workbench page. Note that the active page may not be
	 * the one that the user perceives as active in some situations so this
	 * method of obtaining the activate page should only be used if no other
	 * method is available.
	 * 
	 * @return the active workbench page
	 */
	public static IWorkbenchPage getActivePage() {
		return TeamUIPlugin.getActivePage();
	}
	
	/**
	 * Register for changes made to Team properties.
	 */
	public static void addPropertyChangeListener(IPropertyChangeListener listener) {
		propertyChangeListeners.add(listener);
	}
	
	/**
	 * Remove a Team property changes.
	 */
	public static void removePropertyChangeListener(IPropertyChangeListener listener) {
		propertyChangeListeners.remove(listener);
	}
	
	/**
	 * Broadcast a Team property change.
	 */
	public static void broadcastPropertyChange(PropertyChangeEvent event) {
		for (Iterator it = propertyChangeListeners.iterator(); it.hasNext();) {
			IPropertyChangeListener listener = (IPropertyChangeListener)it.next();			
			listener.propertyChange(event);
		}
	}
	
	/**
	 * Run an operation involving the given resource. If an exception is thrown
	 * and the code on the status is IResourceStatus.OUT_OF_SYNC_LOCAL then
	 * the user will be prompted to refresh and try again. If they agree, then the
	 * supplied operation will be run again.
	 */
	public static void runWithRefresh(Shell parent, IResource[] resources, 
									  IRunnableWithProgress runnable, IProgressMonitor monitor) 
	throws InvocationTargetException, InterruptedException {
		boolean firstTime = true;
		while(true) {
			try {
				runnable.run(monitor);
				return;
			} catch (InvocationTargetException e) {
				if (! firstTime) throw e;
				IStatus status = null;
				if (e.getTargetException() instanceof CoreException) {
					status = ((CoreException)e.getTargetException()).getStatus();
				} else if (e.getTargetException() instanceof TeamException) {
					status = ((TeamException)e.getTargetException()).getStatus();
				} else {
					throw e;
				}
				if (status.getCode() == IResourceStatus.OUT_OF_SYNC_LOCAL) {
					if (promptToRefresh(parent, resources, status)) {
						try {
							for (int i = 0; i < resources.length; i++) {
								resources[i].refreshLocal(IResource.DEPTH_INFINITE, null);
							}
						} catch (CoreException coreEx) {
							// Throw the original exception to the caller
							log(coreEx);
							throw e;
						}
						firstTime = false;
						// Fall through and the operation will be tried again
					} else {
						// User chose not to continue. Treat it as a cancel.
						throw new InterruptedException();
					}
				} else {
					throw e;
				}
			}
		}
	}
	
	private static boolean promptToRefresh(final Shell shell, final IResource[] resources, final IStatus status) {
		final boolean[] result = new boolean[] { false};
		Runnable runnable = new Runnable() {
			public void run() {
				Shell shellToUse = shell;
				if (shell == null) {
					shellToUse = new Shell(Display.getCurrent());
				}
				String question;
				if (resources.length == 1) {
					question = NLS.bind(CVSUIMessages.CVSUIPlugin_refreshQuestion, new String[] { status.getMessage(), resources[0].getFullPath().toString() }); 
				} else {
					question = NLS.bind(CVSUIMessages.CVSUIPlugin_refreshMultipleQuestion, new String[] { status.getMessage() }); 
				}
				result[0] = MessageDialog.openQuestion(shellToUse, CVSUIMessages.CVSUIPlugin_refreshTitle, question); 
			}
		};
		Display.getDefault().syncExec(runnable);
		return result[0];
	}
	
	/**
	 * Creates a busy cursor and runs the specified runnable.
	 * May be called from a non-UI thread.
	 * 
	 * @param parent the parent Shell for the dialog
	 * @param cancelable if true, the dialog will support cancelation
	 * @param runnable the runnable
	 * 
	 * @exception InvocationTargetException when an exception is thrown from the runnable
	 * @exception InterruptedException when the progress monitor is canceled
	 */
	public static void runWithProgress(Shell parent, boolean cancelable,
									   final IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
		Utils.runWithProgress(parent, cancelable, runnable);
	}
	
	/**
	 * Returns the image descriptor for the given image ID.
	 * Returns null if there is no such image.
	 */
	public ImageDescriptor getImageDescriptor(String id) {
		return (ImageDescriptor)imageDescriptors.get(id);
	}
	
	/**
	 * Returns the singleton plug-in instance.
	 * 
	 * @return the plugin instance
	 */
	public static CVSUIPlugin getPlugin() {
		return plugin;
	}
	
	/**
	 * Returns the repository manager
	 * 
	 * @return the repository manager
	 */
	public synchronized RepositoryManager getRepositoryManager() {
		if (repositoryManager == null) {
			repositoryManager = new RepositoryManager();
			repositoryManager.startup();
		}
		return repositoryManager;
	}
    
	/**
	 * Initializes the table of images used in this plugin.
	 */
	private void initializeImages() {
		// objects
		createImageDescriptor(ICVSUIConstants.IMG_REPOSITORY); 
		createImageDescriptor(ICVSUIConstants.IMG_REFRESH);
		createImageDescriptor(ICVSUIConstants.IMG_REFRESH_ENABLED);
		createImageDescriptor(ICVSUIConstants.IMG_REFRESH_DISABLED);
		createImageDescriptor(ICVSUIConstants.IMG_LINK_WITH_EDITOR);
		createImageDescriptor(ICVSUIConstants.IMG_LINK_WITH_EDITOR_ENABLED);
		createImageDescriptor(ICVSUIConstants.IMG_COLLAPSE_ALL);
		createImageDescriptor(ICVSUIConstants.IMG_COLLAPSE_ALL_ENABLED);
		createImageDescriptor(ICVSUIConstants.IMG_NEWLOCATION);
		createImageDescriptor(ICVSUIConstants.IMG_CVSLOGO);
		createImageDescriptor(ICVSUIConstants.IMG_TAG);
		createImageDescriptor(ICVSUIConstants.IMG_MODULE);
		createImageDescriptor(ICVSUIConstants.IMG_CLEAR);
		createImageDescriptor(ICVSUIConstants.IMG_CLEAR_ENABLED);
		createImageDescriptor(ICVSUIConstants.IMG_CLEAR_DISABLED);
		createImageDescriptor(ICVSUIConstants.IMG_BRANCHES_CATEGORY);
		createImageDescriptor(ICVSUIConstants.IMG_VERSIONS_CATEGORY);
		createImageDescriptor(ICVSUIConstants.IMG_DATES_CATEGORY);
		createImageDescriptor(ICVSUIConstants.IMG_PROJECT_VERSION);
		createImageDescriptor(ICVSUIConstants.IMG_WIZBAN_MERGE);
		createImageDescriptor(ICVSUIConstants.IMG_WIZBAN_SHARE);
		createImageDescriptor(ICVSUIConstants.IMG_WIZBAN_DIFF);
		createImageDescriptor(ICVSUIConstants.IMG_WIZBAN_KEYWORD);
		createImageDescriptor(ICVSUIConstants.IMG_WIZBAN_NEW_LOCATION);
		createImageDescriptor(ICVSUIConstants.IMG_WIZBAN_IMPORT);
		createImageDescriptor(ICVSUIConstants.IMG_MERGEABLE_CONFLICT);
		createImageDescriptor(ICVSUIConstants.IMG_QUESTIONABLE);
		createImageDescriptor(ICVSUIConstants.IMG_MERGED);
		createImageDescriptor(ICVSUIConstants.IMG_EDITED);
		createImageDescriptor(ICVSUIConstants.IMG_NO_REMOTEDIR);
		createImageDescriptor(ICVSUIConstants.IMG_CVS_CONSOLE);
		createImageDescriptor(ICVSUIConstants.IMG_DATE);
		createImageDescriptor(ICVSUIConstants.IMG_CHANGELOG);
		createImageDescriptor(ICVSUIConstants.IMG_FILTER_HISTORY);
		createImageDescriptor(ICVSUIConstants.IMG_LOCALMODE);
		createImageDescriptor(ICVSUIConstants.IMG_LOCALREMOTE_MODE);
		createImageDescriptor(ICVSUIConstants.IMG_REMOTEMODE);
		createImageDescriptor(ICVSUIConstants.IMG_LOCALMODE_DISABLED);
		createImageDescriptor(ICVSUIConstants.IMG_LOCALREMOTE_MODE_DISABLED);
		createImageDescriptor(ICVSUIConstants.IMG_REMOTEMODE_DISABLED);
		createImageDescriptor(ICVSUIConstants.IMG_LOCALREVISION_TABLE);
		createImageDescriptor(ICVSUIConstants.IMG_REMOTEREVISION_TABLE);
		createImageDescriptor(ICVSUIConstants.IMG_COMPARE_VIEW);
		
		// special
		createImageDescriptor("glyphs/glyph1.gif");  //$NON-NLS-1$
		createImageDescriptor("glyphs/glyph2.gif");  //$NON-NLS-1$
		createImageDescriptor("glyphs/glyph3.gif");  //$NON-NLS-1$
		createImageDescriptor("glyphs/glyph4.gif");  //$NON-NLS-1$
		createImageDescriptor("glyphs/glyph5.gif");  //$NON-NLS-1$
		createImageDescriptor("glyphs/glyph6.gif");  //$NON-NLS-1$
		createImageDescriptor("glyphs/glyph7.gif");  //$NON-NLS-1$
		createImageDescriptor("glyphs/glyph8.gif");  //$NON-NLS-1$
	}
	/**
	 * Convenience method for logging statuses to the plugin log
	 * 
	 * @param status  the status to log
	 */
	public static void log(IStatus status) {
		getPlugin().getLog().log(status);
	}
	
	public static void log(CoreException e) {
		log(e.getStatus().getSeverity(), CVSUIMessages.simpleInternal, e); 
	}
	
	/**
	 * Log the given exception along with the provided message and severity indicator
	 */
	public static void log(int severity, String message, Throwable e) {
		log(new Status(severity, ID, 0, message, e));
	}
	
	// flags to tailor error reporting
	/**
	 * Use this flag if you are calling openError from a none UI thread. The
	 * flag is checked in openDialog method.
	 */
	public static final int PERFORM_SYNC_EXEC = 1;
	/**
	 * Use this flag to log all TeamExceptions and its descendants.
	 */
	public static final int LOG_TEAM_EXCEPTIONS = 2;
	/**
	 * Use this flag to log all CoreExceptions excluding TeamExceptions and its
	 * descendants.
	 */
	public static final int LOG_NONTEAM_CORE_EXCEPTIONS = 4;
	/**
	 * Use this flag to log an exception other than CoreException (e.g.
	 * IOException).
	 */
	public static final int LOG_OTHER_EXCEPTIONS = 8;
	/**
	 * Use this flag to log all CoreExceptions.
	 */
	public static final int LOG_CORE_EXCEPTIONS = LOG_TEAM_EXCEPTIONS
			| LOG_NONTEAM_CORE_EXCEPTIONS;
	/**
	 * Use this flag to log all exceptions different than TeamException and its
	 * descendant.
	 */
	public static final int LOG_NONTEAM_EXCEPTIONS = LOG_NONTEAM_CORE_EXCEPTIONS
			| LOG_OTHER_EXCEPTIONS;
	
	/**
	 * Convenience method for showing an error dialog 
	 * @param shell a valid shell or null
	 * @param exception the exception to be report
	 * @param title the title to be displayed
	 * @return IStatus the status that was displayed to the user
	 */
	public static IStatus openError(Shell shell, String title, String message, Throwable exception) {
		return openError(shell, title, message, exception, LOG_OTHER_EXCEPTIONS);
	}
	
	/**
	 * Convenience method for showing an error dialog 
	 * @param shell a valid shell or null
	 * @param exception the exception to be report
	 * @param title the title to be displayed
	 * @param flags customized attributes for the error handling
	 * @return IStatus the status that was displayed to the user
	 */
	public static IStatus openError(Shell providedShell, String title, String message, Throwable exception, int flags) {
		// Unwrap InvocationTargetExceptions
		if (exception instanceof InvocationTargetException) {
			Throwable target = ((InvocationTargetException)exception).getTargetException();
			// re-throw any runtime exceptions or errors so they can be handled by the workbench
			if (target instanceof RuntimeException) {
				throw (RuntimeException)target;
			}
			if (target instanceof Error) {
				throw (Error)target;
			} 
			return openError(providedShell, title, message, target, flags);
		}
		
		// Determine the status to be displayed (and possibly logged)
		IStatus status = null;
		boolean log = false;
		if (exception instanceof TeamException) {
			status = ((TeamException)exception).getStatus();
			log = ((flags & LOG_TEAM_EXCEPTIONS) > 0);
		} else if (exception instanceof CoreException) {
			status = ((CoreException)exception).getStatus();
			log = ((flags & LOG_NONTEAM_CORE_EXCEPTIONS) > 0);
		} else if (exception instanceof InterruptedException) {
			return new CVSStatus(IStatus.OK, CVSUIMessages.ok); 
		} else if (exception != null) {
			status = new CVSStatus(IStatus.ERROR, CVSUIMessages.internal, exception); 
			log = ((flags & LOG_OTHER_EXCEPTIONS) > 0);
			if (title == null) title = CVSUIMessages.internal; 
		}
		
		// Check for a build error and report it differently
		if (status.getCode() == IResourceStatus.BUILD_FAILED) {
			message = CVSUIMessages.buildError; 
			log = true;
		}
		
		// Check for multi-status with only one child
		if (status.isMultiStatus() && status.getChildren().length == 1) {
			status = status.getChildren()[0];
		}
		if (status.isOK()) return status;
		
		// Log if the user requested it
		if (log) CVSUIPlugin.log(status.getSeverity(), status.getMessage(), exception);

		return openDialog(providedShell, title, message, status, flags);
	}

	/**
	 * Convenience method for showing an error dialog
	 * 
	 * @param shell a valid shell or null
	 * @param status the status to be reported
	 * @param title the title to be displayed
	 * @param flags customized attributes for the error handling
	 * @return IStatus the status that was displayed to the user
	 */
	public static IStatus openError(Shell providedShell, String title,
			String message, IStatus status, int flags) {
		boolean log = false;

		// Check for a build error and report it differently
		if (status.getCode() == IResourceStatus.BUILD_FAILED) {
			message = CVSUIMessages.buildError;
			log = true;
		}

		// Check for multi-status with only one child
		if (status.isMultiStatus() && status.getChildren().length == 1) {
			status = status.getChildren()[0];
		}
		if (status.isOK())
			return status;

		// Log if the user requested it
		if (log)
			CVSUIPlugin.log(status);

		return openDialog(providedShell, title, message, status, flags);
	}

	private static IStatus openDialog(Shell providedShell, final String title,
			final String message, final IStatus status, int flags) {
		// Create a runnable that will display the error status
		final IOpenableInShell openable = new IOpenableInShell() {
			public void open(Shell shell) {
				if (status.getSeverity() == IStatus.INFO
						&& !status.isMultiStatus()) {
					MessageDialog.openInformation(shell,
							CVSUIMessages.information, status.getMessage());
				} else {
					ErrorDialog.openError(shell, title, message, status);
				}
			}
		};
		openDialog(providedShell, openable, flags);

		// return the status we display
		return status;
	}

	/**
	 * Interface that allows a shell to be passed to an open method. The
	 * provided shell can be used without sync-execing, etc.
	 */
	public interface IOpenableInShell {
		public void open(Shell shell);
	}
	
	/**
	 * Open the dialog code provided in the IOpenableInShell, ensuring that 
	 * the provided shell is valid. This method will provide a shell to the
	 * IOpenableInShell if one is not provided to the method.
	 * 
	 * @param providedShell
	 * @param openable
	 * @param flags
	 */
	public static void openDialog(Shell providedShell, final IOpenableInShell openable, int flags) {
		// If no shell was provided, try to get one from the active window
		if (providedShell == null) {
			IWorkbenchWindow window = CVSUIPlugin.getPlugin().getWorkbench().getActiveWorkbenchWindow();
			if (window != null) {
				providedShell = window.getShell();
				// sync-exec when we do this just in case
				flags = flags | PERFORM_SYNC_EXEC;
			}
		}
		
		// Create a runnable that will display the error status
		final Shell shell = providedShell;
		Runnable outerRunnable = new Runnable() {
			public void run() {
				Shell displayShell;
				if (shell == null) {
					Display display = Display.getCurrent();
					displayShell = new Shell(display);
				} else {
					displayShell = shell;
				}
				openable.open(displayShell);
				if (shell == null) {
					displayShell.dispose();
				}
			}
		};
		
		// Execute the above runnable as determined by the parameters
		if (shell == null || (flags & PERFORM_SYNC_EXEC) > 0) {
			Display display;
			if (shell == null) {
				display = Display.getCurrent();
				if (display == null) {
					display = Display.getDefault();
				}
			} else {
				display = shell.getDisplay();
			}
			display.syncExec(outerRunnable);
		} else {
			outerRunnable.run();
		}
	}
	
	
	/**
	 * Initializes the preferences for this plugin if necessary.
	 */
	protected void initializeDefaultPluginPreferences() {
		IPreferenceStore store = getPreferenceStore();
		// Get the plugin preferences for CVS Core
		Preferences corePrefs = CVSProviderPlugin.getPlugin().getPluginPreferences();
		
		store.setDefault(ICVSUIConstants.PREF_REPOSITORIES_ARE_BINARY, false);
		store.setDefault(ICVSUIConstants.PREF_SHOW_COMMENTS, true);
		store.setDefault(ICVSUIConstants.PREF_WRAP_COMMENTS, true);
		store.setDefault(ICVSUIConstants.PREF_SHOW_TAGS, true);
		store.setDefault(ICVSUIConstants.PREF_SHOW_SEARCH, false);
		store.setDefault(ICVSUIConstants.PREF_REVISION_MODE, 0);
		store.setDefault(ICVSUIConstants.PREF_GROUPBYDATE_MODE, true);
		store.setDefault(ICVSUIConstants.PREF_HISTORY_VIEW_EDITOR_LINKING, false);
		store.setDefault(ICVSUIConstants.PREF_PRUNE_EMPTY_DIRECTORIES, CVSProviderPlugin.DEFAULT_PRUNE);
		store.setDefault(ICVSUIConstants.PREF_TIMEOUT, CVSProviderPlugin.DEFAULT_TIMEOUT);
		store.setDefault(ICVSUIConstants.PREF_CONSIDER_CONTENTS, true);
		store.setDefault(ICVSUIConstants.PREF_SYNCVIEW_REGEX_FILTER_PATTERN, ""); //default pattern is empty string //$NON-NLS-1$		
		store.setDefault(ICVSUIConstants.PREF_COMPRESSION_LEVEL, CVSProviderPlugin.DEFAULT_COMPRESSION_LEVEL);
		store.setDefault(ICVSUIConstants.PREF_TEXT_KSUBST, CVSProviderPlugin.DEFAULT_TEXT_KSUBST_OPTION.toMode());
		store.setDefault(ICVSUIConstants.PREF_USE_PLATFORM_LINEEND, true);
		store.setDefault(ICVSUIConstants.PREF_REPLACE_UNMANAGED, true);
		store.setDefault(ICVSUIConstants.PREF_CVS_RSH, CVSProviderPlugin.DEFAULT_CVS_RSH);
		store.setDefault(ICVSUIConstants.PREF_CVS_RSH_PARAMETERS, CVSProviderPlugin.DEFAULT_CVS_RSH_PARAMETERS);
		store.setDefault(ICVSUIConstants.PREF_CVS_SERVER, CVSProviderPlugin.DEFAULT_CVS_SERVER);
		store.setDefault(ICVSUIConstants.PREF_EXT_CONNECTION_METHOD_PROXY, "ext"); //$NON-NLS-1$
		store.setDefault(ICVSUIConstants.PREF_PROMPT_ON_CHANGE_GRANULARITY, true);
		store.setDefault(ICVSUIConstants.PREF_DETERMINE_SERVER_VERSION, true);
		store.setDefault(ICVSUIConstants.PREF_CONFIRM_MOVE_TAG, CVSProviderPlugin.DEFAULT_CONFIRM_MOVE_TAG);
		store.setDefault(ICVSUIConstants.PREF_DEBUG_PROTOCOL, false);
		store.setDefault(ICVSUIConstants.PREF_WARN_REMEMBERING_MERGES, true);
		store.setDefault(ICVSUIConstants.PREF_SHOW_COMPARE_REVISION_IN_DIALOG, false);
		store.setDefault(ICVSUIConstants.PREF_COMMIT_SET_DEFAULT_ENABLEMENT, false);
		store.setDefault(ICVSUIConstants.PREF_AUTO_REFRESH_TAGS_IN_TAG_SELECTION_DIALOG, false);
        store.setDefault(ICVSUIConstants.PREF_AUTO_SHARE_ON_IMPORT, true);
        store.setDefault(ICVSUIConstants.PREF_ENABLE_WATCH_ON_EDIT, false);
        store.setDefault(ICVSUIConstants.PREF_USE_PROJECT_NAME_ON_CHECKOUT, false);
        store.setDefault(ICVSUIConstants.PREF_COMMIT_FILES_DISPLAY_THRESHOLD, 1000);
        store.setDefault(ICVSUIConstants.PREF_COMMIT_COMMENTS_MAX_HISTORY, RepositoryManager.DEFAULT_MAX_COMMENTS);
		
		PreferenceConverter.setDefault(store, ICVSUIConstants.PREF_CONSOLE_COMMAND_COLOR, new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, ICVSUIConstants.PREF_CONSOLE_MESSAGE_COLOR, new RGB(0, 0, 255));
		PreferenceConverter.setDefault(store, ICVSUIConstants.PREF_CONSOLE_ERROR_COLOR, new RGB(255, 0, 0));
		store.setDefault(ICVSUIConstants.PREF_CONSOLE_SHOW_ON_MESSAGE, false);
		store.setDefault(ICVSUIConstants.PREF_CONSOLE_LIMIT_OUTPUT, true);	
		store.setDefault(ICVSUIConstants.PREF_CONSOLE_HIGH_WATER_MARK, 500000);	
		store.setDefault(ICVSUIConstants.PREF_CONSOLE_WRAP, false);	
		store.setDefault(ICVSUIConstants.PREF_CONSOLE_WIDTH, 80);
			
		store.setDefault(ICVSUIConstants.PREF_FILETEXT_DECORATION, CVSDecoratorConfiguration.DEFAULT_FILETEXTFORMAT);
		store.setDefault(ICVSUIConstants.PREF_FOLDERTEXT_DECORATION, CVSDecoratorConfiguration.DEFAULT_FOLDERTEXTFORMAT);
		store.setDefault(ICVSUIConstants.PREF_PROJECTTEXT_DECORATION, CVSDecoratorConfiguration.DEFAULT_PROJECTTEXTFORMAT);
		
		store.setDefault(ICVSUIConstants.PREF_FIRST_STARTUP, true);
		store.setDefault(ICVSUIConstants.PREF_ADDED_FLAG, CVSDecoratorConfiguration.DEFAULT_ADDED_FLAG);
		store.setDefault(ICVSUIConstants.PREF_DIRTY_FLAG, CVSDecoratorConfiguration.DEFAULT_DIRTY_FLAG);	
		store.setDefault(ICVSUIConstants.PREF_SHOW_ADDED_DECORATION, true);
		store.setDefault(ICVSUIConstants.PREF_SHOW_HASREMOTE_DECORATION, true);
		store.setDefault(ICVSUIConstants.PREF_SHOW_DIRTY_DECORATION, false);
		store.setDefault(ICVSUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION, true);
		store.setDefault(ICVSUIConstants.PREF_CALCULATE_DIRTY, true);	
		store.setDefault(ICVSUIConstants.PREF_USE_FONT_DECORATORS, false);
		store.setDefault(ICVSUIConstants.PREF_PROMPT_ON_MIXED_TAGS, true);
		store.setDefault(ICVSUIConstants.PREF_PROMPT_ON_SAVING_IN_SYNC, true);
		store.setDefault(ICVSUIConstants.PREF_SAVE_DIRTY_EDITORS, ICVSUIConstants.OPTION_PROMPT);
		
		store.setDefault(ICVSUIConstants.PREF_ANNOTATE_PROMPTFORBINARY, MessageDialogWithToggle.PROMPT);
		store.setDefault(ICVSUIConstants.PREF_ALLOW_EMPTY_COMMIT_COMMENTS, MessageDialogWithToggle.PROMPT);
		store.setDefault(ICVSUIConstants.PREF_INCLUDE_CHANGE_SETS_IN_COMMIT, MessageDialogWithToggle.NEVER);
		store.setDefault(ICVSUIConstants.PREF_ALLOW_COMMIT_WITH_WARNINGS, MessageDialogWithToggle.ALWAYS);
		store.setDefault(ICVSUIConstants.PREF_ALLOW_COMMIT_WITH_ERRORS, MessageDialogWithToggle.PROMPT);
		
		store.setDefault(ICVSUIConstants.PREF_UPDATE_HANDLING, ICVSUIConstants.PREF_UPDATE_HANDLING_TRADITIONAL);
		store.setDefault(ICVSUIConstants.PREF_UPDATE_PREVIEW, ICVSUIConstants.PREF_UPDATE_PREVIEW_IN_SYNCVIEW);
		
		store.setDefault(ICVSUIConstants.PREF_ENABLE_MODEL_SYNC, true);
		store.setDefault(ICVSUIConstants.PREF_OPEN_COMPARE_EDITOR_FOR_SINGLE_FILE, true);
		
		// Set the watch/edit preferences defaults and values
		store.setDefault(ICVSUIConstants.PREF_CHECKOUT_READ_ONLY, corePrefs.getDefaultBoolean(CVSProviderPlugin.READ_ONLY));
		store.setDefault(ICVSUIConstants.PREF_EDIT_ACTION, ICVSUIConstants.PREF_EDIT_IN_BACKGROUND);
		store.setDefault(ICVSUIConstants.PREF_EDIT_PROMPT, ICVSUIConstants.PREF_EDIT_PROMPT_IF_EDITORS);
        store.setDefault(ICVSUIConstants.PREF_UPDATE_PROMPT, ICVSUIConstants.PREF_UPDATE_PROMPT_NEVER);
		// Ensure that the preference values in UI match Core
		store.setValue(ICVSUIConstants.PREF_CHECKOUT_READ_ONLY, corePrefs.getBoolean(CVSProviderPlugin.READ_ONLY));
		
		// Forward the values to the CVS plugin
		CVSProviderPlugin.getPlugin().setPruneEmptyDirectories(store.getBoolean(ICVSUIConstants.PREF_PRUNE_EMPTY_DIRECTORIES));
		CVSProviderPlugin.getPlugin().setTimeout(store.getInt(ICVSUIConstants.PREF_TIMEOUT));
		CVSProviderPlugin.getPlugin().setCvsRshCommand(store.getString(ICVSUIConstants.PREF_CVS_RSH));
		CVSProviderPlugin.getPlugin().setCvsRshParameters(store.getString(ICVSUIConstants.PREF_CVS_RSH_PARAMETERS));
		CVSProviderPlugin.getPlugin().setCvsServer(store.getString(ICVSUIConstants.PREF_CVS_SERVER));
		CVSRepositoryLocation.setExtConnectionMethodProxy(store.getString(ICVSUIConstants.PREF_EXT_CONNECTION_METHOD_PROXY));
		CVSProviderPlugin.getPlugin().setQuietness(CVSPreferencesPage.getQuietnessOptionFor(store.getInt(ICVSUIConstants.PREF_QUIETNESS)));
		CVSProviderPlugin.getPlugin().setCompressionLevel(store.getInt(ICVSUIConstants.PREF_COMPRESSION_LEVEL));
		CVSProviderPlugin.getPlugin().setReplaceUnmanaged(store.getBoolean(ICVSUIConstants.PREF_REPLACE_UNMANAGED));
		CVSProviderPlugin.getPlugin().setDefaultTextKSubstOption(KSubstOption.fromMode(store.getString(ICVSUIConstants.PREF_TEXT_KSUBST)));
		CVSProviderPlugin.getPlugin().setUsePlatformLineend(store.getBoolean(ICVSUIConstants.PREF_USE_PLATFORM_LINEEND));
		CVSProviderPlugin.getPlugin().setRepositoriesAreBinary(store.getBoolean(ICVSUIConstants.PREF_REPOSITORIES_ARE_BINARY));
		CVSProviderPlugin.getPlugin().setDetermineVersionEnabled(store.getBoolean(ICVSUIConstants.PREF_DETERMINE_SERVER_VERSION));
		CVSProviderPlugin.getPlugin().setDebugProtocol(CVSProviderPlugin.getPlugin().isDebugProtocol() || store.getBoolean(ICVSUIConstants.PREF_DEBUG_PROTOCOL));
        CVSProviderPlugin.getPlugin().setAutoshareOnImport(store.getBoolean(ICVSUIConstants.PREF_AUTO_SHARE_ON_IMPORT));
        
        // code to transfer CVS preference to Team preference
        if (store.getBoolean(ICVSUIConstants.PREF_SHOW_AUTHOR_IN_EDITOR)) {
        	store.setValue(ICVSUIConstants.PREF_SHOW_AUTHOR_IN_EDITOR, false);
        	IPreferenceStore teamStore = TeamUIPlugin.getPlugin().getPreferenceStore();
        	if (teamStore.isDefault(IPreferenceIds.SHOW_AUTHOR_IN_COMPARE_EDITOR))
        		teamStore.setValue(IPreferenceIds.SHOW_AUTHOR_IN_COMPARE_EDITOR, true);
        }
	}
	
	/**
	 * @see Plugin#start(BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);

		// register debug options listener
		Hashtable properties = new Hashtable(2);
		properties.put(DebugOptions.LISTENER_SYMBOLICNAME, ID);
		debugRegistration = context.registerService(DebugOptionsListener.class, Policy.DEBUG_OPTIONS_LISTENER, properties);

		initializeImages();
		
		CVSAdapterFactory factory = new CVSAdapterFactory();
		Platform.getAdapterManager().registerAdapters(factory, ICVSRemoteFile.class);
		Platform.getAdapterManager().registerAdapters(factory, ICVSRemoteFolder.class);
		Platform.getAdapterManager().registerAdapters(factory, ICVSRepositoryLocation.class);
		Platform.getAdapterManager().registerAdapters(factory, RepositoryRoot.class);
		
		try {
            console = new CVSOutputConsole();
        } catch (RuntimeException e) {
            // Don't let the console bring down the CVS UI
            log(IStatus.ERROR, "Errors occurred starting the CVS console", e); //$NON-NLS-1$
        }
		
		IPreferenceStore store = getPreferenceStore();
		if (store.getBoolean(ICVSUIConstants.PREF_FIRST_STARTUP)) {
			// If we enable the decorator in the XML, the CVS plugin will be loaded
			// on startup even if the user never uses CVS. Therefore, we enable the 
			// decorator on the first start of the CVS plugin since this indicates that 
			// the user has done something with CVS. Subsequent startups will load
			// the CVS plugin unless the user disables the decorator. In this case,
			// we will not re-enable since we only enable automatically on the first startup.
			PlatformUI.getWorkbench().getDecoratorManager().setEnabled(CVSLightweightDecorator.ID, true);
			store.setValue(ICVSUIConstants.PREF_FIRST_STARTUP, false);
		}

	}
	
	/**
	 * @see Plugin#stop(BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		try {
			// unregister debug options listener
			debugRegistration.unregister();
			debugRegistration = null;

			try {
				if (repositoryManager != null)
					repositoryManager.shutdown();
			} catch (TeamException e) {
				throw new CoreException(e.getStatus());
			}
			
			if (console != null)
			    console.shutdown();
		} finally {
			super.stop(context);
		}
	}
	
	/**
	 * @return the CVS console
	 */
	public CVSOutputConsole getConsole() {
		return console;
	}
    
    public IEditorPart openEditor(ICVSRemoteFile file, IProgressMonitor monitor) throws InvocationTargetException {
        IWorkbench workbench = getWorkbench();
        IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
        try {
			return Utils.openEditor(page, (IFileRevision)file.getAdapter(IFileRevision.class), monitor);
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
    }

	/**
	 * Helper method which access the preference store to determine if the 
	 * project name from the project description file (.project) should be used
	 * as the project name on checkout.
	 */
	public boolean isUseProjectNameOnCheckout() {
		return getPreferenceStore().getBoolean(ICVSUIConstants.PREF_USE_PROJECT_NAME_ON_CHECKOUT);
	}

	public ActiveChangeSetManager getChangeSetManager() {
		return CVSProviderPlugin.getPlugin().getChangeSetManager();
	}

	public org.osgi.service.prefs.Preferences getInstancePreferences() {
		return InstanceScope.INSTANCE.getNode(getBundle().getSymbolicName());
	}
}
