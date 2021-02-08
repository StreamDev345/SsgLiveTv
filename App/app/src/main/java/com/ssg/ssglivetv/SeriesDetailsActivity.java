package com.ssg.ssglivetv;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bosphere.fadingedgelayout.FadingEdgeLayout;
import com.ssg.adapter.CommentAdapter;
import com.ssg.adapter.EpisodeAdapter;
import com.ssg.adapter.HomeSeriesAdapter;
import com.ssg.adapter.SeasonAdapter;
import com.ssg.cast.Casty;
import com.ssg.cast.MediaData;
import com.ssg.db.DatabaseHelper;
import com.ssg.dialog.DialogUtil;
import com.ssg.dialog.RateDialog;
import com.ssg.fragment.ChromecastScreenFragment;
import com.ssg.fragment.EmbeddedImageFragment;
import com.ssg.fragment.ExoPlayerFragment;
import com.ssg.fragment.ReportFragment;
import com.ssg.item.ItemComment;
import com.ssg.item.ItemEpisode;
import com.ssg.item.ItemSeason;
import com.ssg.item.ItemSeries;
import com.ssg.ssglivetv.R;
import com.ssg.util.API;
import com.ssg.util.BannerAds;
import com.ssg.util.Constant;
import com.ssg.util.Events;
import com.ssg.util.GlobalBus;
import com.ssg.util.IsRTL;
import com.ssg.util.NetworkUtils;
import com.ssg.util.RvOnClickListener;
import com.github.ornolfr.ratingview.RatingView;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SeriesDetailsActivity extends AppCompatActivity implements RateDialog.RateDialogListener {
    ProgressBar mProgressBar, mProgressBarEpisode;
    LinearLayout lyt_not_found;
    NestedScrollView nestedScrollView;
    RelativeLayout lytParent;
    WebView webView;
    RatingView ratingView;
    TextView textTitle, textCategory, textRate, textReport, textRelViewAll, textComViewAll, textNoComment, textSeason, textNoEpisode, textSeasonDrop, textCount;
    ImageView imageEditRate, imageFav;
    RecyclerView rvRelated, rvComment, rvEpisode;
    ItemSeries itemSeries;
    ArrayList<ItemSeries> mListItemRelated;
    ArrayList<ItemComment> mListItemComment;
    ArrayList<ItemSeason> mListItemSeason;
    ArrayList<ItemEpisode> mListItemEpisode;
    HomeSeriesAdapter homeSeriesAdapter;
    CommentAdapter commentAdapter;
    EpisodeAdapter episodeAdapter;
    String Id;
    LinearLayout lytRelated, lytEpisode, lytSeason;
    EditText editTextComment;

    ProgressDialog pDialog;
    MyApplication myApplication;
    DatabaseHelper databaseHelper;
    private int selectedSeason = 0;
    private int selectedEpisode = 0;
    private FragmentManager fragmentManager;
    Toolbar toolbar;
    private int playerHeight;
    FrameLayout frameLayout;
    boolean isFullScreen = false;
    boolean isPlayerIsYt = false;
    private YouTubePlayer youTubePlayer;
    public boolean isYouTubePlayerFullScreen = false;
    boolean isFromNotification = false;
    LinearLayout mAdViewLayout;
    private Casty casty;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_series_details);
        IsRTL.ifSupported(this);
        GlobalBus.getBus().register(this);

        FadingEdgeLayout feRecent = findViewById(R.id.feRecent);
        FadingEdgeLayout feEpisode = findViewById(R.id.feEpisode);
        IsRTL.changeShadowInRtl(this, feRecent);
        IsRTL.changeShadowInRtl(this, feEpisode);
        mAdViewLayout = findViewById(R.id.adView);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        casty = Casty.create(this)
                .withMiniController();

        Intent intent = getIntent();
        Id = intent.getStringExtra("Id");
        if (intent.hasExtra("isNotification")) {
            isFromNotification = true;
        }
        mListItemRelated = new ArrayList<>();
        mListItemComment = new ArrayList<>();
        mListItemSeason = new ArrayList<>();
        mListItemEpisode = new ArrayList<>();

        itemSeries = new ItemSeries();

        pDialog = new ProgressDialog(this);
        myApplication = MyApplication.getInstance();
        databaseHelper = new DatabaseHelper(this);
        fragmentManager = getSupportFragmentManager();

        lytRelated = findViewById(R.id.lytRelated);
        lytEpisode = findViewById(R.id.lytEpisode);
        lytSeason = findViewById(R.id.lytSeason);
        mProgressBar = findViewById(R.id.progressBar1);
        mProgressBarEpisode = findViewById(R.id.progressBar);
        lyt_not_found = findViewById(R.id.lyt_not_found);
        lytParent = findViewById(R.id.lytParent);
        nestedScrollView = findViewById(R.id.nestedScrollView);
        webView = findViewById(R.id.webView);
        ratingView = findViewById(R.id.ratingView);
