package main.domain;

/**
 *
 * @author DEI-ESINF
 * @param <E>
 */
public class AVL <E extends Comparable<E>> extends BST<E> {

    private int balanceFactor(Node<E> node){
        if (node == null) {
            return 0;
        }
        return height(node.getRight()) - height(node.getLeft());
    }

    private Node<E> rightRotation(Node<E> node){
        Node<E> leftChild = node.getLeft();
        node.setLeft(leftChild.getRight());
        leftChild.setRight(node);
        return leftChild;
    }

    private Node<E> leftRotation(Node<E> node){
        Node<E> rightChild = node.getRight();
        node.setRight(rightChild.getLeft());
        rightChild.setLeft(node);
        return rightChild;
    }

    private Node<E> twoRotations(Node<E> node){
        int balance = balanceFactor(node);
        if (balance < -1) {
            // Left-heavy: check if left child is right-heavy (LR case)
            if (balanceFactor(node.getLeft()) > 0) {
                // LR case: left rotation on left child, then right rotation on node
                node.setLeft(leftRotation(node.getLeft()));
            }
            // LL case: just right rotation
            return rightRotation(node);
        } else {
            // Right-heavy: check if right child is left-heavy (RL case)
            if (balanceFactor(node.getRight()) < 0) {
                // RL case: right rotation on right child, then left rotation on node
                node.setRight(rightRotation(node.getRight()));
            }
            // RR case: just left rotation
            return leftRotation(node);
        }
    }

    private Node<E> balanceNode(Node<E> node)
    {
        if (node == null) {
            return null;
        }
        int balance = balanceFactor(node);
        if (balance < -1 || balance > 1) {
            return twoRotations(node);
        }
        return node;
    }

    @Override
    public void insert(E element){
        root = insert(element, root);
    }

    private Node<E> insert(E element, Node<E> node){
        if (node == null) {
            return new Node<>(element, null, null);
        }
        int cmp = element.compareTo(node.getElement());
        if (cmp < 0) {
            node.setLeft(insert(element, node.getLeft()));
        } else if (cmp > 0) {
            node.setRight(insert(element, node.getRight()));
        }
        // If element already exists, do nothing
        // Balance the node after insertion
        return balanceNode(node);
    }

    @Override
    public void remove(E element){
        root = remove(element, root());
    }

    private Node<E> remove(E element, Node<E> node) {
        if (node == null) {
            return null;
        }
        if (element.compareTo(node.getElement())==0) { // node is the Node to be removed

            if (node.getLeft() == null && node.getRight() == null) { //node is a leaf (has no childs)
                return null;
            }
            if (node.getLeft() == null) {   //has only right child
                return node.getRight();
            }
            if (node.getRight() == null) {  //has only left child
                return node.getLeft();
            }
            //has two child trees
            //replace the elem with the smallest of right subtree and remove it
            E smallElem = smallestElement(node.getRight());
            node.setElement(smallElem);
            node.setRight( remove(smallElem, node.getRight()) );
            node = balanceNode(node);
        }
        else if (element.compareTo(node.getElement()) < 0) {
            node.setLeft( remove(element, node.getLeft()) );
            node = balanceNode(node);
        }
        else {
            node.setRight( remove(element, node.getRight()) );
            node = balanceNode(node);
        }
        return node;
    }

    @SuppressWarnings("unchecked")
    public boolean equals(Object otherObj) {
        if (this == otherObj)
            return true;
        if (otherObj == null || this.getClass() != otherObj.getClass())
            return false;
        AVL<E> second = (AVL<E>) otherObj;
        return equals(root, second.root);
    }

    public boolean equals(Node<E> root1, Node<E> root2) {
        if (root1 == null && root2 == null)
            return true;
        else if (root1 != null && root2 != null) {
            if (root1.getElement().compareTo(root2.getElement()) == 0) {
                return equals(root1.getLeft(), root2.getLeft())
                        && equals(root1.getRight(), root2.getRight());
            } else
                return false;
        }
        else return false;
    }

    // Explicitly delegate to parent class methods to help IDE recognition
    @Override
    public int size() {
        return super.size();
    }

    @Override
    public Iterable<E> inOrder() {
        return super.inOrder();
    }

    @Override
    public int height() {
        return super.height();
    }

    @Override
    public E smallestElement() {
        return super.smallestElement();
    }

}