package ar.edu.itba.dcc.tp.optimizer;

public interface Optimization {

	public boolean optimize(FlowDigraph bd, boolean doInterblockOptimizations);

}
