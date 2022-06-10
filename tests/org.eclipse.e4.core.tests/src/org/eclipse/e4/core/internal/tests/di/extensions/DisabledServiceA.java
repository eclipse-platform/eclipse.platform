package org.eclipse.e4.core.internal.tests.di.extensions;

import org.osgi.service.component.annotations.Component;

@Component(name = "DisabledServiceA", enabled = false, property = { "component=disabled" })
public class DisabledServiceA implements TestService {

}
