package de.wortmonster.jbdtrigger;
import org.json.*;
/** Learns start consumption from temperature-tagged rides. Recent live consumption still wins. */
final class TemperatureHistory {
    static void validate(JSONArray rides)throws Exception{if(rides.length()>100)throw new Exception("Zu viele Temperaturfahrten");for(int i=0;i<rides.length();i++){JSONObject r=rides.getJSONObject(i);double t=r.getDouble("temperature"),d=r.getDouble("km"),e=r.getDouble("wh");if(!Double.isFinite(t)||t< -40||t>60||!Double.isFinite(d)||d<1||!Double.isFinite(e)||e<=0||e/d>200)throw new Exception("Ungültige Temperaturfahrt");}}
    static double estimate(JSONArray rides,double temperature,double fallback){
        if(!Double.isFinite(temperature))return fallback;double wh=0,km=0;int count=0;
        for(int i=0;i<rides.length();i++){JSONObject r=rides.optJSONObject(i);if(r==null)continue;double difference=Math.abs(r.optDouble("temperature",Double.NaN)-temperature);if(difference>5||!Double.isFinite(difference))continue;
            double distance=r.optDouble("km",0),energy=r.optDouble("wh",0);if(distance<1||energy<=0||energy/distance>200)continue;double weight=1/(1+difference);wh+=energy*weight;km+=distance*weight;count++;
        }
        // Avoid a confident model based on one short trip.
        return count>=3&&km>=5?wh/km:fallback;
    }
}
