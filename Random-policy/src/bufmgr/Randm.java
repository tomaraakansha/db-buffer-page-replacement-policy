
package bufmgr;

import java.util.Random;

import diskmgr.*;
import global.*;

/**
 * class Randm is a subclass of class Replacer use the Randm replacement policy
 * algorithm for page replacement
 */
class Randm extends Replacer {

	// Frame State Constants
	protected static final int AVAILABLE = 10;
	protected static final int REFERENCED = 11;
	protected static final int PINNED = 12;

	public Randm(BufMgr mgrArg) {
		super(mgrArg);
		// initialize the frame states
		for (int i = 0; i < frametab.length; i++) {
			frametab[i].state = AVAILABLE;
		}
	}

	/**
	 * Notifies the replacer of a new page.
	 */
	public void newPage(FrameDesc fdesc) {
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
		frametab[fdesc.index] = fdesc;
	}

	/**
	 * Finding a free frame in the buffer pool or choosing a page to replace using
	 * Random policy
	 *
	 * @return return the frame number return -1 if buffer is full
	 */

	public int pickVictim() {
		Random rand = new Random(131298); 
		int head = rand.nextInt(frametab.length);
		int i = 0;
		for (i = 0; i < frametab.length; i++) {
			head = (head + 1) % frametab.length;
			if (frametab[head].state != PINNED)
				break;
		}
		if (i == frametab.length)
			head = -1;
		return head;
	}

}