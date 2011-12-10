package ar.edu.itba.dcc.tp.optimizer;

import ar.edu.itba.dcc.tp.semantic.Instruction;

public class Expression extends Instruction implements Comparable<Expression> {

	private String expression;

	public Expression(Instruction i) throws IllegalArgumentException {
		super(i.getLabel(), i.getInstructionType(), i.getOp1(), i.getOp2(), i
				.getOp3());
		String op2 = getOp2();
		String op3 = getOp3();
		switch (getInstructionType()) {
		case ADD:
			if (op2.compareTo(op3) > 0) {
				op2 = getOp3();
				op3 = getOp2();
			}
			this.expression = String.format("%s + %s", op2, op3);
			break;
		case SUB:
			this.expression = String.format("%s - %s", op2, op3);
			break;
		case MULT:
			if (op2.compareTo(op3) > 0) {
				op2 = getOp3();
				op3 = getOp2();
			}
			this.expression = String.format("%s * %s", op2, op3);
			break;
		case DIV:
			this.expression = String.format("%s / %s", op2, op3);
			break;
		case MOD:
			this.expression = String.format("%s %% %s", op2, op3);
			break;
		default:
			throw new IllegalArgumentException(
					"Instruction has no replaceable subexpression");
		}
	}

	@Override
	public String toString() {
		return expression;
	}

	public int compareTo(Expression o) {
		return expression.compareTo(o.toString());
	}
}
