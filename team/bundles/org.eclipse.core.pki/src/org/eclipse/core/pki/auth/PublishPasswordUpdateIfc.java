package org.eclipse.core.pki.auth;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

public interface PublishPasswordUpdateIfc extends Publisher<String>{
	
	//public static PublishPasswordUpdate getInstance();
	public void subscribe(Subscriber subscriber);
	public int getSubscriberCount();
	public void publishMessage(String message);
	public void close();
}
