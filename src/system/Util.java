package system;

import java.util.ArrayList;

import ar.edu.itba.dcc.tp.util.ReflectionUtils;

@SuppressWarnings( { "unchecked", "fallthrough" })
public class Util {
	public static String _int2string(int i) {
		return new Integer(i).toString();
	}

	public static double _int2double(int i) {
		return new Double(i);
	}

	public static String _bool2string(int b) {
		return b == 1 ? "true" : "false";
	}

	public static int _double2int(double d) {
		return (int) d;
	}

	public static String _double2string(double d) {
		return new Double(d).toString();
	}

	public static double _add_real(double d1, double d2) {
		return d1 + d2;
	}

	public static double _sub_real(double d1, double d2) {
		return d1 - d2;
	}

	public static double _mult_real(double d1, double d2) {
		return d1 * d2;
	}

	public static double _div_real(double d1, double d2) {
		return d1 / d2;
	}

	public static double _uminus_real(double d1) {
		return -d1;
	}

	public static void _exit(String constraintName) {
		throw new IllegalStateException("Constraint " + constraintName + " failed");
	}

	public static Integer _int2Integer(int i) {
		return i;
	}

	public static Double _double2Double(double d) {
		return d;
	}

	public static int _Integer2int(Object i) {
		return (Integer) i;
	}

	public static double _Double2double(Object d) {
		return (Double) d;
	}

	public static ArrayList _newArray(int[] dimentions, String type) {
		ArrayList ret = new ArrayList(dimentions[0]);
		if (dimentions.length == 1) {
			for (int i = 0; i < dimentions[0]; i++) {
				ret.add(addArrayComponent(type));
			}
			return ret;
		} else {
			int[] newDimentions = _shift(dimentions);
			for (int i = 0; i < dimentions[0]; i++) {
				ret.add(i, _newArray(newDimentions, type));
			}

			return ret;
		}
	}

	private static Object addArrayComponent(String type) {
		if (type.equals("java.lang.Integer")) {
			return new Integer(0);
		} else if (type.equals("java.lang.Double")) {
			return new Double(0);
		} else if (type.equals("java.lang.String")) {
			return new String("");
		} else {
			return ReflectionUtils.createInstance(type.substring(1, type.length() - 1));
			// return new Object();
		}
	}

	public static void _arraySet(Object array, int[] indexes, Object data) {
		if (indexes.length == 1) {
			/* De ser necesario aumentar el tamaño del arreglo */
			if (((ArrayList) array).size() <= indexes[0]) {
				int oldSize = ((ArrayList) array).size();
				((ArrayList) array).ensureCapacity(indexes[0] + 1);
				for (int i = oldSize; i < indexes[0] + 1; i++) {
					((ArrayList) array).add(addArrayComponent(data.getClass().toString().substring(6)));
				}
			}
			((ArrayList) array).set(indexes[0], data);
		} else {
			if (((ArrayList) array).size() <= indexes[0]) {
				int oldSize = ((ArrayList) array).size();
				((ArrayList) array).ensureCapacity(indexes[0] + 1);
				for (int i = oldSize; i < indexes[0] + 1; i++) {
					((ArrayList) array).add(new ArrayList());
				}
			}
			_arraySet((ArrayList) ((ArrayList) array).get(indexes[0]), _shift(indexes), data);
		}
	}

	public static Object _arrayGet(Object array, int[] indexes) {
		if (indexes.length == 1) {
			return ((ArrayList) array).get(indexes[0]);
		} else {
			return _arrayGet((ArrayList) ((ArrayList) array).get(indexes[0]), _shift(indexes));
		}
	}

	public static int _array_size(Object array) {
		return ((ArrayList) array).size() * 4;
	}

	private static int[] _shift(int[] vec) {
		/* Shiftear el arreglo de dimensiones */
		int[] newVec = new int[vec.length - 1];
		for (int i = 0; i < newVec.length; i++) {
			newVec[i] = vec[i + 1];
		}
		return newVec;
	}
}
