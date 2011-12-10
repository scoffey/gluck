package ar.edu.itba.dcc.tp.semantic.nodes;

import java.util.ArrayList;
import java.util.List;

public class Sentence {
    private List<Integer> nextList = new ArrayList<Integer>();
    private List<Integer> quitList = new ArrayList<Integer>();

    public List<Integer> getNextList() {
		return nextList;
	}
    
    public List<Integer> getQuitList() {
		return quitList;
	}
}
