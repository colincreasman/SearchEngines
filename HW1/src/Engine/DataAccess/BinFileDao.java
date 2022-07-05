package Engine.DataAccess;

import App.Driver;
import App.Driver.WeighingScheme;
import Engine.Indexes.Posting;
import Engine.Weights.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static App.Driver.ActiveConfiguration.*;
import static App.Driver.WeighingScheme.*;

public class BinFileDao extends FileDao {
    private DataOutputStream mActiveWriter;
    private RandomAccessFile mActiveReader;
    private File mActiveFile;

    public BinFileDao() {
        super();
    }

    public BinFileDao(File sourceDir) {
        super(sourceDir);
    }

    @Override
    public void create(String name) {
        String filePath = mSourceDir + "/" + name + ".bin"; // construct the entire file path using the static mSourceDir and the ".bin" extension for this implementation
        File binFile = new File(filePath);

        // make sure there is no current index folder in the given path before indexing
        if (binFile.exists()) {
            // if there's already a folder, delete it and its contents
            binFile.delete();
        }

        try {
            // if creating the new file is successful, add it to the list of binFiles
            if (binFile.createNewFile()) {
                mFiles.add(binFile);
            }
        } catch (IOException ex) {
            System.out.println("Error: File could not be created in the current source directory. ");

        }
    }

    @Override
    public void open(String name) {
        String filePath = mSourceDir + "/" + name + ".bin"; // construct the entire file path using the static mSourceDir and the ".bin" extension for this implementation
        File binFile = new File(filePath);
        // create the file if it isn't already in the source directory before continuing
        if (!mFiles.contains(binFile)) {
            create(name);
        }

        // make sure the requested file isn't already open
        if (!binFile.equals(mActiveFile)) {
            try {
                FileOutputStream fileStream = new FileOutputStream(binFile);
                mActiveWriter = new DataOutputStream(fileStream);
                mActiveReader = new RandomAccessFile(binFile, "r");
                mActiveFile = binFile;
            } catch (FileNotFoundException ex) {
                System.out.println("Error: The active reader/writer could not be opened because the file does not exist ");
            }
        }
    }

