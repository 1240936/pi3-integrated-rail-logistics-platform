package main.domain;

import java.util.List;

/**
 *
 * @author DEI-ESINF
 */
public class Utils {
    public static <E extends Comparable<E>> Iterable<E> sortByBST(List<E> listUnsorted){
        BST<E> bst = new BST<>();
        for (E element : listUnsorted) {
            bst.insert(element);
        }
        return bst.inOrder();
    }
}