//        editTextComment = findViewById(R.id.editText_comment_md);

        textTitle = findViewById(R.id.textTitle);
        textCategory = findViewById(R.id.textCategory);
        textRate = findViewById(R.id.textRate);
        textReport = findViewById(R.id.textReport);
        textRelViewAll = findViewById(R.id.textRelViewAll);
//        textComViewAll = findViewById(R.id.textComViewAll);
//        textNoComment = findViewById(R.id.textView_noComment_md);
        textSeason = findViewById(R.id.textSeason);
        textNoEpisode = findViewById(R.id.textNoEpisode);
        textSeasonDrop = findViewById(R.id.textSeasonDrop);
        textCount = findViewById(R.id.textViews);

        frameLayout = findViewById(R.id.playerSection);
        frameLayout = findViewById(R.id.playerSection);
        int columnWidth = NetworkUtils.getScreenWidth(this);
        frameLayout.setLayoutParams(new RelativeLayout.LayoutParams(columnWidth, columnWidth / 2));
        playerHeight = frameLayout.getLayoutParams().height;

//        editTextComment.setClickable(true);
//        editTextComment.setFocusable(false);
        textTitle.setSelected(true);

        rvRelated = findViewById(R.id.rv_related);
//        rvComment = findViewById(R.id.rv_comment);
        rvEpisode = findViewById(R.id.rv_episode);

        imageEditRate = findViewById(R.id.imageEditRate);
        imageFav = findViewById(R.id.imageFav);
        webView.setBackgroundColor(Color.TRANSPARENT);

        rvRelated.setHasFixedSize(true);
        rvRelated.setLayoutManager(new LinearLayoutManager(SeriesDetailsActivity.this, LinearLayoutManager.HORIZONTAL, false));
        rvRelated.setFocusable(false);
        rvRelated.setNestedScrollingEnabled(false);

        rvEpisode.setHasFixedSize(true);
        rvEpisode.setLayoutManager(new LinearLayoutManager(SeriesDetailsActivity.this, LinearLayoutManager.HORIZONTAL, false));
        rvEpisode.setFocusable(false);
        rvEpisode.setNestedScrollingEnabled(false);

