package xyz.philipjones.muzik.models;

import java.util.List;


// This class is a model for the UnicodeRange object
// It is used to store the UnicodeRange object from the JSON file
public class UnicodeRange {
    private String name;
    private List<List<String>> ranges;

    // Default constructor
    public UnicodeRange() {}

    // Only Getters because you will never need to set these values programatically

    public String getName() {
        return name;
    }

    public List<List<String>> getRanges() {
        return ranges;
    }
}
