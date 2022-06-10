package cecs429.text;

import java.util.ArrayList;
import java.util.List;

/**
 * A BasicTokenProcessor creates terms from tokens by removing all non-alphanumeric characters from the token, and
 * converting it to all lowercase.
 */
public class BasicTokenProcessor implements TokenProcessor {
	@Override
	public List<String> processToken(String token) {
		ArrayList<String> terms = new ArrayList<>();
		String singleTerm = token.replaceAll("\\W", "").toLowerCase();
		terms.add(singleTerm);
		return terms;
	}
}
