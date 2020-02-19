package korat.instrumentation;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javassist.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// DFS of all classes included in the finitization and their fields
// Add all bytecode to an array and SHA256 hash it
// CtClass.getNestedClasses????
public class HashingInstrumenter extends AbstractInstrumenter {

    public String hash;

    private HashSet<CtClass> alreadyInstrumented;

    private ArrayList<CtClass> toInstrument;

    private byte[] bytecode;

    private ArrayList<byte[]> classFilesBytecode;

    public HashingInstrumenter() {
        alreadyInstrumented = new HashSet<CtClass>();
        toInstrument = new ArrayList<CtClass>();
        classFilesBytecode = new ArrayList<byte[]>();
    }

    public void instrument(CtClass clz) throws CannotCompileException, NotFoundException, IOException {
        if (!isMainKoratClass(clz))
            return;

        addClassesForHashing(clz);
        getByteCode();
        byte[] shaHash = getHash();
        hash = bytesToHexString(shaHash);
    }

    public byte[] getHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(bytecode);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Couldn't find algorithm: SHA-256");
            return null;
        }
    }

    private void getByteCode() throws IOException, CannotCompileException {
        int currentIndex = 0;

        // Get all the bytecode since toBytecode freezes and prunes the class
        for (CtClass clz : toInstrument) {
            classFilesBytecode.add(clz.toBytecode());
        }

        int bytecodeLength = 0;

        // Get total size of bytecode
        for (byte[] bytes : classFilesBytecode) {
            bytecodeLength += bytes.length;
        }

        bytecode = new byte[bytecodeLength];

        for (byte [] bytes : classFilesBytecode) {
            System.arraycopy(bytes, 0, bytecode, currentIndex, bytes.length);
            currentIndex += bytes.length;
        }
    }

    private void addClassesForHashing(CtClass clazz) throws CannotCompileException, NotFoundException, IOException {
        addClassToWorklist(clazz);

        for (CtClass nestedClass : clazz.getNestedClasses()) {
            addClassesForHashing(nestedClass);
        }
    }

    private void addClassToWorklist(CtClass clazz) throws CannotCompileException, NotFoundException {
        if (skipHashing(clazz)) {
            return;
        }

        toInstrument.add(clazz);
        alreadyInstrumented.add(clazz);
    }

    private boolean skipHashing(CtClass clazz) {
        return alreadyInstrumented.contains(clazz) ||
                clazz.isInterface() ||
                clazz.isPrimitive() ||
                isInstrumentingClass(clazz);
    }

    private boolean isInstrumentingClass(CtClass clazz) {
        return clazz.getName().contains("Korat");
    }

    private CtField[] getFieldsFromClass(CtClass clazz) {
        return clazz.getDeclaredFields();
    }

    // Naive logic for now. True iff clazz has repOK method.
    private boolean isMainKoratClass(CtClass clazz) {
        for (CtMethod method : clazz.getMethods()) {
            if (method.getName().contains("repOK")) {
                return true;
            }
        }

        return false;
    }

    // https://www.baeldung.com/sha-256-hashing-java
    private static String bytesToHexString(byte[] hash) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
