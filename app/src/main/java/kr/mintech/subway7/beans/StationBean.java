package kr.mintech.subway7.beans;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class StationBean
{
  public String nameCn;
  public String nameKo;
  public String nameEn;
  public String stationId;
  public String line;
  public double lon = -1, lat = -1;
  
  public String beforeLine, afterLine;
  
  public ArrayList<StationExitBean> exits = new ArrayList<>();
  public String exitsJsonString = "";
  public ArrayList<String> lines = new ArrayList<String>();
  public boolean isDuplicate = false;
  
  
  public void setJSONObject(JSONObject object)
  {
    try
    {
      if (object.has("isDuplicate"))
        this.isDuplicate = object.getBoolean("isDuplicate");
      if (object.has("name_cn"))
        this.nameCn = object.getString("name_cn");
      if (object.has("name"))
        this.nameCn = object.getString("name");
      if (object.has("name_ko"))
        this.nameKo = object.getString("name_ko");
      if (object.has("name_en"))
        this.nameEn = object.getString("name_en");
      if (object.has("id"))
        this.stationId = object.getString("id");
      if (object.has("line"))
        this.line = object.getString("line");
      if (object.has("location"))
      {
        JSONObject locationObj = object.getJSONObject("location");
        try
        {
          if (locationObj.has("latitude"))
            this.lat = locationObj.getDouble("latitude");
          if (locationObj.has("longitude"))
            this.lon = locationObj.getDouble("longitude");
        }
        catch (Exception e)
        {
          
        }
      }
      
      if (object.has("before_station_line"))
        this.beforeLine = object.getString("before_station_line");
      if (object.has("after_station_line"))
        this.afterLine = object.getString("after_station_line");
      
      if (object.has("lines"))
      {
        JSONArray lineArray = object.getJSONArray("lines");
        for (int i = 0; i < lineArray.length(); i++)
        {
          lines.add(lineArray.getString(i));
        }
      }
      
      if (object.has("exits"))
      {
        exitsJsonString = object.getString("exits");
        JSONObject exitObject = object.getJSONObject("exits");
        for (int i = 1; i < 20; i++)
        {
          if (exitObject.has(Integer.toString(i)))
          {
            StationExitBean bean = new StationExitBean();
            bean.exitTitle = i;
            bean.lat = exitObject.getJSONObject(Integer.toString(i)).getDouble("latitude");
            bean.lon = exitObject.getJSONObject(Integer.toString(i)).getDouble("longitude");
            exits.add(bean);
          }
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  public class StationExitBean
  {
    public int exitTitle;
    public double lat, lon;
    public float distance;
  }
}
