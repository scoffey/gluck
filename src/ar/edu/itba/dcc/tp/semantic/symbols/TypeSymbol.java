package ar.edu.itba.dcc.tp.semantic.symbols;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;

/**
 * Superclase de todos los tipos de datos que se pueden almacenar en la tabla de
 * símbolos.
 */
public abstract class TypeSymbol extends Symbol {

	public TypeSymbol(String id) {
		super(id);
	}

	public String getId() {
		return id;
	}

	@Override
	public void print(int margin) {
		/* Lo anulo */
	}

	/**
	 * Determina si el tipo actual es asignable a otro tipo (ya sea por
	 * promoción o democión). Todas las subclases deben implementar este método,
	 * para ver si se pueden asignar a los otros tipos.
	 * 
	 * @param typeSymbol Tipo con el cual comparar si me puedo asignar.
	 * @return True si soy asignable a typeSymbol. False en caso contrario.
	 */
	public boolean isAssignableTo(TypeSymbol typeSymbol) {
		try {
			this.castTo(typeSymbol);
		} catch (IncompatibleTypesException e) {
			return false;
		}
		return true;
	}

	/**
	 * Determina si el tipo actual puede asignarse a otro tipo. De ser así,
	 * retorna el nombre de la subrutina que realiza el casteo. Si no hace falta
	 * castear, retorna null. Si no se puede asignar tira una excepción.
	 * 
	 * @param typeSymbol Tipo al cual me quiero castear.
	 * @return Nombre de la subrutina que me castea, null si soy el mismo tipo.
	 * @throws IncompatibleTypesException Cuando no me puedo asignar a
	 *             typeSymbol.
	 */
	public abstract String castTo(TypeSymbol typeSymbol) throws IncompatibleTypesException;

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return String que representa el tipo de dato que espera Jasmin. Por
	 *         ejemplo, I para int, Ljava/lan/String; para string, etc.
	 */
	public abstract String jasminType(String packageName, String className);

	/**
	 * Parsea un tipo definido por el usuario
	 * 
	 * @param substring
	 * @return
	 */
	public static TypeSymbol parseType(StringBuffer s) {

		if (s.charAt(0) == 'I') {
			s.deleteCharAt(0);
			return IntTypeSymbol.getInstance();
		} else if (s.charAt(0) == 'D') {
			s.deleteCharAt(0);
			return DoubleTypeSymbol.getInstance();
		} else if (s.charAt(0) == 'L') {
			String objName = s.substring(1, s.indexOf(";"));
			s.delete(0, s.indexOf(";") + 1);
			if (objName.equals("java/lang/String")) {
				return new StringTypeSymbol();
			} else if (objName.equals("java/lang/Integer")) {
				return IntegerWrapperTypeSymbol.getInstance();
			} else if (objName.equals("java/lang/Double")) {
				return DoubleWrapperTypeSymbol.getInstance();
			} else if (objName.equals("java/util/ArrayList")) {
				// SUPER TODO: como levantamos el tipo de un arreglo definido en
				// un modulo externo ya compilado
				// s.deleteCharAt(0);
				return new ArrayTypeSymbol(IntTypeSymbol.getInstance());
			} else if (objName.equals("java/lang/Object")) {
				return ObjectWrapperTypeSymbol.getInstance();
			}

		} else {
			/* Elinar el [... */
			String t = "";
			while (s.charAt(0) == '[') {
				t += s.charAt(0);
				s.deleteCharAt(0);
			}
			t += s.charAt(0);
			s.deleteCharAt(0);
			return new NativeArrayTypeSymbol(t);
		}

		return null;
	}
}
