/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.client;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;

/**
 * Abstract base class for requests that are to be sent to the server.
 */
public abstract class Request {
	public static final ExpandModules EXPAND_MODULES = new ExpandModules();
	public static final ValidRequests VALID_REQUESTS = new ValidRequests();

	/*** Response handler map ***/
	private static final Map<String,ResponseHandler> responseHandlers = new HashMap<>();
	
	private static void initializeHandlerCache() {
		synchronized(responseHandlers) {
			registerResponseHandler(new CheckedInHandler());
			registerResponseHandler(new CopyHandler());
			registerResponseHandler(new ModTimeHandler());
			registerResponseHandler(new NewEntryHandler());
			registerResponseHandler(new RemovedHandler());
			registerResponseHandler(new RemoveEntryHandler());
			registerResponseHandler(new StaticHandler(true));
			registerResponseHandler(new StaticHandler(false));
			registerResponseHandler(new StickyHandler(true));
			registerResponseHandler(new StickyHandler(false));
			registerResponseHandler(new UpdatedHandler(UpdatedHandler.HANDLE_UPDATED));
			registerResponseHandler(new UpdatedHandler(UpdatedHandler.HANDLE_UPDATE_EXISTING));
			registerResponseHandler(new UpdatedHandler(UpdatedHandler.HANDLE_CREATED));
			registerResponseHandler(new UpdatedHandler(UpdatedHandler.HANDLE_MERGED));
			registerResponseHandler(new ValidRequestsHandler());
			registerResponseHandler(new ModuleExpansionHandler());
			registerResponseHandler(new MTHandler());
			registerResponseHandler(new NotifiedHandler());
			registerResponseHandler(new TemplateHandler());
		}
	}
	private static void registerResponseHandler(ResponseHandler handler) {
		synchronized(responseHandlers) {
			responseHandlers.put(handler.getResponseID(), handler);
		}
	}
	
	/**
	 * This method is invoked by Session to get a mutable copy of the
	 * global list of acceptable response handlers.
	 * 
	 * @return a map of response handlers
	 */
	protected static Map<String,ResponseHandler> getReponseHandlerMap() {
		synchronized(responseHandlers) {
			if (responseHandlers.isEmpty()) {
				initializeHandlerCache();
			}
			Map<String,ResponseHandler> copy = new HashMap<>();
			for (Iterator iter = responseHandlers.values().iterator(); iter.hasNext();) {
				ResponseHandler handler = (ResponseHandler) iter.next();
				copy.put(handler.getResponseID(), handler.getInstance());
			}
			return copy;
		}
	}
	/**
	 * Prevents client code from instantiating us.
	 */
	protected Request() { }

	/**
	 * Returns the string used to invoke this request on the server.
	 * [template method]
	 * 
	 * @return the request identifier string
	 */
	protected abstract String getRequestId();

