/**
 * Created by jsookikian on 1/26/17.
 */
import java.io.*;
import java.util.*;
import java.text.*;
import java.math.*;
import java.util.regex.*;

public class Solution {
    private enum COMMAND_TYPE {
        DATA_COMMAND, TRANSACTION_COMMAND, END_COMMAND
    }

    private enum COMMAND_NAME {
        SET, GET, UNSET, NUMEQUALTO, BEGIN, ROLLBACK, COMMIT, END
    }

    private final static boolean PRINT_ON = true;
    private final static boolean PRINT_OFF = false;
    class Parser {

        public Command parseInput(String input) throws InvalidCommandException {
            String[] cmd = input.split("\\s+");
            COMMAND_NAME name;
            COMMAND_TYPE type;
            if (!isCommandString(cmd[0])) {
                throw new InvalidCommandException();
            }

            if (isEndProgramCommand(cmd[0])) {
                return new Command(COMMAND_NAME.END, COMMAND_TYPE.END_COMMAND, null, null);
            }

            if (isTransactionCommand(cmd[0])) {
                name = getTransactionCommandName(cmd[0]);
                type = COMMAND_TYPE.TRANSACTION_COMMAND;

                return new Command(name, type, null, null);
            }

            // We can assume the command is a data command at this point
            Command toReturn = parseDataCommand(cmd);
            if (toReturn == null) {
                throw new InvalidCommandException();
            }
            else {
                return toReturn;

            }

        }

        private Command parseDataCommand(String[] cmd) {
            COMMAND_TYPE type = COMMAND_TYPE.DATA_COMMAND;
            COMMAND_NAME name = getDataCommandName(cmd[0]);
            switch(name) {
                case SET:
                    if (cmd.length >= 3) {
                        // SET [name] [value]
                        return new Command(name, type, cmd[1], cmd[2]);
                    }
                    else {
                        return null;
                    }
                    // Both cases follow the same rules: [command] [name]
                case GET:
                case UNSET:
                    if (cmd.length >= 2) {
                        // UNSET [name]
                        // GET [name]
                        return new Command(name, type, cmd[1], null);
                    }
                    else {
                        return null;
                    }
                case NUMEQUALTO:
                    if (cmd.length >= 2) {
                        // NUMEQUALTO [value]
                        return new Command(name, type, cmd[1], null);
                    }
                    else {
                        return null;
                    }
                default:
                    return null;
            }
        }

        private boolean isCommandString(String command) {
            return command.matches("\\b(SET|GET|UNSET|NUMEQUALTO|BEGIN|ROLLBACK|COMMIT|END)\\b");
        }

        private boolean isDataCommand(String command) {
            return command.matches("\\b(SET|GET|UNSET|NUMEQUALTO)\\b");
        }

        private boolean isTransactionCommand(String command) {
            return command.matches("\\b(BEGIN|ROLLBACK|COMMIT)\\b");
        }

        private boolean isEndProgramCommand(String command) {
            return command.matches("\\b(END)\\b");
        }

        // Returns the enum value for the name of the command
        private COMMAND_NAME getTransactionCommandName(String command) {
            if (command.equals("BEGIN")) {
                return COMMAND_NAME.BEGIN;
            }
            else if (command.equals("ROLLBACK")) {
                return COMMAND_NAME.ROLLBACK;
            }
            else {
                return COMMAND_NAME.COMMIT;
            }
        }

        // Returns the enum value for the name of the command
        private COMMAND_NAME getDataCommandName(String command) {
            if (command.equals("SET")) {
                return COMMAND_NAME.SET;
            }
            else if (command.equals("GET")) {
                return COMMAND_NAME.GET;
            }
            else if (command.equals("UNSET")) {
                return COMMAND_NAME.UNSET;
            }
            else {
                return COMMAND_NAME.NUMEQUALTO;
            }
        }
    }

    class Database {

        private HashMap<String, String> entries; // Variable for Database entries
        private LinkedList<TransactionBlock> blocks; // Transaction Blocks pointer
        private Parser parser;

        // Initialize database entries hashmap and Transaction Block list
        public Database() {
            entries = new HashMap<String, String>();
            blocks = new LinkedList<TransactionBlock>();
            parser = new Parser();
        }


