package korat.utils;

import korat.instrumentation.HashingInstrumenter;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;

public class HistoryManager {

    private String fileName = ".history.json";

    private HashingInstrumenter hasher;

    // As defined in TestCradle
    private long valid;

    private long explored;

    private long finBound;

    private CtClass toHash;

    private String hash;

    private Implementation[] previousRuns;

    private Gson gson;

    public HistoryManager() {
        hasher = new HashingInstrumenter();
    }

    public HistoryManager(Class clazz, long finBound, long valid, long explored) throws NotFoundException, IOException, CannotCompileException {
        this();

        this.valid = valid;
        this.explored = explored;
        this.finBound = finBound;

        this.toHash = ClassPool.getDefault().get(clazz.getName());

        this.gson = new Gson();

        generateHash();

        this.previousRuns = loadHistory();
    }

    public void save() throws FileNotFoundException {
        History thisRun = new History(finBound, explored, valid);

        for (Implementation i : previousRuns) {
            if (i != null && i.shaHash.equals(this.hash)) {
                i.add(thisRun);
                saveToFile();
                return;
            }
        }
        
        ArrayList<History> newHistoryForImplementation = new ArrayList<History>();
        newHistoryForImplementation.add(thisRun);
        Implementation newTestImplementation = new Implementation(this.hash, newHistoryForImplementation);

        List<Implementation> temp = new ArrayList<Implementation>(Arrays.asList(previousRuns));
        temp.add(newTestImplementation);
        previousRuns = (Implementation[]) temp.toArray();

        saveToFile();
    }

    private void saveToFile() throws FileNotFoundException {
        String json = gson.toJson(previousRuns);

        if (!historyFileExists()) {
            createEmptyHistory();
        }

        PrintWriter writer = new PrintWriter(fileName);
        writer.println(json);
        writer.close();
    }

    private Implementation[] loadHistory() throws IOException {
        Implementation[] history;

        if (historyFileExists()) {
            byte[] rawText = Files.readAllBytes(Paths.get(fileName));
            String rawJson = new String(rawText, StandardCharsets.UTF_8);
            history = gson.fromJson(rawJson, Implementation[].class);
        } else {
            createEmptyHistory();
            history = null;
        }

        return history;
    }

    private void createEmptyHistory() {
        try {
            Files.createFile(Paths.get(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean historyFileExists() {
        return Files.exists(Paths.get(fileName));
    }

    private void generateHash() throws IOException, CannotCompileException, NotFoundException {
        hasher.instrument(this.toHash);
        this.hash = hasher.hash;
    }
}

class History {
    public long finitizationBound;

    public long explored;

    public long valid;

    public History(long bound, long explored, long valid) {
        this.finitizationBound = bound;
        this.explored = explored;
        this.valid = valid;
    }

    public void setFields(History other) {
        this.valid = other.valid;
        this.explored = other.explored;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        History that = (History) o;
        return finitizationBound == that.finitizationBound;
    }

    @Override
    public int hashCode() {
        return Objects.hash(finitizationBound, explored, valid);
    }
}

class Implementation {
    public String shaHash;

    public List<History> previousRuns;

    public Implementation(String shaHash, List<History> previousRuns) {
        this.shaHash = shaHash;
        this.previousRuns = previousRuns;
    }

    public void add(History newRun) {
        for (History run : previousRuns) {
            if (run.equals(newRun)) {
                run.setFields(newRun);
                return;
            }
        }

        previousRuns.add(newRun);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Implementation that = (Implementation) o;
        return shaHash.equals(that.shaHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shaHash);
    }
}
