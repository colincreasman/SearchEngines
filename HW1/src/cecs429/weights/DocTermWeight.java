package cecs429.weights;

import edu.csulb.Driver.WeighingScheme;
import edu.csulb.Driver.ActiveConfiguration;
import cecs429.indexes.*;
import cecs429.documents.*;
import static edu.csulb.Driver.ActiveConfiguration.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static edu.csulb.Driver.ActiveConfiguration.*;
import static edu.csulb.Driver.RunMode.BUILD;

public class DocTermWeight implements Weight {
    private String filePath;
    private WeighingStrategy mWeigher;
    private String mTerm;
    private double mValue;
    private int mDocId;
    private Document mDocument;
    private int mTermFrequency; // tf(t,d)

    public DocTermWeight() {
    }

    public DocTermWeight(int docId, int termFrequency) {
        mDocId = docId;
//        mDocument = activeCorpus.getDocument(docId);
        mTermFrequency = termFrequency;
    }

    @Override
    public void calculate(WeighingScheme scheme) {
        mWeigher = scheme.getInstance();
        mValue = mWeigher.calculateWdt(this);
    }

    @Override
    public void read(WeighingScheme scheme) {
        mWeigher = scheme.getInstance();
        mValue = mWeigher.readWdt(this);
    }

    public int getTermFrequency() {
        return mTermFrequency;
    }

    public void setTermFrequency(int termFrequency) {
        mTermFrequency = termFrequency;
    }

    public double getValue() {
        // if the value hasn't been calculated yet, call calculate now with the current active scheme
        if (mValue == 0) {
            if (hasDiskIndex || runMode == BUILD) {
                calculate(activeWeighingScheme);
            } else {
                read(activeWeighingScheme);
            }
        }
        return mValue;
    }

    public void setValue(double w) {
        mValue = w;
    }

    public String getTerm() {
        return mTerm;
    }

    public Document getDocument() {
        return mDocument;
    }

    public int getDocId() {
        return mDocId;
    }
}