package Engine.DataAccess;

import Engine.Indexes.Posting;
import Engine.Weights.*;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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
            long weightIncrement = writeTermWeights(currPosting);
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
            double defaultWdt = termWeight.calculate(new DefaultWeigher());
            double tfIdfWdt = termWeight.calculate(new TfIdfWeigher());
            double okapiWdt = termWeight.calculate(new OkapiWeigher());
            double wackyWdt = termWeight.calculate(new WackyWeigher());
            // write the weights to file in the same order so they can easily be read later on
            mActiveWriter.writeDouble(defaultWdt);
            mActiveWriter.writeDouble(tfIdfWdt);
            mActiveWriter.writeDouble(okapiWdt);
            mActiveWriter.writeDouble(wackyWdt);
            return byteAddress += 32; // return the byteAddress incremented by 32 to account for the 8 bytes used to write each weight as a double
        }

}
