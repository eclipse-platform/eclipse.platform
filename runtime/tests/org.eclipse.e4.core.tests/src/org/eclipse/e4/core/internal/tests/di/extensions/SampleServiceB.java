package org.eclipse.e4.core.internal.tests.di.extensions;

import org.osgi.service.component.annotations.Component;

@Component(property = "service.ranking:Integer=40")
public class SampleServiceB implements TestService {

}
