package Engine.Text;

import org.tartarus.snowball.SnowballStemmer;


import java.util.ArrayList;
import java.util.List;

public class HyphenTokenProcessor implements TokenProcessor {

    /**
     * Normalizes a token into a term (or list of terms)
     *
     * @param token
     */
    // "note: do not perform the split on hyphens step on query literals; use the whole literal, including the hyphen"
    @Override
    public List<String> processToken(String token) {
        // perform all of necessary processing functions on the single original token before removing hyphens
        List<String> results = new ArrayList<>();
        String token1 = fixNonAlphaNumerics(token);
        String token2 = fixPunctuation(token1);
        String token3 = fixHyphens(token2);
        String token4 = fixCase(token3);
        String stemmedTerm = stem(token4);
        results.add(stemmedTerm);
        return results;
    }

    // Removes all non-alphanumeric characters from the beginning and end of the token, but not the middle.
    public String fixNonAlphaNumerics(String token) {
        // initialize vals for first and last indexes
        int first = 0;
        int last = token.length() - 1;

        // automatically return the token as-is if it is only a single character long
        if (first == last) {
            return token;
        }

        // convert to character array
        char[] tokenCharacters = token.toCharArray();

        // find the first index in the char array that is an alphanumeric character
        for (int i = 0; i < tokenCharacters.length; i++) {
            if (Character.isLetterOrDigit(tokenCharacters[i])) {
                first = i;
                break;
            }
        }

        //  find the last index that is an alphanumeric character by counting backwards from the end
        for (int i = tokenCharacters.length - 1; i > 0; i--) {
            if (Character.isLetterOrDigit(tokenCharacters[i])) {
                last = i + 1;
                break;
            }
        }
        // then substring between the two to extract the term.
        String result = token.substring(first, last);
        //System.out.println("Testing fixNonAplhanumerics: " + result);
        return result;
    }

    // Removes all apostrophes or quotation marks (single or double quotes) from anywhere in the string
    public String fixPunctuation(String token) {
        // first check if the token contains any relevant punctuations
        // if (token.contains("\"") || token.contains("\'")) {
        // if so, use a regex to remove them
        token = token.replaceAll("\"", "");
        token = token.replaceAll("\'", "");
        // }
        ///System.out.println("Testing fixPunctuation: " + token);
        return token;
    }

    /**
     * (a) Removes the hyphens from the token for the first processed token, AND
     * (b) Splits the original hyphenated token into multiple tokens without a hyphen
     * each of which becomes its own processed token
     *
     * @param token
     * @return
     */
    public String fixHyphens(String token) {
        // first check if the token even contains any hyphens before processing
        if (token.contains("-")) {
            // create a clone of the original token and remove all hyphens from it
            return token.replaceAll("[\\s\\-()]", "");
        }
        // if there are no hyphens in the original token, add it to the list in its original form
        else {
            return token;
        }
    }
    //converts the token to lowercase
    public String fixCase(String token) {
        //System.out.println("Testing fixCase: " + token.toLowerCase());
        return token.toLowerCase();
    }

    public String stem(String token) {
        String term = "";
        try {
            Class<?> stemClass = Class.forName("org.tartarus.snowball.ext." + "english" + "Stemmer");
            SnowballStemmer stemmer = (SnowballStemmer) stemClass.getDeclaredConstructor().newInstance();

            stemmer.setCurrent(token);
            stemmer.stem();
            term = stemmer.getCurrent();
            return term;
            //System.out.println("testing with github way term is: " + term);
        }
        catch (Exception ex) {
            return null;
        }
        //System.out.println("Testing Stem: " + term);
        // return term;
    }
}

