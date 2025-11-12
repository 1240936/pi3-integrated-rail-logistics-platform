package main.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * AVL Tree implementation with automatic balancing.
 *
 * IMPORTANT: AVL is a BST (Binary Search Tree)!
 * - Every AVL tree is a BST, but not every BST is an AVL
 * - AVL = BST + balancing property
 * - The BST property (ordering) is ALWAYS maintained, even after rotations
 * - Rotations are designed to preserve the BST property
 *
 * Properties:
 * - Maintains balance factor in [-1, 1] for all nodes
 * - Guaranteed height: O(log n)
 * - All operations maintain the BST property
 *
 * In this project we keep only the AVL variant, as it delivers the balanced behaviour needed in production.
 *
 * @param <E> type of elements stored in the tree
 */
public class AVLTree<E> {
    private AVLNode<E> root;
    private final Comparator<E> comparator;

    /**
     * Constructs an empty AVL tree with the given comparator.
     *
     * @param comparator comparator to order elements
     */
    public AVLTree(Comparator<E> comparator) {
        this.root = null;
        this.comparator = comparator;
    }

    /**
     * Inserts an element into the AVL tree and rebalances if necessary.
     * Duplicates are not allowed.
     *
     * @param elem element to insert
     * @return true if inserted, false if duplicate
     */
    public boolean insert(E elem) {
        if (elem == null) {
            return false;
        }
        AVLNode<E> newRoot = insert(root, elem);
        if (newRoot == root && root != null && comparator.compare(elem, root.getElement()) == 0) {
            return false; // duplicate
        }
        root = newRoot;
        return true;
    }

    /**
     * Recursive insertion with automatic balancing.
     *
     *
     * Process:
     * 1. Inserts as a normal BST (maintains ordering property)
     *    - During this step, the tree may temporarily become unbalanced
     * 2. Checks and corrects balance using rotations
     *    - Rotations preserve BST property
     *    - Rotations restore AVL property (balancing)
     *
     * GUARANTEE: After this method returns, the tree is:
     * - Valid BST (ordering maintained)
     * - Balanced AVL (balance factor in [-1, 1])
     *
     * Rotations are designed to maintain the order: left < node < right
     *
     * @param node current node
     * @param elem element to insert
     * @return root of the balanced subtree (maintains BST and AVL properties)
     */
    private AVLNode<E> insert(AVLNode<E> node, E elem) {
        if (node == null) {
            return new AVLNode<>(elem);
        }

        // STEP 1: Insert as BST (maintains ordering property)
        // NOTE: At this point, the tree may temporarily become unbalanced
        int cmp = comparator.compare(elem, node.getElement());
        if (cmp < 0) {
            // Smaller element → insert to the left (BST property maintained)
            node.setLeft(insert(node.getLeft(), elem));
        } else if (cmp > 0) {
            // Larger element → insert to the right (BST property maintained)
            node.setRight(insert(node.getRight(), elem));
        } else {
            // Duplicate - return unchanged node
            return node;
        }

        // STEP 2: Check and correct balance
        // If insertion caused imbalance, apply rotations to correct
        // Rotations preserve BST property and restore AVL property
        updateBalanceFactor(node);  // Calculate balance factor
        return balanceNode(node);   // Apply rotations if necessary (maintains BST, restores AVL)
    }

    /**
     * Removes an element from the AVL tree and rebalances if necessary.
     *
     * @param elem element to remove
     * @return true if removed, false if not found
     */
    public boolean remove(E elem) {
        if (elem == null) {
            return false;
        }
        boolean[] found = {false};
        AVLNode<E> newRoot = remove(elem, root, found);
        root = newRoot;
        return found[0];
    }

    /**
     * Recursive removal with automatic balancing.
     * Handles the same three cases as BST, but balances after removal.
     *
     * @param elem element to remove
     * @param node current node
     * @param found flag to track if element was found
     * @return the (possibly new) root of the balanced subtree
     */
    private AVLNode<E> remove(E elem, AVLNode<E> node, boolean[] found) {
        if (node == null) {
            return null;
        }

        int cmp = comparator.compare(elem, node.getElement());
        if (cmp < 0) {
            node.setLeft(remove(elem, node.getLeft(), found));
        } else if (cmp > 0) {
            node.setRight(remove(elem, node.getRight(), found));
        } else {
            // Found the node to remove
            found[0] = true;
            if (node.getLeft() == null) {
                return node.getRight(); // Case 1 or 2: leaf or only right child
            } else if (node.getRight() == null) {
                return node.getLeft(); // Case 2: only left child
            } else {
                // Case 3: two children
                E min = smallestElement(node.getRight());
                node.setElement(min);
                node.setRight(remove(min, node.getRight(), found));
            }
        }

        // Update balance factor and balance if necessary
        updateBalanceFactor(node);
        return balanceNode(node);
    }

