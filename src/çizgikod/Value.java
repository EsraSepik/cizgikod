package çizgikod;

// Enum for ÇizgiKod language's supported data types
enum ÇizgiKodType {
    INT, STRING, BOOLEAN, FLOAT, CHAR, VOID, UNKNOWN
}

/**
 * Represents a value in the ÇizgiKod language, holding its type and actual data.
 * This class is crucial for the interpreter to manage variable values and expression results.
 */
public class Value {
    private ÇizgiKodType type; // The ÇizgiKod data type (e.g., INT, STRING)
    private Object data;      // The actual Java object holding the value (e.g., Integer, String, Boolean)

    // Constructor for creating a Value with a specified type and data
    public Value(ÇizgiKodType type, Object data) {
        this.type = type;
        this.data = data;
    }

    // Constructor for creating a VOID value (e.g., for statements that don't return a value)
    public Value() {
        this.type = ÇizgiKodType.VOID;
        this.data = null;
    }

    // Getter for the ÇizgiKodType
    public ÇizgiKodType getType() {
        return type;
    }

    // Getter for the raw Java object data
    public Object getData() {
        return data;
    }

    // Setter for the data (useful for assignments)
    public void setData(Object data) {
        this.data = data;
    }

    // Overrides toString for easy printing of Value objects
    @Override
    public String toString() {
        if (data == null) {
            return "null (" + type + ")";
        }
        return data.toString(); // Simply return the string representation of the data
    }

    // Helper methods to convert data to specific types safely
    public Integer asInt() {
        return (Integer) data;
    }

    public Double asFloat() {
        return (Double) data;
    }

    public String asString() {
        return (String) data;
    }

    public Boolean asBoolean() {
        return (Boolean) data;
    }
    // Note: char handling might need specific casting or parsing if it's stored as String
}
