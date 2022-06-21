package cecs429.queries;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Parses boolean queries according to the base requirements of the CECS 429 project.
 * Does not handle phrase queries, NOT queries, NEAR queries, or wildcard queries... yet.
 */
public class RankedQueryParser implements QueryParser {
	//private static TokenProcessor mProcessor;
	/**
	 * Identifies a portion of a string with a starting index and a length.
	 */
	private List<String> terms;
	private int corpusSize;

	public RankedQueryParser() {
		terms = new ArrayList<>();
		corpusSize = 0;
	}

	/**
	 * Given a "bag of terms" query, parses and returns a single RankedQuery representing all the query terms
	 */
	public QueryComponent parseQuery(String query, int corpusSize) {
		terms = Arrays.asList(query.split(" "));
		return new RankedQuery(terms, corpusSize);
	}

	@Override
	public QueryComponent parseQuery(String query) {
		return null;
	}
}