    /**
     * Finds the smallest element in the subtree rooted at node.
     *
     * @param node root of subtree
     * @return smallest element
     */
    private E smallestElement(AVLNode<E> node) {
        while (node.getLeft() != null) {
            node = node.getLeft();
        }
        return node.getElement();
    }

    /**
     * Updates the balance factor of a node based on the heights of its children.
     *
     * Formula: BF = height(right subtree) - height(left subtree)
     *
     * This method is called after each insertion/removal to check
     * if the node is balanced. If BF is outside [-1, 1], applies rotations.
     *
     * @param node node to update
     */
    private void updateBalanceFactor(AVLNode<E> node) {
        int leftHeight = height(node.getLeft());    // Height of left subtree
        int rightHeight = height(node.getRight());  // Height of right subtree
        node.setBalanceFactor(rightHeight - leftHeight);  // BF = right - left
    }

    /**
     * Calculates the height of a node.
     *
     * @param node node to measure
     * @return height (-1 if null)
     */
    private int height(AVLNode<E> node) {
        if (node == null) {
            return -1;
        }
        return 1 + Math.max(height(node.getLeft()), height(node.getRight()));
    }

    /**
     * Balances a node if the balance factor is outside [-1, 1].
     * Applies rotations as needed.
     *
     * IMPORTANT:
     * - If the node is unbalanced (BF < -1 or BF > 1), applies rotations
     * - Rotations preserve the BST property (ordering)
     * - Rotations restore the AVL property (balancing)
     * - If already balanced, returns without changes
     *
     * GUARANTEE: After this method returns, the subtree is balanced.
     *
     * @param node node to balance
     * @return root of the balanced subtree (maintains BST, restores AVL)
     */
    private AVLNode<E> balanceNode(AVLNode<E> node) {
        if (node == null) {
            return null;
        }

        updateBalanceFactor(node);
        int bf = node.getBalanceFactor();

        // Unbalanced to the left: balance factor < -1
        if (bf < -1) {
            AVLNode<E> leftChild = node.getLeft();
            updateBalanceFactor(leftChild);
            if (leftChild.getBalanceFactor() <= 0) {
                // Left-Left case: simple right rotation
                return rightRotation(node);
            } else {
                // Left-Right case: double rotation (left-right)
                return leftRightRotation(node);
            }
        }
        // Unbalanced to the right: balance factor > 1
        else if (bf > 1) {
            AVLNode<E> rightChild = node.getRight();
            updateBalanceFactor(rightChild);
            if (rightChild.getBalanceFactor() >= 0) {
                // Right-Right case: simple left rotation
                return leftRotation(node);
            } else {
                // Right-Left case: double rotation (right-left)
                return rightLeftRotation(node);
            }
        }

        // Already balanced (BF in [-1, 1]), return without changes
        return node;
    }

    /**
     * Simple right rotation.
     * Used when the node is unbalanced to the left.
     *
     * The rotation only reorganizes nodes, maintaining order:
     * - All elements to the left of leftson remain to the left
     * - All elements to the right of node remain to the right
     * - leftson.getRight() stays between leftson and node (order preserved)
     *
     * @param node node to rotate
     * @return new root after rotation (maintains BST property)
     */
    private AVLNode<E> rightRotation(AVLNode<E> node) {
        AVLNode<E> leftson = node.getLeft();

        // Move right subtree of left child to left of node
        // Preserves order: everything in leftson.getRight() is between leftson and node
        node.setLeft(leftson.getRight());

        // Move node to right of left child
        // Preserves order: node > leftson
        leftson.setRight(node);

        // Update balance factors
        updateBalanceFactor(node);
        updateBalanceFactor(leftson);

        return leftson;  // New root (BST property preserved)
    }

