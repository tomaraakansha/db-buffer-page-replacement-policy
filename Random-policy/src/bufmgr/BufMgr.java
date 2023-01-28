package bufmgr;

import global.GlobalConst;
import global.Minibase;
import global.Page;
import global.PageId;
import java.util.HashMap;

/* revised slightly by sharma on 8/22/2022 */

/**
 * <h3>Minibase Buffer Manager</h3> The buffer manager reads disk pages into a
 * mains memory page as needed. The collection of main memory pages (called
 * frames) used by the buffer manager for this purpose is called the buffer
 * pool. This is just an array of Page objects. The buffer manager is used by
 * access methods, heap files, and relational operators to read, write,
 * allocate, and de-allocate pages. policy class name has to be changed in the
 * constructior using name of the class you have implementaed
 */
public class BufMgr implements GlobalConst {

	/** Actual pool of pages (can be viewed as an array of byte arrays). */
	protected Page[] bufpool;

	/**
	 * Array of descriptors, each containing the pin count, dirty status, etc\ .
	 */
	protected FrameDesc[] frametab;

	/** Maps current page numbers to frames; used for efficient lookups. */
	protected HashMap<Integer, FrameDesc> pagemap;

	/** The replacement policy to use. */
	protected Replacer replacer;

	int totPageHits;
	int totPageRequests;
	int pageFaults;
	float aggregateBHR;
	int[][] rfrArr;

	/**
	 * Constructs a buffer mamanger with the given settings.
	 * 
	 * @param numbufs number of buffers in the buffer pool
	 */
	public BufMgr(int numbufs) {
		numbufs = NUMBUF;
		// initializing buffer pool and frame table
		bufpool = new Page[numbufs];
		frametab = new FrameDesc[numbufs];

		totPageHits = 0;
		totPageRequests = 0;
		pageFaults = 0;
		aggregateBHR = -1;

		rfrArr = new int[10000][3];
		for (int i = 0; i < 9; i++)
			rfrArr[i][2] = -1;
		for (int i = 9; i < rfrArr.length; i++)
			rfrArr[i][2] = i;

		for (int i = 0; i < frametab.length; i++) {
			bufpool[i] = new Page();
			frametab[i] = new FrameDesc(i);
		}

		// initializing page map and replacer here.
		pagemap = new HashMap<Integer, FrameDesc>(numbufs);
		replacer = new Randm(this); // change Policy to replacement class name
	}

	/**
	 * Allocates a set of new pages, and pins the first one in an appropriate frame
	 * in the buffer pool.
	 * 
	 * @param firstpg  holds the contents of the first page
	 * @param run_size number of pages to allocate
	 * @return page id of the first new page
	 * @throws IllegalArgumentException if PIN_MEMCPY and the page is pinned
	 * @throws IllegalStateException    if all pages are pinned (i.e. pool exceeded)
	 */

	public PageId newPage(Page firstpg, int run_size) {
		// Allocating set of new pages on disk using run size.
		PageId firstpgid = Minibase.DiskManager.allocate_page(run_size);
		try {
			// pin the first page using pinpage() function using the id of firstpage, page
			// firstpg and skipread = PIN_MEMCPY(true)
			pinPage(firstpgid, firstpg, PIN_MEMCPY);
		} catch (Exception e) {
			// pinning failed so deallocating the pages from disk
			for (int i = 0; i < run_size; i++) {
				firstpgid.pid += i;
				Minibase.DiskManager.deallocate_page(firstpgid);
			}
			return null;
		}

		// notifying replacer
		replacer.newPage(pagemap.get(Integer.valueOf(firstpgid.pid)));

		// return the page id of the first page
		return firstpgid;
	}

	/**
	 * Deallocates a single page from disk, freeing it from the pool if needed.
	 * 
	 * @param pageno identifies the page to remove
	 * @throws IllegalArgumentException if the page is pinned
	 */
	public void freePage(PageId pageno) {
		// the frame descriptor as the page is in the buffer pool
		FrameDesc tempfd = pagemap.get(Integer.valueOf(pageno.pid));
		// the page is in the pool so it cannot be null.
		if (tempfd != null) {
			// checking the pin count of frame descriptor
			if (tempfd.pincnt > 0)
				throw new IllegalArgumentException("Page currently pinned");
			// remove page as it's pin count is 0, remove the page, updating its pin count
			// and dirty status, the policy and notifying replacer.
			pagemap.remove(Integer.valueOf(pageno.pid));
			tempfd.pageno.pid = INVALID_PAGEID;
			tempfd.pincnt = 0;
			tempfd.dirty = false;
			tempfd.state = Randm.AVAILABLE;
			replacer.freePage(tempfd);
		}
		// deallocate the page from disk
		Minibase.DiskManager.deallocate_page(pageno);
	}

