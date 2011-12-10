package ar.edu.itba.dcc.tp.optimizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ar.edu.itba.dcc.tp.semantic.Instruction;
import static ar.edu.itba.dcc.tp.semantic.Instruction.InstructionType;

public class InstructionLoader {

	public ArrayList<Instruction> load(InputStream stream) {
		return load(new InputStreamReader(stream));
	}
	
	public ArrayList<Instruction> load(File f) throws FileNotFoundException {
		return load(new FileReader(f));
	}
	
	public ArrayList<Instruction> load(Reader input) {
		ArrayList<Instruction> instructions = new ArrayList<Instruction>();
		try {
			BufferedReader br = new BufferedReader(input);
			String line;
			Pattern p = Pattern.compile("([^:\\s]+)?:?\\s+(.*)");
			while ((line = br.readLine()) != null) {
				Matcher m = p.matcher(line);
				if (m.find()) {
					String label = m.group(1);
					Instruction i = getInstruction(m.group(2).split(":="));
					if (i != null) {
						if (label != null)
							i.setLabel(label.trim());
						instructions.add(i);
					} else {
						System.err.println("Unknown instruction: " + line);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return instructions;
	}

	private Instruction getInstruction(String[] chunks) {
		if (chunks.length == 2) {
			return getAssignmentInstruction(chunks[0].trim(), chunks[1].trim());
		}
		Instruction i = null;
		String s = chunks[0].trim();
		if (s.startsWith("IF ")) {
			i = getConditionalJumpInstruction(s.substring(3));
		} else if (s.startsWith("GOTO ")) {
			i = new Instruction(null, InstructionType.GOTO, s.substring(5)
					.trim(), null, null);
		} else if (s.startsWith("PARAM ")) {
			i = new Instruction(null, InstructionType.PARAM, s.substring(6)
					.trim(), null, null);
		} else if (s.startsWith("CALL ")) {
			String[] args = s.substring(5).split(",");
			if (args.length != 2)
				return null;
			i = new Instruction(null, InstructionType.CALL, args[0].trim(),
					args[1].trim(), null);
		} else if (s.equals("RET")) {
			i = new Instruction(null, InstructionType.RET, null, null, null);
		}
		return i;
	}

	private Instruction getAssignmentInstruction(String lvalue,
			String expression) {
		Pattern p = Pattern.compile("(\\S+)\\s*([-+*/%])\\s*(\\S+)");
		Matcher m = p.matcher(expression);
		if (!m.find()) {
			return new Instruction(null, InstructionType.ASSIGN, lvalue,
					expression, null);
		}
		String op2 = m.group(1);
		String operator = m.group(2);
		String op3 = m.group(3);
		Instruction.InstructionType type = null;
		switch (operator.charAt(0)) {
		case '+':
			type = InstructionType.ADD;
			break;
		case '-':
			type = InstructionType.SUB;
			break;
		case '*':
			type = InstructionType.MULT;
			break;
		case '/':
			type = InstructionType.DIV;
			break;
		case '%':
			type = InstructionType.MOD;
			break;
		default:
			return null;
		}
		return new Instruction(null, type, lvalue, op2, op3);
	}

	private Instruction getConditionalJumpInstruction(String s) {
		String[] chunks = s.split("GOTO ");
		if (chunks.length != 2) {
			return null;
		}
		String op3 = chunks[1].trim();
		Pattern p = Pattern.compile("(\\S+)\\s*(=|!=|<=|<|>=|>)\\s*(\\S+)");
		Matcher m = p.matcher(chunks[0].trim());
		if (!m.find()) {
			return null;
		}
		String op1 = m.group(1);
		String comparator = m.group(2);
		String op2 = m.group(3);
		Instruction.InstructionType type = null;
		switch (comparator.charAt(0)) {
		case '=':
			type = InstructionType.IF_EQ;
			break;
		case '!':
			type = InstructionType.IF_NEQ;
			break;
		case '<':
			type = (comparator.length() == 1) ? InstructionType.IF_LT
					: InstructionType.IF_LTE;
			break;
		case '>':
			type = (comparator.length() == 1) ? InstructionType.IF_GT
					: InstructionType.IF_GTE;
			break;
		default:
			return null;
		}
		return new Instruction(null, type, op1, op2, op3);
	}

}