    @Override
    public void close(String name) {
        String filePath = mSourceDir + "/" + name + ".bin";
        File binFile = new File(filePath);

        // check if the file requesting to be closed is open
        if (binFile.equals(mActiveFile)) {
            try {
                mActiveWriter.close();
                mActiveReader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println("Error: The active reader/writer could not be closed because it was never opened. ");
        }
    }

    /**
     * writes a list of posting for a given term
     * Each posting's data is written in the order: docId (as a gap), w(d,t) values, tf(t,d), and finally the list of term positions (as gap) {p1, p2...}
     * returns back a long value to indicate how many total bytes of the active file were used up by the list of postings
     */
    public long writePostings(long byteAddress, List<Posting> termPostings) throws IOException {
        // there is only a single dFt value for a list of postings, so write it to file first
        int dFt = termPostings.size();
        mActiveWriter.writeInt(dFt);

        //  before iterating through each posting we can take the first docId as-is (without calculating gaps)
        int docId = termPostings.get(0).getDocumentId();

        for (int i = 0; i < dFt; i++) {
            // if this is not the first posting for a given term, the docId must be re-assigned using the gap between itself and the previous docId
            if (i > 0) {
                int oldId = termPostings.get(i - 1).getDocumentId();
                int currId = termPostings.get(i).getDocumentId();
                docId = (currId - oldId);
            }

            // write docId before any other postings data
            mActiveWriter.writeInt(docId);
            byteAddress += 4;

            // use the current byteAddress to write the weights for the current posting and use the result to increment
            Posting currPosting = termPostings.get(i);
            long weightIncrement = writeTermWeights(byteAddress, currPosting);
            byteAddress += weightIncrement;

            // find tf(t,d) from the number of term locations and write it to file
            List<Integer> positions = currPosting.getTermPositions();
            int termFrequency = positions.size(); // tf(t,d)
            mActiveWriter.writeInt(termFrequency);

            // use the current byteCount to write the current list of term positions and use the result to increment it
            long positionIncrement = writePositions(byteAddress, positions);
            byteAddress += positionIncrement;
        }
        // return the final byteAddress after incrementing everything written
        return byteAddress;
    }

    /**
     * writes a list of term positions as gaps
     * returns back a long value to indicate how many total bytes of the active file were used up by the list of postings
     */
    public long writePositions(long byteAddress, List<Integer> positions) throws IOException {

        //  before iterating through each position we can take the first term position as-is (without calculating gaps)
        int termPosition = positions.get(0);
        for (int i = 0; i < positions.size(); i++) {
            // for every position after the first, re-assign it as a gap
            if (i > 0) {
                int oldPosition = positions.get(i - 1);
                int currPosition = positions.get(i);
                termPosition = (currPosition - oldPosition);
            }

            // write the updated position to the file and increment the byte counter
            mActiveWriter.writeInt(termPosition);
            byteAddress += 4;
        }
        return byteAddress;
    }

    /**
     * writes the term weights of the provided posting to disk using each of the 4 weighing strategies calculated values
     * returns back a long value to indicate how many total bytes of the active file were used up by the list of postings
     */
    public long writeTermWeights(long byteAddress, Posting p) throws IOException {
        // obtain a reference to current posting's DocTermWeight
        DocTermWeight termWeight = p.getDocTermWeight();
        // now use the Weight reference to calculate values with each type of weigher
        for (WeighingScheme scheme : WeighingScheme.values() ) {
            termWeight.calculate(scheme);
            mActiveWriter.writeDouble(termWeight.getValue());
            byteAddress += 8; // increment the byteAddress 8 to write each weight as a double
        }
        return byteAddress;
    }

    public void writeDocWeights(List<DocWeight> docWeights) throws IOException {
        int avgDocLength = 0;

        for (DocWeight w : docWeights) {
            double docLd = w.getValue();
            long docLength = w.getDocLength();
            long byteSize = w.getByteSize();;
            int avgFrequency = w.getAvgTermFrequency();

            // write the per-doc weight data of each docWeight in the order of: docWeight, docLength, byteSize, avgTfTd,
            // starts at byte 0
            mActiveWriter.writeDouble(docLd);  // now at byte 8
            mActiveWriter.writeLong(docLength); // now at byte 16
            mActiveWriter.writeLong(byteSize); // now at byte 24
            mActiveWriter.writeInt(avgFrequency); // now at byte 28
            // next DocWeight starts writing @ byte 28

            avgDocLength += docLength;
        }
        avgDocLength = avgDocLength / docWeights.size();
        mActiveWriter.writeInt(avgDocLength);// will be written at byte location: (docWeightsOut.length() - 4)
    }

    // TODO: Change this to use each new weighing strategy
    public double readDocWeight(int docId) {
        double weight = 0.0;
        try {
            int byteLocation = (docId * 28); // 28 bytes used up for the 4 values written for each doc
            // start reading from the beginning of the file, the first byte should hold the first doc id
            mActiveReader.seek(byteLocation);
            weight = mActiveReader.readDouble();
        }
        catch (Exception ex) {
            System.out.println("Failed to read the doc weight from disk for docId: " + docId);
            ex.printStackTrace();
        }
        return weight;
    }

//    public double readTermWeight(int docId, WeighingScheme scheme) {
//        // open the termLocations file to get the byte location of the passed in doc
//        DbFileDao dbFileDao = new DbFileDao();
//        open("termLocations");
//
//        String term = w.getTerm();
//        int docId = w.getDocument().getId();
//        long byteLocation = mDbDao.readTermLocation(term);
//
//        Weight wv = (Weight) dao.read(w);
//        String term = w.getTerm();
//        return 0;
//
//        // find out how much to offset the read bytes by checking the ordinal of the passed in weighting scheme
//        int weightOffset
//
//    }

    // reads select postings data from the disk to return a list of postings,
    // each constructed with vals for its docId and docWeights
    // all other postings data will be handled in the getPostings() method calling this one
    // initialize necessary vars for results and structs to read from the termsDB
    public List<Posting> readPostings(long byteLocation) {
        int termDocFrequency = 0;
        double termDocWeight = 0.0;
        List<Posting> results = new ArrayList<>();

        try {
            // start by seeking to the byte location of the term - this is where all of its postings data begins
            // now we can easily find any other postings data we need by incrementing the necessary amount of bytes from that initial position
            mActiveReader.seek(byteLocation);
            // first get tf(d) (the amount of docs the term occurs in) which should be at the exact byte indicated by the termLocation
            int dFt = mActiveReader.readInt();

            // before iterating through all the term's docs, initialize the first docId to 0
            int currentDocId = 0;

            // now we can use that tf(d) value find the rest of the term's data
            for (int i = 0; i < dFt; i++) {
                // for the first value only, read the doc ID as-is with no gaps
                if (i == 0) {
                    currentDocId = mActiveReader.readInt();
                }
                // otherwise, read the next docId and use it to increment the gap between itself and the previous docId
                else {
                    currentDocId += mActiveReader.readInt();
                }
                // use the ordinal of the active weighing scheme to find out how many bytes(if any) to jump before reading the correct weight type
                int weightBytes = activeWeighingScheme.ordinal();
                mActiveReader.skipBytes(weightBytes);
                // read w(d,t)
                double currentWeight = mActiveReader.readDouble();

                // read tf(t,d)
                int currTermFrequency = mActiveReader.readInt();

                // now jump ahead by the 4 * tf(t,d) to get to the next docId without reading each term position
                mActiveReader.skipBytes(4 * currTermFrequency);

                List<Integer> termPositions = new ArrayList<>();

                int currTermPosition = 0;

                for (int j = 0; j < currTermFrequency; j++) {
                    if (j == 0) {
                        currTermPosition = mActiveReader.readInt();
                    }
                    else {
                        currTermPosition += mActiveReader.readInt();
                    }
                    termPositions.add(currTermPosition);
                }

                // instantiate a generic posting and set its data with the values read from file
                Posting currPosting = new Posting(currentDocId, termPositions); // don't need to set tf(t,d) bc it will be calculated automatically by instantiating a posting with position
                currPosting.setDocTermWeight(currentWeight);
                results.add(currPosting);
            }
//            mActiveReader.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return results;
    }

    // reads select postings data from the disk to return a list of postings,
    // each constructed with vals for its docId and docWeights
    // all other postings data will be handled in the getPostings() method calling this one
    // initialize necessary vars for results and structs to read from the termsDB
    public List<Posting> readPostingsWithoutPositions(long byteLocation) {
        int termDocFrequency = 0;
        double termDocWeight = 0.0;
        List<Posting> results = new ArrayList<>();

        try {
            // start by seeking to the byte location of the term - this is where all of its postings data begins
            // now we can easily find any other postings data we need by incrementing the necessary amount of bytes from that initial position
            mActiveReader.seek(byteLocation);

            // first get tf(d) (the amount of docs the term occurs in) which should be at the exact byte indicated by the termLocation
            int dFt = mActiveReader.readInt();

            // before iterating through all the term's docs, initialize the first docId to 0
            int currentDocId = 0;

            // now we can use that tf(d) value find the rest of the term's data
            for (int i = 0; i < dFt; i++) {
                // for the first value only, read the doc ID as-is with no gaps
                if (i == 0) {
                    currentDocId = mActiveReader.readInt();
                }
                // otherwise, read the next docId and use it to increment the gap between itself and the previous docId
                else {
                    currentDocId += mActiveReader.readInt();
                }
                // use the ordinal of the active weighing scheme to find out how many bytes(if any) to jump before reading the correct weight type
                int weightBytes = activeWeighingScheme.ordinal();
                mActiveReader.skipBytes(weightBytes);
                // read w(d,t)
                double currentWeight = mActiveReader.readDouble();

                // read tf(t,d)
                int currTermFrequency = mActiveReader.readInt();

                // now jump ahead by the 4 * tf(t,d) to get to the next docId without reading each term position
                mActiveReader.skipBytes(4 * currTermFrequency);

                // instantiate a generic posting and set its data with the values read from file
                Posting currPosting = new Posting(currentDocId);
                currPosting.setDocTermWeight(currentWeight);
                currPosting.setTermFrequency(currTermFrequency);
                results.add(currPosting);
            }
            mActiveReader.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return results;
    }
}
