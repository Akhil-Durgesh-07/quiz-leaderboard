import java.net.*;
import java.io.*;
import java.util.*;
import org.json.*;

public class Main {

    public static void main(String[] args) throws Exception {

        String regNo = "RA2311028010169";

        Set<String> seen = new HashSet<>();
        Map<String, Integer> scores = new HashMap<>();

        for (int i = 0; i < 10; i++) {

            String urlStr = "https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/messages?regNo="
                    + regNo + "&poll=" + i;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(response.toString());
            JSONArray events = json.getJSONArray("events");

            for (int j = 0; j < events.length(); j++) {

                JSONObject event = events.getJSONObject(j);

                String roundId = event.getString("roundId");
                String participant = event.getString("participant");
                int score = event.getInt("score");

                String key = roundId + "-" + participant;

                if (!seen.contains(key)) {
                    seen.add(key);

                    scores.put(participant,
                            scores.getOrDefault(participant, 0) + score);
                }
            }

            System.out.println("Poll " + i + " done");

            Thread.sleep(5000); // IMPORTANT
        }

        // Convert to leaderboard list
        List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());

        list.sort((a, b) -> b.getValue() - a.getValue());

        int total = 0;

        JSONArray leaderboard = new JSONArray();

        for (Map.Entry<String, Integer> entry : list) {

            JSONObject obj = new JSONObject();
            obj.put("participant", entry.getKey());
            obj.put("totalScore", entry.getValue());

            leaderboard.put(obj);

            total += entry.getValue();
        }

        System.out.println("Total Score: " + total);
        System.out.println("Leaderboard: " + leaderboard.toString());

        // SUBMIT
        URL url = new URL("https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/submit");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JSONObject submit = new JSONObject();
        submit.put("regNo", regNo);
        submit.put("leaderboard", leaderboard);

        OutputStream os = conn.getOutputStream();
        os.write(submit.toString().getBytes());
        os.flush();
        os.close();

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));

        String output;
        StringBuilder responseSubmit = new StringBuilder();
        String line2;

        while ((line2 = br.readLine()) != null) {
            responseSubmit.append(line2);
        }

        System.out.println("Submit Response: " + responseSubmit.toString());

        conn.disconnect();
    }
}