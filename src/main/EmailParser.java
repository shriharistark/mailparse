package main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailParser {

    List<String> models;

    EmailParser(List<String> models) {
        this.models = models;
    }

    public String getOrderID(String payload) {
        for(String model : this.models) {
            Pattern pattern = Pattern.compile(model);
            Matcher m = pattern.matcher(payload);
            if(m.find()) {
                return m.group(0);
            }
        }
        return "";
    }

    public static void main(String[] args) throws Exception {

        Trainer trainer = new Trainer();
        List<String> models = trainer.getModels();

        EmailParser parser = new EmailParser(models);

        List<String> testFileNames = Arrays.asList("test-1.txt", "test-2.txt", "test-3.txt", "test-4.txt", "test-5.txt");

        for(String filename : testFileNames) {
            String fileContent = (new Helper()).readFile(filename);
            String orderID = parser.getOrderID(fileContent);
            System.out.println(filename + " has order-id: " + orderID);
        }
    }
}

/**
 * Trainer will read example order ids and will generate a regex string based on example data.
 * eg. OD110270945699460000 will be generating a regex like [a-zA-Z]{2}[0-9]{18}
 * List of these generated regex are called model and we will iterate test data and check if they match regex.
 */
class Trainer {

    private Helper helper = new Helper();

    static class TypeID {
        static String NUMBER = "N", ALPHABET = "A", SYMBOL = "S", MISC = "M";
    }

    private static HashMap<String, String> typeMatches = new HashMap<String, String>() {{
        put(TypeID.NUMBER, "[0-9]");
        put(TypeID.ALPHABET, "[a-zA-Z]");
        put(TypeID.SYMBOL, "[-_=+{};:<>]");
        put(TypeID.MISC, ".");
    }};

    public List<String> getModels() throws Exception {

        List<String> samples = Arrays.asList(helper.getExampleSetOrderIds());
        List<String> models = new ArrayList<String>();

        for(String sample : samples)
            models.add( generateRegexForString(sample) );

        return models;
    }

    static String generateRegexForString(String src) {

        StringBuilder resultRegex = new StringBuilder();

        for(int i = 0; i < src.length(); i++) {
            String cur = src.substring(i, i+1), type = TypeID.MISC;
            if( Pattern.matches(typeMatches.get(TypeID.NUMBER), cur) ) {
                type = TypeID.NUMBER;
            }
            else if( Pattern.matches(typeMatches.get(TypeID.ALPHABET), cur)) {
                type = TypeID.ALPHABET;
            }
            else if ( Pattern.matches(typeMatches.get(TypeID.SYMBOL), cur) ) {
                type = TypeID.SYMBOL;
            }
            resultRegex.append(type);
        }

        return getFormattedRegex( getCompression(resultRegex.toString()) );
    }

    static String getCompression(String src) {

        char last = src.charAt(0);
        char[] chars = src.toCharArray();
        int counter = 0;
        StringBuilder result = new StringBuilder();

        for(Character c : chars) {
            if(c.equals(last)) {
                counter++;
            }
            else {
                result.append(last + "{" + Integer.toString(counter) + "}");
                counter = 1;
                last = c;
            }
        }

        result.append(last + "{" + Integer.toString(counter) + "}");

        return result.toString();
    }

    static String getFormattedRegex(String src) {

        StringBuilder result = new StringBuilder();
        String[] chars = src.split("");

        for(String c : chars) {
            if(typeMatches.containsKey(c))
                result.append(typeMatches.get(c));
            else
                result.append(c);
        }

        result.append("([\\S+]+)?");
        return result.toString();
    }

}

class Helper {

    public String readFile(String fileName) {
        try {

            BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/resources/" + fileName)));
            StringBuilder builder = new StringBuilder();

            while(reader.ready())
                builder.append(reader.readLine()).append("\n");

            return builder.toString();

        }
        catch (Exception e) {
            return "";
        }
    }

    // here we give example order ids based on which we will learn how to parse
    String[] getExampleSetOrderIds() {
        return new String[]{"OD110270945699460000", "402-3106314-1963522", "142148297"};
    }

}
