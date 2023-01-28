
package bufmgr;

import diskmgr.*;
import global.*;

/**
 * class Policy is a subclass of class Replacer use the given replacement
 * policy algorithm for page replacement
 */
class FIFO extends Replacer {
  // replace Policy above with impemented policy name (e.g., Lru, Clock)

  //
  // Frame State Constants
  //
  protected static final int AVAILABLE = 10;
  protected static final int REFERENCED = 11;
  protected static final int PINNED = 12;

  // Following are the fields required for LRU and MRU policies:
  /**
   * private field
   * An array to hold number of frames in the buffer pool
   */

  private int frames[];

  /**
   * private field
   * number of frames used
   */
  private int nframes;

  /** Clock head; required for the default clock algorithm. */
  protected int head;

  /**
   * This pushes the given frame to the end of the list.
   * 
   * @param frameNo the frame number
   */
  private void update(int frameNo) {
    int copy = frames[frameNo];
    int i;
    // This function is to be used for LRU and MRU
    // LRU : put the LRU element or frameNo to end of array
    for (i = frameNo; i < frames.length - 1; i++) {
      frames[i] = frames[i + 1];
    }
    frames[frames.length - 1] = copy;
  }

  /**
   * Class constructor
   * Initializing frames[] pinter = null.
   */
  public FIFO(BufMgr mgrArg) {
    super(mgrArg);
    // initialize the frame states
    for (int i = 0; i < frametab.length; i++) {
      frametab[i].state = AVAILABLE;
    }
    frames = new int[frametab.length];
    // initialize the frames with indexs
    for (int i = 0; i < frames.length; i++) {
      frames[i] = i;
    }
    // initialize parameters for LRU and MRU
    nframes = 0;
    frames = new int[frametab.length];

    // initialize the clock head for Clock policy
    head = -1;
  }

  /**
   * Notifies the replacer of a new page.
   */
  public void newPage(FrameDesc fdesc) {
    // no need to update frame state
    frametab[fdesc.index] = fdesc;
  }

  /**
   * Notifies the replacer of a free page.
   */
  public void freePage(FrameDesc fdesc) {
    fdesc.state = AVAILABLE;
    frametab[fdesc.index] = fdesc;
  }

  /**
   * Notifies the replacer of a pined page.
   */
  public void pinPage(FrameDesc fdesc) {
    frametab[fdesc.index] = fdesc;
  }

  /**
   * Notifies the replacer of an unpinned page.
   */
  public void unpinPage(FrameDesc fdesc) {
    // frametab[fdesc.index].state = fdesc.state;
    // frametab[fdesc.index].pincnt = fdesc.pincnt;
    frametab[fdesc.index] = fdesc;
  }

  /**
   * Finding a free frame in the buffer pool
   * or choosing a page to replace using your policy
   *
   * @return return the frame number
   *         return -1 if failed
   */

  public int pickVictim() {
    // remove the below statement and write your code
    int victim = 0;
    int flength = frametab.length;
    int i;
    for (i = 0; i < flength; i++) {
      // if the index is not pinned, replace the index and update
      if (frametab[i].state == AVAILABLE) {
        victim = i;
        break;
      }
    }
    if (victim == flength) {
      for (i = 0; i < flength; i++) {
        // if the index is not pinned, replace the index and update
        if (frametab[frames[i]].state != PINNED) {
          victim = frames[i];
          update(i);
          break;
        }
      }
    }
    if (victim == flength)
      return -1;
    return victim; // for compilation
  }
}
