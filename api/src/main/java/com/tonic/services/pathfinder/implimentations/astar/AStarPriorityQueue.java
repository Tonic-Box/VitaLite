package com.tonic.services.pathfinder.implimentations.astar;

/**
 * Min-heap priority queue optimized for A* pathfinding.
 * Uses parallel primitive arrays for cache-friendly access.
 * Each entry contains (position, fScore) pairs.
 */
public class AStarPriorityQueue
{
    private int[] positions;    // Node positions (compressed WorldPoints)
    private int[] fScores;      // f-scores (g + h) for priority
    private int size;
    private final int capacity;

    public AStarPriorityQueue(int capacity) {
        this.capacity = capacity;
        this.positions = new int[capacity];
        this.fScores = new int[capacity];
        this.size = 0;
    }

    /**
     * Adds a node to the priority queue.
     * @param position Compressed position
     * @param fScore Priority (lower is better)
     */
    public void enqueue(int position, int fScore) {
        if (size >= capacity) {
            throw new IllegalStateException("Priority queue capacity exceeded");
        }

        positions[size] = position;
        fScores[size] = fScore;
        size++;

        // Bubble up to maintain heap property
        heapifyUp(size - 1);
    }

    /**
     * Removes and returns the node with lowest f-score.
     * @return Compressed position of node with minimum f-score
     */
    public int dequeue() {
        if (size == 0) {
            throw new IllegalStateException("Priority queue is empty");
        }

        int result = positions[0];

        // Move last element to root and shrink
        size--;
        if (size > 0) {
            positions[0] = positions[size];
            fScores[0] = fScores[size];
            heapifyDown(0);
        }

        return result;
    }

    /**
     * Returns the f-score of the minimum element without removing it.
     */
    public int peekFScore() {
        if (size == 0) {
            throw new IllegalStateException("Priority queue is empty");
        }
        return fScores[0];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    /**
     * Restores heap property by moving element up the tree.
     */
    private void heapifyUp(int index) {
        int currentIndex = index;
        int currentFScore = fScores[currentIndex];
        int currentPosition = positions[currentIndex];

        while (currentIndex > 0) {
            int parentIndex = (currentIndex - 1) >>> 1; // Divide by 2
            int parentFScore = fScores[parentIndex];

            if (currentFScore >= parentFScore) {
                break;
            }

            // Swap with parent
            positions[currentIndex] = positions[parentIndex];
            fScores[currentIndex] = parentFScore;
            currentIndex = parentIndex;
        }

        positions[currentIndex] = currentPosition;
        fScores[currentIndex] = currentFScore;
    }

    /**
     * Restores heap property by moving element down the tree.
     */
    private void heapifyDown(int index) {
        int currentIndex = index;
        int currentFScore = fScores[currentIndex];
        int currentPosition = positions[currentIndex];
        int halfSize = size >>> 1; // size / 2

        while (currentIndex < halfSize) {
            int leftChild = (currentIndex << 1) + 1;  // 2 * index + 1
            int rightChild = leftChild + 1;
            int smallestChild = leftChild;

            // Find smallest child
            if (rightChild < size && fScores[rightChild] < fScores[leftChild]) {
                smallestChild = rightChild;
            }

            int smallestFScore = fScores[smallestChild];

            if (currentFScore <= smallestFScore) {
                break;
            }

            // Swap with smallest child
            positions[currentIndex] = positions[smallestChild];
            fScores[currentIndex] = smallestFScore;
            currentIndex = smallestChild;
        }

        positions[currentIndex] = currentPosition;
        fScores[currentIndex] = currentFScore;
    }

    /**
     * Clears the queue for reuse.
     */
    public void clear() {
        size = 0;
    }
}
