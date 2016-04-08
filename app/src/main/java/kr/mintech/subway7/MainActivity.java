package kr.mintech.subway7;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import kr.mintech.subway7.beans.StationBean;
import kr.mintech.subway7.controllers.SubwayManager;
import kr.mintech.subway7.controllers.SubwaySearchListAdapter;
import kr.mintech.subway7.fragments.SubwayFragment;

public class MainActivity extends Activity {
    private SubwayFragment subwayFragment;
    private EditText editSearch;
    private ArrayList<String> subwayNames = new ArrayList<String>();
    private ImageView backImage;
    private ImageView typingCancel;
    private ListView searchListView;
    private SubwaySearchListAdapter adapter;
    private RelativeLayout searchContainer;
    private RelativeLayout emptySearchContainer;

    public static Activity MainActivity;


    @SuppressLint({"SetJavaScriptEnabled", "InflateParams"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.w("oncreate", "들어는 갔니");
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(getString(R.string.term_subway));
        MainActivity = this;

        editSearch = (EditText) findViewById(R.id.edit_search);
        searchContainer = (RelativeLayout) findViewById(R.id.search_container);
        emptySearchContainer = (RelativeLayout) findViewById(R.id.empty_search_container);
        typingCancel = (ImageView) findViewById(R.id.search_cancel);
        Log.d("1.어디서", "문제인가");
        typingCancel.setVisibility(View.GONE);
        Log.d("2.어디서", "문제인가");
        typingCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                editSearch.setText("");
            }
        });

        searchListView = (ListView) findViewById(R.id.search_listview);
        adapter = new SubwaySearchListAdapter(MainActivity.this);
        searchListView.setAdapter(adapter);

        Log.w("WARN", "GetIntent data : " + getIntent().getDoubleExtra("Latitude", -1));
        subwayNames.clear();

        //위경도로 지하철역 찾기
        double[] latLong = null;
        if (getIntent().hasExtra("near_by_station_lat_lon"))
            latLong = getIntent().getDoubleArrayExtra("near_by_station_lat_lon");

        double[] destLatLong = null;
        String destName = "";
        if (getIntent().hasExtra("set_dest_station_lat_lon")) {
            destLatLong = getIntent().getDoubleArrayExtra("set_dest_station_lat_lon");
            if (getIntent().hasExtra("dest_name"))
                destName = getIntent().getStringExtra("dest_name");
        }
        Log.d("3. 어디서", "문제인가");
        if (subwayFragment == null) {
            subwayFragment.viewClear();
            subwayFragment = new SubwayFragment();
        }

        if (latLong != null)
            subwayFragment.findNearByStation(latLong[0], latLong[1]);

        if (destLatLong != null)
            subwayFragment.findNearByStation(destLatLong[0], destLatLong[1], destLatLong[2], destName);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content, subwayFragment);
        ft.commit();
        Log.d("4. 어디서", "문제인가");
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }


            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }


            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.w("WARN", "length: " + editSearch.getText().toString().length());
                if (editSearch.getText().toString().length() > 1) {
                    typingCancel.setVisibility(View.VISIBLE);
                    searchContainer.setVisibility(View.VISIBLE);
                    if (SubwayManager.getInstance(MainActivity.this).getSubwayStationsWithTitle(editSearch.getText().toString()).size() > 0) {
                        searchListView.setVisibility(View.VISIBLE);
                        searchListView.bringToFront();
                        emptySearchContainer.setVisibility(View.GONE);
                    } else {
                        searchListView.setVisibility(View.GONE);
                        emptySearchContainer.setVisibility(View.VISIBLE);
                    }
                } else {
                    typingCancel.setVisibility(View.GONE);
                    searchContainer.setVisibility(View.GONE);
                    searchListView.setVisibility(View.GONE);
                }

                adapter.clear();
                adapter.addAll(SubwayManager.getInstance(MainActivity.this).getSubwayStationsWithTitle(editSearch.getText().toString()));
            }
        };
        Log.d("5. 어디서", "문제인가");
//        editSearch.addTextChangedListener(watcher);
        Log.d("5.1 어디서", "문제인가");
        editSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d("5.5 어디서", "문제인가");
                if (!hasFocus) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);
                }
            }
        });
        editSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d("6. 어디서", "문제인가");

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    if (SubwayManager.getInstance(MainActivity.this).getSubwayStationsWithTitle(editSearch.getText().toString()).size() > 0) {
                        searchListView.setVisibility(View.VISIBLE);
                        searchListView.bringToFront();
                        emptySearchContainer.setVisibility(View.GONE);
                    } else {
                        searchListView.setVisibility(View.GONE);
                        emptySearchContainer.setVisibility(View.VISIBLE);
                    }
                    adapter.clear();
                    adapter.addAll(SubwayManager.getInstance(MainActivity.this).getSubwayStationsWithTitle(editSearch.getText().toString()));
                }
                return true;

            }
        });

        searchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);

                searchContainer.setVisibility(View.GONE);
                searchListView.setVisibility(View.GONE);
                StationBean bean = (StationBean) adapter.getItem(position);
                subwayFragment.findStation(bean.stationId, bean.nameKo);
                Log.d("7. 어디서", "문제인가");
            }
        });

        backImage = (ImageView) findViewById(R.id.image_back_button);
        backImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (BlinkingMap.blinkingactivity != null)
//                    BlinkingMap.blinkingactivity.finish();
//                finish();
                Log.d("8. 어디서", "문제인가");
            }
        });
    }


    @Override
    protected void onNewIntent(Intent intent) {
        Log.w("WARN", "Subway onNewIntent");

        double[] latLong = null;
        if (intent.hasExtra("near_by_station_lat_lon"))
            latLong = intent.getDoubleArrayExtra("near_by_station_lat_lon");

        double[] destLatLong = null;
        String destName = "";
        if (intent.hasExtra("set_dest_station_lat_lon")) {
            destLatLong = intent.getDoubleArrayExtra("set_dest_station_lat_lon");
            if (intent.hasExtra("dest_name")) {
                destName = intent.getStringExtra("dest_name");
            }
        }

        if (subwayFragment == null) {
//    MainActivity.subwayFrament.viewClear();
            subwayFragment = new SubwayFragment();
        }

        if (latLong != null)
            subwayFragment.findNearByStation(latLong[0], latLong[1]);

        if (destLatLong != null)
            subwayFragment.findNearByStation(destLatLong[0], destLatLong[1], destLatLong[2], destName);
        super.onNewIntent(intent);
    }
}

