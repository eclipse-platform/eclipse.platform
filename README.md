# eclipse.platform
This repository contains the basis for the [Eclipse IDE](https://www.eclipse.org/eclipseide/) and a sub-repository of the [eclipse-platform](https://github.com/eclipse-platform) organization. 

It contains multiple modules:

### [eclipse.platform](https://github.com/eclipse-platform/eclipse.platform/tree/master/platform)
Platform provides images like the plash screen.

### [eclipse.resources](https://github.com/eclipse-platform/eclipse.platform/tree/master/resources)
Rsources provides java interfaces like IResource and implementations of workspace, folders, files and file system abstraction.

### [eclipse.runtime](https://github.com/eclipse-platform/eclipse.platform/tree/master/runtime) 
Runtime provides java interfaces like IJob, ISchedulingRule and implements scheduling of multithreaded jobs with exclusive access to a resource.

### [eclipse.update](https://github.com/eclipse-platform/eclipse.platform/tree/master/update)
Update provides java interface IPlatformConfiguration. IPlatformConfiguration represents the runtime configuration.

