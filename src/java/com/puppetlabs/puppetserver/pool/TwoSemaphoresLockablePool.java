package com.puppetlabs.puppetserver.pool;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An implementation of LockablePool that uses two semaphores; one to restrict
 * access to the queue for "borrow" requests, and another that is used for the
 * "lock" method to keep track of when all of the outstanding instances have been
 * returned.  (More details below.)
 *
 * One concern that I have about this implementation is that the Semaphore class
 * seems to have really, really strict behavior about which thread gets the lock
 * next, and I'm a bit worried about starving agent requests under high load, but
 * we can explore that further if we decide we're leaning towards using this.
 */
public class TwoSemaphoresLockablePool<E> implements LockablePool<E> {
    // the main queue, like we've always had.
    private final LinkedBlockingDeque<E> queue;

    // a semaphore used to protect access to the queue during "borrow" calls
    private final ReducibleSemaphore borrowSemaphore = new ReducibleSemaphore(0);

    // a semaphore used to block the "lock" method from returning until all of
    // the instances are back in the pool.
    //private final ReducibleSemaphore lockSemaphore = new ReducibleSemaphore(0);

    // the main lock, used to allow re-entrant borrows and to prevent
    // "register" and "lock" from being called at the same time.
    private final ReentrantLock writeLock = new ReentrantLock();

    // a counter to track how many registered instances we have.  This might
    // go away if we combine this with the "register" method we have in the master
    // branch right now, where we are keeping a Set of the registered instances.
    private volatile int count = 0;

    public TwoSemaphoresLockablePool(int capacity) {
        this.queue = new LinkedBlockingDeque<>(capacity);
    }

    public void register(E e) throws InterruptedException {
        // lock to make sure we're the only thread mucking with the size of the pool.
        writeLock.lock();
        try {
            queue.putLast(e);
            count++;
            // increase the value of the semaphores to represent that there is one
            // more instance available.
            increaseAvailable();
        } finally {
            writeLock.unlock();
        }
    }

    public E borrowItem() throws InterruptedException {
        // this trick is how we support re-entrancy; the following
        // stanza will cause normal borrow requests to be blocked
        // waiting for the semaphore, so they can't even *try* to
        // call 'take' on the queue until they get past the semaphore.
        // we skip this block for threads that hold the write lock,
        // so that thread can access the queue directly.
        if (! writeLock.isHeldByCurrentThread()) {
            boolean locked = false;

            do {
                // this is what'll cause normal borrows to block
                borrowSemaphore.acquire();

                locked = writeLock.isLocked();
                if (locked)
                    borrowSemaphore.release();
            } while (locked);

            // we don't need to 'acquire' a ticket from the second semaphore,
            // it's only relevant to the "lock" method, but we do need to update
            // it to reflect the current number of instances available.
            //lockSemaphore.reduce(1);
        }
        return queue.takeFirst();
    }

    public void returnItem(E e) throws InterruptedException {
        queue.putFirst(e);
        if (! writeLock.isHeldByCurrentThread()) {
            // we only bump the semaphore counts if we're not holding the write
            // lock, because we only decreased them in "borrow" if we were not
            // holding the write lock.
            increaseAvailable();
        }
    }

    public synchronized void lock() throws InterruptedException {
        // get the main lock for the pool, mostly to make sure that there
        // are no calls to "register" in progress, so we know for sure
        // that 'count' is the correct number of instances in the pool.
        writeLock.lock();
        // this is the most important line in this whole implementation;
        // what we're doing here is reducing the number of tickets available in
        // the borrow semaphore by whatever the current size of the pool is.
        // this will effectively mean that the range of the semaphore is now
        // from a minimum of (0 - count) to a maximum of 0.  This means that
        // no more borrows will be possible until we increase the semaphore again
        // in "unlock".
        // borrowSemaphore.reduce(count);
        borrowSemaphore.acquire(count);
        // now we wait until enough "returnItem" calls have been made so that
        // the lock semaphore can acquire one ticket for each instance; effectively,
        // this means that all of the instances have been returned to the pool.
        // lockSemaphore.acquire(count);

        // side note: my original attempt at implementing this only used a semaphore,
        // and did an 'acquire(0)' on the line above.  The Semaphore class does
        // allow calling 'acquire(0)', and if you do that it will block until the
        // ticket count is non-negative, which seemed promising.  But... the problem
        // is that if there is another thread that has called 'acquire(1)' on the same
        // semaphore, that thread may be "in line" before our 'acquire(0)' thread, and
        // the implementation of Semaphore always only tries to give a ticket to the
        // next thread "in line".  Even if that thread is asking for a larger number of
        // tickets that can't be satisfied, and our thread is asking for zero.  So
        // that results in deadlock for some of our test cases.
    }

    public synchronized void unlock() {
        // assert writeLock.isHeldByCurrentThread();

        // add back the count to the lock semaphore
        // lockSemaphore.release(count);
        // release the main lock
        writeLock.unlock();
        // add the count back to the borrow semaphore; this shifts its range
        // back to have a minimum of zero and maximum of (count).  This
        // immediately unblocks pending borrows.
        borrowSemaphore.release(count);
    }

    private void increaseAvailable() {
        // convenience method to increment both semaphores at the same
        // time.
        borrowSemaphore.release();
        // lockSemaphore.release();
    }


}
