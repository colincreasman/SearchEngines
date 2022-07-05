package Engine.Documents;

import Engine.Weights.DocWeight;

import java.io.Reader;
import java.security.DigestException;

/**
 * Represents a document in an index.
 */
public interface Document {
	/**
	 * The ID used by the index to represent the document.
	 */
	int getId();

	DocWeight getWeight();

	void setWeight(DocWeight w);

	/**
	 * Gets a stream over the content of the document.
	 */
	Reader getContent();
	/**
	 * The title of the document, for displaying to the user.
	 */
	String getTitle();

	long getByteSize();
}