        private void evaluateCommand(String input) {
            try {
                Command cmd = parser.parseInput(input);
                System.out.println(input);
                switch (cmd.type) {
                    case DATA_COMMAND:
                        if (transactionInProgress()) {
                            AddTransaction(cmd);
                        }
                        else {
                            // Use
                            executeDataCommand(cmd, PRINT_ON);
                        }
                        break;
                    case TRANSACTION_COMMAND:
                        executeTransactionCommand(cmd);
                        break;
                    case END_COMMAND:
                        end();
                        break;
                }
            }
            catch (InvalidCommandException e) {
                System.out.println("INVALID COMMAND");
            }

        }

        /* A function to execute the data commands
           The print flag is used to withold printing commands that are sent from
           the commit operation, because the results printed would be ambiguous as to what the values
           correspond to */
        private void executeDataCommand(Command cmd, boolean printFlag) {
            switch(cmd.name) {
                case GET:
                    String value = get(cmd.arg1);
                    // Check the value returned from the database
                    if (value == null) {
                        value = "NULL";
                    }
                    if (printFlag) {
                        System.out.println("> " + value);
                    }
                    break;
                case UNSET:
                    unset(cmd.arg1);
                    break;
                case NUMEQUALTO:
                    int equalTo =  numEqualTo(cmd.arg1);
                    if (printFlag) {
                        System.out.println("> " + equalTo);
                    }
                    break;
                case SET:
                    set(cmd.arg1, cmd.arg2);
                    break;
            }
        }

        private void executeTransactionCommand(Command cmd) {
            switch (cmd.name) {
                // Run begin command
                case BEGIN:
                    begin();
                    break;
                // Run rollback command
                case ROLLBACK:
                    if (!rollback()) {
                        System.out.println("> NO TRANSACTION");
                    }
                    break;
                // Run Commit command
                case COMMIT:
                    // If commit fails, print NO TRANSACTION
                    if (!commit()) {
                        System.out.println("> NO TRANSACTION");
                    }
                    break;
            }
        }

        public boolean transactionInProgress() {
            return !blocks.isEmpty();
        }

        // Add the data command to the most recent Transaction Block's cache
        private boolean AddTransaction(Command command) {
            return blocks.getLast().addToCache(command);
        }


        // Add the data to the database entries.
        private void set(String name, String value) {
            entries.put(name, value);

        }

        // Get the value of the key from the database
        private String get(String name) {
            return entries.get(name);
        }

        // Remove item from database
        private void unset(String name) {
                entries.remove(name);
        }

        // Find the number of items equal to the value
        private int numEqualTo(String value) {
            // Return value that keeps track of the number of matched entries
            int count = 0;
            /*
                Iterate through all values in the database.
                If a match is found, increment the count.
            */
            for (Map.Entry<String, String> entry : entries.entrySet()) {
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

        private boolean commit() {
            // Check if there are any Transaction blocks to commit
            if (blocks.isEmpty()) {
                return false;
            }
            // Traverse the Transaction Blocks from first to last
            // Execute each command in its cache as you go (input has already been validated)
            else {
                while (!blocks.isEmpty()) {
                    TransactionBlock curBlock = blocks.getFirst();
                    int curBlockCacheSize = curBlock.cache.size();
                    for (int i = 0; i < curBlockCacheSize; i++) {
                        Command curCommand = curBlock.cache.get(i);
                        executeDataCommand(curCommand, PRINT_OFF);
                    }
                    blocks.removeFirst();
                }
                return true;
            }
        }

        private boolean rollback() {
            // If there are no transactions in progress, return false
            if (blocks.isEmpty()) {
                return false;
            }
            // Else, remove most recent transaction block
            else {
                blocks.removeLast();
                return true;
            }
        }
    }

    class TransactionBlock {
        private ArrayList<Command> cache;

        public TransactionBlock() {
            cache = new ArrayList<>();
        }

        public boolean addToCache(Command command) {
            try {
                cache.add(command);
                return true;
            }
            catch (Exception e) {
                return false;
            }
        }
    }

    class Command {
        public COMMAND_NAME name; //
        public COMMAND_TYPE type; // Transaction, Data, or End command
        public String arg1; // First argument for the command
        public String arg2; // Second argument for the command


        public Command(COMMAND_NAME name, COMMAND_TYPE type, String arg1, String arg2) {
            this.name = name;
            this.type = type;
            this.arg1 = arg1;
            this.arg2 = arg2;
        }
    }

    class InvalidCommandException extends Exception {
        public InvalidCommandException() {
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