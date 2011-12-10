package ar.edu.itba.dcc.tp.semantic;

/**
 * Representa una isntrucción de código intermedio. Por ahora como es todo muy
 * simple no vale la pena hacer una jerarquia de instrucciones.
 */
public class Instruction {

	private String label;
	private InstructionType instructionType;
	private String op1, op2, op3;
	/*
	 * Si la instruction es call, goto o un if contiene una referencia a la
	 * instruccion a la que se resuelve el goto
	 */
	private int pointer;

	public enum InstructionType {
		ASSIGN, ADD, SUB, MULT, DIV, MOD, GOTO, IF_EQ, IF_NEQ, IF_GT, IF_GTE, IF_LT, IF_LTE, RET, PARAM, CALL;
	};

	public Instruction(String label, InstructionType instructionType, String op1, String op2, String op3) {
		super();
		this.label = label;
		this.instructionType = instructionType;
		this.op1 = op1;
		this.op2 = op2;
		this.op3 = op3;
	}

	public InstructionType getInstructionType() {
		return instructionType;
	}

	public void setInstructionType(InstructionType instructionType) {
		this.instructionType = instructionType;
	}

	public String getOp1() {
		return op1;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public void setOp1(String op1) {
		this.op1 = op1;
	}

	public String getOp2() {
		return op2;
	}

	public void setOp2(String op2) {
		this.op2 = op2;
	}

	public String getOp3() {
		return op3;
	}

	public void setOp3(String op3) {
		this.op3 = op3;
	}

	public boolean isConditionalJump() {
		switch (instructionType) {
		case IF_EQ:
		case IF_NEQ:
		case IF_GT:
		case IF_GTE:
		case IF_LT:
		case IF_LTE:
			return true;
		default:
			return false;
		}
	}

	public boolean writes(String operand) {
		switch (getInstructionType()) {
		case ASSIGN:
		case ADD:
		case SUB:
		case MULT:
		case DIV:
		case MOD:
			return op1.equals(operand);
		default:
			return false;
		}
	}

	public boolean reads(String operand) {
		switch (getInstructionType()) {
		case ASSIGN:
			return op2.equals(operand);
		case ADD:
		case SUB:
		case MULT:
		case DIV:
		case MOD:
			return op2.equals(operand) || op3.equals(operand);
		case GOTO:
		case PARAM:
			return op1.equals(operand);
		case IF_EQ:
		case IF_NEQ:
		case IF_GT:
		case IF_GTE:
		case IF_LT:
		case IF_LTE:
			return op1.equals(operand) || op2.equals(operand) || op3.equals(operand);
		case CALL:
			return op1.equals(operand) || op2.equals(operand);
		default:
			return false;
		}
	}

	public void setPointer(int pointer) {
		this.pointer = pointer;
	}

	public int getPointer() {
		return pointer;
	}

	public String getWrittenOperands() {
		switch (getInstructionType()) {
		case ASSIGN:
		case ADD:
		case SUB:
		case MULT:
		case DIV:
		case MOD:
			return op1;
		default:
			return null;
		}
	}

	public String[] getReadOperands() {
		switch (getInstructionType()) {
		case ASSIGN:
			return new String[] { op2 };
		case ADD:
		case SUB:
		case MULT:
		case DIV:
		case MOD:
			return new String[] { op2, op3 };
		case PARAM:
			return new String[] { op1 };
		case IF_EQ:
		case IF_NEQ:
		case IF_GT:
		case IF_GTE:
		case IF_LT:
		case IF_LTE:
			return new String[] { op1, op2 };
		case CALL:
			return new String[] { op2 };
		default:
			return null;
		}
	}

	public boolean isAssignment() {
		switch (getInstructionType()) {
		case ASSIGN:
		case ADD:
		case SUB:
		case MULT:
		case DIV:
		case MOD:
			return true;
		default:
			return false;
		}

	}

	public boolean isConmutative() {
		switch (getInstructionType()) {
		case ADD:
		case MULT:
			return true;
		default:
			return false;
		}
	}

	@Override
	public String toString() {
		String s = (label == null) ? "" : label + ":";
		switch (getInstructionType()) {
		case ASSIGN:
			return String.format("%-18s %-15s:= %-7s", s, op1, op2);
		case ADD:
			return String.format("%-18s %-15s:= %-7s + %-7s", s, op1, op2, op3);
		case SUB:
			return String.format("%-18s %-15s:= %-7s - %-7s", s, op1, op2, op3);
		case MULT:
			return String.format("%-18s %-15s:= %-7s * %-7s", s, op1, op2, op3);
		case DIV:
			return String.format("%-18s %-15s:= %-7s / %-7s", s, op1, op2, op3);
		case MOD:
			return String.format("%-18s %-15s:= %-7s %% %-7s", s, op1, op2, op3);
		case GOTO:
			return String.format("%-18s GOTO %-10s", s, "(" + op1 + ")");
		case IF_EQ:
			return String.format("%-18s IF %-7s = %-7s GOTO %s", s, op1, op2, "(" + op3 + ")");
		case IF_NEQ:
			return String.format("%-18s IF %-7s != %-7s GOTO %s", s, op1, op2, "(" + op3 + ")");
		case IF_GT:
			return String.format("%-18s IF %-7s > %-7s GOTO %s", s, op1, op2, "(" + op3 + ")");
		case IF_GTE:
			return String.format("%-18s IF %-7s >= %-7s GOTO %s", s, op1, op2, "(" + op3 + ")");
		case IF_LT:
			return String.format("%-18s IF %-7s < %-7s GOTO %s", s, op1, op2, "(" + op3 + ")");
		case IF_LTE:
			return String.format("%-18s IF %-7s <= %-7s GOTO %s", s, op1, op2, "(" + op3 + ")");
		case PARAM:
			return String.format("%-18s PARAM %-15s", s, op1);
		case CALL:
			return String.format("%-18s CALL %-15s, %s", s, op1, op2);
		case RET:
			return String.format("%-18s RET", s);
		default:
			return null;
		}
	}

	public boolean isCastCall() {
		return instructionType == InstructionType.CALL
				&& (getOp1().equals("_int2double") || getOp1().equals("_int2string") || getOp1().equals("_double2int")
						|| getOp1().equals("_bool2string") || getOp1().equals("_double2string"));
	}

	public boolean isDoubleMathOperationCall() {
		return instructionType == InstructionType.CALL
				&& (getOp1().equals("_add_real") || getOp1().equals("_sub_real") || getOp1().equals("_mult_real")
						|| getOp1().equals("_div_real") || getOp1().equals("_uminus_real"));
	}

}