    /**
     * Simple left rotation.
     * Used when the node is unbalanced to the right.
     *
     * The rotation only reorganizes nodes, maintaining order:
     * - All elements to the left of node remain to the left
     * - All elements to the right of rightson remain to the right
     * - rightson.getLeft() stays between node and rightson (order preserved)
     *
     * @param node node to rotate
     * @return new root after rotation (maintains BST property)
     */
    private AVLNode<E> leftRotation(AVLNode<E> node) {
        AVLNode<E> rightson = node.getRight();

        // Move left subtree of right child to right of node
        // Preserves order: everything in rightson.getLeft() is between node and rightson
        node.setRight(rightson.getLeft());

        // Move node to left of right child
        // Preserves order: node < rightson
        rightson.setLeft(node);

        // Update balance factors
        updateBalanceFactor(node);
        updateBalanceFactor(rightson);

        return rightson;  // New root (BST property preserved)
    }

    /**
     * Double rotation: Left-Right.
     * First rotates left child to the left, then rotates node to the right.
     *
     * @param node node to rotate
     * @return new root after rotations
     */
    private AVLNode<E> leftRightRotation(AVLNode<E> node) {
        node.setLeft(leftRotation(node.getLeft()));
        return rightRotation(node);
    }

    /**
     * Double rotation: Right-Left.
     * First rotates right child to the right, then rotates node to the left.
     *
     * @param node node to rotate
     * @return new root after rotations
     */
    private AVLNode<E> rightLeftRotation(AVLNode<E> node) {
        node.setRight(rightRotation(node.getRight()));
        return leftRotation(node);
    }

    /**
     * Checks if an element exists in the AVL tree.
     *
     * @param elem element to search for
     * @return true if found
     */
    public boolean contains(E elem) {
        return contains(root, elem);
    }

    private boolean contains(AVLNode<E> node, E elem) {
        if (node == null) {
            return false;
        }
        int cmp = comparator.compare(elem, node.getElement());
        if (cmp < 0) {
            return contains(node.getLeft(), elem);
        } else if (cmp > 0) {
            return contains(node.getRight(), elem);
        } else {
            return true;
        }
    }

    /**
     * Returns the root node of the tree.
     *
     * @return root node
     */
    public AVLNode<E> getRoot() {
        return root;
    }

    /**
     * Returns the size of the tree (number of nodes).
     *
     * @return size
     */
    public int size() {
        return size(root);
    }

    private int size(AVLNode<E> node) {
        if (node == null) {
            return 0;
        }
        return 1 + size(node.getLeft()) + size(node.getRight());
    }

    /**
     * Returns the height of the tree.
     *
     * @return height
     */
    public int height() {
        return height(root);
    }

    /**
     * Returns all elements in in-order traversal.
     *
     * @return list of elements in sorted order
     */
    public List<E> inOrder() {
        List<E> result = new ArrayList<>();
        inOrder(root, result);
        return result;
    }

    private void inOrder(AVLNode<E> node, List<E> result) {
        if (node != null) {
            inOrder(node.getLeft(), result);
            result.add(node.getElement());
            inOrder(node.getRight(), result);
        }
    }

    /**
     * Checks if the tree is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return root == null;
    }

    /**
     * Finds the k-th smallest element in the AVL tree (0-indexed).
     * The AVL tree guarantees O(log n) height, maintaining efficiency.
     *
     * @param k index of the element (0-indexed)
     * @return the k-th smallest element, or null if k is out of bounds
     */
    public E findKthElement(int k) {
        if (k < 0 || k >= size()) {
            return null;
        }
        int[] count = {0};
        return findKthElement(root, k, count);
    }

    /**
     * Recursive helper method to find the k-th element.
     * Traverses in order: first left, then current node, then right.
     */
    private E findKthElement(AVLNode<E> node, int k, int[] count) {
        if (node == null) {
            return null;
        }

        // First search in left subtree
        E leftResult = findKthElement(node.getLeft(), k, count);
        if (leftResult != null) {
            return leftResult;
        }

        // Check if current node is the k-th element
        if (count[0] == k) {
            return node.getElement();
        }
        count[0]++;

        // If not found, search in right subtree
        return findKthElement(node.getRight(), k, count);
    }


}