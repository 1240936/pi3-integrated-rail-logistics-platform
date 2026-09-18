package main.domain;

import java.util.List;
import java.util.Map;

/**
 *
 * @author DEI-ESINF
 * @param <E>
 */
public interface BSTInterface<E> {
    public boolean isEmpty();
    public void insert(E element);
    public void remove(E element);
    public int size();
    public int height();

    public E smallestElement();
    public Iterable<E> inOrder();
    public Iterable<E> preOrder();
    public Iterable<E> posOrder();
    public Map<Integer,List<E>> nodesByLevel();

}