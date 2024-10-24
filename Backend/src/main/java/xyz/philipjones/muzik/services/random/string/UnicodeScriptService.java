package xyz.philipjones.muzik.services.random.string;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.UnicodeScript;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Random;

@Service
public class UnicodeScriptService {

    private final String filePath = "src/main/resources/UnicodeScripts.json";
    private final List<UnicodeScript> unicodeScripts;

    public UnicodeScriptService() {
        this.unicodeScripts = setJson();
    }

    public List<UnicodeScript> getJson() {
        return this.unicodeScripts;
    }

    public UnicodeScript generateRandomScript() {
        Random random = new Random();
        List<UnicodeScript> unicodeScripts = getJson();

        return unicodeScripts.get(random.nextInt(unicodeScripts.size()));
    }

    public List<String> generateRandomRange(UnicodeScript unicodeScript) {
        Random random = new Random();
        List<List<String>> ranges = unicodeScript.getRanges();
//        System.out.println("Ranges:" + ranges);
        return ranges.get(random.nextInt(ranges.size()));
    }


    // Privates

    private List<UnicodeScript> setJson() {
        Gson gson = new Gson();
        try (BufferedReader br = new BufferedReader(new FileReader(this.filePath))) {
            Type rangeListType = new TypeToken<List<UnicodeScript>>() {
            }.getType();
            return gson.fromJson(br, rangeListType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}