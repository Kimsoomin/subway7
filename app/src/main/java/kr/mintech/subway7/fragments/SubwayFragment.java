package kr.mintech.subway7.fragments;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import kr.mintech.subway7.R;
import kr.mintech.subway7.SubwayStationsActivity;
import kr.mintech.subway7.beans.StationBean;
import kr.mintech.subway7.controllers.SubwayManager;


@SuppressWarnings("deprecation")
@SuppressLint("SetJavaScriptEnabled")
public class SubwayFragment extends Fragment
{
  private static int LCOATION_TIME_OUT_SECOND = 30 * 1000;
  
  private static View view;
  private WebView webview;
  private Handler handler = new Handler();
  private LinearLayout stationsInfoLayout;
  private TextView stationsInfoText;
  private TextView startStationName, endStationName;
  private ImageView startStationImage, endStationImage;
  
  private ArrayList<StationBean> stations = new ArrayList<>();
  @SuppressWarnings("unused")
  private ImageView detailStationInfo;
  private boolean isLoadEnded;
  private Runnable afterLoadSubwaysRunnable;
  
  //출발역 찾기 때문에 사용 
  private static double startStationLat = -1, startStationLong = -1;
  private static String startStationId, endStationId;
  
  private LinearLayout btnFindFirstStation;
  
  private double findNearByStationLat = -1, findNearByStationLon = -1;
  private static double setDestFindNearStation = -1;
  private String destName;
  
  private ProgressBar progressBar;
  private Button btnMap, btnNearByStation;
  
  private LocationManager locationManager;
  private LocationListener locationListener;
  
  private Activity activity;
  
  private LinearLayout containerNearByStationInfo;
  private LinearLayout nearStationImage;
  private TextView nearStationName;
  private ImageView nearStationX;
  