//        rvComment.setHasFixedSize(true);
//        rvComment.setLayoutManager(new LinearLayoutManager(SeriesDetailsActivity.this, LinearLayoutManager.VERTICAL, false));
//        rvComment.setFocusable(false);
//        rvComment.setNestedScrollingEnabled(false);

        BannerAds.ShowBannerAds(this, mAdViewLayout);

        if (NetworkUtils.isConnected(SeriesDetailsActivity.this)) {
            getDetails();
        } else {
            showToast(getString(R.string.conne_msg1));
        }

    }

    private void getDetails() {

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API());
        jsObj.addProperty("method_name", "get_single_series");
        jsObj.addProperty("series_id", Id);
        params.put("data", API.toBase64(jsObj.toString()));
        client.post(Constant.API_URL, params, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
                super.onStart();
                mProgressBar.setVisibility(View.VISIBLE);
                lytParent.setVisibility(View.GONE);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                mProgressBar.setVisibility(View.GONE);
                lytParent.setVisibility(View.VISIBLE);

                String result = new String(responseBody);
                try {
                    JSONObject mainJson = new JSONObject(result);
                    JSONArray jsonArray = mainJson.getJSONArray(Constant.ARRAY_NAME);
                    if (jsonArray.length() > 0) {
                        JSONObject objJson;
                        for (int i = 0; i < jsonArray.length(); i++) {
                            objJson = jsonArray.getJSONObject(i);
                            if (objJson.has(Constant.STATUS)) {
                                lyt_not_found.setVisibility(View.VISIBLE);
                            } else {
                                itemSeries.setId(objJson.getString(Constant.SERIES_ID));
                                itemSeries.setSeriesName(objJson.getString(Constant.SERIES_TITLE));
                                itemSeries.setSeriesDesc(objJson.getString(Constant.SERIES_DESC));
                                itemSeries.setSeriesPoster(objJson.getString(Constant.SERIES_POSTER));
                                itemSeries.setSeriesCover(objJson.getString(Constant.SERIES_COVER));
                                itemSeries.setTotalViews(objJson.getString(Constant.MOVIE_TOTAL_VIEW));
                                itemSeries.setRateAvg(objJson.getString(Constant.SERIES_RATE));

                                JSONArray jsonArraySeason = objJson.getJSONArray(Constant.SEASON_ARRAY);
                                if (jsonArraySeason.length() != 0) {
                                    for (int j = 0; j < jsonArraySeason.length(); j++) {
                                        JSONObject objSeason = jsonArraySeason.getJSONObject(j);
                                        ItemSeason itemSeason = new ItemSeason();
                                        itemSeason.setSeasonId(objSeason.getString(Constant.SEASON_ID));
                                        itemSeason.setSeasonName(objSeason.getString(Constant.SEASON_NAME));
                                        mListItemSeason.add(itemSeason);
                                    }
                                }

                                JSONArray jsonArrayChild = objJson.getJSONArray(Constant.RELATED_ITEM_ARRAY_NAME);
                                if (jsonArrayChild.length() != 0) {
                                    for (int j = 0; j < jsonArrayChild.length(); j++) {
                                        JSONObject objChild = jsonArrayChild.getJSONObject(j);
                                        ItemSeries item = new ItemSeries();
                                        item.setId(objChild.getString(Constant.SERIES_ID));
                                        item.setSeriesName(objChild.getString(Constant.SERIES_TITLE));
                                        item.setSeriesPoster(objChild.getString(Constant.SERIES_POSTER));
                                        mListItemRelated.add(item);
                                    }
                                }

//                                JSONArray jsonArrayComment = objJson.getJSONArray(Constant.COMMENT_ARRAY);
//                                if (jsonArrayComment.length() != 0) {
//                                    for (int j = 0; j < jsonArrayComment.length(); j++) {
//                                        JSONObject objComment = jsonArrayComment.getJSONObject(j);
//                                        ItemComment itemComment = new ItemComment();
//                                        itemComment.setUserName(objComment.getString(Constant.COMMENT_NAME));
//                                        itemComment.setCommentText(objComment.getString(Constant.COMMENT_DESC));
//                                        itemComment.setCommentDate(objComment.getString(Constant.COMMENT_DATE));
//                                        mListItemComment.add(itemComment);
//                                    }
//                                }
                            }
                        }
                        displayData();

                    } else {
                        mProgressBar.setVisibility(View.GONE);
                        lytParent.setVisibility(View.GONE);
                        lyt_not_found.setVisibility(View.VISIBLE);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                mProgressBar.setVisibility(View.GONE);
                lytParent.setVisibility(View.GONE);
                lyt_not_found.setVisibility(View.VISIBLE);
            }
        });
    }

    private void displayData() {
        setTitle(itemSeries.getSeriesName());
        textTitle.setText(itemSeries.getSeriesName());
        textRate.setText(itemSeries.getRateAvg());
        ratingView.setRating(Float.parseFloat(itemSeries.getRateAvg()));
        textCount.setText(getString(R.string.count, NetworkUtils.viewFormat(Integer.parseInt(itemSeries.getTotalViews()))));
        
        String mimeType = "text/html";
        String encoding = "utf-8";
        String htmlText = itemSeries.getSeriesDesc();

        boolean isRTL = Boolean.parseBoolean(getResources().getString(R.string.isRTL));
        String direction = isRTL ? "rtl" : "ltr";

        String text = "<html dir=" + direction + "><head>"
                + "<style type=\"text/css\">@font-face {font-family: MyFont;src: url(\"file:///android_asset/fonts/custom.ttf\")}body{font-family: MyFont;color: #9f9f9f;font-size:14px;margin-left:0px;line-height:1.3}"
                + "</style></head>"
                + "<body>"
                + htmlText
                + "</body></html>";

        webView.loadDataWithBaseURL(null, text, mimeType, encoding, null);

        if (!mListItemSeason.isEmpty()) {
            textCategory.setText(getString(R.string.total_num_season, mListItemSeason.size()));
            changeSeason(selectedSeason);
        } else {
            lytEpisode.setVisibility(View.GONE);
            lytSeason.setVisibility(View.GONE);
            textCategory.setVisibility(View.GONE);
            setImageIfSeasonAndEpisodeNone(itemSeries.getSeriesCover());
        }

        if (!mListItemRelated.isEmpty()) {
            homeSeriesAdapter = new HomeSeriesAdapter(SeriesDetailsActivity.this, mListItemRelated);
            rvRelated.setAdapter(homeSeriesAdapter);

            homeSeriesAdapter.setOnItemClickListener(new RvOnClickListener() {
                @Override
                public void onItemClick(int position) {
                    String seriesId = mListItemRelated.get(position).getId();
                    Intent intent = new Intent(SeriesDetailsActivity.this, SeriesDetailsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("Id", seriesId);
                    startActivity(intent);
                }
            });

        } else {
            lytRelated.setVisibility(View.GONE);
        }

//        if (!mListItemComment.isEmpty()) {
//            commentAdapter = new CommentAdapter(SeriesDetailsActivity.this, mListItemComment);
//            rvComment.setAdapter(commentAdapter);
//        } else {
//            textNoComment.setVisibility(View.VISIBLE);
//        }

//        editTextComment.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (myApplication.getIsLogin()) {
//                    showCommentBox();
//                } else {
//                    String message = getString(R.string.login_first, getString(R.string.login_first_comment));
//                    showToast(message);
//
//                    Intent intentLogin = new Intent(SeriesDetailsActivity.this, SignInActivity.class);
//                    intentLogin.putExtra("isOtherScreen", true);
//                    startActivity(intentLogin);
//                }
//            }
//        });

//        textComViewAll.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(SeriesDetailsActivity.this, AllCommentActivity.class);
//                intent.putExtra("postId", Id);
//                intent.putExtra("postType", "series");
//                startActivity(intent);
//            }
//        });

        textRelViewAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SeriesDetailsActivity.this, RelatedAllSeriesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("postId", Id);
                startActivity(intent);
            }
        });

        ratingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myApplication.getIsLogin()) {
                    DialogUtil.showRateDialog(SeriesDetailsActivity.this, SeriesDetailsActivity.this, Id, "series");
                } else {
                    String message = getString(R.string.login_first, getString(R.string.login_first_report));
                    showToast(message);

                    Intent intentLogin = new Intent(SeriesDetailsActivity.this, SignInActivity.class);
                    intentLogin.putExtra("isOtherScreen", true);
                    startActivity(intentLogin);
                }
            }
        });

        lytSeason.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSeason(selectedSeason);
            }
        });

        textReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myApplication.getIsLogin()) {
                    Bundle bundle = new Bundle();
                    bundle.putString("postId", Id);
                    bundle.putString("postType", "series");
                    ReportFragment reportFragment = new ReportFragment();
                    reportFragment.setArguments(bundle);
                    reportFragment.show(getSupportFragmentManager(), reportFragment.getTag());
                } else {
                    String message = getString(R.string.login_first, getString(R.string.login_first_rate));
                    showToast(message);

                    Intent intentLogin = new Intent(SeriesDetailsActivity.this, SignInActivity.class);
                    intentLogin.putExtra("isOtherScreen", true);
                    startActivity(intentLogin);
                }
            }
        });

        isFavourite();
        imageFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues fav = new ContentValues();
                if (databaseHelper.getFavouriteById(Id, DatabaseHelper.TABLE_SERIES)) {
                    databaseHelper.removeFavouriteById(Id, DatabaseHelper.TABLE_SERIES);
                    imageFav.setImageResource(R.drawable.ic_fav);
                    showToast(getString(R.string.favourite_remove));
                } else {
                    fav.put(DatabaseHelper.SERIES_ID, Id);
                    fav.put(DatabaseHelper.SERIES_TITLE, itemSeries.getSeriesName());
                    fav.put(DatabaseHelper.SERIES_POSTER, itemSeries.getSeriesPoster());
                    databaseHelper.addFavourite(DatabaseHelper.TABLE_SERIES, fav, null);
                    imageFav.setImageResource(R.drawable.ic_fav_hover);
                    showToast(getString(R.string.favourite_add));
                }
            }
        });

        saveRecent();

        casty.setOnConnectChangeListener(new Casty.OnConnectChangeListener() {
            @Override
            public void onConnected() {

            }

            @Override
            public void onDisconnected() {
                if (!mListItemEpisode.isEmpty()) {
                    playEpisode(selectedEpisode);
                } else {
                    setImageIfSeasonAndEpisodeNone(itemSeries.getSeriesCover());
                }

            }
        });
    }

    private void changeSeason(int seasonId) {
        ItemSeason itemSeason = mListItemSeason.get(seasonId);
        textSeason.setText(itemSeason.getSeasonName());
        textSeasonDrop.setText(itemSeason.getSeasonName());
        if (NetworkUtils.isConnected(SeriesDetailsActivity.this)) {
            getEpisode(itemSeason.getSeasonId());
        } else {
            showToast(getString(R.string.conne_msg1));
        }
    }

    private void getEpisode(String seasonId) {

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API());
        jsObj.addProperty("method_name", "get_episodes");
        jsObj.addProperty("series_id", Id);
        jsObj.addProperty("season_id", seasonId);
        params.put("data", API.toBase64(jsObj.toString()));
        client.post(Constant.API_URL, params, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
                super.onStart();
                mProgressBarEpisode.setVisibility(View.VISIBLE);
                rvEpisode.setVisibility(View.GONE);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                mProgressBarEpisode.setVisibility(View.GONE);
                rvEpisode.setVisibility(View.VISIBLE);

                String result = new String(responseBody);
                try {
                    JSONObject mainJson = new JSONObject(result);
                    JSONArray jsonArray = mainJson.getJSONArray(Constant.ARRAY_NAME);
                    if (jsonArray.length() > 0) {
                        textNoEpisode.setVisibility(View.GONE);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject objJson = jsonArray.getJSONObject(i);
                            ItemEpisode itemEpisode = new ItemEpisode();
                            itemEpisode.setId(objJson.getString(Constant.EPISODE_ID));
                            itemEpisode.setEpisodeTitle(objJson.getString(Constant.EPISODE_TITLE));
                            itemEpisode.setEpisodePoster(objJson.getString(Constant.EPISODE_POSTER));
                            itemEpisode.setEpisodeUrl(objJson.getString(Constant.EPISODE_URL));
                            itemEpisode.setEpisodeType(objJson.getString(Constant.EPISODE_TYPE));
                            itemEpisode.setPlaying(false);
                            mListItemEpisode.add(itemEpisode);
                        }
                        displayEpisode();

                    } else {
                        mProgressBarEpisode.setVisibility(View.GONE);
                        rvEpisode.setVisibility(View.GONE);
                        textNoEpisode.setVisibility(View.VISIBLE);

                        setImageIfSeasonAndEpisodeNone(itemSeries.getSeriesCover());
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                mProgressBarEpisode.setVisibility(View.GONE);
                rvEpisode.setVisibility(View.GONE);
                textNoEpisode.setVisibility(View.VISIBLE);
            }
        });
    }

    private void displayEpisode() {
        episodeAdapter = new EpisodeAdapter(SeriesDetailsActivity.this, mListItemEpisode);
        rvEpisode.setAdapter(episodeAdapter);
        //play 1st episode by default
        if (!mListItemEpisode.isEmpty()) {
            playEpisode(0);
        }

        episodeAdapter.setOnItemClickListener(new RvOnClickListener() {
            @Override
            public void onItemClick(int position) {
                playEpisode(position);
            }
        });
    }

    private void setImageIfSeasonAndEpisodeNone(String imageCover) {
        EmbeddedImageFragment embeddedImageFragment = EmbeddedImageFragment.newInstance("", imageCover, false);
        fragmentManager.beginTransaction().replace(R.id.playerSection, embeddedImageFragment).commitAllowingStateLoss();
    }

    private void playEpisode(int playPosition) {
        ItemEpisode itemEpisode = mListItemEpisode.get(playPosition);
        if (!itemEpisode.getEpisodeUrl().isEmpty()) {
            mListItemEpisode.get(selectedEpisode).setPlaying(false);
            episodeAdapter.notifyItemChanged(selectedEpisode, mListItemEpisode);

            selectedEpisode = playPosition;
            mListItemEpisode.get(playPosition).setPlaying(true);
            episodeAdapter.notifyItemChanged(playPosition, mListItemEpisode);

            switch (itemEpisode.getEpisodeType()) {
                case "server_url":
                case "local_url":
                    if (casty.isConnected()) {
                        ChromecastScreenFragment chromecastScreenFragment = new ChromecastScreenFragment();
                        fragmentManager.beginTransaction().replace(R.id.playerSection, chromecastScreenFragment).commitAllowingStateLoss();
                    } else {
                        ExoPlayerFragment exoPlayerFragment = ExoPlayerFragment.newInstance(itemEpisode.getEpisodeUrl());
                        fragmentManager.beginTransaction().replace(R.id.playerSection, exoPlayerFragment).commitAllowingStateLoss();
                    }
                    break;
                case "youtube_url":
                    isPlayerIsYt = true;
                    String videoId = NetworkUtils.getVideoId(itemEpisode.getEpisodeUrl());
                    playYoutube(videoId);
                    break;
                default:
                    EmbeddedImageFragment embeddedImageFragment = EmbeddedImageFragment.newInstance(itemEpisode.getEpisodeUrl(), itemEpisode.getEpisodePoster(), true);
                    fragmentManager.beginTransaction().replace(R.id.playerSection, embeddedImageFragment).commitAllowingStateLoss();
                    break;
            }
        } else {
            showToast(getString(R.string.stream_not_found));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        casty.addMediaRouteMenuItem(menu);
        getMenuInflater().inflate(R.menu.menu_details, menu);
        if (casty.isConnected()) {
            menu.findItem(R.id.menu_cast_play).setVisible(true);
        } else {
            menu.findItem(R.id.menu_cast_play).setVisible(false);
        }
        return true;
    }


    private void playViaCast() {
        if (!mListItemEpisode.isEmpty()) {
            ItemEpisode itemEpisode = mListItemEpisode.get(selectedEpisode);
            if (itemEpisode.getEpisodeType().equals("server_url") || itemEpisode.getEpisodeType().equals("local_url")) {
                casty.getPlayer().loadMediaAndPlay(createSampleMediaData(itemEpisode.getEpisodeUrl(), itemEpisode.getEpisodeTitle(), itemEpisode.getEpisodePoster()));

                ChromecastScreenFragment chromecastScreenFragment = new ChromecastScreenFragment();
                fragmentManager.beginTransaction().replace(R.id.playerSection, chromecastScreenFragment).commitAllowingStateLoss();
            } else {
                showToast(getResources().getString(R.string.cast_youtube));
            }
        } else {
            showToast(getString(R.string.stream_not_found));
        }

    }

    private MediaData createSampleMediaData(String videoUrl, String videoTitle, String videoImage) {
        return new MediaData.Builder(videoUrl)
                .setStreamType(MediaData.STREAM_TYPE_BUFFERED)
                .setContentType(getType(videoUrl))
                .setMediaType(MediaData.MEDIA_TYPE_MOVIE)
                .setTitle(videoTitle)
                .setSubtitle(getString(R.string.app_name))
                .addPhotoUrl(videoImage)
                .build();
    }

    private String getType(String videoUrl) {
        if (videoUrl.endsWith(".mp4")) {
            return "videos/mp4";
        } else if (videoUrl.endsWith(".m3u8")) {
            return "application/x-mpegurl";
        } else {
            return "application/x-mpegurl";
        }
    }

    public void showToast(String msg) {
        Toast.makeText(SeriesDetailsActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.menu_cast_play:
                playViaCast();
                break;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }

    private void showSeason(int position) {
        final Dialog mDialog = new Dialog(SeriesDetailsActivity.this, R.style.Theme_AppCompat_Translucent);
        mDialog.setContentView(R.layout.dialog_season);
        TextView textSeriesName = mDialog.findViewById(R.id.textSeasonName);
        RecyclerView rvSeason = mDialog.findViewById(R.id.rv_season);
        rvSeason.setHasFixedSize(true);
        rvSeason.setLayoutManager(new LinearLayoutManager(SeriesDetailsActivity.this, LinearLayoutManager.VERTICAL, false));
        rvSeason.setFocusable(false);
        rvSeason.setNestedScrollingEnabled(false);

        textSeriesName.setText(itemSeries.getSeriesName());
        SeasonAdapter seasonAdapter = new SeasonAdapter(SeriesDetailsActivity.this, mListItemSeason, position);
        rvSeason.setAdapter(seasonAdapter);

        seasonAdapter.setOnItemClickListener(new RvOnClickListener() {
            @Override
            public void onItemClick(int position) {
                selectedSeason = position;
                mDialog.dismiss();
                mListItemEpisode.clear();
                selectedEpisode = 0;
                changeSeason(selectedSeason);
            }
        });
        mDialog.show();
    }

//    private void showCommentBox() {
//        final Dialog mDialog = new Dialog(SeriesDetailsActivity.this, R.style.Theme_AppCompat_Translucent);
//        mDialog.setContentView(R.layout.dialog_comment);
//        final EditText edt_comment = mDialog.findViewById(R.id.edt_comment);
//        final ImageView img_sent = mDialog.findViewById(R.id.image_sent);
//        mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
//        img_sent.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String comment = edt_comment.getText().toString();
//                if (!comment.isEmpty()) {
//                    if (NetworkUtils.isConnected(SeriesDetailsActivity.this)) {
//                        sentComment(comment);
//                        mDialog.dismiss();
//                    } else {
//                        showToast(getString(R.string.conne_msg1));
//                    }
//                }
//            }
//        });
//        mDialog.show();
//    }

//    private void sentComment(String comment) {
//        AsyncHttpClient client = new AsyncHttpClient();
//        RequestParams params = new RequestParams();
//
//        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API());
//        jsObj.addProperty("method_name", "user_comment");
//        jsObj.addProperty("post_id", Id);
//        jsObj.addProperty("user_id", myApplication.getUserId());
//        jsObj.addProperty("comment_text", comment);
//        jsObj.addProperty("type", "series");
//        jsObj.addProperty("is_limit", "true");
//        params.put("data", API.toBase64(jsObj.toString()));
//
//        client.post(Constant.API_URL, params, new AsyncHttpResponseHandler() {
//            @Override
//            public void onStart() {
//                super.onStart();
//                showProgressDialog();
//            }
//
//            @Override
//            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
//                dismissProgressDialog();
//                String result = new String(responseBody);
//                try {
//                    JSONObject mainJson = new JSONObject(result);
//                    String strMessage = mainJson.getString(Constant.MSG);
//                    showToast(strMessage);
//
//                    JSONArray jsonArray = mainJson.getJSONArray(Constant.ARRAY_NAME);
//                    if (jsonArray.length() != 0) {
//                        mListItemComment.clear();
//                        for (int j = 0; j < jsonArray.length(); j++) {
//                            JSONObject objComment = jsonArray.getJSONObject(j);
//                            ItemComment itemComment = new ItemComment();
//                            itemComment.setUserName(objComment.getString(Constant.COMMENT_NAME));
//                            itemComment.setCommentText(objComment.getString(Constant.COMMENT_DESC));
//                            itemComment.setCommentDate(objComment.getString(Constant.COMMENT_DATE));
//                            mListItemComment.add(itemComment);
//                        }
//                    }
//
//                    if (!mListItemComment.isEmpty()) {
//                        commentAdapter = new CommentAdapter(SeriesDetailsActivity.this, mListItemComment);
//                        rvComment.setAdapter(commentAdapter);
//                        textNoComment.setVisibility(View.GONE);
//                    } else {
//                        textNoComment.setVisibility(View.VISIBLE);
//                    }
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
//                dismissProgressDialog();
//            }
//
//        });
//    }

    public void showProgressDialog() {
        pDialog.setMessage(getString(R.string.loading));
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(true);
        pDialog.show();
    }

    public void dismissProgressDialog() {
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GlobalBus.getBus().unregister(this);
    }

//    @Subscribe
//    public void getComment(Events.Comment comment) {
//        if (comment.getPostType().equals("series")) {
//            ArrayList<ItemComment> itemComments = comment.getItemComments();
//            CommentAdapter commentAdapter = new CommentAdapter(SeriesDetailsActivity.this, itemComments);
//            rvComment.setAdapter(commentAdapter);
//            textNoComment.setVisibility(View.GONE);
//        }
//    }

    @Subscribe
    public void getFullScreen(Events.FullScreen fullScreen) {
        isFullScreen = fullScreen.isFullScreen();
        if (fullScreen.isFullScreen()) {
            gotoFullScreen();
        } else {
            gotoPortraitScreen();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        View v1 = findViewById(R.id.view_fake);
        v1.requestFocus();
    }

    private void gotoPortraitScreen() {
        nestedScrollView.setVisibility(View.VISIBLE);
        toolbar.setVisibility(View.VISIBLE);
        mAdViewLayout.setVisibility(View.VISIBLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        frameLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, playerHeight));
    }

    private void gotoFullScreen() {
        nestedScrollView.setVisibility(View.GONE);
        toolbar.setVisibility(View.GONE);
        mAdViewLayout.setVisibility(View.GONE);
        frameLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onBackPressed() {
        if (isPlayerIsYt) {
            if (isYouTubePlayerFullScreen && youTubePlayer != null) {
                youTubePlayer.setFullscreen(false);
            } else {
                if (isFromNotification) {
                    Intent intent = new Intent(SeriesDetailsActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    super.onBackPressed();
                }
            }
        } else {
            if (isFullScreen) {
                Events.FullScreen fullScreen = new Events.FullScreen();
                fullScreen.setFullScreen(false);
                GlobalBus.getBus().post(fullScreen);
            } else {
                if (isFromNotification) {
                    Intent intent = new Intent(SeriesDetailsActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    super.onBackPressed();
                }
            }
        }
    }

    private void playYoutube(String videoId) {
        YouTubePlayerSupportFragment youTubePlayerFragment = YouTubePlayerSupportFragment.newInstance();
        fragmentManager.beginTransaction().replace(R.id.playerSection, youTubePlayerFragment).commitAllowingStateLoss();
        youTubePlayerFragment.initialize(getString(R.string.youtube_api_key), new YouTubePlayer.OnInitializedListener() {

            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
                if (!wasRestored) {
                    youTubePlayer = player;
                    youTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT);
                    youTubePlayer.loadVideo(videoId);
                    youTubePlayer.play();
                    youTubePlayer.setOnFullscreenListener(new YouTubePlayer.OnFullscreenListener() {
                        @Override
                        public void onFullscreen(boolean _isFullScreen) {
                            isYouTubePlayerFullScreen = _isFullScreen;
                        }
                    });
                }
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
                String errorMessage = youTubeInitializationResult.toString();
                Log.d("errorMessage:", errorMessage);
            }
        });
    }

    @Override
    public void confirm(String rateAvg) {
        ratingView.setRating(Float.parseFloat(rateAvg));
        textRate.setText(rateAvg);
    }

    @Override
    public void cancel() {

    }

    private void isFavourite() {
        if (databaseHelper.getFavouriteById(Id, DatabaseHelper.TABLE_SERIES)) {
            imageFav.setImageResource(R.drawable.ic_fav_hover);
        } else {
            imageFav.setImageResource(R.drawable.ic_fav);
        }
    }

    private void saveRecent() {
        if (!databaseHelper.getRecentById(Id, "series")) {
            ContentValues recent = new ContentValues();
            recent.put(DatabaseHelper.RECENT_ID, Id);
            recent.put(DatabaseHelper.RECENT_TITLE, itemSeries.getSeriesName());
            recent.put(DatabaseHelper.RECENT_IMAGE, itemSeries.getSeriesPoster());
            recent.put(DatabaseHelper.RECENT_TYPE, "series");
            databaseHelper.addRecent(DatabaseHelper.TABLE_RECENT, recent, null);
        }
    }
}