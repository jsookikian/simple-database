/**
 * Created by jsookikian on 1/26/17.
 */
import java.io.*;
import java.util.*;
import java.text.*;
import java.math.*;
import java.util.regex.*;

public class Solution {
    private HashMap<String, Double> entries; // Variable for Database entries
    private LinkedList<TransactionBlock> blocks; // Transaction Blocks pointer
    // A list of valid commands
    private final static String[] validCommands =  {"SET", "GET", "UNSET", "NUMEQUALTO", "BEGIN", "ROLLBACK", "COMMIT", "END"};

    private final static int INVALID = 0;
    private final static int TRANSACTION_COMMAND = 1;
    private final static int DATA_COMMAND = 2;
    private final static int END_COMMAND = 3;


    class Database {

        // Database Constructor.
        // Initialize database entries hashmap and Transaction Block list
        public Database() {
            entries = new HashMap<String, Double>();
            blocks = new LinkedList<TransactionBlock>();
        }


        // Parses the command and evaluate its type.
        public int parseInput(String input) {
            // Parse the command by whitespace
            String[] cmd = input.split("\\s+");

            // If the command is too long, too short, or not a valid command string, it is invalid input.
            if (cmd.length == 0 || cmd.length > 3 || !isValidCommandString(cmd[0])) {
                return INVALID;
            }

            // If the command is a transaction block specific command
            else if (cmd.length == 1) {
                switch (cmd[0]) {
                    // Transaction Commands
                    case "BEGIN":
                    case "ROLLBACK":
                    case "COMMIT":
                        return TRANSACTION_COMMAND;
                    // End the Program
                    case "END":
                        end();
                    default:
                        return INVALID;
                }
            }

            // Command will be a GET, UNSET, or NUMEQUALTO command if it is valid.
            else if (cmd.length == 2) {
                switch(cmd[0]) {
                    case "GET":
                    case "UNSET":
                    case "NUMEQUALTO":
                        return DATA_COMMAND;
                    default:
                        return INVALID;
                }
            }
            // The command will be a SET command.
            else if (cmd.length == 3) {
                if (cmd[0].equals("SET")) {
                    return DATA_COMMAND;
                }
            }

            return INVALID;

        }

        public void evaluateCommand(String command) {
            // Parse input to figure out what type it is (INVALID, DATA_COMMAND, OR TRANSACTION_COMMAND)
            int inputType = parseInput(command);
            // Print INVALID if it is not valid input
            if (inputType == INVALID) {
                System.out.println("INVALID COMMAND");
            }
            // If a transaction has started and a data command is entered, cache the data command
            // in the most recent transaction block
            else if (!blocks.isEmpty() && inputType == DATA_COMMAND) {
                AddTransaction(command);
            }
            // If it is a transaction command, execute it.
            else if (inputType == TRANSACTION_COMMAND) {
                TransactionCommand(command);
            }
            // Since there are no transaction blocks, the data command can be committed immediately
            else {
                executeDataCommand(command);
            }
        }


        // Command will be a transaction block specific command
        public void TransactionCommand(String command) {
            switch (command) {
                // Run begin command
                case "BEGIN":
                    begin();
                    break;
                // Run rollback command
                case "ROLLBACK":
                    rollback();
                    break;
                // Run Commit command
                case "COMMIT":
                    commit();
                    break;
                default:
            }
        }
        // Add the data command to the most recent Transaction Block's cache
        public void AddTransaction(String command) {
            blocks.getLast().addToCache(command);
        }


