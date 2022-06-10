package org.eclipse.e4.core.internal.tests.di.extensions;

import org.osgi.service.component.annotations.Component;

@Component(name = "DisabledServiceB", enabled = false, property = { "component=disabled", "service.ranking:Integer=5" })
public class DisabledServiceB implements TestService {

}