  private double nearByLat = -1, nearByLon = -1;
  private boolean isZoomed = false;
  private int defaultZoom = 300;
  
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    if (view == null)
    {
      int resId = R.layout.fragment_subway;
      view = inflater.inflate(resId, null);
    }
    return view;
  }
  
  
  public static void viewClear()
  {
    view = null;
    startStationLat = -1;
    startStationLong = -1;
    setDestFindNearStation = -1;
  }
  
  
  public void findNearByStation(double lat, double lon)
  {
    Log.w("WARN", "지하철 찾기로 진입함");
    findNearByStationLat = lat;
    findNearByStationLon = lon;
    startStationLat = lat;
    startStationLong = lon;
  }
  
  
  public void findNearByStation(double lat, double lon, double type, String destName)
  {
    Log.w("WARN", "지하철을 찾고 출도착역으로 설정 " + lat + " / " + lon + " / " + type);
    findNearByStationLat = lat;
    findNearByStationLon = lon;
    setDestFindNearStation = type;
    this.destName = destName;
    
    try
    {
      if (findNearByStationLat != -1)
      {
        //가까운 지하철역을 찾아야 함
        if (setDestFindNearStation == 0)
          webview.loadUrl("javascript:subway.findNearByStation('" + findNearByStationLat + "', '" + findNearByStationLon + "')");
        else
        {
          if (setDestFindNearStation == 1)
          {
            nearByLat = findNearByStationLat;
            nearByLon = findNearByStationLon;
          }
          webview.loadUrl("javascript:subway.findNearByStation('" + findNearByStationLat + "', '" + findNearByStationLon + "')");
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  
  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  @Override
  public void onAttach(final Activity activity)
  {
    this.activity = activity;
    if (view == null)
      loadMapView();
    super.onAttach(activity);
  }
  
  
  @Override
  public void onResume()
  {
    if (SubwayManager.getInstance(getActivity()).stations.size() == 0)
      loadAllStations(null);
    super.onResume();
  }
  
  
  @SuppressLint("NewApi")
  private void loadMapView()
  {
    startStationId = "";
    endStationId = "";
    int resId = R.layout.fragment_subway;
    view = LayoutInflater.from(activity).inflate(resId, null);
    
    ((LinearLayout) view.findViewById(R.id.top_info_layout)).bringToFront();
    
    stationsInfoLayout = (LinearLayout) view.findViewById(R.id.stations_info);
    stationsInfoText = (TextView) view.findViewById(R.id.text_stations_info);
    detailStationInfo = (ImageView) view.findViewById(R.id.image_stations_info_detail);
    btnFindFirstStation = (LinearLayout) view.findViewById(R.id.layout_find_start_station);
    progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
    
    startStationName = (TextView) view.findViewById(R.id.start_station_name);
    endStationName = (TextView) view.findViewById(R.id.end_station_name);
    startStationImage = (ImageView) view.findViewById(R.id.start_station_image);
    endStationImage = (ImageView) view.findViewById(R.id.end_station_image);
    
    containerNearByStationInfo = (LinearLayout) view.findViewById(R.id.container_near_station_info);
    nearStationImage = (LinearLayout) view.findViewById(R.id.near_station_line);
    nearStationName = (TextView) view.findViewById(R.id.near_station_name);
    nearStationX = (ImageView) view.findViewById(R.id.image_x);
    
    webview = (WebView) view.findViewById(R.id.webview);
    
    webview.getSettings().setJavaScriptEnabled(true);
    if (Build.VERSION.SDK_INT >= 16)
    {
      webview.getSettings().setAllowFileAccessFromFileURLs(true);
      webview.getSettings().setAllowUniversalAccessFromFileURLs(true);
    }
    
    webview.getSettings().setUseWideViewPort(true);
    webview.getSettings().setBuiltInZoomControls(true);
    webview.getSettings().setDisplayZoomControls(false);
    webview.getSettings().setAppCacheEnabled(true);
    webview.getSettings().setSupportZoom(true);
    if (Build.VERSION.SDK_INT > 18)
    {
      webview.getSettings().setDomStorageEnabled(true);
      webview.getSettings().setRenderPriority(RenderPriority.HIGH);
      webview.getSettings().setAppCacheMaxSize(10 * 1024 * 1024);
      webview.getSettings().setAppCachePath(activity.getApplicationContext().getCacheDir().getAbsolutePath());
      webview.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
      webview.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
      
    }
    webview.addJavascriptInterface(new JavaScriptInterface(), "myClient");
    webview.setWebViewClient(webViewClient);
    webview.setWebChromeClient(webChromeClient);
    
    webview.loadUrl("file:///android_asset/subway.html");
    
    btnMap = (Button) view.findViewById(R.id.btn_map);
    btnNearByStation = (Button) view.findViewById(R.id.btn_near_by_station);
    btnMap.setVisibility(View.GONE);
    btnNearByStation.setVisibility(View.GONE);
    
    locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
    locationListener = new SubwayLocationListener();
//
//    btnMap.setOnClickListener(new OnClickListener()
//    {
//      @Override
//      public void onClick(View arg0)
//      {
//        MapCheckUtil.checkMapExist(activity, new Runnable()
//        {
//          @Override
//          public void run()
//          {
//            Intent i = new Intent(activity, BlinkingMap.class);
//            startActivity(i);
//          }
//        });
//      }
//    });
    btnNearByStation.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View arg0)
      {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
          buildAlertMessageNoGps();
        else
        {
          btnNearByStation.setEnabled(false);
          progressBar.setVisibility(View.VISIBLE);
          progressBar.bringToFront();
          
          handler.postDelayed(timeOutFindStationCallBack, LCOATION_TIME_OUT_SECOND);
          locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
          locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
      }
    });
    
    progressBar.setVisibility(View.VISIBLE);
    progressBar.bringToFront();
    if (SubwayManager.getInstance(getActivity()).stations.size() == 0)
      loadAllStations(null);
  }
  
  private Runnable timeOutFindStationCallBack = new Runnable()
  {
    public void run()
    {
      btnNearByStation.setEnabled(true);
      locationManager.removeUpdates(locationListener);
      progressBar.setVisibility(View.GONE);
      Builder builder = new Builder(getActivity());
      builder.setTitle(getString(R.string.app_name));
      builder.setMessage(getString(R.string.map_gps_Location_fail));
      builder.setPositiveButton(android.R.string.ok, null);
      builder.create().show();
    }
  };
  
  
  private void buildAlertMessageNoGps()
  {
    final Builder builder = new Builder(activity);
    builder.setTitle(R.string.app_name).setMessage(R.string.msg_please_gps_enable).setCancelable(false).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
    {
      public void onClick(final DialogInterface dialog, final int id)
      {
        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
      }
    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
    {
      public void onClick(final DialogInterface dialog, final int id)
      {
        dialog.cancel();
      }
    });
    final AlertDialog alert = builder.create();
    alert.show();
  }
  
  private class SubwayLocationListener implements LocationListener
  {
    
    @Override
    public void onLocationChanged(Location loc)
    {
      handler.removeCallbacks(timeOutFindStationCallBack);
      Log.w("WARN", "LocationChanged! " + loc.getLatitude() + " / " + loc.getLongitude());
      locationManager.removeUpdates(locationListener);
      double max_latitude = 37.70453488762476;
      double max_longitude = 127.17361450195312;
      double min_latitude = 37.43677099171195;
      double min_longitude = 126.76300048828125;
      
      if (loc.getLongitude() > max_longitude || loc.getLongitude() < min_longitude || loc.getLatitude() > max_latitude || loc.getLatitude() < min_latitude)
        showDontSupportOutsideSeoul();
      else
      {
        setDestFindNearStation = -1;
        webview.loadUrl("javascript:subway.findNearByStation('" + loc.getLatitude() + "', '" + loc.getLongitude() + "')");
      }
      btnNearByStation.setEnabled(true);
      progressBar.setVisibility(View.GONE);
    }
    
    
    @Override
    public void onProviderDisabled(String provider)
    {
    }
    
    
    @Override
    public void onProviderEnabled(String provider)
    {
    }
    
    
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
    }
  }
  
  
  public void loadAllStations(Runnable run)
  {
    this.afterLoadSubwaysRunnable = run;
    Log.w("WARN", "모든 지하철역 요청");
    if (webview == null)
      loadMapView();
    else
    {
      String jsString = null;
      try
      {
        InputStream is = getActivity().getAssets().open("subway_data.js");
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        jsString = new String(buffer, "UTF-8");
        
        String[] jsStrings = jsString.split("\n");
        
        String stationsString = jsStrings[1];
        stationsString = stationsString.replaceFirst("station_ids = ", "");
        JSONArray stations = new JSONArray(stationsString);
        
        String stationsInfoString = jsStrings[0];
        stationsInfoString = stationsInfoString.replaceFirst("stations = ", "");
        
        JSONObject stationInfoJSONObject = new JSONObject(stationsInfoString);
        
        ArrayList<StationBean> tempArray = new ArrayList<StationBean>();
        for (int i = 0; i < stations.length(); i++)
        {
          try
          {
            JSONObject obj = stationInfoJSONObject.getJSONObject(stations.getString(i));
            StationBean bean = new StationBean();
            bean.setJSONObject(obj);
            tempArray.add(bean);
          }
          catch (Exception e)
          {
            e.printStackTrace();
          }
        }
        Log.w("WARN", "Stations size : " + tempArray.size());
        SubwayManager.getInstance(activity).stations.clear();
        SubwayManager.getInstance(activity).stations.addAll(tempArray);
        if (afterLoadSubwaysRunnable != null)
          afterLoadSubwaysRunnable.run();
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
      }
      catch (JSONException e)
      {
        e.printStackTrace();
      }
    }
    
  }
  
  
  public boolean isLoadEnded()
  {
    return isLoadEnded;
  }
  
  
  public void findStation(final String stationId, String stationName)
  {
    handler.post(new Runnable()
    {
      @Override
      public void run()
      {
        webview.loadUrl("javascript:showDestinationPopUp('" + stationId + "')");
      }
    });
    
  }
  
  private WebViewClient webViewClient = new WebViewClient()
  {
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
    {
      //Log.w("MainActivity.java | onReceivedError", "|" + errorCode + "|" + description + "|" + failingUrl + "|");
      super.onReceivedError(view, errorCode, description, failingUrl);
    }
  };
  
  private WebChromeClient webChromeClient = new WebChromeClient()
  {
    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage)
    {
      //Log.w("MainActivity.java | onConsoleMessage", "" + consoleMessage + "|" + consoleMessage.message() + "|" + consoleMessage.sourceId() + "|");
      return super.onConsoleMessage(consoleMessage);
    }
    
    
    @Override
    public boolean onJsAlert(WebView view, String url, String message, JsResult result)
    {
//      Log.w("MainActivity.java | onJsAlert", "" + url + "|" + message + "|" + result + "|");
      return super.onJsAlert(view, url, message, result);
    }
  };
  
  
  private void showFindStartStation(final String stationId)
  {
    Log.w("WARN", "show find start station : " + stationId);
    btnFindFirstStation.setVisibility(View.VISIBLE);
    btnFindFirstStation.bringToFront();
    btnFindFirstStation.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View arg0)
      {
        StationBean stationBean = SubwayManager.getInstance(activity).findStation(startStationId);
        
        if (stationBean != null)
        {
          double max_latitude = 37.70453488762476;
          double max_longitude = 127.17361450195312;
          double min_latitude = 37.43677099171195;
          double min_longitude = 126.76300048828125;
          
          if (stationBean.lon > max_longitude || stationBean.lon < min_longitude || stationBean.lat > max_latitude || stationBean.lat < min_latitude)
            showDontSupportOutsideSeoul();
          else
          {
//            MapCheckUtil.checkMapExist(activity, new Runnable()
//            {
//              @Override
//              public void run()
//              {
//                Intent i = new Intent(activity, BlinkingMap.class);
//                i.putExtra("lineId", stationId);
//                i.putExtra("Latitude", startStationLat);
//                i.putExtra("Longitude", startStationLong);
//                startActivity(i);
//              }
//            });
          }
        }
        else
        {
          loadAllStations(new Runnable()
          {
            @Override
            public void run()
            {
              showFindStartStation(startStationId);
            }
          });
        }
      }
    });
  }
  
  
  /*
   * AlertDialog
   */
  private void showDontSupportOutsideSeoul()
  {
    Builder builder = new Builder(getActivity());
    builder.setTitle(getString(R.string.app_name));
    builder.setMessage(getString(R.string.msg_dont_support_outside_seoul));
    builder.setPositiveButton(android.R.string.ok, null);
    builder.create().show();
  }
  
  
  private void showDialogChangeConfirmStation(final boolean isSetDestination, final String stationId)
  {
    //isSetDestination true : 시작역이 이미 도착역으로 지정된 경우 
    ///isSetDestination false :도착역이 이미 시작역으로 지정된 경우
    if (SubwayManager.getInstance(activity).stations.size() == 0)
    {
      loadAllStations(new Runnable()
      {
        @Override
        public void run()
        {
          showDialogChangeConfirmStation(isSetDestination, stationId);
        }
      });
    }
    else
    {
      Builder builder = new Builder(getActivity());
      builder.setTitle(getString(R.string.app_name));
      if (isSetDestination)
        builder.setMessage(getString(R.string.msg_same_end_start_station));
      else
        builder.setMessage(getString(R.string.msg_same_start_end_station));
      builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
      {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
          btnFindFirstStation.setVisibility(View.GONE);
          if (isSetDestination)
          {
            startStationId = stationId;
            endStationId = null;
            webview.loadUrl("javascript:subway.clear_end_station()");
            stationsInfoLayout.setVisibility(View.GONE);
            
            findNearByStationLat = -1;
            findNearByStationLon = -1;
            if (SubwayManager.getInstance(activity).getLatitudeWithSubwayId(stationId) != -1)
            {
              double lat = SubwayManager.getInstance(activity).getLatitudeWithSubwayId(stationId);
              double lon = SubwayManager.getInstance(activity).getLongitudeWithSubwayId(stationId);
              Log.w("WARN", "출발역 경위도 : " + lat + " / " + lon);
              startStationLat = lat;
              startStationLong = lon;
            }
            
            webview.loadUrl("javascript:subway.set_start_station('" + stationId + "')");
          }
          else
          {
            findNearByStationLat = -1;
            findNearByStationLon = -1;
            
            startStationId = null;
            webview.loadUrl("javascript:subway.clear_start_station()");
            stationsInfoLayout.setVisibility(View.GONE);
            
            endStationId = stationId;
            destName = "";
            Log.w("WARN", "StationId: " + stationId);
            webview.loadUrl("javascript:subway.set_end_station('" + stationId + "')");
          }
        }
      });
      builder.setNegativeButton(android.R.string.cancel, null);
      builder.create().show();
    }
  }
  
  private class JavaScriptInterface
  {
    @JavascriptInterface
    public void completeLoadAllStation(String stationsJSONString)
    {
      Log.w("WARN", "모든 지하철역 가져와서 메모리에 저장함 : " + stationsJSONString);
    }
    
    
    @JavascriptInterface
    public void setStartStation(final String stationId)
    {
      handler.post(new Runnable()
      {
        @Override
        public void run()
        {
          if (!TextUtils.isEmpty(endStationId))
          {
            if (endStationId.equals(stationId))
            {
              //시작역이 이미 도착역으로 지정된 경우 
              showDialogChangeConfirmStation(true, stationId);
              return;
            }
          }
          
          if (SubwayManager.getInstance(activity).stations.size() == 0)
          {
            loadAllStations(new Runnable()
            {
              @Override
              public void run()
              {
                setStartStation(stationId);
              }
            });
          }
          else
          {
            findNearByStationLat = -1;
            findNearByStationLon = -1;
            startStationId = stationId;
            if (SubwayManager.getInstance(activity).getLatitudeWithSubwayId(stationId) != -1)
            {
              double lat = SubwayManager.getInstance(activity).getLatitudeWithSubwayId(stationId);
              double lon = SubwayManager.getInstance(activity).getLongitudeWithSubwayId(stationId);
              Log.w("WARN", "출발역 경위도 : " + lat + " / " + lon);
              startStationLat = lat;
              startStationLong = lon;
            }
            
            webview.loadUrl("javascript:subway.set_start_station('" + stationId + "')");
          }
        }
      });
    }
    
    
    @JavascriptInterface
    public void setEndStation(final String stationId)
    {
      handler.post(new Runnable()
      {
        @Override
        public void run()
        {
          if (!TextUtils.isEmpty(startStationId))
          {
            if (startStationId.equals(stationId))
            {
              //도착역이 이미 시작역으로 지정된 경우
              showDialogChangeConfirmStation(false, stationId);
              return;
            }
          }
          
          findNearByStationLat = -1;
          findNearByStationLon = -1;
          endStationId = stationId;
          destName = "";
          Log.w("WARN", "StationId: " + stationId);
          webview.loadUrl("javascript:subway.set_end_station('" + stationId + "')");
        }
      });
    }
    
    
    @JavascriptInterface
    public void onSvgLoadEnded()
    {
      Log.w("WARN", "SVG 로드 완료");
      isLoadEnded = true;
      
      handler.post(new Runnable()
      {
        @Override
        public void run()
        {
          progressBar.setVisibility(View.GONE);
          defaultZoom = (int) (webview.getScale() * 100);
          Log.w("WARN", "Default Zoom : " + defaultZoom);
          btnMap.setVisibility(View.VISIBLE);
          btnNearByStation.setVisibility(View.VISIBLE);
          btnNearByStation.bringToFront();
          btnMap.bringToFront();
          
          if (findNearByStationLat != -1)
          {
            //가까운 지하철역을 찾아야 함
            if (setDestFindNearStation == 0)
              webview.loadUrl("javascript:subway.findNearByStation('" + findNearByStationLat + "', '" + findNearByStationLon + "')");
            else
            {
              if (setDestFindNearStation == 1)
              {
                nearByLat = findNearByStationLat;
                nearByLon = findNearByStationLon;
              }
              webview.loadUrl("javascript:subway.findNearByStation('" + findNearByStationLat + "', '" + findNearByStationLon + "')");
            }
          }
        }
      });
      
    }
    
    
    @JavascriptInterface
    public void completeMarkStation(final String stationsJSONString, final int time)
    {
      Log.w("WARN", "지하철 출도착 마킹 완료 : " + stationsJSONString + " ,time : " + time);
      if (SubwayManager.getInstance(activity).stations.size() == 0)
      {
        handler.post(new Runnable()
        {
          @Override
          public void run()
          {
            loadAllStations(new Runnable()
            {
              @Override
              public void run()
              {
                completeMarkStation(stationsJSONString, time);
              }
            });
          }
        });
      }
      else
      {
        stations.clear();
        JSONArray stationsJsonArray;
        try
        {
          stationsJsonArray = new JSONArray(stationsJSONString);
          for (int i = 0; i < stationsJsonArray.length(); i++)
          {
            StationBean bean = new StationBean();
            bean.setJSONObject(stationsJsonArray.getJSONObject(i));
            stations.add(bean);
          }
        }
        catch (JSONException e)
        {
          e.printStackTrace();
        }
        
        Log.w("WARN", "마킹 스테이션 사이즈 : " + stations.size());
        
        handler.post(new Runnable()
        {
          @Override
          public void run()
          {
            try
            {
              stationsInfoLayout.setVisibility(View.VISIBLE);
              stationsInfoLayout.bringToFront();
              containerNearByStationInfo.setVisibility(View.GONE);
              
              StationBean firstFindStationBean = stations.get(0);
              
              StationBean firstStationBean = stations.get(0);
              StationBean lastStationBean = stations.get(stations.size() - 1);
              
              startStationName.setText(firstStationBean.nameCn);
              endStationName.setText(lastStationBean.nameCn);
              if (Locale.getDefault().getLanguage().contains("ko"))
              {
                startStationName.setText(firstStationBean.nameKo);
                endStationName.setText(lastStationBean.nameKo);
              }
              if (firstStationBean.line.contains("환승"))
                startStationImage.setImageResource(SubwayManager.getInstance(activity).getSubwayLineResourceId(firstStationBean.beforeLine));
              else
                startStationImage.setImageResource(SubwayManager.getInstance(activity).getSubwayLineResourceId(firstStationBean.line));
              if (lastStationBean.line.contains("환승"))
                endStationImage.setImageResource(SubwayManager.getInstance(activity).getSubwayLineResourceId(lastStationBean.beforeLine));
              else
                endStationImage.setImageResource(SubwayManager.getInstance(activity).getSubwayLineResourceId(lastStationBean.line));
              
              String stationsInfoString = Integer.toString(stations.size() - 1) + "个站预计" + Integer.toString(time) + "分钟到达。";
              if (Locale.getDefault().getLanguage().contains("ko"))
                stationsInfoString = Integer.toString(stations.size() - 1) + "개 역의 이동시간은 " + Integer.toString(time) + "분입니다";
              stationsInfoText.setText(stationsInfoString);
              final String infoString = stationsInfoString;
              
              stationsInfoLayout.setOnClickListener(new OnClickListener()
              {
                @Override
                public void onClick(View arg0)
                {
                  Intent i = new Intent(activity, SubwayStationsActivity.class);
                  i.putExtra("stations_info", infoString);
                  i.putExtra("stations_json", stationsJSONString);
                  if (nearByLat != -1)
                  {
                    i.putExtra("near_by_lat", nearByLat);
                    i.putExtra("near_by_lon", nearByLon);
                    if (!TextUtils.isEmpty(destName))
                      i.putExtra("dest_name", destName);
                  }
                  activity.startActivity(i);
                }
              });
              
              Log.w("WARN", "Findstation line Id : " + firstFindStationBean.stationId);
              showFindStartStation(firstFindStationBean.stationId);
            }
            catch (Exception e)
            {
              e.printStackTrace();
            }
          }
        });
      }
    }
    
    
    @JavascriptInterface
    public void completedFindNearByStation(final String stationId)
    {
      if (SubwayManager.getInstance(activity).stations.size() == 0)
      {
        handler.post(new Runnable()
        {
          @Override
          public void run()
          {
            loadAllStations(new Runnable()
            {
              @Override
              public void run()
              {
                completedFindNearByStation(stationId);
              }
            });
          }
        });
      }
      else
      {
        final StationBean nearByStationWithTransfer = SubwayManager.getInstance(activity).findStationWithTransfer(stationId);
        final StationBean nearByStation = SubwayManager.getInstance(activity).findStation(stationId);
        Log.w("WARN", "가까운 지하철 역 찾음:" + stationId);
        Log.w("WARN", "가까운 지하철 역 찾음:" + nearByStation.stationId);
        Log.w("WARN", "가까운 지하철 역 찾음:" + nearByStation.nameKo);
        handler.post(new Runnable()
        {
          @Override
          public void run()
          {
            if (setDestFindNearStation == -1)
            {
              handler.post(new Runnable()
              {
                @Override
                public void run()
                {
                  webview.loadUrl("javascript:showDestinationPopUp('" + stationId + "')");
                }
              });
            }
            else
            {
              if (setDestFindNearStation == 0)
              {
                if (!TextUtils.isEmpty(endStationId))
                {
                  if (endStationId.equals(stationId))
                  {
                    //시작역이 이미 도착역으로 지정된 경우 
                    showDialogChangeConfirmStation(true, stationId);
                    return;
                  }
                }
                
                startStationId = stationId;
                if (TextUtils.isEmpty(endStationId))
                {
                  Log.w("WARN", "가까운 역 정보 표시");
                  containerNearByStationInfo.setVisibility(View.VISIBLE);
                  containerNearByStationInfo.bringToFront();
                  nearStationImage.removeAllViews();
                  if (nearByStationWithTransfer.lines.size() == 0)
                  {
                    Log.w("WARN", "가까운 역 정보 표시 1");
                    ImageView imageView = new ImageView(getActivity());
                    int imageSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
                    imageView.setLayoutParams(new LinearLayout.LayoutParams(imageSize, imageSize));
                    imageView.setScaleType(ScaleType.FIT_XY);
                    imageView.setImageResource(SubwayManager.getInstance(getActivity()).getSubwayLineResourceId(nearByStationWithTransfer.line));
                    nearStationImage.addView(imageView);
                  }
                  else
                  {
                    Log.w("WARN", "가까운 역 정보 표시 2");
                    for (int i = 0; i < nearByStationWithTransfer.lines.size(); i++)
                    {
                      ImageView imageView = new ImageView(getActivity());
                      int imageSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
                      imageView.setLayoutParams(new LinearLayout.LayoutParams(imageSize, imageSize));
                      imageView.setScaleType(ScaleType.FIT_XY);
                      imageView.setImageResource(SubwayManager.getInstance(getActivity()).getSubwayLineResourceId(nearByStationWithTransfer.lines.get(i)));
                      nearStationImage.addView(imageView);
                    }
                  }
                  nearStationName.setText(nearByStation.nameCn);
                  nearStationX.setOnClickListener(new OnClickListener()
                  {
                    @Override
                    public void onClick(View v)
                    {
                      containerNearByStationInfo.setVisibility(View.GONE);
                      startStationLat = -1;
                      startStationLong = -1;
                      startStationId = "";
                      
                      findNearByStationLat = -1;
                      findNearByStationLon = -1;
                      setDestFindNearStation = -1;
                      webview.loadUrl("javascript:subway.clear_start_station()");
                    }
                  });
                }
                
                webview.loadUrl("javascript:subway.setCenterWithStationIdWithoutPopup('" + stationId + "')");
                if (SubwayManager.getInstance(activity).getLatitudeWithSubwayId(stationId) != -1)
                {
                  double lat = SubwayManager.getInstance(activity).getLatitudeWithSubwayId(stationId);
                  double lon = SubwayManager.getInstance(activity).getLongitudeWithSubwayId(stationId);
                  startStationLat = lat;
                  startStationLong = lon;
                }
                Log.w("WARN", "가까운 지하철 역 출발지로 선택 :" + nearByStation.stationId);
                handler.postDelayed(new Runnable()
                {
                  @Override
                  public void run()
                  {
                    webview.loadUrl("javascript:subway.set_start_station('" + stationId + "')");
                  }
                }, 500);
              }
              else if (setDestFindNearStation == 1)
              {
                handler.post(new Runnable()
                {
                  @Override
                  public void run()
                  {
                    if (!TextUtils.isEmpty(startStationId))
                    {
                      Log.w("WARN", "Current startStationId : " + startStationId);
                      Log.w("WARN", "Change stationId : " + stationId);
                      if (startStationId.equals(stationId))
                      {
                        //도착역이 이미 시작역으로 지정된 경우 
                        showDialogChangeConfirmStation(false, stationId);
                        return;
                      }
                    }
                    
                    endStationId = stationId;
                    webview.loadUrl("javascript:subway.setCenterWithStationIdWithoutPopup('" + stationId + "')");
                    handler.postDelayed(new Runnable()
                    {
                      @Override
                      public void run()
                      {
                        webview.loadUrl("javascript:subway.set_end_station('" + stationId + "')");
                        if (TextUtils.isEmpty(startStationId))
                        {
                          containerNearByStationInfo.setVisibility(View.VISIBLE);
                          containerNearByStationInfo.bringToFront();
                          nearStationImage.removeAllViews();
                          if (nearByStationWithTransfer.lines.size() == 0)
                          {
                            ImageView imageView = new ImageView(getActivity());
                            int imageSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
                            imageView.setLayoutParams(new LinearLayout.LayoutParams(imageSize, imageSize));
                            imageView.setImageResource(SubwayManager.getInstance(getActivity()).getSubwayLineResourceId(nearByStationWithTransfer.line));
                            nearStationImage.addView(imageView);
                          }
                          else
                          {
                            for (int i = 0; i < nearByStationWithTransfer.lines.size(); i++)
                            {
                              ImageView imageView = new ImageView(getActivity());
                              int imageSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
                              imageView.setLayoutParams(new LinearLayout.LayoutParams(imageSize, imageSize));
                              imageView.setImageResource(SubwayManager.getInstance(getActivity()).getSubwayLineResourceId(nearByStationWithTransfer.lines.get(i)));
                              nearStationImage.addView(imageView);
                            }
                          }
                          nearStationName.setText(nearByStation.nameCn);
                          nearStationX.setOnClickListener(new OnClickListener()
                          {
                            @Override
                            public void onClick(View v)
                            {
                              findNearByStationLat = -1;
                              findNearByStationLon = -1;
                              setDestFindNearStation = -1;
                              endStationId = "";
                              
                              containerNearByStationInfo.setVisibility(View.GONE);
                              webview.loadUrl("javascript:subway.clear_end_station()");
                            }
                          });
                        }
                      }
                    }, 500);
                    
                  }
                });
              }
            }
          }
        });
      }
    }
    
    
    @JavascriptInterface
    public void setNativeZoom(final String stationId)
    {
      handler.post(new Runnable()
      {
        @Override
        public void run()
        {
          Log.w("WARN", "Scale : " + webview.getScale());
          int zoomLevel = 0;
          if (isZoomed)
          {
            zoomLevel = defaultZoom;
            isZoomed = false;
            webview.setInitialScale(zoomLevel);
          }
          else
          {
            zoomLevel = defaultZoom + 1;
            isZoomed = true;
            webview.setInitialScale(zoomLevel);
          }
          
          handler.postDelayed(new Runnable()
          {
            @Override
            public void run()
            {
              webview.loadUrl("javascript:subway.setCenterWithStationId('" + stationId + "')");
            }
          }, 200);
        }
      });
    }
    
    
    @JavascriptInterface
    public void completedSetDestStation(final String stationId, final int type)
    {
      final StationBean nearByStationWithTransfer = SubwayManager.getInstance(activity).findStationWithTransfer(stationId);
      final StationBean nearByStation = SubwayManager.getInstance(activity).findStation(stationId);
      handler.post(new Runnable()
      {
        @Override
        public void run()
        {
          webview.loadUrl("javascript:subway.setCenterWithStationIdWithoutPopup('" + stationId + "')");
          progressBar.setVisibility(View.GONE);
          
          containerNearByStationInfo.setVisibility(View.VISIBLE);
          containerNearByStationInfo.bringToFront();
//          nearStationImage.setImageResource(SubwayManager.getInstance(activity).getSubwayLineResourceId(nearByStation.line));
          nearStationImage.removeAllViews();
          if (nearByStationWithTransfer.lines.size() == 0)
          {
            ImageView imageView = new ImageView(getActivity());
            int imageSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
            imageView.setLayoutParams(new LinearLayout.LayoutParams(imageSize, imageSize));
            imageView.setImageResource(SubwayManager.getInstance(getActivity()).getSubwayLineResourceId(nearByStationWithTransfer.line));
            nearStationImage.addView(imageView);
          }
          else
          {
            for (int i = 0; i < nearByStationWithTransfer.lines.size(); i++)
            {
              ImageView imageView = new ImageView(getActivity());
              int imageSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
              imageView.setLayoutParams(new LinearLayout.LayoutParams(imageSize, imageSize));
              imageView.setImageResource(SubwayManager.getInstance(getActivity()).getSubwayLineResourceId(nearByStationWithTransfer.lines.get(i)));
              nearStationImage.addView(imageView);
            }
          }
          nearStationName.setText(nearByStation.nameCn);
          nearStationX.setOnClickListener(new OnClickListener()
          {
            @Override
            public void onClick(View v)
            {
              if (type == 0)
                webview.loadUrl("javascript:subway.clear_start_station()");
              else
                webview.loadUrl("javascript:subway.clear_end_station()");
              
              findNearByStationLat = -1;
              findNearByStationLon = -1;
              setDestFindNearStation = -1;
              
              containerNearByStationInfo.setVisibility(View.GONE);
            }
          });
        }
      });
    }
  }
  
}
