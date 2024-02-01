

Progress Reporting
==================

**Are you working with Progress Monitors, and wish you did not have too?**

_by Henrik Lindberg - originally posted on [Eclipse by Planatery Transitions](http://henrik-eclipse.blogspot.com/2009/05/progress-monitor-patterns.html)_

Contents
--------

*   [1 Problem](#Problem)
*   [2 Antipattern](#Antipattern)
*   [3 Good pattern](#Good-pattern)
*   [4 Good pattern - regular loop](#Good-pattern---regular-loop)
*   [5 Rules of Thumb](#Rules-of-Thumb)

Problem
-------

I recently got to investigate issues in p2 why the progress bar did not move while it was very clear that things where happening as subtasks changed the text. Looking into what was going on made me find a new Progress Monitor anti pattern.

The problem is how to handle a case where you need to do something like this:

    for (int i = 0; i < candidates.length; i++) {
      if (loadCandidate1(i)) {
        break;
      }
    }

Where a call to `loadCandidate(int)` is potentially a long running task. The first loaded candidate means we are done.

Antipattern
-----------

Here is an implementation of the above example using the antipattern - i.e. "don't do this":
 

    public void antiPattern(IProgressMonitor monitor) {
      SubMonitor sub = SubMonitor.convert(monitor, candidates.length);
      for (int i = 0; i < candidates.length; i++) {
        if (loadCandidate(i, sub.newChild(1))) {
          break;
        }
      }
    }
     
    public boolean loadCandidate(int index, IProgressMonitor monitor) {
      SubMonitor sub = SubMonitor.convert(monitor, 1000);
      try {
        for (int i = 0; i < 1000; i++) {
          sub.worked(1);
        }
        // ...
        return true;
      } finally {
        monitor.done();
      }
      return false;
    }

Well, what is wrong with this, you may ask...Well, the length of the progress bar will be divided into as many slots as there are candidates, and if the first candidate succeeds and uses its 1000 ticks, and the remaining candidates are never considered, we will end up reporting the 1000 ticks on a fraction of the overall progress bar. This means that for 10 candidates, you will see the progress-bar slowly go to about 10% of the overall length, to suddenly jump to 100%.

Good pattern
------------

Here is the good pattern that makes use of the full progress bar:

    public void goodPattern(IProgressMonitor monitor) {
      SubMonitor sub = SubMonitor.convert(monitor, 1000);
      for (int i = 0; i < candidates.length; i++) {
        sub.setWorkRemaining(1000);
        if (goodLoadCandidate(i, sub.newChild(1000))) {
          break;
        }
            break;
      }
    }
     
    public boolean goodLoadCandidate(int index, IProgressMonitor monitor) {
      SubMonitor sub = SubMonitor.convert(monitor, 1000);
      sub.beginTask(null, 1000);
      try {
        // code that loads...
        for (int i = 0; i < 1000; i++) {
          sub.worked(1);
        }
     
        return true;
      } finally {
        // ignore errors
      }
      return false;
    }

Notice how [`setWorkRemaining(int)`](http://help.eclipse.org/stable/nftopic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/core/runtime/SubMonitor.html#setWorkRemaining(int)) is called each time in the loop. This reallocates the remaining ticks, but there is no need to compute how many that actually remains, the child allocation of 1000 ticks will always give the child 1000 ticks to report, even if there were not enough ticks left in the parent. All the scaling is performed by the SubMonitor, so you don’t have to worry about it.

Now, let’s say that the routine you are calling do need to perform a bit of work even if it does not need all of its ticks. Don’t worry, that will work too as long as it is a small portion of the allocation. If you risk consuming a larger part, you may be better off doing a true partitioning of the progress-bar as shown in the next section.

Good pattern - regular loop
---------------------------

Here is the good pattern for a regular loop where each iteration does consume ticks.

    public void goodLoopPattern(IProgressMonitor monitor) {
      SubMonitor sub = SubMonitor.convert(monitor, candidates.length*100);
      for (int i = 0; i < candidates.length; i++) {
        if (goodLoopCandidate(i, sub.newChild(100))) {
          break;
        }
      }
    }
     
    public boolean goodLoopCandidate(int index, IProgressMonitor monitor) {
      SubMonitor sub = SubMonitor.convert(monitor, 1000);
      sub.beginTask(null, 1000);
      try {
        // code that loads...
        for (int i = 0; i < 1000; i++) {
          sub.worked(1);
        }
     
        return true;
      } finally {
        // ignore errors
      }
      return false;
    }

Here I simply use SubMonitor’s [`newChild(int)`](http://help.eclipse.org/stable/nftopic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/core/runtime/SubMonitor.html#newChild(int)), this works without calling “done” because the next call to newChild (or “done” for that matter) will consume the ticks allocated for the child.

Rules of Thumb
--------------

1.  Use SubMonitor.
2.  Always begin by converting the monitor you get to a SubMonitor.
3.  Use newChild(n) to create a sub monitor to pass to methods that take an IProgressMonitor as argument.
4.  Never call “done” - but document that the caller must do so unless they used a SubMonitor.

