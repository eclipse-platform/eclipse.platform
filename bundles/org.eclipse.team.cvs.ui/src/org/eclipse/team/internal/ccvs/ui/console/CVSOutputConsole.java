/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.console;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.listeners.IConsoleListener;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.*;

/**
 * Console that shows the output of CVS commands. It is shown as a page in the generic 
 * console view. It supports coloring for message, command, and error lines in addition
 * the font can be configured.
 * 
 * @since 3.0 
 */
public class CVSOutputConsole extends MessageConsole implements IConsoleListener, IPropertyChangeListener {
	
	// created colors for each line type - must be disposed at shutdown
	private Color commandColor;
	private Color messageColor;
	private Color errorColor;
	
	// used to time the commands
	private long commandStarted = 0;
	
	// streams for each command type - each stream has its own color
	private MessageConsoleStream commandStream;
	private MessageConsoleStream messageStream;
	private MessageConsoleStream errorStream;
	
	// preferences for showing the cvs console when cvs output is provided 
	private boolean showOnMessage;
	
	private ConsoleDocument document;
	private IConsoleManager consoleManager;
	
	// format for timings printed to console
	private static final DateFormat TIME_FORMAT;
	
	static {
		DateFormat format;
		try {
			format = new SimpleDateFormat(CVSUIMessages.Console_resultTimeFormat); 
		} catch (RuntimeException e) {
			// This can happen if the bundle contains an invalid  format
			format = new SimpleDateFormat("'(took 'm:ss.SSS')')"); //$NON-NLS-1$
		}
		TIME_FORMAT = format;
	}

	// Indicates whether the console is visible in the Console view
	private boolean visible = false;
	// Indicates whether the console's streams have been initialized
	private boolean initialized = false;
	
	/*
	 * Constant used for indenting error status printing
	 */
	private static final String NESTING = "   "; //$NON-NLS-1$
	
	/**
	 * Used to notify this console of lifecycle methods <code>init()</code>
	 * and <code>dispose()</code>.
	 */
	public class MyLifecycle implements org.eclipse.ui.console.IConsoleListener {
		@Override
		public void consolesAdded(IConsole[] consoles) {
			for (IConsole console : consoles) {
				if (console == CVSOutputConsole.this) {
					init();
				}
			}

		}
		@Override
		public void consolesRemoved(IConsole[] consoles) {
			for (IConsole console : consoles) {
				if (console == CVSOutputConsole.this) {
					ConsolePlugin.getDefault().getConsoleManager().removeConsoleListener(this);
					dispose();
				}
			}
		}
	}
	