	/**
	 * Executes a request and processes the responses.
	 * 
	 * @param session the open CVS session
	 * @param listener the command output listener, or null to discard all messages
	 * @param monitor the progress monitor
	 * @return a status code indicating success or failure of the operation
	 */
	protected IStatus executeRequest(Session session, ICommandOutputListener listener,
		IProgressMonitor monitor) throws CVSException {
		// send request
		session.sendRequest(getRequestId());

		// This number can be tweaked if the monitor is judged to move too
		// quickly or too slowly. After some experimentation this is a good
		// number for both large projects (it doesn't move so quickly as to
		// give a false sense of speed) and smaller projects (it actually does
		// move some rather than remaining still and then jumping to 100).
		final int TOTAL_WORK = 300;
		monitor.beginTask(CVSMessages.Command_receivingResponses, TOTAL_WORK); 
		monitor.subTask(CVSMessages.Command_receivingResponses); 
		int halfWay = TOTAL_WORK / 2;
		int currentIncrement = 4;
		int nextProgress = currentIncrement;
		int worked = 0;
		
		// If the session is connected to a CVSNT server (1.11.1.1), we'll need to do some special handling for
		// some errors. Unfortunately, CVSNT 1.11.1.1 will drop the connection after so some functionality is
		// still affected
		boolean isCVSNT = session.isCVSNT();

		session.clearErrors();
		for (;;) {
			// update monitor work amount
			if (--nextProgress <= 0) {
				monitor.worked(1);
				worked++;
				if (worked >= halfWay) {
					// we have passed the current halfway point, so double the
					// increment and reset the halfway point.
					currentIncrement *= 2;
					halfWay += (TOTAL_WORK - halfWay) / 2;				
				}
				// reset the progress counter to another full increment
				nextProgress = currentIncrement;
			}			
			Policy.checkCanceled(monitor);

			// retrieve a response line
			String response = session.readLine();
			int spacePos = response.indexOf(' ');
			String argument;
			if (spacePos != -1) {
				argument = response.substring(spacePos + 1);
				response = response.substring(0, spacePos);
			} else argument = "";  //$NON-NLS-1$

			// handle completion responses
			if (response.equals("ok")) {  //$NON-NLS-1$
				break;
			} else if (response.equals("error") || (isCVSNT && response.isEmpty())) {  //$NON-NLS-1$
				argument = argument.trim();
				boolean serious = false;
				if (argument.length() == 0) {
					argument = getServerErrorMessage();
				} else {
					argument = NLS.bind(CVSMessages.Command_seriousServerError, new String[] { argument }); 
					if (!session.hasErrors()) {
						session.addError(new CVSStatus(IStatus.ERROR, CVSStatus.SERVER_ERROR, argument,session.getLocalRoot()));
					}
					serious = true;
				}
					
				if (!session.hasErrors()) {
					session.addError(new CVSStatus(IStatus.ERROR, CVSStatus.SERVER_ERROR, CVSMessages.Command_noMoreInfoAvailable,session.getLocalRoot()));
				}
				IStatus status = new MultiStatus(CVSProviderPlugin.ID, CVSStatus.SERVER_ERROR, 
						session.getErrors(),
					argument, null);
				if (serious) {
					throw new CVSServerException(status);
				} else {
					// look for particularly bad errors in the accumulated statuses
					IStatus[] errors = session.getErrors();
					for (IStatus s : errors) {
						if (s.getCode() == CVSStatus.PROTOCOL_ERROR) {
							throw new CVSServerException(status);
						}
					}
				}
				return status;
			// handle message responses
			} else if (response.equals("MT")) {  //$NON-NLS-1$
				// Handle the MT response
				MTHandler handler = (MTHandler) session.getResponseHandler(response);
				if (handler != null) {
					handler.handle(session, argument, monitor);
				} else {
					throw new CVSException(new org.eclipse.core.runtime.Status(IStatus.ERROR,
						CVSProviderPlugin.ID, TeamException.IO_FAILED,
						NLS.bind(CVSMessages.Command_unsupportedResponse, new String[] { response, argument }), null)); 
				}
				// If a line is available, pass it on to the message listener 
				// and console as if it were an M response
				if (handler.isLineAvailable()) {
					String line = handler.getLine();
					IStatus status = listener.messageLine(line, session.getCVSRepositoryLocation(), session.getLocalRoot(), monitor);
					session.addError(status); // The session ignores OK status
					ConsoleListeners.getInstance().messageLineReceived(session, line, status);

				}
			} else if (response.equals("M")) {  //$NON-NLS-1$
				IStatus status = listener.messageLine(argument, session.getCVSRepositoryLocation(), session.getLocalRoot(), monitor);
				session.addError(status); // The session ignores OK status
				ConsoleListeners.getInstance().messageLineReceived(session, argument, status);
			} else if (response.equals("E")) { //$NON-NLS-1$
				IStatus status = listener.errorLine(argument, session.getCVSRepositoryLocation(), session.getLocalRoot(), monitor);
				session.addError(status); // The session ignores OK status
				ConsoleListeners.getInstance().errorLineReceived(session, argument, status);
			// handle other responses
			} else {
				ResponseHandler handler = session.getResponseHandler(response);
				if (handler != null) {
					handler.handle(session, argument, monitor);
				} else {
					throw new CVSException(new org.eclipse.core.runtime.Status(IStatus.ERROR,
						CVSProviderPlugin.ID, TeamException.IO_FAILED,
						NLS.bind(CVSMessages.Command_unsupportedResponse, new String[] { response, argument }), null)); 
				}
			}
		}
		if (!session.hasErrors()) {
			return ICommandOutputListener.OK;
		} else {
			return new MultiStatus(CVSProviderPlugin.ID, IStatus.INFO,
				session.getErrors(),
				NLS.bind(CVSMessages.Command_warnings, new String[] { getDisplayText() }), null);
		}
	}
	
	/*
	 * Provide the message that is used for the status that is generated when the server
	 * reports as error.
	 */
	protected String getServerErrorMessage() {
		return NLS.bind(CVSMessages.Command_serverError, new String[] { getDisplayText() });
	}
	protected String getDisplayText() {
		return getRequestId();
	}
}
