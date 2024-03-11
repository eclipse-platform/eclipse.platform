/*******************************************************************************
 *  Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Paul Pazderski  - Bug 545769: fixed rare UTF-8 character corruption bug
 *     Paul Pazderski  - Bug 552015: console finished signaled to late if input is connected to file
 *     Paul Pazderski  - Bug 251642: add termination time in console label
 *     Paul Pazderski  - Bug 558463: add handling of raw stream content instead of strings
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.console;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBinaryStreamListener;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IBinaryStreamMonitor;
import org.eclipse.debug.core.model.IBinaryStreamsProxy;
import org.eclipse.debug.core.model.IFlushableStreamMonitor;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleColorProvider;
import org.eclipse.debug.ui.console.IConsoleHyperlink;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.UIJob;

/**
 * A console for a system process with standard I/O streams.
 *
 * @since 3.0
 */
@SuppressWarnings("deprecation")
public class ProcessConsole extends IOConsole implements IConsole, IDebugEventSetListener, IPropertyChangeListener {
	private IProcess fProcess = null;

	private final List<StreamListener> fStreamListeners = new ArrayList<>();

	private final IConsoleColorProvider fColorProvider;

	/**
	 * The input stream which can supply user input in console to the system process
	 * stdin.
	 */
	private IOConsoleInputStream fUserInput;
	/**
	 * The stream connected to the system processe's stdin. May be the
	 * <i>fUserInput</i> stream to supply user input or a FileInputStream to supply
	 * input from a file.
	 */
	private volatile InputStream fInput;

	private FileOutputStream fFileOutputStream;

	private boolean fAllocateConsole = true;
	private String fStdInFile = null;

	private volatile boolean fStreamsClosed = false;

	/**
	 * Create process console with default encoding.
	 *
	 * @param process	   the process to associate with this console
	 * @param colorProvider the colour provider for this console
	 */
	public ProcessConsole(IProcess process, IConsoleColorProvider colorProvider) {
		this(process, colorProvider, null);
	}

	/**
	 * Create process console.
	 *
	 * @param process the process to associate with this console
	 * @param colorProvider the colour provider for this console
	 * @param encoding the desired encoding for this console
	 */
	public ProcessConsole(IProcess process, IConsoleColorProvider colorProvider, String encoding) {
		super(IInternalDebugCoreConstants.EMPTY_STRING, IDebugUIConstants.ID_PROCESS_CONSOLE_TYPE, null, encoding, true);
		fProcess = process;
		fUserInput = getInputStream();

		ILaunchConfiguration configuration = process.getLaunch().getLaunchConfiguration();
		String file = null;
		boolean append = false;
		if (configuration != null) {
			try {
				file = configuration.getAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, (String) null);
				fStdInFile = configuration.getAttribute(IDebugUIConstants.ATTR_CAPTURE_STDIN_FILE, (String) null);
				if (file != null || fStdInFile != null) {
					IStringVariableManager stringVariableManager = VariablesPlugin.getDefault().getStringVariableManager();
					if (file != null) {
						file = stringVariableManager.performStringSubstitution(file);
						append = configuration.getAttribute(IDebugUIConstants.ATTR_APPEND_TO_FILE, false);
					}

					if (fStdInFile != null) {
						fStdInFile = stringVariableManager.performStringSubstitution(fStdInFile);
					}
				}
			} catch (CoreException e) {
			}
		}

		if (file != null && configuration != null) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceRoot root = workspace.getRoot();
			IPath path = IPath.fromOSString(file);
			IFile ifile = root.getFileForLocation(path);
			String message = null;

