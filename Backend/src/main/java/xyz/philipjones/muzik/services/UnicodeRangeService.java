package xyz.philipjones.muzik.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.UnicodeRange;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Random;

@Service
public class UnicodeRangeService {

    private final String filePath = "src/main/resources/UnicodeRanges.json";
    private List<UnicodeRange> unicodeRange;

    public UnicodeRangeService() {
        this.unicodeRange = setJson();
    }

    public List<UnicodeRange> getJson() {
        return this.unicodeRange;
    }

    public UnicodeRange generateRandomRangeGroup() {
        Random random = new Random();
        List<UnicodeRange> unicodeRanges = getJson();

        return unicodeRanges.get(random.nextInt(unicodeRanges.size()));
    }

    public List<String> generateRandomRange(UnicodeRange unicodeRange) {
        Random random = new Random();
        List<List<String>> ranges = unicodeRange.getRanges();

        return ranges.get(random.nextInt(ranges.size()));
    }


    // Privates

    private List<UnicodeRange> setJson() {
        Gson gson = new Gson();
        try (BufferedReader br = new BufferedReader(new FileReader(this.filePath))) {
            Type rangeListType = new TypeToken<List<UnicodeRange>>() {
            }.getType();
            return gson.fromJson(br, rangeListType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}