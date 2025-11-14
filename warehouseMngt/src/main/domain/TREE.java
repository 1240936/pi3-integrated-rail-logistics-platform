package main.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * @author DEI-ESINF
 * @param <E>
 */
public class TREE<E extends Comparable<E>> extends BST<E>{



    /*
     * @param element A valid element within the tree
     * @return the path to a given element in the tree
     */
    public List<E> path(E elem) {
        List<E> pathList = new ArrayList<>();
        path(root, elem, pathList);
        return pathList;
    }

    private boolean path(Node<E> node, E elem, List<E> pathList) {
        if (node == null) {
            return false;
        }
        pathList.add(node.getElement());
        if (elem.compareTo(node.getElement()) == 0) {
            return true;
        }
        if (elem.compareTo(node.getElement()) < 0) {
            if (path(node.getLeft(), elem, pathList)) {
                return true;
            }
        } else {
            if (path(node.getRight(), elem, pathList)) {
                return true;
            }
        }
        pathList.remove(pathList.size() - 1);
        return false;
    }

    /*
     * @return the set of the leaf node elements of the tree
     */
    public Set<E> leafs(){
        Set<E> leafSet = new HashSet<>();
        leafs(root, leafSet);
        return leafSet;
    }

    private void leafs(Node<E> node, Set<E> leafSet) {
        if (node == null) {
            return;
        }
        if (node.getLeft() == null && node.getRight() == null) {
            leafSet.add(node.getElement());
        } else {
            leafs(node.getLeft(), leafSet);
            leafs(node.getRight(), leafSet);
        }
    }

    /*
     * @return an array with the minimum and the maximum values of the tree
     */
    @SuppressWarnings("unchecked")
    public E[] range() {
        if (isEmpty()) {
            return (E[]) new Comparable[0];
        }
        E min = smallestElement();
        E max = largestElement(root);
        E[] result = (E[]) new Comparable[2];
        result[0] = min;
        result[1] = max;
        return result;
    }

    private E largestElement(Node<E> node) {
        if (node.getRight() == null) {
            return node.getElement();
        }
        return largestElement(node.getRight());
    }

    /*
     *  @return the set of elements belonging to the diameter of the BST
     */
    public Set<E> diameter(){
        Set<E> diameterSet = new HashSet<>();
        if (isEmpty()) {
            return diameterSet;
        }
        List<E> longestPath = findLongestPath(root);
        diameterSet.addAll(longestPath);
        return diameterSet;
    }

    private List<E> findLongestPath(Node<E> node) {
        if (node == null) {
            return new ArrayList<>();
        }
        List<E> leftPath = findLongestPath(node.getLeft());
        List<E> rightPath = findLongestPath(node.getRight());

        if (leftPath.size() > rightPath.size()) {
            leftPath.add(0, node.getElement());
            return leftPath;
        } else {
            rightPath.add(0, node.getElement());
            return rightPath;
        }
    }

    /*
     *  @return the previous element of the tree for a given element
     */
    public E findPredecessor (E element){
        Node<E> node = find(root, element);
        if (node == null) {
            return null;
        }
        // If node has left subtree, predecessor is the largest in left subtree
        if (node.getLeft() != null) {
            return largestElement(node.getLeft());
        }
        // Otherwise, find the first ancestor where we came from right
        E predecessor = null;
        Node<E> current = root;
        while (current != null) {
            int cmp = element.compareTo(current.getElement());
            if (cmp < 0) {
                current = current.getLeft();
            } else if (cmp > 0) {
                predecessor = current.getElement();
                current = current.getRight();
            } else {
                break;
            }
        }
        return predecessor;
    }

    /*
     * – verify if the current and tree BST are identical.
     */
    public boolean identical(BST<E> tree){
        return identical(root, tree.root());
    }

    private boolean identical(Node<E> node1, Node<E> node2) {
        if (node1 == null && node2 == null) {
            return true;
        }
        if (node1 == null || node2 == null) {
            return false;
        }
        if (node1.getElement().compareTo(node2.getElement()) != 0) {
            return false;
        }
        return identical(node1.getLeft(), node2.getLeft())
                && identical(node1.getRight(), node2.getRight());
    }

    /*
     * – remove all elements in the current BST that are outside the range [low, high]
     */
    public void truncate(E low, E high){
        root = truncate(root, low, high);
    }

    private Node<E> truncate(Node<E> node, E low, E high) {
        if (node == null) {
            return null;
        }
        E element = node.getElement();
        if (element.compareTo(low) < 0) {
            // Element is less than low, remove it and its left subtree
            return truncate(node.getRight(), low, high);
        }
        if (element.compareTo(high) > 0) {
            // Element is greater than high, remove it and its right subtree
            return truncate(node.getLeft(), low, high);
        }
        // Element is in range, keep it and recursively truncate children
        node.setLeft(truncate(node.getLeft(), low, high));
        node.setRight(truncate(node.getRight(), low, high));
        return node;
    }

    /*
     *– return true if BST<E> tree is a sub tree of the BST<E>.
     */
    public boolean isSubTree(BST<E> tree){
        if (tree.isEmpty()) {
            return true;
        }
        Node<E> subtreeRoot = find(root, tree.root().getElement());
        if (subtreeRoot == null) {
            return false;
        }
        return isSubTree(subtreeRoot, tree.root());
    }

    private boolean isSubTree(Node<E> node1, Node<E> node2) {
        if (node2 == null) {
            return true;
        }
        if (node1 == null) {
            return false;
        }
        if (node1.getElement().compareTo(node2.getElement()) != 0) {
            return false;
        }
        return isSubTree(node1.getLeft(), node2.getLeft())
                && isSubTree(node1.getRight(), node2.getRight());
    }

    public boolean isSymmetric(){
        return isSymmetric(root);
    }

    private boolean isSymmetric(Node<E> node) {
        if (node == null) {
            return true;
        }
        return isMirror(node.getLeft(), node.getRight());
    }

    private boolean isMirror(Node<E> left, Node<E> right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.getElement().compareTo(right.getElement()) == 0
                && isMirror(left.getLeft(), right.getRight())
                && isMirror(left.getRight(), right.getLeft());
    }

    public TREE<E> minimumSubtree (Set<E> elems ) {
        TREE<E> result = new TREE<>();
        if (elems.isEmpty()) {
            return result;
        }
        List<E> sortedElems = new ArrayList<>(elems);
        sortedElems.sort(null);
        result.root = buildMinimumSubtree(sortedElems, 0, sortedElems.size() - 1);
        return result;
    }

    private Node<E> buildMinimumSubtree(List<E> elems, int start, int end) {
        if (start > end) {
            return null;
        }
        int mid = (start + end) / 2;
        Node<E> node = new Node<>(elems.get(mid), null, null);
        node.setLeft(buildMinimumSubtree(elems, start, mid - 1));
        node.setRight(buildMinimumSubtree(elems, mid + 1, end));
        return node;
    }

}