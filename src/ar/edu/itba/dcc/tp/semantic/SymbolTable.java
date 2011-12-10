package ar.edu.itba.dcc.tp.semantic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.dcc.tp.assembler.ClassInfoCollector;
import ar.edu.itba.dcc.tp.run.Compiler;
import ar.edu.itba.dcc.tp.semantic.symbols.BoolTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.DoubleTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.FunctionSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.IntTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.StringTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.Symbol;
import ar.edu.itba.dcc.tp.semantic.symbols.TypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.UserDefinedTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.VariableSymbol;

/**
 * Tabla de símbolos. Se maneja como un conjunto de símbolos de un único nivel
 * (contexto). Cuando se agrega una función, se crea una nueva tabla de símbolos
 * dentro de la función, y se setea la variable currentFunction en la tabla
 * actual para saber a donde buscar.
 */
public class SymbolTable {
	private ArrayList<ArrayList<Symbol>> currentList;
	private ArrayList<ArrayList<Symbol>> symbols;
	private int depth;
	private FunctionSymbol currentFunction;

	/**
	 * Crea una nueva tabla de símbolos, de 1 nivel de profundidad (el contexto
	 * global).
	 */
	public SymbolTable() {
		this.symbols = new ArrayList<ArrayList<Symbol>>();
		this.symbols.add(new ArrayList<Symbol>());
		this.depth = 0;

		this.symbols.get(0).add(IntTypeSymbol.getInstance());
		this.symbols.get(0).add(DoubleTypeSymbol.getInstance());
		this.symbols.get(0).add(new StringTypeSymbol());
		this.symbols.get(0).add(BoolTypeSymbol.getInstance());
		this.currentList = new ArrayList<ArrayList<Symbol>>();
		this.currentList.add(new ArrayList<Symbol>());
	}

