

FAQ What is the Eclipse Platform?
=================================

Those who download the generic Eclipse Platform-usually by mistake-are somewhat confounded by what they see. The platform was conceived as the generic foundation for an IDE. That is, the platform is an IDE without any particular programming language in mind. You can create generic projects, edit files in a generic text editor, and share the projects and files with a Concurrent Versions System (CVS) server. The platform is essentially a glorified version of a file-system browser.

As an end user, what you don't see when you download and run the platform is that the architecture is designed from the ground up for extensibility. In fact, everything you see is a plug-in, and everything you see can be tweaked, replaced, or augmented using various hooks. To draw a computing analogy, it's like the Internet Protocol (IP): exceedingly generic, not terribly interesting by itself, but a solid foundation on which very interesting applications can be built.

A carefully designed subset of the Eclipse Platform has been produced in Eclipse 3.0: the Rich Client Platform (RCP). Despite the name, this is not the version of the platform sold for fat profits to rich clients. This is the portion of the platform that is interesting for non-development-environment applications. We thought this part so interesting that we dedicated almost half the book to RCP alone.

