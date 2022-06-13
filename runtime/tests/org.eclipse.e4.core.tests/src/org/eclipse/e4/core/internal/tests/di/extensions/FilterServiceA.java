package org.eclipse.e4.core.internal.tests.di.extensions;

import org.osgi.service.component.annotations.Component;

@Component(property = { "filtervalue=Test", "service.ranking:Integer=1" })
public class FilterServiceA implements TestService {

}