	public void loadStandardLibrary() throws IOException {
		/* Agregar los simbolos de nuestra biblioteca */
		if (Compiler.getInstance().getMode() == Compiler.CompilerModes.MODE_BYTECODES
				|| Compiler.getInstance().getMode() == Compiler.CompilerModes.MODE_JASMIN) {
			ClassInfoCollector collector = new ClassInfoCollector(Compiler.getInstance().currentFile.getPath()
					+ Compiler.getInstance().currentFile.getLocationPath() + "system/Util");
			for (String method : collector.getStaticMethods()) {
				try {
					addFunction(new FunctionSymbol(method, "system/Util", true));
				} catch (DuplicatedSymbolException e) {
					// Nunca debe pasar
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * Agrega una variable al contexto actual de la tabla de símbolos.
	 * 
	 * @param symbol Símbolo que representa la variable a agregar.
	 * @throws DuplicatedSymbolException Cuando ya existe una variable del mismo
	 *             nombre en el contexto actual.
	 */
	public void addVariableSymbol(VariableSymbol symbol) throws DuplicatedSymbolException {

		/* Obtener la tabla de símbolos. */
		// SymbolTable current = this;
		// if (currentFunction != null) {
		// current = currentFunction.getSymbolTable();
		// }
		if (currentFunction == null) {
			for (Symbol s : symbols.get(0)) {
				if (s instanceof VariableSymbol && s.getId().equals(symbol.getId())
						&& !((VariableSymbol) s).getType().equals(symbol.getType())) {
					throw new DuplicatedSymbolException();
				}
			}
			symbols.get(0).add(symbol);
		} else {

			/* Agregar el simbolo a la profundidad actual. */
			for (Symbol s : currentList.get(depth)) {
				if (s instanceof VariableSymbol && ((VariableSymbol) s).getId().equals(symbol.getId())) {
					throw new DuplicatedSymbolException();
				}
			}

			boolean exists = false;
			for (int i = depth - 1; i >= 0; i--) {
				for (Symbol s : currentList.get(i)) {
					if (s instanceof VariableSymbol && s.getId().equals(symbol.getId())) {
						exists = true;
					}
				}
			}

			if (exists) {
				symbol.setAddr(symbol.getAddr() + "?" + symbol.getType() + "?" + depth);
			} else {
				symbol.setAddr(symbol.getId());
			}

			if (currentList.get(depth).contains(symbol)) {
				throw new DuplicatedSymbolException();
			}
			currentList.get(depth).add(symbol);
		}

	}

	/**
	 * Obtiene una variable con un nombre dado en el contexto actual, o
	 * cualquiera más general.
	 * 
	 * @param id Nombre de la variable a buscar.
	 * @return El símbolo que representa a la variable, o null si no existe.
	 */
	public VariableSymbol getVariableSymbol(String id) {

		/* Si el contexto actual es una funcion, busco primero ahí. */
		if (currentFunction != null) {

			for (Symbol vs : currentList.get(depth)) {
				if (vs instanceof VariableSymbol && ((VariableSymbol) vs).getId().equals(id)) {
					return (VariableSymbol) vs;
				}
			}

			for (int i = depth - 1; i >= 0; i--) {
				for (Symbol s : currentList.get(i)) {
					if (s instanceof VariableSymbol && ((VariableSymbol) s).getId().equals(id)) {
						return (VariableSymbol) s;
					}
				}
			}

			for (int i = depth; i >= 0; i--) {
				for (Symbol s : currentFunction.getSymbolTable().symbols.get(i)) {
					if (s.getId().equals(id) && s instanceof VariableSymbol) {
						return (VariableSymbol) s;
					}
				}
			}
		}

		/* Si no estaba en la funcion, la busco en el global. */
		for (Symbol s : symbols.get(0)) {
			if (s.getId().equals(id) && s instanceof VariableSymbol) {
				return (VariableSymbol) s;
			}
		}

		return null;
	}

	/**
	 * Agrega un nuevo tipo al contexto global.
	 * 
	 * @param symbol Símbolo que representa el tpio a agregar.
	 * @throws DuplicatedSymbolException Cuando ya existe un tipo con este
	 *             nombre.
	 */
	public void addTypeSymbol(TypeSymbol symbol) throws DuplicatedSymbolException {
		if (symbols.get(0).contains(symbol)) {
			throw new DuplicatedSymbolException();
		}
		symbols.get(0).add(symbol);
	}

	/**
	 * Obtiene un símbolo de tipo type, y lo busca solamente en el contexto
	 * global.
	 * 
	 * @param id Nombre del tipo a buscar.
	 * @return El símbolo que representa al tipo, o null si no existe.
	 */
	public TypeSymbol getTypeSymbol(String id) {
		for (Symbol s : symbols.get(0)) {
			if (s.getId().equals(id) && s instanceof TypeSymbol) {
				return (TypeSymbol) s;
			}
		}
		return null;
	}

	/**
	 * Entra en un contexto a nivel de bloque. Debe ser usado para entrar en
	 * bloques que solo están dentro de funciones (por ejemplo estructuras de
	 * control). Para entrar en contextos de funciones utilizar
	 * createFunctionContext en la primera pasada, y enterFunctionContext en la
	 * segunda.
	 */
	public void enterBlockContext() {

		currentList.add(new ArrayList<Symbol>());

		if (currentFunction == null) {
			/* El archivo no es valido sintacticamente. */
			return;
		}

		depth++;

		ArrayList<ArrayList<Symbol>> symbols = currentFunction.getSymbolTable().symbols;
		while (symbols.size() <= depth) {
			symbols.add(new ArrayList<Symbol>());
		}

	}

	/**
	 * Sale de un contexto de nivel bloque. No sirve para salir de contextos de
	 * funciones. Utilizar leaveFunctionContext en este caso.
	 */
	public void leaveBlockContext() {
		if (currentFunction == null) {
			// El archivo no es sintácticamente válido.
			return;
		}

		/* Pasar los simbolos de la lista auxiliar al contexto que corresponda */
		currentFunction.getSymbolTable().symbols.get(depth).addAll(currentList.get(depth));
		currentList.get(depth).clear();

		depth--;
	}

	/**
	 * Agrega la función a la tabla de símbolos y crea un nuevo contexto para
	 * dicha función. Debe ser usada solamente en la primera pasada, para
	 * construir el nodo función al encontrar una declaración. En la segunda
	 * pasada utilizar enterFunctionContext.
	 * 
	 * @param functionSymbol Nuevo símbolo a insertar.
	 * @throws DuplicatedSymbolException Cuando ya existía una función con ese
	 *             nombre.
	 */
	public void createFunctionContext(FunctionSymbol functionSymbol) throws DuplicatedSymbolException {
		if (symbols.contains(functionSymbol)) {
			throw new DuplicatedSymbolException();
		}
		symbols.get(0).add(functionSymbol);
		this.currentFunction = functionSymbol;
	}

	public void addFunction(FunctionSymbol functionSymbol) throws DuplicatedSymbolException {
		if (symbols.contains(functionSymbol)) {
			throw new DuplicatedSymbolException();
		}
		symbols.get(0).add(functionSymbol);
	}

	/**
	 * Entra a un contexto de una función. Debe ser utilizado en la segunda
	 * pasada, con el function symbol retornado por getFunctionSymbol al
	 * encontrar la declaración de la función.
	 * 
	 * @param functionSymbol Función en cuyo contexto estamos entrando.
	 */
	public void enterFunctionContext(FunctionSymbol functionSymbol) {
		currentList.add(new ArrayList<Symbol>());
		this.currentFunction = functionSymbol;
	}

	/**
	 * Sale de un contexto de una función. Se puede utilizar tanto en la primera
	 * pasada como en la segunda.
	 */
	public void leaveFunctionContext() {

		/* Pasar los simbolos de la lista auxiliar al contexto que corresponda */
		currentFunction.getSymbolTable().symbols.get(depth).addAll(currentList.get(depth));
		currentList.get(depth).clear();

		this.currentFunction = null;
	}

	/**
	 * Obtiene una función o una constraint del contexto global.
	 * 
	 * @param id Nombre de la función o constraint.
	 * @param isFunction True si buscamos una función, false para constraints.
	 * @return El símbolo correspondiente, o null si no existe.
	 */
	public FunctionSymbol getFunctionSymbol(String id, boolean isFunction) {
		for (Symbol s : symbols.get(0)) {
			if (s.getId().equals(id) && s instanceof FunctionSymbol && ((FunctionSymbol) s).isFunction() == isFunction) {
				return (FunctionSymbol) s;
			}
		}
		return null;
	}

	/**
	 * @return Los simbolos en el contexto de la función actual.
	 */
	public List<Symbol> getLocalFunctionSymbols() {
		return this.currentFunction.getSymbolTable().symbols.get(0);
	}

	/**
	 * @return True si estamos en el contexto global, false en caso contrario.
	 */
	public boolean isTopContext() {
		return this.currentFunction == null;
	}

	/**
	 * Importa los símbolos de tipo función que estén en el contexto global de
	 * la tabla que recibe por parámetro a la tabla actual, y los marca como
	 * externos.
	 * 
	 * @param externalSymbolTable Tabla de la cual importar los símbolos.
	 */
	public void importFromExternalTable(SymbolTable externalSymbolTable) {
		if (externalSymbolTable == null) {
			return;
		}

		List<Symbol> externalSymbols = externalSymbolTable.symbols.get(0);
		try {
			for (Symbol s : externalSymbols) {
				if (s instanceof FunctionSymbol && !s.isExternal() && !((FunctionSymbol) s).isGetterOrSetter()) {
					s.setExternal();
					this.symbols.get(0).add(s);
				} else if (s instanceof UserDefinedTypeSymbol && !s.isExternal()) {
					s.setExternal();
					this.symbols.get(0).add(s);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); // No debe pasar.
		}
	}

	public void addAllSymbols(SymbolTable symbolTable) {
		if (symbolTable == null) {
			return;
		}

		List<Symbol> externalSymbols = symbolTable.symbols.get(0);
		try {
			for (Symbol s : externalSymbols) {
				if (s instanceof FunctionSymbol && !s.isExternal()) {
					this.symbols.get(0).add(s);
				} else if (s instanceof UserDefinedTypeSymbol && !s.isExternal()) {
					this.symbols.get(0).add(s);
				}

			}
		} catch (Exception e) {
			e.printStackTrace(); // No debe pasar.
		}
	}

	/**
	 * Imprime por salida estándar la tabla de símbolos.
	 */
	public void print(int margin) {
		for (int i = 0; i < symbols.size(); i++) {
			for (Symbol s : symbols.get(i)) {
				if (s.getId().length() > 0 && s.getId().charAt(0) != '_') {
					if (!s.isExternal()) {
						s.print(margin);
					}
				}
			}
		}
	}

	/**
	 * @return Lis<VariableSymbol> con todas las varaibles globales
	 */
	public List<VariableSymbol> getVariableSymbols() {
		List<VariableSymbol> ret = new ArrayList<VariableSymbol>();

		for (ArrayList<Symbol> array : symbols) {
			for (Symbol s : array) {
				if (s instanceof VariableSymbol) {
					ret.add((VariableSymbol) s);
				}
			}
		}
		return ret;
	}

	public List<VariableSymbol> getFunctionVariableSymbols(FunctionSymbol function) {
		return function.getSymbolTable().getVariableSymbols();
	}

	/**
	 * @return List<FunctionSymbol> con todas las funciones.
	 */
	public List<FunctionSymbol> getFunctionSymbols() {
		List<FunctionSymbol> ret = new ArrayList<FunctionSymbol>();

		for (Symbol s : symbols.get(0)) {
			if (s instanceof FunctionSymbol && ((FunctionSymbol) s).isFunction()) {
				ret.add((FunctionSymbol) s);
			}
		}
		return ret;
	}

	/**
	 * @return List<UserDefinedTypeSymbol> con todos los tipos definidos por el
	 *         usuario.
	 */
	public List<UserDefinedTypeSymbol> getUserDefinedTypeSymbols() {
		List<UserDefinedTypeSymbol> ret = new ArrayList<UserDefinedTypeSymbol>();

		for (Symbol s : symbols.get(0)) {
			if (s instanceof UserDefinedTypeSymbol) {
				ret.add((UserDefinedTypeSymbol) s);
			}
		}
		return ret;
	}

}
