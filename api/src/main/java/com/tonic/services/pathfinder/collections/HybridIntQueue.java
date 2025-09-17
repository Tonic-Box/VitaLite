package com.tonic.services.pathfinder.collections;

/**
 * A hybrid queue that stores integers, combining a standard FIFO queue with a delayed release mechanism.
 *
 * <p>This data structure maintains a main circular queue for immediate enqueuing and dequeuing of elements,
 * and a separate "transport" layer for elements that should only become available after a specified
 * number of "expansions." Once the expansion count meets or exceeds an element's release time,
 * that element is automatically transferred into the main queue. If the main queue is ever empty
 * and delayed elements remain, it will forcibly release the earliest delayed element, ensuring that
 * there's always a value to dequeue.</p>
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>A fast, circular buffer for the main queue.</li>
 *   <li>A secondary storage for delayed-release elements, providing deferred availability.</li>
 *   <li>Automatic "expansion" tracking, incremented on each dequeue, controls when delayed elements are released.</li>
 *   <li>Optional forced early release of delayed elements if the main queue becomes empty.</li>
 * </ul>
 *
 * <p>This structure is useful for scenarios where elements are expected to become "ready" only after
 * certain conditions or time steps have passed, but you still want a queue-like interface for retrieving them.</p>
 */
public final class HybridIntQueue {
    private final int[] data;
    private final int capacityMask;
    private int head;
    private int tail;

    // Maintain a global expansion count
    private int expansions = 0;

    // Transport min-heap arrays
    private int[] transportValues;
    private int[] transportReleaseTimes;
    private int transportCount = 0;
    private int transportCapacity;

    /**
     * Creates a new HybridIntQueue with the specified capacity.
     * @param capacity The initial capacity of the queue.
     */
    public HybridIntQueue(final int capacity) {
        int adjustedCapacity = nextPowerOfTwo(capacity);
        data = new int[adjustedCapacity];
        capacityMask = adjustedCapacity - 1;
        head = 0;
        tail = 0;

        // Initialize transport arrays with some initial capacity (can be tuned)
        this.transportCapacity = 64;
        this.transportValues = new int[transportCapacity];
        this.transportReleaseTimes = new int[transportCapacity];
    }

    /**
     * Returns the number of elements in the queue.
     * @return The number of elements in the queue.
     */
    public int size() {
        return (tail - head) & capacityMask;
    }

    /**
     * Enqueues a new value into the queue.
     * @param value The value to enqueue.
     */
    public void enqueue(final int value) {
        if (((tail + 1) & capacityMask) == head) {
            // Queue is full; handle resize if necessary
            throw new IllegalStateException("Main queue is full");
        }
        data[tail] = value;
        tail = (tail + 1) & capacityMask;
    }

    /**
     * Enqueue a transport node that should only become available after 'delay' expansions.
     * @param value The value to enqueue.
     * @param delay The number of expansions to delay.
     */
    public void enqueueTransport(int value, int delay) {
        if (transportCount == transportCapacity) {
            growTransportArrays();
        }
        transportValues[transportCount] = value;
        transportReleaseTimes[transportCount] = expansions + delay;
        siftUp(transportCount);
        transportCount++;
    }

    /**
     * Dequeues a value from the queue.
     * @return The dequeued value, or -1 if the queue is empty.
     */
    public int dequeue() {
        // Release all ready transports
        releaseReadyTransports();

        if (isMainQueueEmpty()) {
            // Main queue is empty. Force release the earliest transport if any.
            if (transportCount > 0) {
                // The earliest transport is at the root of the heap
                int earliestValue = transportValues[0];
                removeMinTransport();
                enqueue(earliestValue);
            }
        }

        if (isMainQueueEmpty()) {
            // Still nothing to dequeue
            return -1; // or throw exception
        }

        int value = data[head];
        head = (head + 1) & capacityMask;
        expansions++;

        // After expansion, release ready transports again
        releaseReadyTransports();

        return value;
    }

    /**
     * Checks if the entire queue (main + transport) is empty.
     * @return True if empty, false otherwise.
     */
    public boolean isEmpty() {
        return isMainQueueEmpty() && transportCount == 0;
    }

    private boolean isMainQueueEmpty() {
        return head == tail;
    }

    /**
     * Releases all transport elements that are ready to be enqueued.
     */
    private void releaseReadyTransports() {
        while (transportCount > 0 && transportReleaseTimes[0] <= expansions) {
            // The earliest transport is ready
            enqueue(transportValues[0]);
            removeMinTransport();
        }
    }

    /**
     * Removes and returns the transport element with the smallest release time.
     */
    private void removeMinTransport() {
        if (transportCount == 0) return;
        // Replace root with the last element
        transportCount--;
        if (transportCount > 0) {
            transportValues[0] = transportValues[transportCount];
            transportReleaseTimes[0] = transportReleaseTimes[transportCount];
            siftDown(0);
        }
    }

    /**
     * Sifts up the element at the given index to maintain heap property.
     */
    private void siftUp(int idx) {
        while (idx > 0) {
            int parent = (idx - 1) >>> 1;
            if (transportReleaseTimes[idx] >= transportReleaseTimes[parent]) {
                break;
            }
            swapTransportElements(idx, parent);
            idx = parent;
        }
    }

    /**
     * Sifts down the element at the given index to maintain heap property.
     */
    private void siftDown(int idx) {
        int leftChild;
        while ((leftChild = (idx << 1) + 1) < transportCount) {
            int smallest = leftChild;
            int rightChild = leftChild + 1;
            if (rightChild < transportCount && transportReleaseTimes[rightChild] < transportReleaseTimes[leftChild]) {
                smallest = rightChild;
            }
            if (transportReleaseTimes[idx] <= transportReleaseTimes[smallest]) {
                break;
            }
            swapTransportElements(idx, smallest);
            idx = smallest;
        }
    }

    /**
     * Swaps two transport elements in the heap.
     */
    private void swapTransportElements(int i, int j) {
        int tempValue = transportValues[i];
        transportValues[i] = transportValues[j];
        transportValues[j] = tempValue;

        int tempTime = transportReleaseTimes[i];
        transportReleaseTimes[i] = transportReleaseTimes[j];
        transportReleaseTimes[j] = tempTime;
    }

    private void growTransportArrays() {
        int newCapacity = transportCapacity << 1;
        int[] newValues = new int[newCapacity];
        int[] newTimes = new int[newCapacity];
        System.arraycopy(transportValues, 0, newValues, 0, transportCount);
        System.arraycopy(transportReleaseTimes, 0, newTimes, 0, transportCount);
        transportValues = newValues;
        transportReleaseTimes = newTimes;
        transportCapacity = newCapacity;
    }

    /**
     * Computes the next power of two greater than or equal to n.
     */
    private static int nextPowerOfTwo(int n) {
        if (n <= 0) throw new IllegalArgumentException("Capacity must be positive");
        return 1 << (32 - Integer.numberOfLeadingZeros(n - 1));
    }
}