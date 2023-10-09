package org.eclipse.e4.core.internal.di;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name("DIEvent")
@Label("Inject")
@Category({ "Eclipse", "Platform", "DI" })
@StackTrace(false)
public class JfrDiEvent extends Event {

	@Label("target")
	String target;

	@Label("arguments")
	String arguments;

	public static final ThreadLocal<JfrDiEvent> EVENT = new ThreadLocal<>() {
		@Override
		protected JfrDiEvent initialValue() {
			return new JfrDiEvent();
		}
	};

}
