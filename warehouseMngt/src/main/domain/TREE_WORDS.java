package main.domain;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 *
 * @author DEI-ESINF
 */
public class TREE_WORDS extends BST<TextWord> {

    public void createTree() throws FileNotFoundException{
        Scanner readfile = new Scanner(new File("src/PL/xxx.xxx"));
        while(readfile.hasNextLine()){
            String[] pal = readfile.nextLine().split("(\\,)|(\\s)|(\\.)");
            for(String word : pal)
                if (word.length() > 0 )
                    insert(new TextWord(word, 1));
        }
        readfile.close();
    }

    /**
     * Inserts a new word in the tree, or increments the number of its occurrences.
     * @param element
     */
    @Override
    public void insert(TextWord element){
        root = insert(element, root);
    }

    private Node<TextWord> insert(TextWord element, Node<TextWord> node){
        if (node == null) {
            return new Node<>(element, null, null);
        }

        int cmp = element.compareTo(node.getElement());
        if (cmp < 0) {
            node.setLeft(insert(element, node.getLeft()));
        } else if (cmp > 0) {
            node.setRight(insert(element, node.getRight()));
        } else {
            // Word already exists - increment occurrences
            TextWord existingWord = node.getElement();
            existingWord.incOcorrences();
        }
        return node;
    }

    /**
     * Returns a map with a list of words for each occurrence found.
     * @return a map with a list of words for each occurrence found.
     */
    public Map<Integer,List<String>> getWordsOccurrences(){
        Map<Integer, List<String>> result = new HashMap<>();
        getWordsOccurrences(root, result);
        return result;
    }

    private void getWordsOccurrences(Node<TextWord> node, Map<Integer, List<String>> result) {
        if (node == null) {
            return;
        }

        // Process left subtree
        getWordsOccurrences(node.getLeft(), result);

        // Process current node
        TextWord word = node.getElement();
        int occurrences = word.getOcorrences();
        result.computeIfAbsent(occurrences, k -> new ArrayList<>()).add(word.getWord());

        // Process right subtree
        getWordsOccurrences(node.getRight(), result);
    }

}