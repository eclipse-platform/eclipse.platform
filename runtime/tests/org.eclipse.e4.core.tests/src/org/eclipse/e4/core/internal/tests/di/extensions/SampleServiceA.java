package org.eclipse.e4.core.internal.tests.di.extensions;

import org.osgi.service.component.annotations.Component;

@Component(property = "service.ranking:Integer=50")
public class SampleServiceA implements TestService {

}
