package cecs429.documents;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.google.gson.stream.JsonReader;
/**
 * Represents a document that is saved as a JSON file
 */
public class JsonFileDocument implements FileDocument {
    private int mDocumentId;
    private Path mFilePath;
    private String mDocumentTitle;

    /**
     * Constructs a TextFileDocument with the given document ID representing the file at the given
     * absolute file path.
     */
    public JsonFileDocument(int id, Path absoluteFilePath) {
        mDocumentId = id;
        mFilePath = absoluteFilePath;
    }


    @Override
    public int getId() {
        return mDocumentId;
    }

    /**
     * Returns the content of a .json is the value of the "body"" key, which you can read as a string. You will need to find a way to construct a stream around that string in memory
     * @return
     */
    @Override
    public Reader getContent() {
        // try to read content as a .json file
        try {
            // use a JsonReader to start reading the file
            JsonReader reader = new JsonReader(Files.newBufferedReader(mFilePath));

            // initialize string to hold body content
            String content = "";
            reader.beginObject();

            // parse through objects (names) in the file using beginObject
            while (reader.hasNext()) {
                String currentName = reader.nextName();
                // find the "Body" section
                if (Objects.equals(currentName, "body")) {
                    // extract the content as a string
                    content = reader.nextString();
                    break;
                }
                // find the "title" section and store it in the corresponding class variable
                // simplifies getTitle()
                else if (Objects.equals(currentName, "title")) {
                    mDocumentTitle = reader.nextString();
                }
            }
            // close the reader and stream
            reader.endObject();
            reader.close();
            // wrap the content in a StringReader
            StringReader contentStream = new StringReader(content);
            return contentStream;
        }

        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return title of document as found in the "title" key.
     */
    @Override
    public String getTitle() {
        return mDocumentTitle;
    }

    @Override
    public Path getFilePath() {
        return mFilePath;
    }
}
