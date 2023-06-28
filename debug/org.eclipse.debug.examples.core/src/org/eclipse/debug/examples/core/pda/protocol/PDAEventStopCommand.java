/*******************************************************************************
 * Copyright (c) 2008, 2013 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.protocol;


/**
 * Optionally stop the interpreter when an error event <code>event_name</code>
 * is encountered; <code>{0|1}</code> specifies stop (<code>1</code>) or
 * continue (<code>0</code>). The possible events are <code>unimpinstr</code> and
 * <code>nosuchlabel</code>. Reply is <code>ok</code>. When an event is encountered,
 * the interpreter sends the error event (for example <code>unimlpemented instruction
 * foo</code>) and corresponding suspend event (for example <code>suspended event
 * unimpinstr</code>).
 *
 * <pre>
 *    C: eventstop {event_name} {0|1}
 *    R: ok
 *    ...
 *    E: suspended event {event_name}
 * </pre>
 *
 * Where event_name could be <code>unimpinstr</code> or <code>nosuchlabel</code>.
 */

public class PDAEventStopCommand extends PDACommand {

	public static final int UNIMPINSTR = 0;
	public static final int NOSUCHLABEL = 1;

	public PDAEventStopCommand(int event, boolean enable) {
		super("eventstop " +  //$NON-NLS-1$
			  (event == UNIMPINSTR ? "unimpinstr " : "nosuchlabel ") +  //$NON-NLS-1$ //$NON-NLS-2$
			  (enable ? "1" : "0")); //$NON-NLS-1$ //$NON-NLS-2$
	}


	@Override
	public PDACommandResult createResult(String resultText) {
		return new PDACommandResult(resultText);
	}
}
