/*
 * Description:
 * This is a moderately advanced cribdragging program. It compares 2 messages that were XORed
 * with a key in order to attempt to recover the plaintexts. It also searches for partial word
 * matches from a list of common english words in the text file dictionary.txt, which can be
 * replaced by your own common word dictionary.
 *
 * Note: Each word in dictionary.txt should be on a separate line
 * */

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

class Cribdragging {

    public static void main(String[] args) throws FileNotFoundException {

        // These are the plaintexts and key
        String pt1 = "Hello this is the first secret";              // These messages and keys can
        String pt2 = "this is another secret message";              // be changed to anything as long
        String key = "supersecretmessagessupersecret";              // as they are equal in length


        //Convert plaintext messages to binary
        ArrayList<Integer> pt1Binary = textToBinary(pt1);
        ArrayList<Integer> pt2Binary = textToBinary(pt2);
        ArrayList<Integer> keyBinary = textToBinary(key);

        // Initialize ciphertext ArrayLists
        ArrayList<Integer> ciphertext1 = new ArrayList<>();
        ArrayList<Integer> ciphertext2 = new ArrayList<>();

        // XOR messages with key, and store them in ciphertext1 and ciphertext2
        xor(pt1Binary, keyBinary, ciphertext1); //message 1
        xor(pt2Binary, keyBinary, ciphertext2); //message 2


        String cribword = "";
        Scanner myObj = new Scanner(System.in);
        boolean isRunning = true;
        boolean skipIntro = false;

        while (isRunning) {   // Main while loop

            // STEP 1
            // Try a cribword
            if (!skipIntro) {       // Intro asks you to enter a cribword
                System.out.println("---------------\nEnter cribword:");
                cribword = myObj.nextLine();
            }
            skipIntro = false;
            System.out.println("Cribword is: " + cribword + "\n");      // Output the guess


            // STEP 2
            //  Convert cribword to binary
            ArrayList<Integer> cribBinary = new ArrayList<>(textToBinary(cribword));


            // STEP 3
            // XOR BOTH ciphertext messages
            ArrayList<Integer> xordCipher = new ArrayList<>();
            xor(ciphertext1, ciphertext2, xordCipher);


            // STEP 4
            // XOR the resulting ciphertext with cribword
            ArrayList<Integer> guess = new ArrayList<>();
            xor(cribBinary, xordCipher, guess);


            // STEP 5
            // Convert results into (hopefully) readable text, end program if you find the complete message
            if (pt1.equalsIgnoreCase(convertGuess(guess, pt1)) || pt2.equalsIgnoreCase(convertGuess(guess, pt2))) {
                System.out.println("CONGRATULATIONS, you deciphered the secret message!");
                isRunning = false;
            }
            System.out.println("Result is: " + convertGuess(guess, key) + "\n");    // Output result


            // If complete message is not found, search for partial matches in dictionary
            if (isRunning) {
                cribword = search(convertGuess(guess, key));

                if (cribword.equals("NO MATCHES FOUND")) {      // if no matches found, return to intro
                    System.out.println(cribword);
                    continue;
                }
                else{                                                       // If partial match is found, ask if
                    System.out.println("Partial match: \"" + cribword +     // you want to use it in next cribword
                            "\"\nWould you like to use it in your next guess? (y/n)");
                }

                String usrInput = myObj.nextLine();
                if (usrInput.equals("y")) {                     // If you choose yes, cribword = result + match found
                    String nextCrib = convertGuess(guess, key);
                    nextCrib = nextCrib.replace("?", "");
                    try {
                        nextCrib = nextCrib.substring(0, nextCrib.lastIndexOf(" "));
                        cribword = nextCrib + " " + cribword;
                    } catch (StringIndexOutOfBoundsException e){
                        cribword = nextCrib + " ";
                    }
                    skipIntro = true;   // Skip asking for new cribword input
                }
            }

        }
    }


    // XORs two ArrayLists representing a message in binary
    private static void xor(ArrayList<Integer> firstList, ArrayList<Integer> secondList, ArrayList<Integer> xordList) {
        for (int i = 0; i < firstList.size(); i++) {
            try {
                xordList.add(firstList.get(i) ^ secondList.get(i));         // xor normally
            } catch (IndexOutOfBoundsException e) {
                for (int j = 0; j < ((firstList.size() - secondList.size()) / 8); j++) {
                    for (int k = 0; k < 8; k++) {
                        if (k == 2) {
                            xordList.add(1);                                // adds "SPACE" (00100000)
                        } else {                                            // if input was too long
                            xordList.add(0);
                        }
                    }
                }
            }
        }
    }

    // Converts the XORd guess into readable text, "?" for every unknown character
    private static String convertGuess(ArrayList<Integer> guess, String message) {
        String singleLetter;
        StringBuilder finalGuess = new StringBuilder();

        // read each binary "letter" by separating into 8 digit sections, then convert to readable text
        ArrayList<String> eightDigitString = new ArrayList<>();
        for (int j = 0; j < (guess.size() / 8); j++) {
            for (int i = 0; i < 8; i++) {
                eightDigitString.add(String.valueOf(guess.get(i + (j * 8))));
            }
            singleLetter = eightDigitString.stream().map(Object::toString).collect(Collectors.joining(""));
            finalGuess.append((char) Integer.parseInt(singleLetter, 2));
            eightDigitString.clear();
        }


        // Add "?" for every unknown character after guess
        if (finalGuess.length() < message.length()) {
            for (int i = 0; i < (message.length() - finalGuess.length()); i++) {
                finalGuess.append("?");
            }
        }
        return finalGuess.toString();
    }

    // Takes a String and converts it into binary, stored in an ArrayList
    private static ArrayList<Integer> textToBinary(String s) {
        byte[] bytes = s.getBytes();
        ArrayList<Integer> binary = new ArrayList<>();
        for (byte b : bytes) {
            int val = b;
            for (int i = 0; i < 8; i++) {
                binary.add((val & 128) == 0 ? 0 : 1);
                val <<= 1;
            }

        }
        return binary;
    }

    // Reads through the provided dictionary file in order to look for partial matches
    private static String search(String str) throws FileNotFoundException {

        Scanner input = new Scanner(new FileReader("dictionary.txt"));
        String found = "NO MATCHES FOUND";
        String lastword = str;

        // only keep the partial word at end, gets rid of trailing "?"
        if (str.contains(" ")) {
            lastword = str.substring(str.lastIndexOf(" ") + 1);
        }
        if (str.contains("?")) {
            lastword = lastword.replace("?", "");
        }

        while (input.hasNext() && (!lastword.equals(""))) {
            String line = input.nextLine();
            if (line.startsWith(lastword.toLowerCase())) {    // check if this word contains the partial word
                found = line + " ";
                break;
            }
        }

        return found;
    }


}