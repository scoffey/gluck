package ar.edu.itba.dcc.tp.optimizer.optimizations;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import ar.edu.itba.dcc.tp.optimizer.Block;
import ar.edu.itba.dcc.tp.optimizer.FlowDigraph;
import ar.edu.itba.dcc.tp.optimizer.Optimization;
import ar.edu.itba.dcc.tp.semantic.Instruction;
import ar.edu.itba.dcc.tp.semantic.Instruction.InstructionType;

public class ConstantPropagationOptimization implements Optimization {

	public boolean optimize(FlowDigraph fd, boolean doInterblockOptimizations) {
		// No se considera doInterblockOptimizations porque es innecesariamente
		// complicado considerar ambos casos para este algoritmo.
		TreeSet<Block> reachables = fd.getBlocksReachableBy(fd.getRoot());
		boolean hasChangedOnce = false;
		boolean thereAreChanges = true;
		while (thereAreChanges) {
			thereAreChanges = false;
			for (Block b : reachables) {
				thereAreChanges |= optimize(b);
				hasChangedOnce |= thereAreChanges;
			}
		}
		return hasChangedOnce;
	}

	private boolean optimize(Block block) {
		List<Instruction> newInstructions = new ArrayList<Instruction>();

		/* Agregar los simbolos de mis predecesores a mi tabla de definiciones */
		boolean hasChanged = block.updateConstantsDefinitions();

		for (Instruction i : block) {
			if (i.isAssignment()) {
				/* Resolver la parte derecha segun el tipo de operacion */
				if (i.getInstructionType().equals(InstructionType.ASSIGN)) {
					String op;
					op = lookupOperand(block, i.getOp1(), i.getOp2());
					newInstructions.add(new Instruction(i.getLabel(), InstructionType.ASSIGN, i.getOp1(), op, null));
				} else {
					String op1 = lookupOperand(block, null, i.getOp2());
					String op2 = lookupOperand(block, null, i.getOp3());
					Integer ret = solveOperation(i, op1, op2);

					if (ret != null) {
						block.getConstantsDefinitions().put(i.getOp1(), ret);
					}

					newInstructions.add(new Instruction(i.getLabel(), i.getInstructionType(), i.getOp1(), op1, op2));
				}
			} else {
				newInstructions.add(i);
			}

		}
		block.setInstructions(newInstructions);

		return hasChanged;
	}

	private Integer solveOperation(Instruction i, String op1, String op2) {
		int op1Value, op2Value;
		try {
			op1Value = Integer.parseInt(op1);
			op2Value = Integer.parseInt(op2);
		} catch (NumberFormatException e) {
			return null; // Si no son constantes no se puede resolver
		}

		int ret = 0;
		switch (i.getInstructionType()) {
		case ADD:
			ret = op1Value + op2Value;
			break;
		case SUB:
			ret = op1Value - op2Value;
			break;
		case MULT:
			ret = op1Value * op2Value;
			break;
		case DIV:
			ret = op1Value / op2Value;
			break;
		case MOD:
			ret = op1Value % op2Value;
			break;
		}
		return ret;
	}

	private String lookupOperand(Block b, String lvalue, String op) {
		try {
			int opValue = Integer.parseInt(op);
			if (lvalue != null) {
				/* Obtener el indice si es que existe */
				int begin = lvalue.lastIndexOf('[') + 1;
				int end = lvalue.lastIndexOf(']') - 1;
				if (begin >= 0 && end >= 0) {
					String index = lvalue.substring(begin, end);

					Integer indexProp = b.getConstantsDefinitions().get(index);
					if (indexProp != null) {
						lvalue = new String(lvalue.substring(0, begin - 1) + "[" + indexProp + "]"
								+ lvalue.substring(end + 1, lvalue.length()));
					}
				}

				b.getConstantsDefinitions().put(lvalue, opValue);
			}
			return op;
		} catch (NumberFormatException e) {
			String rvalue = op;
			Integer rvalueValue;
			if ((rvalueValue = b.getConstantsDefinitions().get(op)) != null) {
				rvalue = rvalueValue.toString();
				if (lvalue != null) {
					b.getConstantsDefinitions().put(lvalue, Integer.parseInt(rvalue));
				}
			}
			return rvalue;
		}
	}
}
