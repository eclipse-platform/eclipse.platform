# Linux native binding for platform.resources

This fragment provides native binding to the linux host system using the [Project Panama](https://openjdk.org/projects/panama/).
The wrapped API is the inotify API (https://man7.org/linux/man-pages/man7/inotify.7.html)

Generate the code:
- sudo apt-get install libinotifytools0-dev
- get a suitable jextract from https://jdk.java.net/panama/ (Java 19) or https://jdk.java.net/panama/17/ (Java 17) 
- enter the src<java version> directory
- jextract --source -t org.eclipse.core.internal.resources.refresh.linux.inotify /usr/include/inotifytools/inotify.h