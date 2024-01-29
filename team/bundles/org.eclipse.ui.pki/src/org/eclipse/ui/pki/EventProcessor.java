/*******************************************************************************
 * Copyright (c) 2023 Eclipse Platform, Security Group and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Platform - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.pki;

import java.util.Map;

import org.eclipse.osgi.framework.eventmgr.CopyOnWriteIdentityMap;
import org.eclipse.osgi.framework.eventmgr.EventDispatcher;
import org.eclipse.osgi.framework.eventmgr.EventManager;
import org.eclipse.osgi.framework.eventmgr.ListenerQueue;

public class EventProcessor implements EventDispatcher<Object, Object, Object> {
	private EventManager eventManager=null;
	private ListenerQueue<PKIController, Object, EventManager> queue = null;
	private PKIController localStartup=null;
	private static EventProcessor eventProcessor = null;
	private boolean isPending=false;
	public static EventProcessor getInstance() {
		if ( eventProcessor == null ) {
			synchronized(EventProcessor.class) {
				if ( eventProcessor == null ) {
					eventProcessor = new EventProcessor();
				}
			}
		}
		return eventProcessor; 
	}
	private EventProcessor() {}
	public void initializeEvent(PKIController startup) {
		//System.out.println("EventProcessor, initialize.");
		this.localStartup=startup;
		eventManager =  new EventManager("PKI event");
		Map<PKIController, Object> listeners = new CopyOnWriteIdentityMap<PKIController, Object>();
		listeners.put(startup,null);
		queue = new ListenerQueue<PKIController, Object, EventManager>(eventManager);
	}
	public void sendEvent(int event) {
		
		if ( this.localStartup != null ) {
			this.dispatchEvent(this.localStartup.eventRunner(event), queue, event, queue );
		} else {
			System.out.println("EventProcessor,  waited and wated and waited some more..  But alas, nothing.");
			isPending=true;
		}
	}
	
	public void dispatchEvent(Object eventListener, Object listenerObject, int eventAction, Object eventObject) {
		// TODO Auto-generated method stub
		((Runnable) eventListener).run();
	}
	
	public boolean isEventPending() {
		return isPending;
	}
}