        public void executeDataCommand(String command) {
            String[] cmd = command.split("\\s+");

            // If the command is too long, too short, or not a valid command string, it is invalid input.
            if (cmd.length == 0 || cmd.length > 3 || !isValidCommandString(cmd[0])) {
                System.out.println("INVALID INPUT");
            }
            // Command will be a GET, UNSET, or NUMEQUALTO command.
            else if (cmd.length == 2) {
                switch(cmd[0]) {
                    case "GET":
                        System.out.println(get(cmd[1]));
                        break;
                    case "UNSET":
                        unset(cmd[1]);
                        break;
                    case "NUMEQUALTO":
                        System.out.println(numEqualTo(Double.parseDouble(cmd[1])));
                        break;
                    default:
                        System.out.println("INVALID INPUT");
                }
            }
            else if (cmd.length == 3) {
                if (cmd[0].equals("SET")) {
                    set(cmd[1], Double.parseDouble(cmd[2]));
                }
            }
        }

        // Check if the command matches a valid command string.
        private boolean isValidCommandString(String cmd) {
            boolean valid = false;
            for (int i = 0; i < validCommands.length; i++) {
                if (cmd.equals(validCommands[i])) {
                    valid = true;
                    break;
                }
            }
            return valid;

        }
/*
        private boolean isValidNumberString(String value) {
            boolean valid = false;
            try {

            }
            catch () NumberFormatException e) {

            }
            finally {
                return valid;
            }
        }

*/      // Add the data to the database entries.
        // Runtime: O(1)
        private void set(String name, Double value) {
            entries.put(name, value);
        }

        // Get the value of the key from the database
        // Runtime: O(1)
        private Double get(String name) {
            return entries.get(name);
        }

        // Remove item from database
        private void unset(String name) {
            entries.remove(name);
        }

        /*
            Find the number of items equal to the value
            Runtime: O(n)
         */
        private int numEqualTo(Double value) {
            // Return value that keeps track of the number of matched entries
            int count = 0;

            for (Map.Entry<String, Double> entry : entries.entrySet()) {
                if (entry.getValue().equals(value)) {
                    count += 1;
                }
            }

            return count;
        }

        // Exit the program
        private void end() {
            System.exit(0);
        }

        /*
            Open a new transaction block.
            Transaction blocks can be nested; a BEGIN can be issued inside of an existing block

            Runtime: O(1)
         */
        private void begin() {
            // If there are no transactions, create a new Transaction Block
            if (blocks.isEmpty()) {
                blocks.add(new TransactionBlock());
            }
            // Else, add a new transaction block to the end
            else {
                blocks.addLast(new TransactionBlock());
            }
        }

        /*
           Close all open transaction blocks, permanently applying the changes made in them.
           Print nothing if successful, or print "NO TRANSACTION" if no transaction is in progress.

           Runtime: n = number of transaction blocks
                    m = number of commands in the transaction blocks cache

                    -> O(n * m)
        */
        private void commit() {
            // Check if there are any Transaction blocks
            if (blocks.isEmpty()) {
                System.out.println("NO TRANSACTION");
            }
            // Traverse the Transaction Blocks from first to last
            // Execute each command in its cache as you go (input has already been validated)
            else {
                while (!blocks.isEmpty()) {
                    TransactionBlock curBlock = blocks.getFirst();
                    int curBlockCacheSize = curBlock.cache.size();
                    for (int i = 0; i < curBlockCacheSize; i++) {
                        String curCommand = curBlock.cache.get(i);
                        executeDataCommand(curCommand);
                    }
                    blocks.removeFirst();
                }
            }

        }

        /*
            Undo all of the commands issued in the most recent transaction block, and close the block.
            Print nothing if successful, or print "NO TRANSACTION" if no transaction is in progress.
         */
        private void rollback() {
            if (blocks.isEmpty()) {
                System.out.println("NO TRANSACTION");
            }
            else {
                blocks.removeLast();
            }
        }
    }

    // A class for the transaction blocks.
    class TransactionBlock {
        private ArrayList<String> cache;

        public TransactionBlock() {
            cache = new ArrayList<>();
        }

        public void addToCache(String command) {
            cache.add(command);
        }
    }


    public static void main(String args[] ) throws Exception {
        Scanner input = new Scanner(System.in);
        Solution sol = new Solution();
        Solution.Database db = sol.new Database();
        while (input.hasNext()) {
            db.evaluateCommand(input.nextLine());

        }
        
    }
}