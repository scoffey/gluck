package ar.edu.itba.dcc.tp.optimizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ar.edu.itba.dcc.tp.optimizer.optimizations.CommonSubexpressionOptimization;
import ar.edu.itba.dcc.tp.optimizer.optimizations.ConstantPropagationOptimization;
import ar.edu.itba.dcc.tp.semantic.Instruction;
import ar.edu.itba.dcc.tp.semantic.Instruction.InstructionType;

public class Optimizer {

	private ArrayList<Optimization> optimizations;

	private boolean doInterblockOptimizations;

	public Optimizer(Set<String> options) {
		optimizations = new ArrayList<Optimization>();
		doInterblockOptimizations = (options == null || options.contains("fax"));
		if (options == null || options.contains("constant")) {
			optimizations.add(new ConstantPropagationOptimization());
		}
		if (options == null || options.contains("cse")) {
			optimizations.add(new CommonSubexpressionOptimization());
		}
		// // Esta puede salir en la misma optimización intra-bloque por DAG
		// if (options == null || options.contains("expr")) {
		// optimizations.add(new SimplificationOptimization());
		// }
		// if (options == null || options.contains("assign")) {
		// optimizations.add(new AssignmentOptimization());
		// }
		// if (options == null || options.contains("dead")) {
		// optimizations.add(new DeadCodeOptimization());
		// }
		// // Esta se puede hacer en la generación de código intermedio
		// if (options == null || options.contains("loop")) {
		// optimizations.add(new LoopUnrollingOptimization());
		// }
		// if (options == null || options.contains("inline")) {
		// optimizations.add(new InliningOptimization());
		// }
	}

	public void optimize(List<Instruction> instructions) {
		setLabelPointers(instructions);
		FlowDigraph fd = new FlowDigraph(instructions);
		//System.out.println(fd);
		boolean thereAreChanges = true;
		while (thereAreChanges) {
			thereAreChanges = false;
			for (Optimization o : optimizations) {
				// thereAreChanges |= o.optimize(fd, doInterblockOptimizations);
				o.optimize(fd, doInterblockOptimizations);
			}
		}
		for (Block b : fd) {
			for (Instruction i : b) {
				System.out.println(i.toString());
			}
		}
	}

	public int size() {
		return optimizations.size();
	}

	private void setLabelPointers(List<Instruction> instructions) {
		HashMap<String, Integer> labels = new HashMap<String, Integer>();
		int lineNumber = 0;
		for (Instruction i : instructions) {
			if (i.getLabel() != null) {
				labels.put(i.getLabel(), lineNumber);
			}
			lineNumber++;
		}
		for (Instruction i : instructions) {
			InstructionType t = i.getInstructionType();
			String label = null;
			if (i.isConditionalJump()) {
				label = i.getOp3();
			} else if (t.equals(InstructionType.GOTO)
					|| t.equals(InstructionType.CALL)) {
				label = i.getOp1();
			}
			if (label != null) {
				Integer pointer = labels.get(label);
				// pointer puede ser null para labels externos
				i.setPointer(pointer == null ? -1 : pointer.intValue());
			}
		}
	}
	
	public static void main(String[] args) {
		List<Instruction> instructions = new ArrayList<Instruction>();
		
		instructions.add(new Instruction("0", InstructionType.ASSIGN, "#a", "0", ""));
		instructions.add(new Instruction("1", InstructionType.IF_EQ, "2", "3", "4"));
		instructions.add(new Instruction("2", InstructionType.ASSIGN, "#b", "#a", ""));
		instructions.add(new Instruction("3", InstructionType.GOTO, "1", "", ""));
		instructions.add(new Instruction("4", InstructionType.ASSIGN, "#c", "1", ""));
		
		Optimizer optimizer = new Optimizer(new HashSet<String>());
		
		optimizer.optimizations.add(new ConstantPropagationOptimization());
		optimizer.optimize(instructions);
		
	}
}
