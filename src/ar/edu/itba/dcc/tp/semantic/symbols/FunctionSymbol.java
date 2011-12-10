package ar.edu.itba.dcc.tp.semantic.symbols;

import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.dcc.tp.semantic.SymbolTable;

/**
 * Símbolo de tipo función. Contiene el nombre, la lista de parámetros formales
 * de entrada y de salida. Se puede instanciar un símbolo de este tipo en dos
 * situaciones posibles: al parsear una definición de función (en cuyo caso el
 * flag defined se setea en true) o bien al usar la función por primera vez,
 * antes de que sea definida. En ese caso se crea con la lista de parámetros que
 * se piensa que va a tener cuando sea definida, y se setea el flag de defined
 * en false. Cuando se llegue entonces a la definición, se realizan todos los
 * chequeos correspondientes y se formaliza la definición.
 */
public class FunctionSymbol extends Symbol {

	private List<VariableSymbol> inputParams, outputParams;
	private boolean isFunction;
	private SymbolTable symbolTable;
	private boolean isNative;
	private boolean isGetterOrSetter;

	/* Archivo donde fue definido */
	private String sourceFile;

	public FunctionSymbol(String id, List<VariableSymbol> inputParams, boolean isFunction, String sourceFile) {
		this(id, inputParams, new ArrayList<VariableSymbol>(), isFunction, sourceFile);
	}

	public FunctionSymbol(String id, List<VariableSymbol> inputParams, List<VariableSymbol> outputParams,
			boolean isFunction, String sourceFile) {
		this(id, inputParams, outputParams, isFunction, sourceFile, false);
	}

	public FunctionSymbol(String id, List<VariableSymbol> inputParams, List<VariableSymbol> outputParams,
			boolean isFunction, String sourceFile, boolean isNative) {
		super(id);
		this.id = id;
		this.inputParams = inputParams;
		this.outputParams = outputParams;
		this.isFunction = isFunction;
		this.symbolTable = new SymbolTable();
		this.sourceFile = sourceFile.lastIndexOf('.') != -1 ? sourceFile.substring(0, sourceFile.lastIndexOf('.'))
				: sourceFile;
		this.isNative = isNative;
	}

	public FunctionSymbol(String s, String sourceFile, boolean isNative) {
		super("");
		this.isNative = isNative;
		this.sourceFile = sourceFile;
		this.inputParams = new ArrayList<VariableSymbol>();
		this.outputParams = new ArrayList<VariableSymbol>();
		this.isFunction = true; // SUPER TODO;

		StringBuffer sb = new StringBuffer(s);
		int state = 0;
		while (sb.length() > 0) {
			switch (state) {
			case 0:
				this.id = sb.substring(0, sb.lastIndexOf("("));
				sb.delete(0, sb.lastIndexOf("("));
				state = 1;
				break;
			case 1:
				if (sb.charAt(0) == '(') {
					state = 2;
				} else {
					state = -1;
				}
				sb.deleteCharAt(0);
				break;
			case 2:
				if (sb.charAt(0) == ')') {
					state = 3;
					sb.deleteCharAt(0);
				} else {
					inputParams.add(new VariableSymbol("TODO", TypeSymbol.parseType(sb),
							new ArrayList<FunctionSymbol>(), false));
				}
				break;
			case 3:
				if (sb.charAt(0) != 'V') {
					outputParams.add(new VariableSymbol("TODO", TypeSymbol.parseType(sb),
							new ArrayList<FunctionSymbol>(), false));
				} else {
					sb.deleteCharAt(0);
				}
				break;
			default:
				System.out.println("ERROR");
			}

		}
	}

	public SymbolTable getSymbolTable() {
		return symbolTable;
	}

	public List<VariableSymbol> getInputParams() {
		return inputParams;
	}

	public void setInputParams(List<VariableSymbol> inputParams) {
		this.inputParams = inputParams;
	}

	public List<VariableSymbol> getOutputParams() {
		return outputParams;
	}

	public void setOutputParams(List<VariableSymbol> outputParams) {
		this.outputParams = outputParams;
	}

	public boolean isFunction() {
		return isFunction;
	}

	@Override
	public void print(int margin) {
		System.out.printf("%-20s%-20s\n", isFunction ? "fn" : "cons", id);
		symbolTable.print(1);
	}

	public String jasminType(String packageName, String className) {
		StringBuffer ret = new StringBuffer("(");

		for (VariableSymbol vs : inputParams) {
			ret.append(vs.getType().jasminType(packageName, className));
		}

		ret.append(")");
		if (outputParams.size() == 0) {
			ret.append("V");
		} else if (outputParams.size() == 1 && this.isNative()) {
			ret.append(outputParams.get(0).getType().jasminType(packageName, className));
		} else {
			ret.append("L" + /* packageName + className + */sourceFile + "$" + id + ";");
		}

		return ret.toString();
	}

	public String getSourceFile() {
		return sourceFile;
	}

	public boolean isNative() {
		return isNative;
	}

	@Override
	public String toString() {
		return id;
	}

	public boolean hasOutputParams() {
		return outputParams.size() > 0;
	}

	public void setIsGetterOrSetter(boolean val) {
		isGetterOrSetter = val;
	}

	public boolean isGetterOrSetter() {
		return isGetterOrSetter;
	}
}