	/**
	 * Pins a disk page into the buffer pool. If the page is already pinned, this
	 * simply increments the pin count. Otherwise, this selects another page in the
	 * pool to replace, flushing it to disk if dirty.
	 * 
	 * @param pageno   identifies the page to pin
	 * @param page     holds contents of the page, either an input or output param
	 * @param skipRead PIN_MEMCPY (replace in pool); PIN_DISKIO (read the page in)
	 * @throws IllegalArgumentException if PIN_MEMCPY and the page is pinned
	 * @throws IllegalStateException    if all pages are pinned (i.e. pool exceeded)
	 */
	public void pinPage(PageId pageno, Page page, boolean skipRead) {
		// the frame descriptor as the page is in the buffer pool
		FrameDesc tempfd = pagemap.get(Integer.valueOf(pageno.pid));
		if (pageno.pid > 8)
			totPageRequests = totPageRequests + 1;

		if (tempfd != null) {
			// if the page is in the pool and already pinned then by using PIN_MEMCPY(true)
			// throws an exception "Page pinned PIN_MEMCPY not allowed"
			if (skipRead)
				throw new IllegalArgumentException("Page pinned so PIN_MEMCPY not allowed");
			else {
				// else the page is in the pool and has not been pinned so incrementing the
				// pincount and setting Policy status to pinned
				tempfd.pincnt++;
				tempfd.state = Randm.PINNED;
				page.setPage(bufpool[tempfd.index]);

				if (pageno.pid > 8) {
					totPageHits = totPageHits + 1;
					try {
						rfrArr[pageno.pid][1] = rfrArr[pageno.pid][1] + 1;
					} catch (Exception e) {
						System.out.println("Page id is greater than defined size of array");
					}
				}
				return;
			}
		} else {
			// as the page is not in pool choosing a victim
			int i = replacer.pickVictim();
			// if buffer pool is full throws an Exception("Buffer pool exceeded")
			if (i < 0)
				throw new IllegalStateException("Buffer pool exceeded");

			tempfd = frametab[i];

			if (pageno.pid > 8) {
				pageFaults = pageFaults + 1;
				try {
					rfrArr[pageno.pid][0] = rfrArr[pageno.pid][0] + 1;
				} catch (Exception e) {
					System.out.println("Page id is greater than defined size of array");
				}
			}
			// if the victim is dirty writing it to disk
			if (tempfd.pageno.pid != -1) {
				pagemap.remove(Integer.valueOf(tempfd.pageno.pid));
				if (tempfd.dirty)
					Minibase.DiskManager.write_page(tempfd.pageno, bufpool[i]);
			}
			// reading the page from disk to the page given and pinning it.
			if (skipRead)
				bufpool[i].copyPage(page);
			else
				Minibase.DiskManager.read_page(pageno, bufpool[i]);
			page.setPage(bufpool[i]);
		}
		// updating frame descriptor and notifying to replacer
		tempfd.pageno.pid = pageno.pid;
		tempfd.pincnt = 1;
		tempfd.dirty = false;
		pagemap.put(Integer.valueOf(pageno.pid), tempfd);
		tempfd.state = Randm.PINNED;
		replacer.pinPage(tempfd);

	}

	/**
	 * Unpins a disk page from the buffer pool, decreasing its pin count.
	 * 
	 * @param pageno identifies the page to unpin
	 * @param dirty  UNPIN_DIRTY if the page was modified, UNPIN_CLEAN otherrwise
	 * @throws IllegalArgumentException if the page is not present or not pinned
	 */
	public void unpinPage(PageId pageno, boolean dirty) {
		// the frame descriptor as the page is in the buffer pool
		FrameDesc tempfd = pagemap.get(Integer.valueOf(pageno.pid));

		// if page is not present an exception is thrown as "Page not present"
		if (tempfd == null)
			throw new IllegalArgumentException("Page not present");

		// if the page is present but not pinned an exception is thrown as "page not
		// pinned"
		if (tempfd.pincnt == 0) {
			throw new IllegalArgumentException("Page not pinned");
		} else {
			// unpinning the page by decrementing pincount and updating the frame descriptor
			// and notifying replacer
			tempfd.pincnt--;
			tempfd.dirty = dirty;
			if (tempfd.pincnt == 0)
				tempfd.state = Randm.REFERENCED;
			replacer.unpinPage(tempfd);
			return;
		}
	}

