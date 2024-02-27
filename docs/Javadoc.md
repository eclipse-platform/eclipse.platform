

Javadoc
=======

The specifications for the Eclipse platform APIs are captured in the form of Javadoc comments on API packages, interfaces and classes, methods and constructors, and fields. The [Javadoc tool](http://docs.oracle.com/javase/7/docs/technotes/tools/windows/javadoc.html) (running the standard doclet) extracts these specifications and formats them into browsable form (HTML web pages) which become the reference section of the documentation set describing the Eclipse platform to ISVs. As a consequence, the bar is significantly higher for API Javadoc than for non-API. [Oracle's Requirements for Writing Java API Specifications](http://www.oracle.com/technetwork/java/javase/documentation/index-142372.html) deals with required semantic content of documentation comments for API specifications for the Java platform. All Eclipse project APIs should follow these conventions.

[Oracle's How to Write Doc Comments for Javadoc](http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html) contains style guide and tag conventions for documentation comments. These conventions lead to high-quality code and API documentation. All code written for the Eclipse Platform should follow these conventions except as noted below.

@version
--------

Do not use @version tags.

All HTML tags must be explicitly terminated
-------------------------------------------

All HTML tags appearing in Javadoc comments must be explicitly terminated, even the ones that are considered optional in older versions of HTML such as

		<p>...</p>

Various internal tools that post-process the extracted HTML documentation into other forms (e.g., Windows help file) need these tags.

Documenting interface method implementations
--------------------------------------------

When a method declared in an interface gets implemented in some class, there's often not a lot more to say about the method that wasn't already said in the Javadoc for the interface. In such cases, the Javadoc for the method can be omitted entirely. If the interface and the class will be Javadoc'd together, the standard 1.2 doclet automatically copies the method's description and tags from the interface to the class; if Javadoc'd separately, the automatically generated Javadoc for the method in the class will at least link it to the method in the interface.

In the source code, the implementation method should be annotated with the @Override annotation to alert the reader that the contract for the method is inherited from a super type. This reduces the amount of method contract duplication---a serious maintenance headache---without compromising readability of the code.

