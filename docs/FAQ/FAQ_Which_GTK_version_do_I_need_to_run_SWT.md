FAQ Which GTK version do I need to run SWT
==========================================

SWT requires the following GTK+ versions (or newer) to be installed:

* Eclipse/SWT 4.16.x: GTK+ 3.20.0 and its dependencies
* Eclipse/SWT 4.15.x: GTK+ 3.14.0 and its dependencies
* Eclipse/SWT 4.11.x - 4.14.x: GTK+ 3.10.0 and its dependencies
* Eclipse/SWT 4.10.x: GTK+ 3.8.0 and its dependencies
* Eclipse/SWT 4.6.x - 4.9.x: GTK+ 2.24.0 and its dependencies (for GTK+ 2) OR GTK+ 3.0.0 and its dependencies (for GTK+ 3)
* Eclipse/SWT 4.5.x: GTK+ 2.18.0 and its dependencies (for GTK+ 2) OR GTK+ 3.0.0 and its dependencies (for GTK+ 3)
* Eclipse/SWT 4.4.x: GTK+ 2.10.0 and its dependencies (for GTK+ 2) OR GTK+ 3.0.0 and its dependencies (for GTK+ 3)
* Eclipse/SWT 4.3.x: GTK+ 2.10.0 and its dependencies
* Eclipse/SWT 3.8.x: GTK+ 2.6.0 and its dependencies
* Eclipse/SWT 3.6.x - 3.7.x: GTK+ 2.4.1 and its dependencies
* Eclipse/SWT 3.0.x - 3.5.x: GTK+ 2.2.1 and its dependencies
* Eclipse/SWT 2.1.x: GTK+ 2.0.6 and its dependencies

Note that Eclipse/SWT 4.3.x includes early access support for GTK+ 3.x.  
Starting from Eclipse/SWT 4.4.x, Linux builds come with GTK+ 3 support enabled by default.

Starting from Eclipse/SWT 4.10.x, Linux builds only come with GTK+ 3 support, GTK+ 2 is no longer supported.

You can determine which version(s) of GTK you have installed with `rpm -q gtk2` or `rpm -q gtk3`.
