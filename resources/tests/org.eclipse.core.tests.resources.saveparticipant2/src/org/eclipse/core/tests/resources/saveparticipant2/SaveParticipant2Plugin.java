/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.saveparticipant2;

import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.resources.ResourceDeltaVerifier;

/**
 * This plugin was designed to test the save facility provided by the ResourcesPlugin.
 * So, it does not act by itself. Another plugin (like a test) needs to call its methods in order
 * to something happen (like add it as a save participant, provide the expected values and so on...).
 */
public class SaveParticipant2Plugin extends Plugin implements ISaveParticipant {
	/** expected values */
	private ResourceDeltaVerifier deltaVerifier;
	private int expectedPreviousSaveNumber;
	private int saveKind;

	/** lifecycle log */
	protected MultiStatus saveLifecycleLog;

	/** constants */
	private static final String SAVE_NUMBER_LOCATION = "saveNumber";

	private static SaveParticipant2Plugin instance;

	public SaveParticipant2Plugin() {
		super();
		instance = this;
		deltaVerifier = new ResourceDeltaVerifier();
	}

	public void addExpectedChange(IResource[] resources, int status, int changeFlags) {
		deltaVerifier.addExpectedChange(resources, status, changeFlags);
	}

	public void addExpectedChange(IResource resource, int status, int changeFlags) {
		deltaVerifier.addExpectedChange(resource, status, changeFlags);
	}

	public void addExpectedChange(IResource resource, IResource topLevelParent, int status, int changeFlags) {
		deltaVerifier.addExpectedChange(resource, topLevelParent, status, changeFlags);
	}

	public void deregisterAsSaveParticipant() {
		getWorkspace().removeSaveParticipant(getPluginId());
	}

	@Override
	public void doneSaving(ISaveContext context) {
		try {
			writeExpectedSaveNumber(context.getSaveNumber());
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}

	/**
	 * Used to set the expected delta from the saved id.
	 */
	public ResourceDeltaVerifier getDeltaVerifier() {
		return deltaVerifier;
	}

	private String getMessage(int expectedPreviousSaveNumber, int previousSaveNumber) {
		StringBuilder message = new StringBuilder();
		message.append("Expected previous saved id was: ");
		message.append(expectedPreviousSaveNumber);
		message.append("\nPrevious saved id is: ");
		message.append(previousSaveNumber);
		message.append("\n");
		return message.toString();
	}

	public String getPluginId() {
		return getBundle().getSymbolicName();
	}

	/**
	 * @return a status indicating any problem during the save lifecycle of this plugin.
	 */
	public IStatus getSaveLifecycleLog() {
		return saveLifecycleLog;
	}

	private IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	@Override
	public void prepareToSave(ISaveContext context) throws CoreException {
		resetSaveLifecycleLog();
		IStatus status = validate(context);
		if (!status.isOK())
			saveLifecycleLog.addAll(status);
		context.needDelta();
		context.needSaveNumber();
	}

	/**
	 * We do not care about deltas, only saved id.
	 */
	private void readExpectedSavedNumber() {
		IPath location = getStateLocation().append(SAVE_NUMBER_LOCATION);
		try (DataInputStream input = new DataInputStream(new FileInputStream(location.toOSString()))) {
			expectedPreviousSaveNumber = input.readInt();
		} catch (IOException e) {
			expectedPreviousSaveNumber = 0;
		}
	}

	/**
	 * @return a status indicating if the ISavedState is the expected one or not.
	 */
	public IStatus registerAsSaveParticipant() throws CoreException {
		ISavedState state = getWorkspace().addSaveParticipant(getPluginId(), this);
		readExpectedSavedNumber();
		return validate(state);
	}

	public void resetDeltaVerifier() {
		deltaVerifier.reset();
	}

	protected void resetSaveLifecycleLog() {
		String message = "save lifecycle log for SaveParticipantPlugin";
		saveLifecycleLog = new MultiStatus(getPluginId(), IStatus.OK, message, null);
	}

	@Override
	public void rollback(ISaveContext context) {
	}

	@Override
	public void saving(ISaveContext context) throws CoreException {
	}

	public void setExpectedSaveKind(int saveKind) {
		this.saveKind = saveKind;
	}

	private IStatus validate(ISaveContext context) {
		// previous saved id
		if (context.getPreviousSaveNumber() != 0 || expectedPreviousSaveNumber != 0) {
			try {
				// Test if id is different than expected. Also, catch NullPointerException
				// in cases we do not expect any of the ids to be null.
				if (context.getPreviousSaveNumber() != expectedPreviousSaveNumber) {
					String message = getMessage(expectedPreviousSaveNumber, context.getPreviousSaveNumber());
					return new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, null);
				}
			} catch (NullPointerException e) {
				String message = getMessage(expectedPreviousSaveNumber, context.getPreviousSaveNumber());
				return new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, e);
			}
		}
		// save kind
		if (context.getKind() != saveKind) {
			String message = "Save kind was different than expected.";
			return new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, null);
		}
		return new Status(IStatus.OK, getPluginId(), IStatus.OK, "OK", null);
	}

	private IStatus validate(ISavedState state) {
		if (state == null && expectedPreviousSaveNumber == 0)
			return new Status(IStatus.OK, getPluginId(), IStatus.OK, "OK", null);
		try {
			// Test if id or delta are different than expected. Also, catch NullPointerException
			// in cases we do not expect any of the states to be null.
			state.processResourceChangeEvents(deltaVerifier);
			if (state.getSaveNumber() != expectedPreviousSaveNumber) {
				String message = "saved id is different than expected";
				return new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, null);
			}
			if (!deltaVerifier.isDeltaValid()) {
				String message = "delta is different than expected\n";
				message = message + deltaVerifier.getMessage();
				return new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, null);
			}
		} catch (NullPointerException e) {
			String message = "ISavedState is different than expected";
			return new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, e);
		}
		return new Status(IStatus.OK, getPluginId(), IStatus.OK, "OK", null);
	}

	/**
	 * We do not care about deltas, only save number.
	 */
	private void writeExpectedSaveNumber(int saveNumber) throws IOException {
		IPath location = getStateLocation().append(SAVE_NUMBER_LOCATION);
		try (DataOutputStream output = new DataOutputStream(new FileOutputStream(location.toOSString()))) {
			output.writeInt(saveNumber);
		}
	}

	public static SaveParticipant2Plugin getInstance() {
		return instance;
	}
}
