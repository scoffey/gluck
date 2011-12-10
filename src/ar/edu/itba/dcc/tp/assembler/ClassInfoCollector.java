package ar.edu.itba.dcc.tp.assembler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

//import org.objectweb.asm.commons.EmptyVisitor;

public class ClassInfoCollector implements ClassVisitor {
	private String className;
	private String superClass;
	private List<String> staticMethods = new ArrayList<String>();
	private List<String> nonStaticMethods = new ArrayList<String>();
	private List<String> innerClasses = new ArrayList<String>();
	private List<String> staticFieldsNames = new ArrayList<String>();
	private List<String> staticFieldsTypes = new ArrayList<String>();

	public ClassInfoCollector(String className) throws IOException {
		ClassReader reader = null;
		try {

			String file = BytecodeGenerator.magic(className + ".class");
			if (file.charAt(0) == '/') {
				file = file.substring(1);
			}

			reader = new ClassReader(new FileInputStream(new File(file)));
			// reader = new ClassReader(className);
		} catch (IOException e) {
			throw e;
		}
		reader.accept(this, ClassReader.SKIP_CODE);
	}

	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className = name;
		this.superClass = superName;
	}

	public void visitSource(String source, String debug) {
	}

	public void visitOuterClass(String owner, String name, String desc) {
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		return null;
	}

	public void visitAttribute(Attribute attr) {

	}

	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		if (outerName.equals(this.getClassName())) {
			innerClasses.add(name);
		}
	}

	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if ((access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC && (access & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC) {
			staticFieldsNames.add(name);
			staticFieldsTypes.add(desc);
		}
		return null;
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if ((access & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC) {
			if ((access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				staticMethods.add(name + desc);
			} else {
				nonStaticMethods.add(name + desc);
			}
		}
		return null;
	}

	public void visitEnd() {
	}

	public String getClassName() {
		return className;
	}

	public String getSuperClass() {
		return superClass;
	}

	public List<String> getStaticMethods() {
		return staticMethods;
	}

	public List<String> getNonStaticMethods() {
		return nonStaticMethods;
	}

	public List<String> getInnerClasses() {
		return innerClasses;
	}

	public List<String> getStaticFields() {
		return staticFieldsNames;
	}

	public List<String> getStaticFieldsTypes() {
		return staticFieldsTypes;
	}

}
