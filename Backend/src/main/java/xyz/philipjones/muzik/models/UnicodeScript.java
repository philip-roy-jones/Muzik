package xyz.philipjones.muzik.models;

import java.util.List;


// This class is a model for the UnicodeScript object
// It is used to store the UnicodeScript object from the JSON file
public class UnicodeScript {
    private String name;
    private List<List<String>> ranges;

    // Default constructor
    public UnicodeScript() {}

    // Only Getters because you will never need to set these values programatically

    public String getName() {
        return name;
    }

    public List<List<String>> getRanges() {
        return ranges;
    }
}
