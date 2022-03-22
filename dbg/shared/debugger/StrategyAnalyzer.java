import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;


public class StrategyAnalyzer {
    public StrategyAnalyzer() {

    }

    public static boolean complies(HashMap<String, Object> obj, HashMap<String, Object> strategy) {
        // check if leaves comply, and combine

        if (strategy.entrySet().size() == 1) {
            // if it is a leaf, return its compliance
            if (!strategy.containsKey("OR") && !strategy.containsKey("AND") && !strategy.containsKey("NOT")) {
                boolean ret = false;
                String key = (String) strategy.keySet().toArray()[0];
                if (key.equals("id")) {
                    ret = (int) obj.get("id") == (int) strategy.get("id");
                }
                else if (key.equals("idMin")) {
                    ret = (int) obj.get("id") >= (int) strategy.get("idMin");
                }
                else if (key.equals("idMax")) {
                    ret = (int) obj.get("id") <= (int) strategy.get("idMax");
                }
                else if (key.equals("argc")) {
                    ret = (int) obj.get("argc") == (int) strategy.get("argc");
                }
                else if (key.equals("argcMin")) {
                    ret = (int) obj.get("argc") >= (int) strategy.get("argcMin");
                }
                else if (key.equals("argcMax")) {
                    ret = (int) obj.get("argc") <= (int) strategy.get("argcMax");
                }
                else if (key.equals("class")) {
                    ret = ((String) obj.get("class")).matches((String) strategy.get("class"));
                }
                else if (key.equals("name")) {
                    ret = ((String) obj.get("name")).matches((String) strategy.get("name"));
                }
                else if (key.equals("argstypes")) {
                    ret = false;
                    for (String s : (List<String>) obj.get("argstypes")) {
                        if (((String) strategy.get("argstypes")).matches(s)) {
                            ret = true;
                        }
                    }
                }
                return ret;
            } else if (strategy.containsKey("OR")) {
                ArrayList<HashMap<String, Object>> content = (ArrayList<HashMap<String, Object>>) strategy.get("OR");
                for (HashMap<String, Object> c : content) {
                    if (complies(obj, c)) {
                        return true;
                    }
                        
                }
                return false;
            } else if (strategy.containsKey("AND")) {
                ArrayList<HashMap<String, Object>> content = (ArrayList<HashMap<String, Object>>) strategy.get("AND");
                for (HashMap<String, Object> c : content) {
                    if (!complies(obj, c)){
                        return false;
                    }
                }
                return true;
            } else if (strategy.containsKey("NOT")) {
                boolean ret = !complies(obj, (HashMap)strategy.get("NOT"));
                return ret;
            }

        }
        return false;
    }


}
