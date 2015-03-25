package sami;

import java.util.ArrayList;

/**
 *
 * @author nbb
 */
public class CoreHelper {

    public static String shorten(String full, int maxLength) {
        String reduced = "";
        int upperCount = 0;
        for (char c : full.toCharArray()) {
            if (Character.isUpperCase(c) || c == '.') {
                upperCount++;
            }
        }
        int charPerUpper = maxLength / Math.max(1, upperCount); // prevent divide by 0
        int lowerCaseAfterUpperCount = 0;
        for (int i = 0; i < full.length(); i++) {
            if (Character.isUpperCase(full.charAt(i)) || full.charAt(i) == '.') {
                reduced += full.charAt(i);
                lowerCaseAfterUpperCount = 0;
            } else if (lowerCaseAfterUpperCount < charPerUpper) {
                reduced += full.charAt(i);
                lowerCaseAfterUpperCount++;
            }
        }
        return reduced;
    }

    public static String getUniqueName(String name, ArrayList<String> existingNames) {
        boolean invalidName = existingNames.contains(name);
        while (invalidName) {
            int index = name.length() - 1;
            if ((int) name.charAt(index) < (int) '0' || (int) name.charAt(index) > (int) '9') {
                // name does not end with a number - attach a "2"
                name += "2";
            } else {
                // Find the number the name ends with and increment it
                int numStartIndex = -1, numEndIndex = -1;
                while (index >= 0) {
                    if ((int) name.charAt(index) >= (int) '0' && (int) name.charAt(index) <= (int) '9') {
                        if (numEndIndex == -1) {
                            numEndIndex = index;
                        }
                    } else if (numEndIndex != -1) {
                        numStartIndex = index + 1;
                        break;
                    }
                    index--;
                }
                int number = Integer.parseInt(name.substring(numStartIndex, numEndIndex + 1));
                name = name.substring(0, numStartIndex) + (number + 1);
            }
            invalidName = existingNames.contains(name);
        }
        return name;
    }

}
