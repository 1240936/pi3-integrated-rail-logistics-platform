package main.domain;

/**
 * Node structure for AVL Tree.
 * Similar to BSTNode, but adds balance factor tracking.
 *
 * The balance factor (BF) is calculated as:
 * BF = height(right subtree) - height(left subtree)
 *
 * Valid values: BF ∈ [-1, 1]
 * - BF = -1: slightly heavy to the left (balanced)
 * - BF = 0: perfectly balanced
 * - BF = +1: slightly heavy to the right (balanced)
 * - BF < -1 or BF > 1: unbalanced (needs rotation)
 *
 * @param <E> type of element stored in the node
 */
public class AVLNode<E> {
    private E element;
    private AVLNode<E> left;
    private AVLNode<E> right;
    private int balanceFactor; // height(right) - height(left)

    /**
     * Constructs an AVL node with the given element and no children.
     * Balance factor is initialized to 0.
     *
     * @param element the element to store
     */
    public AVLNode(E element) {
        this.element = element;
        this.left = null;
        this.right = null;
        this.balanceFactor = 0;
    }

    public E getElement() {
        return element;
    }

    public void setElement(E element) {
        this.element = element;
    }

    public AVLNode<E> getLeft() {
        return left;
    }

    public void setLeft(AVLNode<E> left) {
        this.left = left;
    }

    public AVLNode<E> getRight() {
        return right;
    }

    public void setRight(AVLNode<E> right) {
        this.right = right;
    }

    public int getBalanceFactor() {
        return balanceFactor;
    }

    public void setBalanceFactor(int balanceFactor) {
        this.balanceFactor = balanceFactor;
    }
}
