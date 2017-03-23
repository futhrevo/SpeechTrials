package in.hedera.reku.speechtrial.speech.text.match;


import org.apache.commons.codec.language.Soundex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by rakeshkalyankar on 17/03/17.
 */

public class SoundsLikeWordMatcher extends WordMatcher{
    protected static Soundex soundex;

    static
    {
        soundex = new Soundex();
    }

    public SoundsLikeWordMatcher(String... wordsIn)
    {
        this(Arrays.asList(wordsIn));
    }

    public SoundsLikeWordMatcher(List<String> wordsIn)
    {
        super(encode(wordsIn));
    }

    @Override
    public boolean isIn(String word)
    {
        return super.isIn(encode(word));
    }

    protected static List<String> encode(List<String> input)
    {
        List<String> encoded = new ArrayList<String>();
        for (String in : input)
        {
            encoded.add(encode(in));
        }
        return encoded;
    }

    private static String encode(String in)
    {
        return soundex.encode(in);
    }
}