	/**
	 * Constructor initializes preferences and colors but doesn't create the console
	 * page yet.
	 */
	public CVSOutputConsole() {
		super("CVS", CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_CVS_CONSOLE)); //$NON-NLS-1$
		showOnMessage = CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_CONSOLE_SHOW_ON_MESSAGE);
		document = new ConsoleDocument();
		consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		CVSProviderPlugin.getPlugin().setConsoleListener(CVSOutputConsole.this);
		CVSUIPlugin.getPlugin().getPreferenceStore().addPropertyChangeListener(CVSOutputConsole.this);
	}
	
	@Override
	protected void init() {
		// Called when console is added to the console view
		super.init();	
		
		initLimitOutput();
		initWrapSetting();
		
		//	Ensure that initialization occurs in the ui thread
		CVSUIPlugin.getStandardDisplay().asyncExec(() -> {
			JFaceResources.getFontRegistry().addListener(CVSOutputConsole.this);
			initializeStreams();
			dump();
		});
	}
	
	private void initWrapSetting() {
		IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
		if(store.getBoolean(ICVSUIConstants.PREF_CONSOLE_WRAP)) {
			setConsoleWidth(store.getInt(ICVSUIConstants.PREF_CONSOLE_WIDTH));
		} else {
			setConsoleWidth(-1);
		}
	}
	
	private void initLimitOutput() {
		IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
		if(store.getBoolean(ICVSUIConstants.PREF_CONSOLE_LIMIT_OUTPUT)) {
			setWaterMarks(1000, store.getInt(ICVSUIConstants.PREF_CONSOLE_HIGH_WATER_MARK));
		} else {
			setWaterMarks(-1, 0);
		}
	}
	
	/*
	 * Initialize thre streams of the console. Must be 
	 * called from the UI thread.
	 */
	private void initializeStreams() {
		synchronized(document) {
			if (!initialized) {
				commandStream = newMessageStream();
				errorStream = newMessageStream();
				messageStream = newMessageStream();
				// install colors
				commandColor = createColor(CVSUIPlugin.getStandardDisplay(), ICVSUIConstants.PREF_CONSOLE_COMMAND_COLOR);
				commandStream.setColor(commandColor);
				messageColor = createColor(CVSUIPlugin.getStandardDisplay(), ICVSUIConstants.PREF_CONSOLE_MESSAGE_COLOR);
				messageStream.setColor(messageColor);
				errorColor = createColor(CVSUIPlugin.getStandardDisplay(), ICVSUIConstants.PREF_CONSOLE_ERROR_COLOR);
				errorStream.setColor(errorColor);
				// install font
				Font f = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry().get(ICVSUIConstants.PREF_CONSOLE_FONT);
				setFont(f);
				initialized = true;
			}
		}
	}

	private void dump() {
		synchronized(document) {
			visible = true;
			ConsoleDocument.ConsoleLine[] lines = document.getLines();
			for (ConsoleDocument.ConsoleLine line : lines) {
				appendLine(line.type, line.line);
			}
			document.clear();
		}
	}
	
	private void appendLine(int type, String line) {
		showConsole();
		synchronized(document) {
			if(visible) {
				switch(type) {
					case ConsoleDocument.COMMAND:
						commandStream.println(line);
						break;
					case ConsoleDocument.MESSAGE:
						messageStream.println("  " + line); //$NON-NLS-1$
						break;
					case ConsoleDocument.ERROR:
						errorStream.println("  " + line); //$NON-NLS-1$
						break;
				}
			} else {
				document.appendConsoleLine(type, line);
			}
		}
	}

	private void showConsole() {
		show(false);
	}
	
	@Override
	protected void dispose() {
		// Here we can't call super.dispose() because we actually want the partitioner to remain
		// connected, but we won't show lines until the console is added to the console manager
		// again.
		
		// Called when console is removed from the console view
		synchronized (document) {
			visible = false;
			JFaceResources.getFontRegistry().removeListener(this);
		}
	}
	
	/**
	 * Clean-up created fonts.
	 */
	public void shutdown() {
		// Call super dispose because we want the partitioner to be
		// disconnected.
		super.dispose();
		if (commandColor != null)
			commandColor.dispose();
		if (messageColor != null)
			messageColor.dispose();
		if (errorColor != null)
			errorColor.dispose();
		CVSUIPlugin.getPlugin().getPreferenceStore().removePropertyChangeListener(this);
	}

	@Override
	public void commandInvoked(Session session, String line) {
		if (!session.isOutputToConsole()) return;
		commandStarted = System.currentTimeMillis();
		appendLine(ConsoleDocument.COMMAND, CVSUIMessages.Console_preExecutionDelimiter); 
		appendLine(ConsoleDocument.COMMAND, line);
	}

	@Override
	public void messageLineReceived(Session session, String line, IStatus status) {
		if (session.isOutputToConsole()) {
			appendLine(ConsoleDocument.MESSAGE, "  " + line); //$NON-NLS-1$
		}
	}

	@Override
	public void errorLineReceived(Session session, String line, IStatus status) {
		if (session.isOutputToConsole()) {
			appendLine(ConsoleDocument.ERROR, "  " + line); //$NON-NLS-1$
		}
	}
	
	@Override
	public void commandCompleted(Session session, IStatus status, Exception exception) {
		if (!session.isOutputToConsole()) return;
		long commandRuntime = System.currentTimeMillis() - commandStarted;
		String time;
		try {
			time = TIME_FORMAT.format(new Date(commandRuntime));
		} catch (RuntimeException e) {
			CVSUIPlugin.log(IStatus.ERROR, CVSUIMessages.Console_couldNotFormatTime, e); 
			time = ""; //$NON-NLS-1$
		}
		String statusText;
		if (status != null) {
			boolean includeRoot = true;
			if (status.getCode() == CVSStatus.SERVER_ERROR) {
				statusText = NLS.bind(CVSUIMessages.Console_resultServerError, new String[] { status.getMessage(), time }); 
				includeRoot = false;
			} else {
				statusText = NLS.bind(CVSUIMessages.Console_resultOk, new String[] { time }); 
			}
			appendLine(ConsoleDocument.COMMAND, statusText);
			outputStatus(status, includeRoot, includeRoot ? 0 : 1);
		} else if (exception != null) {
			if (exception instanceof OperationCanceledException) {
				statusText = NLS.bind(CVSUIMessages.Console_resultAborted, new String[] { time }); 
			} else {
				statusText = NLS.bind(CVSUIMessages.Console_resultException, new String[] { time }); 
			}
			appendLine(ConsoleDocument.COMMAND, statusText);
			if (exception instanceof CoreException) {
				outputStatus(((CoreException)exception).getStatus(), true, 1);
			}
		} else {
			statusText = NLS.bind(CVSUIMessages.Console_resultOk, new String[] { time }); 
		}
		appendLine(ConsoleDocument.COMMAND, CVSUIMessages.Console_postExecutionDelimiter); 
		appendLine(ConsoleDocument.COMMAND, ""); //$NON-NLS-1$
	}
	
	private void outputStatus(IStatus status, boolean includeParent, int nestingLevel) {
		if (includeParent && !status.isOK()) {
			outputStatusMessage(status, nestingLevel);
			nestingLevel++;
		}
		
		// Include a CoreException in the status
		Throwable t = status.getException();
		if (t instanceof CoreException) {
			outputStatus(((CoreException)t).getStatus(), true, nestingLevel);
		}
		
		// Include child status
		IStatus[] children = status.getChildren();
		for (IStatus c : children) {
			outputStatus(c, true, nestingLevel);
		}
	}
	
	private void outputStatusMessage(IStatus status, int nesting) {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < nesting; i++) {
			buffer.append(NESTING);
		}
		buffer.append(messageLineForStatus(status));
		appendLine(ConsoleDocument.COMMAND, buffer.toString());
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		// colors
		if (visible) {
			switch (property) {
			case ICVSUIConstants.PREF_CONSOLE_COMMAND_COLOR: {
				Color newColor = createColor(CVSUIPlugin.getStandardDisplay(),
						ICVSUIConstants.PREF_CONSOLE_COMMAND_COLOR);
				commandStream.setColor(newColor);
				commandColor.dispose();
				commandColor = newColor;
				return;
			}
			case ICVSUIConstants.PREF_CONSOLE_MESSAGE_COLOR: {
				Color newColor = createColor(CVSUIPlugin.getStandardDisplay(),
						ICVSUIConstants.PREF_CONSOLE_MESSAGE_COLOR);
				messageStream.setColor(newColor);
				messageColor.dispose();
				messageColor = newColor;
				return;
			}
			case ICVSUIConstants.PREF_CONSOLE_ERROR_COLOR: {
				Color newColor = createColor(CVSUIPlugin.getStandardDisplay(),
						ICVSUIConstants.PREF_CONSOLE_ERROR_COLOR);
				errorStream.setColor(newColor);
				errorColor.dispose();
				errorColor = newColor;
				// font
				return;
			}
			case ICVSUIConstants.PREF_CONSOLE_FONT:
				setFont(((FontRegistry) event.getSource()).get(ICVSUIConstants.PREF_CONSOLE_FONT));
				return;
			default:
				break;
			}
		}
		switch (property) {
		case ICVSUIConstants.PREF_CONSOLE_SHOW_ON_MESSAGE:
			Object value = event.getNewValue();
			if (value instanceof String) {
				showOnMessage = Boolean.valueOf((String) value).booleanValue();
			} else {
				showOnMessage = ((Boolean) value).booleanValue();
			}
			break;
		case ICVSUIConstants.PREF_CONSOLE_LIMIT_OUTPUT:
			initLimitOutput();
			break;
		case ICVSUIConstants.PREF_CONSOLE_WRAP:
			initWrapSetting();
			break;
		default:
			break;
		}
	}
	
	/**
	 * Returns the NLSd message based on the status returned from the CVS
	 * command.
	 * 
	 * @param status an NLSd message based on the status returned from the CVS
	 * command.
	 */
	private String messageLineForStatus(IStatus status) {
		if (status.getSeverity() == IStatus.ERROR) {
			return NLS.bind(CVSUIMessages.Console_error, new String[] { status.getMessage() }); 
		} else if (status.getSeverity() == IStatus.WARNING) {
			return NLS.bind(CVSUIMessages.Console_warning, new String[] { status.getMessage() }); 
		} else if (status.getSeverity() == IStatus.INFO) {
			return NLS.bind(CVSUIMessages.Console_info, new String[] { status.getMessage() }); 
		}
		return status.getMessage();
	}
	
	/**
	 * Returns a color instance based on data from a preference field.
	 */
	private Color createColor(Display display, String preference) {
		RGB rgb = PreferenceConverter.getColor(CVSUIPlugin.getPlugin().getPreferenceStore(), preference);
		return new Color(display, rgb);
	}

	/**
	 * Show the console.
	 * @param showNoMatterWhat ignore preferences if <code>true</code>
	 */
	public void show(boolean showNoMatterWhat) {
		if(showNoMatterWhat || showOnMessage) {
			if(!visible)
				CVSConsoleFactory.showConsole();
			else
				consoleManager.showConsoleView(this);
		}
	}
	
	@Override
	public String getHelpContextId() {
		return IHelpContextIds.CONSOLE_VIEW;
	}
}