			try {
				String fileLoc = null;
				if (ifile != null) {
					if (append && ifile.exists()) {
						ifile.appendContents(new ByteArrayInputStream(new byte[0]), true, true, new NullProgressMonitor());
					} else {
						if (ifile.exists()) {
							ifile.delete(true, new NullProgressMonitor());
						}
						ifile.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
					}
				}

				File outputFile = new File(file);
				fFileOutputStream = new FileOutputStream(outputFile, append);
				fileLoc = outputFile.getAbsolutePath();

				message = MessageFormat.format(ConsoleMessages.ProcessConsole_1, new Object[] { fileLoc });
				addPatternMatchListener(new ConsoleLogFilePatternMatcher(fileLoc));
			} catch (FileNotFoundException e) {
				message = MessageFormat.format(ConsoleMessages.ProcessConsole_2, new Object[] { file });
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
			}
			if (message != null) {
				try (IOConsoleOutputStream stream = newOutputStream()) {
					stream.write(message);
				} catch (IOException e) {
					DebugUIPlugin.log(e);
				}
			}
			try {
				fAllocateConsole = configuration.getAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, true);
			} catch (CoreException e) {
			}
		}
		if (fStdInFile != null && configuration != null) {
			String message = null;
			try {
				fInput = new FileInputStream(new File(fStdInFile));
				if (fInput != null) {
					setInputStream(fInput);
				}

			} catch (FileNotFoundException e) {
				message = MessageFormat.format(ConsoleMessages.ProcessConsole_3, new Object[] { fStdInFile });
			}
			if (message != null) {
				try (IOConsoleOutputStream stream = newOutputStream()) {
					stream.write(message);
				} catch (IOException e) {
					DebugUIPlugin.log(e);
				}
			}
		}
		fColorProvider = colorProvider;
		if (fInput == null) {
			fInput = getInputStream();
		}


		colorProvider.connect(fProcess, this);

		Color color = fColorProvider.getColor(IDebugUIConstants.ID_STANDARD_INPUT_STREAM);
		if (fInput instanceof IOConsoleInputStream) {
			((IOConsoleInputStream)fInput).setColor(color);
		}

		IConsoleLineTracker[] lineTrackers = DebugUIPlugin.getDefault().getProcessConsoleManager().getLineTrackers(process);
		if (lineTrackers.length > 0) {
			addPatternMatchListener(new ConsoleLineNotifier());
		}
	}

	/**
	 * Computes and returns the image descriptor for this console.
	 *
	 * @return an image descriptor for this console or <code>null</code>
	 */
	protected ImageDescriptor computeImageDescriptor() {
		ILaunchConfiguration configuration = getProcess().getLaunch().getLaunchConfiguration();
		if (configuration != null) {
			ILaunchConfigurationType type;
			try {
				type = configuration.getType();
				return DebugPluginImages.getImageDescriptor(type.getIdentifier());
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
			}
		}
		return null;
	}

	/**
	 * Computes and returns the current name of this console.
	 *
	 * @return a name for this console
	 */
	protected String computeName() {
		String label = null;
		IProcess process = getProcess();
		ILaunchConfiguration config = process.getLaunch().getLaunchConfiguration();

		label = process.getAttribute(IProcess.ATTR_PROCESS_LABEL);
		if (label == null) {
			if (config == null) {
				label = process.getLabel();
			} else {
				// check if PRIVATE config
				if (DebugUITools.isPrivate(config)) {
					label = process.getLabel();
				} else {
					String type = null;
					try {
						type = config.getType().getName();
					} catch (CoreException e) {
					}
					StringBuilder buffer = new StringBuilder();
					buffer.append(config.getName());
					if (type != null) {
						buffer.append(" ["); //$NON-NLS-1$
						buffer.append(type);
						buffer.append("] "); //$NON-NLS-1$
					}

					Date launchTime = parseTimestamp(process.getAttribute(DebugPlugin.ATTR_LAUNCH_TIMESTAMP));
					Date terminateTime = parseTimestamp(process.getAttribute(DebugPlugin.ATTR_TERMINATE_TIMESTAMP));

					String procLabel = process.getLabel();
					if (launchTime != null) {
						// FIXME workaround to remove start time from process label added from jdt for
						// java launches
						int idx = procLabel.lastIndexOf('(');
						if (idx >= 0) {
							int end = procLabel.lastIndexOf(')');
							if (end > idx) {
								String jdtTime = procLabel.substring(idx + 1, end);
								try {
									DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).parse(jdtTime);
									procLabel = procLabel.substring(0, idx);
								} catch (ParseException pe) {
									// not a date. Label just contains parentheses
								}
							}
						}
					}

					DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
					if (launchTime != null && terminateTime != null) {
						String launchTimeStr = dateTimeFormat.format(launchTime);
						// Check if process started and terminated at same day. If so only print the
						// time part of termination time and omit the date part.
						LocalDateTime launchDate = LocalDateTime.ofInstant(launchTime.toInstant(),
								ZoneId.systemDefault());
						LocalDateTime terminateDate = LocalDateTime.ofInstant(terminateTime.toInstant(),
								ZoneId.systemDefault());
						LocalDateTime launchDay = launchDate.truncatedTo(ChronoUnit.DAYS);
						LocalDateTime terminateDay = terminateDate.truncatedTo(ChronoUnit.DAYS);
						String terminateTimeStr;
						if (launchDay.equals(terminateDay)) {
							terminateTimeStr = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(terminateTime);
						} else {
							terminateTimeStr = dateTimeFormat.format(terminateTime);
						}

						buffer.append(MessageFormat.format(ConsoleMessages.ProcessConsole_commandLabel_withStartEnd,
								procLabel, launchTimeStr, terminateTimeStr));
					} else if (launchTime != null) {
						buffer.append(MessageFormat.format(ConsoleMessages.ProcessConsole_commandLabel_withStart,
								procLabel, dateTimeFormat.format(launchTime)));
					} else if (terminateTime != null) {
						buffer.append(MessageFormat.format(ConsoleMessages.ProcessConsole_commandLabel_withEnd,
								procLabel, dateTimeFormat.format(terminateTime)));
					}

					String pid = process.getAttribute(IProcess.ATTR_PROCESS_ID);
					if (pid != null && !pid.isBlank()) {
						buffer.append(" [pid: "); //$NON-NLS-1$
						buffer.append(pid);
						buffer.append("]"); //$NON-NLS-1$
					}
					label = buffer.toString();
				}
			}
		}

		if (process.isTerminated()) {
			return MessageFormat.format(ConsoleMessages.ProcessConsole_0, new Object[] { label });
		}
		return label;
	}

	/**
	 * Get Date from (possibly invalid) timestamp.
	 *
	 * @param timestamp a timestamp as returned from
	 *                  {@link System#currentTimeMillis()} or <code>null</code>
	 * @return Date object for this timestamp or <code>null</code> if timestamp is
	 *         invalid
	 */
	private static Date parseTimestamp(String timestamp) {
		if (timestamp == null) {
			return null;
		}
		try {
			long lTimestamp = Long.parseLong(timestamp);
			return new Date(lTimestamp);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	/**
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String property = evt.getProperty();
		IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
		if (property.equals(IDebugPreferenceConstants.CONSOLE_WRAP) || property.equals(IDebugPreferenceConstants.CONSOLE_WIDTH)) {
			boolean fixedWidth = store.getBoolean(IDebugPreferenceConstants.CONSOLE_WRAP);
			if (fixedWidth) {
				int width = store.getInt(IDebugPreferenceConstants.CONSOLE_WIDTH);
				setConsoleWidth(width);
			} else {
				setConsoleWidth(-1);
			}
		} else if (property.equals(IDebugPreferenceConstants.CONSOLE_LIMIT_CONSOLE_OUTPUT) || property.equals(IDebugPreferenceConstants.CONSOLE_HIGH_WATER_MARK) || property.equals(IDebugPreferenceConstants.CONSOLE_LOW_WATER_MARK)) {
			boolean limitBufferSize = store.getBoolean(IDebugPreferenceConstants.CONSOLE_LIMIT_CONSOLE_OUTPUT);
			if (limitBufferSize) {
				int highWater = store.getInt(IDebugPreferenceConstants.CONSOLE_HIGH_WATER_MARK);
				int lowWater = store.getInt(IDebugPreferenceConstants.CONSOLE_LOW_WATER_MARK);
				if (highWater > lowWater) {
					setWaterMarks(lowWater, highWater);
				}
			} else {
				setWaterMarks(-1, -1);
			}
		} else if (property.equals(IDebugPreferenceConstants.CONSOLE_TAB_WIDTH)) {
			int tabWidth = store.getInt(IDebugPreferenceConstants.CONSOLE_TAB_WIDTH);
			setTabWidth(tabWidth);
		} else if (property.equals(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT)) {
			boolean activateOnOut = store.getBoolean(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT);
			@SuppressWarnings("resource")
			IOConsoleOutputStream stream = getStream(IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM);
			if (stream != null) {
				stream.setActivateOnWrite(activateOnOut);
			}
		} else if (property.equals(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR)) {
			boolean activateOnErr = store.getBoolean(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR);
			@SuppressWarnings("resource")
			IOConsoleOutputStream stream = getStream(IDebugUIConstants.ID_STANDARD_ERROR_STREAM);
			if (stream != null) {
				stream.setActivateOnWrite(activateOnErr);
			}
		} else if (property.equals(IDebugPreferenceConstants.CONSOLE_SYS_OUT_COLOR)) {
			@SuppressWarnings("resource")
			IOConsoleOutputStream stream = getStream(IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM);
			if (stream != null) {
				stream.setColor(fColorProvider.getColor(IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM));
			}
		} else if (property.equals(IDebugPreferenceConstants.CONSOLE_SYS_ERR_COLOR)) {
			@SuppressWarnings("resource")
			IOConsoleOutputStream stream = getStream(IDebugUIConstants.ID_STANDARD_ERROR_STREAM);
			if (stream != null) {
				stream.setColor(fColorProvider.getColor(IDebugUIConstants.ID_STANDARD_ERROR_STREAM));
			}
		} else if (property.equals(IDebugPreferenceConstants.CONSOLE_SYS_IN_COLOR)) {
			if (fInput != null && fInput instanceof IOConsoleInputStream) {
				((IOConsoleInputStream) fInput).setColor(fColorProvider.getColor(IDebugUIConstants.ID_STANDARD_INPUT_STREAM));
			}
		} else if (property.equals(IDebugUIConstants.PREF_CONSOLE_FONT)) {
			setFont(JFaceResources.getFont(IDebugUIConstants.PREF_CONSOLE_FONT));
		} else if (property.equals(IDebugPreferenceConstants.CONSOLE_BAKGROUND_COLOR)) {
			setBackground(DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_BAKGROUND_COLOR));
		} else if (property.equals(IDebugPreferenceConstants.CONSOLE_INTERPRET_CONTROL_CHARACTERS)) {
			setHandleControlCharacters(store.getBoolean(IDebugPreferenceConstants.CONSOLE_INTERPRET_CONTROL_CHARACTERS));
		} else if (property.equals(IDebugPreferenceConstants.CONSOLE_INTERPRET_CR_AS_CONTROL_CHARACTER)) {
			setCarriageReturnAsControlCharacter(store.getBoolean(IDebugPreferenceConstants.CONSOLE_INTERPRET_CR_AS_CONTROL_CHARACTER));
		}
	}

	@Override
	public IOConsoleOutputStream getStream(String streamIdentifier) {
		if (streamIdentifier == null) {
			return null;
		}
		for (StreamListener listener : fStreamListeners) {
			if (streamIdentifier.equals(listener.fStreamId)) {
				return listener.fStream;
			}
		}
		return null;
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsole#getProcess()
	 */
	@Override
	public IProcess getProcess() {
		return fProcess;
	}

	/**
	 * @see org.eclipse.ui.console.IOConsole#dispose()
	 */
	@Override
	protected void dispose() {
		super.dispose();
		fColorProvider.disconnect();
		DebugPlugin.getDefault().removeDebugEventListener(this);
		DebugUIPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		JFaceResources.getFontRegistry().removeListener(this);
		closeStreams();
		disposeStreams();
	}

	/**
	 * cleanup method to close all of the open stream to this console
	 */
	private synchronized void closeStreams() {
		if (fStreamsClosed) {
			return;
		}
		for (StreamListener listener : fStreamListeners) {
			listener.closeStream();
		}
		if (fFileOutputStream != null) {
			synchronized (fFileOutputStream) {
				try {
					fFileOutputStream.flush();
					fFileOutputStream.close();
				} catch (IOException e) {
				}
			}
		}
		try {
			fInput.close();
		} catch (IOException e) {
		}
		if (fInput != fUserInput) {
			try {
				fUserInput.close();
			} catch (IOException e) {
			}
		}
		fStreamsClosed = true;
	}

	/**
	 * disposes the listeners for each of the stream associated with this
	 * console
	 */
	private synchronized void disposeStreams() {
		for (StreamListener listener : fStreamListeners) {
			listener.dispose();
		}
		fFileOutputStream = null;
		fInput = null;
		fUserInput = null;
	}

	/**
	 * @see org.eclipse.ui.console.AbstractConsole#init()
	 */
	@Override
	protected void init() {
		super.init();
		DebugPlugin.getDefault().addDebugEventListener(this);
		// computeName() after addDebugEventListener()
		// see https://github.com/eclipse-jdt/eclipse.jdt.debug/issues/390
		setName(computeName());
		if (fProcess.isTerminated()) {
			closeStreams();
			resetName();
			DebugPlugin.getDefault().removeDebugEventListener(this);
		}
		IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
		store.addPropertyChangeListener(this);
		JFaceResources.getFontRegistry().addListener(this);
		if (store.getBoolean(IDebugPreferenceConstants.CONSOLE_WRAP)) {
			setConsoleWidth(store.getInt(IDebugPreferenceConstants.CONSOLE_WIDTH));
		}
		setTabWidth(store.getInt(IDebugPreferenceConstants.CONSOLE_TAB_WIDTH));

		if (store.getBoolean(IDebugPreferenceConstants.CONSOLE_LIMIT_CONSOLE_OUTPUT)) {
			int highWater = store.getInt(IDebugPreferenceConstants.CONSOLE_HIGH_WATER_MARK);
			int lowWater = store.getInt(IDebugPreferenceConstants.CONSOLE_LOW_WATER_MARK);
			setWaterMarks(lowWater, highWater);
		}

		setHandleControlCharacters(store.getBoolean(IDebugPreferenceConstants.CONSOLE_INTERPRET_CONTROL_CHARACTERS));
		setCarriageReturnAsControlCharacter(store.getBoolean(IDebugPreferenceConstants.CONSOLE_INTERPRET_CR_AS_CONTROL_CHARACTER));

		DebugUIPlugin.getStandardDisplay().asyncExec(() -> {
			setFont(JFaceResources.getFont(IDebugUIConstants.PREF_CONSOLE_FONT));
			setBackground(DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_BAKGROUND_COLOR));
		});
	}

	/**
	 * Notify listeners when name changes.
	 *
	 * @see org.eclipse.debug.core.IDebugEventSetListener#handleDebugEvents(org.eclipse.debug.core.DebugEvent[])
	 */
	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		for (DebugEvent event : events) {
			if (event.getSource().equals(getProcess())) {

				if (event.getKind() == DebugEvent.TERMINATE) {
					closeStreams();
					DebugPlugin.getDefault().removeDebugEventListener(this);
				}

				resetName();
			}
		}
	}

	/**
	 * resets the name of this console to the original computed name
	 */
	private synchronized void resetName() {
		final String newName = computeName();
		String name = getName();
		if (!name.equals(newName)) {
			UIJob job = new UIJob("Update console title") { //$NON-NLS-1$
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					 ProcessConsole.this.setName(newName);
					 warnOfContentChange();
					 return Status.OK_STATUS;
				}
			};
			job.setSystem(true);
			job.schedule();
		}
	}

	/**
	 * send notification of a change of content in this console
	 */
	private void warnOfContentChange() {
		ConsolePlugin.getDefault().getConsoleManager().warnOfContentChange(DebugUITools.getConsole(fProcess));
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsole#connect(org.eclipse.debug.core.model.IStreamsProxy)
	 */
	@Override
	public void connect(IStreamsProxy streamsProxy) {
		IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
		IStreamMonitor streamMonitor = streamsProxy.getErrorStreamMonitor();
		if (streamMonitor != null) {
			connect(streamMonitor, IDebugUIConstants.ID_STANDARD_ERROR_STREAM,
					store.getBoolean(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR));
		}
		streamMonitor = streamsProxy.getOutputStreamMonitor();
		if (streamMonitor != null) {
			connect(streamMonitor, IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM,
					store.getBoolean(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT));
		}
		InputReadJob readJob = new InputReadJob(streamsProxy);
		readJob.setSystem(true);
		readJob.schedule();
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsole#connect(org.eclipse.debug.core.model.IStreamMonitor, java.lang.String)
	 */
	@Override
	public void connect(IStreamMonitor streamMonitor, String streamIdentifier) {
		connect(streamMonitor, streamIdentifier, false);
	}

	/**
	 * Connects the given stream monitor to a new output stream with the given identifier.
	 *
	 * @param streamMonitor stream monitor
	 * @param streamIdentifier stream identifier
	 * @param activateOnWrite whether the stream should displayed when written to
	 */
	@SuppressWarnings("resource")
	private void connect(IStreamMonitor streamMonitor, String streamIdentifier, boolean activateOnWrite) {
		IOConsoleOutputStream stream = null;
		if (fAllocateConsole) {

			stream = newOutputStream();
			Color color = fColorProvider.getColor(streamIdentifier);
			stream.setColor(color);
			stream.setActivateOnWrite(activateOnWrite);
		}
		synchronized (streamMonitor) {
			StreamListener listener = new StreamListener(streamIdentifier, streamMonitor, stream);
			fStreamListeners.add(listener);
		}
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsole#addLink(org.eclipse.debug.ui.console.IConsoleHyperlink, int, int)
	 */
	@Override
	public void addLink(IConsoleHyperlink link, int offset, int length) {
		try {
			addHyperlink(link, offset, length);
		} catch (BadLocationException e) {
			DebugUIPlugin.log(e);
		}
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsole#addLink(org.eclipse.ui.console.IHyperlink, int, int)
	 */
	@Override
	public void addLink(IHyperlink link, int offset, int length) {
		try {
			addHyperlink(link, offset, length);
		} catch (BadLocationException e) {
			DebugUIPlugin.log(e);
		}
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsole#getRegion(org.eclipse.debug.ui.console.IConsoleHyperlink)
	 */
	@Override
	public IRegion getRegion(IConsoleHyperlink link) {
		return super.getRegion(link);
	}

	/**
	 * This class listens to a specified stream monitor to get notified on output
	 * from the process connected to console.
	 * <p>
	 * Received output will be redirected to given {@link IOConsoleOutputStream} to
	 * get it shown in console and to {@link #fFileOutputStream} if set.
	 */
	private class StreamListener implements IStreamListener, IBinaryStreamListener {

		private IOConsoleOutputStream fStream;

		/**
		 * The monitors from which this listener class is notified about new content.
		 * Initial and for a long time IO handling in context of Eclipse console was
		 * based on strings and later extended with a variant passing the raw binary
		 * data.
		 * <p>
		 * As a result of this history it is expectable to have a stream monitor passing
		 * the content decoded as string but optional to have access to the
		 * raw/unchanged data.
		 * <p>
		 * Therefore the following two monitor instances either point to the same class
		 * implementing both interfaces or the binary variant is <code>null</code>.
		 */
		private IStreamMonitor fStreamMonitor;
		private IBinaryStreamMonitor fBinaryStreamMonitor;

		private String fStreamId;

		/** Flag to remember if stream was already closed. */
		private boolean fStreamClosed = false;

		public StreamListener(String streamIdentifier, IStreamMonitor monitor, IOConsoleOutputStream stream) {
			fStreamId = streamIdentifier;
			fStreamMonitor = monitor;
			fStream = stream;
			fStreamMonitor.addListener(this);
			if (fStreamMonitor instanceof IBinaryStreamMonitor && fFileOutputStream != null) {
				fBinaryStreamMonitor = (IBinaryStreamMonitor) monitor;
				fBinaryStreamMonitor.addBinaryListener(this);
			}
			// fix to bug 121454. Ensure that output to fast processes is processed.
			flushAndDisableBuffer();
		}

		/**
		 * Process existing content in monitor and flush and disable buffering if it is
		 * a {@link IFlushableStreamMonitor}.
		 *
		 * @param monitor the monitor which might have buffered content
		 */
		private void flushAndDisableBuffer() {
			byte[] data = null;
			String contents;
			synchronized (fStreamMonitor) {
				if (fBinaryStreamMonitor != null) {
					data = fBinaryStreamMonitor.getData();
				}
				contents = fStreamMonitor.getContents();
				if (fStreamMonitor instanceof IFlushableStreamMonitor) {
					IFlushableStreamMonitor m = (IFlushableStreamMonitor) fStreamMonitor;
					m.flushContents();
					m.setBuffered(false);
				}
			}
			if (data != null) {
				streamAppended(data, fBinaryStreamMonitor);
			}
			streamAppended(contents, fStreamMonitor);
		}

		@Override
		public void streamAppended(byte[] data, IBinaryStreamMonitor monitor) {
			if (fFileOutputStream != null) {
				synchronized (fFileOutputStream) {
					try {
						fFileOutputStream.write(data);
					} catch (IOException e) {
						DebugUIPlugin.log(e);
					}
				}
			}
		}

		@Override
		public void streamAppended(String text, IStreamMonitor monitor) {
			if (text == null || text.length() == 0) {
				return;
			}
			if (fStream != null) {
				try {
					fStream.write(text);
				} catch (IOException e) {
					DebugUIPlugin.log(e);
				}
			}
			// If the monitor does not provide the raw data API and we need to redirect to
			// a file the second best (and in the past only) option is to write the encoded
			// text to file.
			if (fBinaryStreamMonitor == null && fFileOutputStream != null) {
				Charset charset = getCharset();
				byte[] data = charset == null ? text.getBytes() : text.getBytes(charset);
				streamAppended(data, null);
			}
		}

		public void closeStream() {
			if (fStreamMonitor == null) {
				return;
			}
			synchronized (fStreamMonitor) {
				fStreamMonitor.removeListener(this);
				if (fBinaryStreamMonitor != null) {
					fBinaryStreamMonitor.removeBinaryListener(this);
				}
				fStreamClosed = true;

				try {
					if (fStream != null) {
						fStream.close();
					}
				} catch (IOException e) {
					DebugUIPlugin.log(e);
				}
			}
		}

		public void dispose() {
			if (!fStreamClosed) {
				closeStream();
			}
			fStream = null;
			fStreamMonitor = null;
			fStreamId = null;
		}
	}

	private class InputReadJob extends Job {

		private final IStreamsProxy streamsProxy;

		/**
		 * The {@link InputStream} this job is currently reading from or maybe blocking
		 * on. May be <code>null</code>.
		 */
		private InputStream readingStream;

		InputReadJob(IStreamsProxy streamsProxy) {
			super("Process Console Input Job"); //$NON-NLS-1$
			this.streamsProxy = streamsProxy;
		}

		@Override
		protected void canceling() {
			super.canceling();
			if (readingStream != null) {
				// Close stream or job may not be able to cancel.
				// This is primary for IOConsoleInputStream because there is no guarantee an
				// arbitrary InputStream will release a blocked read() on close.
				try {
					readingStream.close();
				} catch (IOException e) {
					DebugUIPlugin.log(e);
				}
			}
		}

		@Override
		public boolean belongsTo(Object family) {
			return ProcessConsole.class == family;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (fInput == null || fStreamsClosed) {
				return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
			}
			if (streamsProxy instanceof IBinaryStreamsProxy) {
				// Pass data without processing. The preferred variant. There is no need for
				// this job to know about encodings.
				try {
					byte[] buffer = new byte[1024];
					int bytesRead = 0;
					while (bytesRead >= 0 && !monitor.isCanceled()) {
						if (fInput == null || fStreamsClosed) {
							break;
						}
						if (fInput != readingStream) {
							readingStream = fInput;
						}
						bytesRead = readingStream.read(buffer);
						if (bytesRead > 0) {
							((IBinaryStreamsProxy) streamsProxy).write(buffer, 0, bytesRead);
						}
					}
				} catch (IOException e) {
					DebugUIPlugin.log(e);
				}
			} else {
				// Decode data to strings. The legacy variant used if the proxy does not
				// implement the binary API.
				Charset encoding = getCharset();
				readingStream = fInput;
				InputStreamReader streamReader = (encoding == null ? new InputStreamReader(readingStream)
						: new InputStreamReader(readingStream, encoding));
				try {
					char[] cbuf = new char[1024];
					int charRead = 0;
					while (charRead >= 0 && !monitor.isCanceled()) {
						if (fInput == null || fStreamsClosed) {
							break;
						}
						if (fInput != readingStream) {
							readingStream = fInput;
							streamReader = (encoding == null ? new InputStreamReader(readingStream)
									: new InputStreamReader(readingStream, encoding));
						}

						charRead = streamReader.read(cbuf);
						if (charRead > 0) {
							String s = new String(cbuf, 0, charRead);
							streamsProxy.write(s);
						}
					}
				} catch (IOException e) {
					DebugUIPlugin.log(e);
				}
			}
			readingStream = null;
			return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
		}
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		if (super.getImageDescriptor() == null) {
			setImageDescriptor(computeImageDescriptor());
		}
		return super.getImageDescriptor();
	}

	private class ConsoleLogFilePatternMatcher implements IPatternMatchListener {
		String fFilePath;

		public ConsoleLogFilePatternMatcher(String filePath) {
			fFilePath = filePath;
		}

		@Override
		public String getPattern() {
			return Pattern.quote(fFilePath);
		}

		@Override
		public void matchFound(PatternMatchEvent event) {
			try {
				addHyperlink(new ConsoleLogFileHyperlink(fFilePath), event.getOffset(), event.getLength());
				removePatternMatchListener(this);
			} catch (BadLocationException e) {
			}
		}

		@Override
		public int getCompilerFlags() {
			return 0;
		}

		@Override
		public String getLineQualifier() {
			return null;
		}

		@Override
		public void connect(TextConsole console) {
		}

		@Override
		public void disconnect() {
		}
	}

	private static class ConsoleLogFileHyperlink implements IHyperlink {
		String fFilePath;
		ConsoleLogFileHyperlink(String filePath) {
			fFilePath = filePath;
		}

		@Override
		public void linkActivated() {
			IEditorInput input;
			IPath path = IPath.fromOSString(fFilePath);
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IFile ifile = root.getFileForLocation(path);
			if (ifile == null) { // The file is not in the workspace
				File file = new File(fFilePath);
				LocalFileStorage lfs = new LocalFileStorage(file);
				input = new StorageEditorInput(lfs, file);

			} else {
				input = new FileEditorInput(ifile);
			}

			IWorkbenchPage activePage = DebugUIPlugin.getActiveWorkbenchWindow().getActivePage();
			try {
				activePage.openEditor(input, EditorsUI.DEFAULT_TEXT_EDITOR_ID, true);
			} catch (PartInitException e) {
			}
		}
		@Override
		public void linkEntered() {
		}
		@Override
		public void linkExited() {
		}
	}

	static class StorageEditorInput extends PlatformObject implements IStorageEditorInput {
		private final File fFile;
		private final IStorage fStorage;

		public StorageEditorInput(IStorage storage, File file) {
			fStorage = storage;
			fFile = file;
		}

		@Override
		public IStorage getStorage() {
			return fStorage;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return null;
		}

		@Override
		public String getName() {
			return getStorage().getName();
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return getStorage().getFullPath().toOSString();
		}

		@Override
		public boolean equals(Object object) {
			return object instanceof StorageEditorInput &&
			 getStorage().equals(((StorageEditorInput)object).getStorage());
		}

		@Override
		public int hashCode() {
			return getStorage().hashCode();
		}

		@Override
		public boolean exists() {
			return fFile.exists();
		}
	}

	@Override
	public String getHelpContextId() {
		return IDebugHelpContextIds.PROCESS_CONSOLE;
	}
}