	/**
	 * Immediately writes a page in the buffer pool to disk, if dirty.
	 */
	public void flushPage(PageId pageno) {
		for (int i = 0; i < frametab.length; i++)
			// checking for pageid or id the pageid is the frame descriptor and the dirty
			// status of the page
			if ((pageno == null || frametab[i].pageno.pid == pageno.pid) && frametab[i].dirty) {
				// writing down to disk if dirty status is true and updating dirty status of
				// page to clean
				Minibase.DiskManager.write_page(frametab[i].pageno, bufpool[i]);
				frametab[i].dirty = false;
			}
	}

	/**
	 * Immediately writes all dirty pages in the buffer pool to disk.
	 */
	public void flushAllPages() {
		for (int i = 0; i < frametab.length; i++)
			flushPage(frametab[i].pageno);
	}

	/**
	 * Gets the total number of buffer frames.
	 */
	public int getNumBuffers() {
		return bufpool.length;
	}

	/**
	 * Gets the total number of unpinned buffer frames.
	 */
	public int getNumUnpinned() {
		int numUnpinned = 0;
		for (int i = 0; i < frametab.length; i++) {
			if (frametab[i].pincnt == 0)
				numUnpinned++;
		}
		return numUnpinned;
	}

	/**
	 * Sort columns 0,1,2 of a 2D array on basis of column 1.
	 */
	public int[][] sortArray(int[][] Arr) {
		int j = 1, i = 0;
		int key1 = 0;
		int key2 = 0;
		int key0 = 0;
		for (j = 1; j < Arr.length; j++) {
			key0 = Arr[j][0];
			key1 = Arr[j][1];
			key2 = Arr[j][2];
			i = j - 1;
			while (i > 0 && key1 <= Arr[i][1]) {
				Arr[i + 1][0] = Arr[i][0];
				Arr[i + 1][1] = Arr[i][1];
				Arr[i + 1][2] = Arr[i][2];
				i--;
			}
			Arr[i + 1][0] = key0;
			Arr[i + 1][1] = key1;
			Arr[i + 1][2] = key2;
		}
		return Arr;

	}

	/**
	 * Prints the BHR of the buffer along with top 20 in desc order of no. of page
	 * hits.
	 */
	public void printBhrAndRefCount() {

		aggregateBHR = (float) totPageHits / pageFaults;

		System.out.println("+----------------------------------------+");
		System.out.println("This is Random Policy ");
		System.out.println("+----------------------------------------+");
		System.out.println("Aggregate Page Hits: " + totPageHits);
		System.out.println("+----------------------------------------+");
		System.out.println("Aggregate Page Loads: " + pageFaults);
		System.out.println("+----------------------------------------+");
		System.out.printf("BHR (for the whole buffer):  %9.5f\n", aggregateBHR);
		System.out.println("+----------------------------------------+");
		System.out.println("The top 20 referenced pages are: ");
		System.out.println("+----------------------------------------+");
		System.out.println("Array index Page No.	 No. of Loads 	 No. of Page HIts ");

		rfrArr = sortArray(rfrArr);
		int refPgCnt = 0;
		int len = rfrArr.length - 1;
		while (refPgCnt < 20) {
			if (rfrArr[len][2] > -1) {
				System.out.println(rfrArr[len][2] + "	" + rfrArr[len][0] + " 	 " + rfrArr[len][1]);
				refPgCnt++;
			}
			len--;
		}
		len = rfrArr.length - 1;
		int refloadcnt=0;
		int refhitcnt=0;
		while (len>-1) {
			refloadcnt=refloadcnt+ rfrArr[len][0] ;
			refhitcnt=refhitcnt+ rfrArr[len][1] ;
			refPgCnt++;
			len--;
		}
		System.out.println("+----------------------------------------+");
		System.out.println("total hitcount of array is: "+ refhitcnt );
		System.out.println("total new loadcount of array is: "+ refloadcnt);
	}

} // public class BufMgr implements GlobalConst
