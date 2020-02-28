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

import org.apache.commons.math3.stat.regression.SimpleRegression;

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
        gson = new Gson();
    }

    public HistoryManager(Class clazz, long finBound, long valid, long explored) throws NotFoundException, IOException, CannotCompileException {
        this(clazz, finBound);
        this.valid = valid;
        this.explored = explored;
    }

    public HistoryManager(Class clazz, long finBound) throws IOException, NotFoundException, CannotCompileException {
        this();

        this.finBound = finBound;
        this.toHash = ClassPool.getDefault().get(clazz.getName());
        generateHash();

        this.previousRuns = loadHistory();
        if (this.previousRuns == null) {
            this.previousRuns = new Implementation[0];
        }
    }

    public void save() throws FileNotFoundException {
        History thisRun = new History(finBound, explored, valid);

        Implementation previous = findPreviousRunWithHash(this.hash);
        if (previous != null) {
            previous.add(thisRun);
            saveToFile();
            return;
        }

        ArrayList<History> newHistoryForImplementation = new ArrayList<History>();
        newHistoryForImplementation.add(thisRun);
        Implementation newTestImplementation = new Implementation(this.hash, newHistoryForImplementation);

        List<Implementation> temp = new ArrayList<Implementation>(Arrays.asList(previousRuns));
        temp.add(newTestImplementation);
        previousRuns = temp.toArray(new Implementation[temp.size()]);

        saveToFile();
    }

    public boolean canPredictModelCount() {
        Implementation previous = findPreviousRunWithHash(this.hash);
        if (previous == null) return false;
        return previous.containsRunWithBound(finBound - 1) &&
                previous.containsRunWithBound(finBound - 2);
    }

    public long predictExplored() {
        if (!canPredictModelCount()) {
            throw new RuntimeException("Cannot predict model count for run: " + this.hash);
        }

        Implementation previous = findPreviousRunWithHash(this.hash);
        History secondToLast = previous.getRunWithBound(finBound - 2);
        History last = previous.getRunWithBound(finBound - 1);

        SimpleRegression regression = new SimpleRegression(true);
        regression.addData(((double) finBound - 1), Math.log((double) last.explored));
        regression.addData(((double) finBound - 2), Math.log((double) secondToLast.explored));

        double lnA = regression.getIntercept();
        double lnB = regression.getSlope();

        double lnX = lnA + finBound * lnB;
        double predictedExplored = Math.exp(lnX);

        return Math.round(predictedExplored);
    }

    public long predictValid() {
        return 0;
    }

    public boolean alreadySeen() {
        if (findPreviousRunWithHash(this.hash) == null) {
            return false;
        } else {
            return true;
        }
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
        if (historyFileExists()) {
            byte[] rawText = Files.readAllBytes(Paths.get(fileName));
            String rawJson = new String(rawText, StandardCharsets.UTF_8);
            return gson.fromJson(rawJson, Implementation[].class);
        } else {
            createEmptyHistory();
            return new Implementation[0];
        }
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

    private Implementation findPreviousRunWithHash(String hash) {
        for (Implementation i : previousRuns) {
            if (i != null && i.shaHash.equals(hash)) {
                return i;
            }
        }

        return null;
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

    public boolean containsRunWithBound(long bound) {
        for (History run : previousRuns) {
            if (run.finitizationBound == bound) {
                return true;
            }
        }

        return false;
    }

    public History getRunWithBound(long bound) {
        for (History run : previousRuns) {
            if (run.finitizationBound == bound) {
                return run;
            }
        }

        return null;
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
