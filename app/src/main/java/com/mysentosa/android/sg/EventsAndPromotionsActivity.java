package com.mysentosa.android.sg;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Response.Listener;
import com.flurry.android.FlurryAgent;
import com.google.gson.Gson;
import com.mysentosa.android.sg.adapter.EventAndPromotionListCursorAdapter;
import com.mysentosa.android.sg.adapter.EventAndPromotionListCursorAdapter.ViewHolder;
import com.mysentosa.android.sg.adapter.PromotionExclusiveAdapter;
import com.mysentosa.android.sg.asynctask.GetEventsPromotionsAsyncTask;
import com.mysentosa.android.sg.custom_dialog.LoginDialog;
import com.mysentosa.android.sg.fragments.SearchFragment;
import com.mysentosa.android.sg.models.GetMyClaimedDealsRequestModel;
import com.mysentosa.android.sg.models.Promotion;
import com.mysentosa.android.sg.provider.utils.SentosaDatabaseStructure.ContentURIs;
import com.mysentosa.android.sg.provider.utils.SentosaDatabaseStructure.EventsPromotionsBase;
import com.mysentosa.android.sg.provider.utils.SentosaDatabaseStructure.Queries;
import com.mysentosa.android.sg.request.GetMyClaimedDealsRequest;
import com.mysentosa.android.sg.request.GetPromotionExclusiveRequest;
import com.mysentosa.android.sg.utils.Const.FlurryStrings;
import com.mysentosa.android.sg.utils.SentosaUtils;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnItemClick;

public class EventsAndPromotionsActivity extends BaseActivity implements LoaderCallbacks<Cursor> {

    public static final String TAG = EventsAndPromotionsActivity.class.getSimpleName();

    public static final String CURRENT_TYPE = "CURRENT_TYPE";
    public static final int TYPE_EVENT = 0, TYPE_PROMOTION = 1;
    private int currentType = TYPE_EVENT;

    private EventAndPromotionListCursorAdapter eventPromotionsCursorAdapter;
    private PromotionExclusiveAdapter exclusiveAdapter;
    private ArrayList<Promotion> listIslanderEx;
    private boolean ischoosingGeneral = true;
    private int currentPage = 1;
    private ArrayList<Promotion> claimedListPromotion;

    private LoginDialog loginDialog;
    private boolean isLoadedIslanderEx = false;

    private Promotion pickedPromotion = null;
    private SearchFragment searchFragment;

    @InjectView(R.id.header_title) TextView headerTitle;
    @InjectView(R.id.header_search) ImageView headerSearch;
    @InjectView(R.id.search_frame) FrameLayout searchFrame;
    @InjectView(R.id.topbar) LinearLayout tabBar;
    @InjectView(R.id.list) ListView list;
    @InjectView(R.id.general) ImageView generalTab;
    @InjectView(R.id.islander_exclusive) ImageView islanderTab;
    @InjectView(R.id.pb_loading) ProgressBar progressBar;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.currentType = this.getIntent().getIntExtra(CURRENT_TYPE, TYPE_EVENT);
        if (currentType == -1)
            finish();
        int id = this.getIntent().getIntExtra(EventsAndPromotionsDetailActivity.ID, -1);

        if (id != -1) {
            Intent mIntent = new Intent(this, EventsAndPromotionsDetailActivity.class);
            mIntent.putExtra(EventsPromotionsBase.ID_COL, new Long(id));
            if (isEvent()) {
                FlurryAgent.logEvent(FlurryStrings.EventsListPage);
                mIntent.putExtra(EventsAndPromotionsDetailActivity.CURRENT_TYPE,
                        EventsAndPromotionsDetailActivity.TYPE_EVENT);
            } else {
                FlurryAgent.logEvent(FlurryStrings.PromotionsListPage);
                mIntent.putExtra(EventsAndPromotionsDetailActivity.CURRENT_TYPE,
                        EventsAndPromotionsDetailActivity.TYPE_PROMOTION);
            }
            this.startActivity(mIntent);
        }

        setContentView(R.layout.events_promo_list_screen);
        ButterKnife.inject(this);

        initializeViews();

        listIslanderEx = new ArrayList<Promotion>();
        claimedListPromotion = new ArrayList<Promotion>();
        new GetEventsPromotionsAsyncTask(this).execute();

    }

    private void initializeViews() {
        headerTitle.setText(isEvent() ? getString(R.string.events) : getString(R.string.deals));
        headerSearch.setVisibility(View.VISIBLE);
        searchFragment = (SearchFragment) getSupportFragmentManager().findFragmentById(R.id.search_fragment);

        SentosaUtils.addListViewFooter(this, list, true);

        eventPromotionsCursorAdapter = new EventAndPromotionListCursorAdapter(this, null);
        list.setAdapter(eventPromotionsCursorAdapter);

        getSupportLoaderManager().initLoader(0, null, this);

        progressBar.setVisibility(View.VISIBLE);

        if (currentType == TYPE_PROMOTION) {
            tabBar.setVisibility(LinearLayout.VISIBLE);
            list.setPadding(0, 5, 0, 0);
            changeStatusButton(generalTab.getId());
            loginDialog = new LoginDialog(this);
        } else {
            tabBar.setVisibility(LinearLayout.GONE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(this, (isEvent()) ? ContentURIs.EVENTS_URI : ContentURIs.PROMOTIONS_URI, null, Queries.EVENTS_PROMOTIONS_LIST_QUERY(isEvent()), null, null);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.i(TAG, "Load finish " + cursor.getCount());
        if (cursor.getCount() > 0) {
            progressBar.setVisibility(View.GONE);
        }
        eventPromotionsCursorAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        eventPromotionsCursorAdapter.swapCursor(null);
        list.invalidate();
    }

    @OnItemClick(R.id.list)
    public void clickDeals(View view, int position) {
        Intent intent = new Intent();
        if (ischoosingGeneral) {
            intent.setClass(this, EventsAndPromotionsDetailActivity.class);
            intent.putExtra(EventsPromotionsBase.ID_COL, ((ViewHolder) view.getTag()).id);
            if (isEvent()) {
                FlurryAgent.logEvent(FlurryStrings.EventDetailsEvents);
                intent.putExtra(EventsAndPromotionsDetailActivity.CURRENT_TYPE,
                        EventsAndPromotionsDetailActivity.TYPE_EVENT);
            } else {
                FlurryAgent.logEvent(FlurryStrings.PromotionsDetailsPromotions);
                intent.putExtra(EventsAndPromotionsDetailActivity.CURRENT_TYPE,
                        EventsAndPromotionsDetailActivity.TYPE_PROMOTION);
            }
            startActivity(intent);
        } else {
            if (SentosaUtils.isUserLogined(EventsAndPromotionsActivity.this)) {
                pickIslanderEx(listIslanderEx.get(position));
            } else {
                // show Login/Register Dialog
                pickedPromotion = listIslanderEx.get(position);
                loginDialog.show();
            }
        }
    }

    @OnClick({R.id.general, R.id.islander_exclusive})
    public void clickTab(View view) {
        switch (view.getId()) {
            case R.id.general:
                changeStatusButton(R.id.general);
                list.setAdapter(eventPromotionsCursorAdapter);
                break;

            case R.id.islander_exclusive:
                if (!isLoadedIslanderEx) {
                    getPromotionEx();
                } else {
                    changeStatusButton(R.id.islander_exclusive);
                    list.setAdapter(exclusiveAdapter);
                }
                break;
        }
    }

    @OnClick(R.id.header_search)
    public void openSearch() {
        if(searchFrame.getVisibility() == View.GONE) {
            searchFrame.setVisibility(View.VISIBLE);
        } else {
            closeSearch();
        }
    }

    @Override
    public void onBackPressed() {
        if(searchFrame.getVisibility() == View.VISIBLE) {
            closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    private void closeSearch() {
        searchFragment.clearSearch();
        searchFrame.setVisibility(View.GONE);
    }

    public boolean isEvent() {
        return currentType == TYPE_EVENT;
    }

    private void changeStatusButton(int chosenImageViewID) {
        switch (chosenImageViewID) {
            case R.id.general:
                generalTab.setImageResource(R.drawable.tab_general_left_orange);
                islanderTab.setImageResource(R.drawable.tab_islander_right_grey);
                ischoosingGeneral = true;
                break;

            case R.id.islander_exclusive:
                generalTab.setImageResource(R.drawable.tab_general_left_grey);
                islanderTab.setImageResource(R.drawable.tab_islander_right_orange);
                ischoosingGeneral = false;
                break;

            default:
                break;
        }
    }

    private void getPromotionEx() {
        mPBLoading.show();
        GetPromotionExclusiveRequest request = new GetPromotionExclusiveRequest(this, mResponseListener, mErrorListener);
        SentosaApplication.mRequestQueue.add(request);
    }

    Listener<ArrayList<Promotion>> mResponseListener = new Listener<ArrayList<Promotion>>() {
        @Override
        public void onResponse(ArrayList<Promotion> listResult) {
            if (listResult != null) {
                isLoadedIslanderEx = true;
                listIslanderEx = listResult;
                exclusiveAdapter = new PromotionExclusiveAdapter(EventsAndPromotionsActivity.this, listIslanderEx);
                list.setAdapter(exclusiveAdapter);
            } else {
                list.setAdapter(null);
            }

            changeStatusButton(R.id.islander_exclusive);
            dismissProgressDialog();
        }
    };

    private void getClaimedDeals() {
        mPBLoading.show();
        GetMyClaimedDealsRequest request = new GetMyClaimedDealsRequest(this, SentosaUtils.getMemberID(this), SentosaUtils.getAccessToken(this), currentPage, mResponseMyClaimedListener, mErrorListener);
        SentosaApplication.mRequestQueue.add(request);
    }

    Listener<GetMyClaimedDealsRequestModel> mResponseMyClaimedListener = new Listener<GetMyClaimedDealsRequestModel>() {

        @Override
        public void onResponse(GetMyClaimedDealsRequestModel result) {
            if (result != null) {
                claimedListPromotion.addAll(result.listPromotions);
            }

            if (result != null && currentPage < result.pageSize) {
                currentPage += 1;
                getClaimedDeals();
            } else {
                dismissProgressDialog();
                SentosaApplication.mClaimedDeals = claimedListPromotion;
                currentPage = 1;

                if (exclusiveAdapter != null) {
                    exclusiveAdapter.notifyDataSetChanged();
                }

                if (pickedPromotion != null) {
                    pickIslanderEx(pickedPromotion);
                    pickedPromotion = null;
                }
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (SentosaUtils.isUserLogined(EventsAndPromotionsActivity.this)) {
            clickTab(islanderTab);
        } else {
            clickTab(generalTab);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SentosaUtils.isUserLogined(EventsAndPromotionsActivity.this)) {
            getClaimedDeals();
        }
    }

    private void pickIslanderEx(Promotion promotion) {
        Intent intent = new Intent();
        if (promotion.isFreeRewardType() || promotion.isOnSiteType()) {
            intent.setClass(EventsAndPromotionsActivity.this, IslanderSpecialDealActivity.class);
            intent.putExtra(IslanderSpecialDealActivity.CURRENT_DEAL, new Gson().toJson(promotion));

        } else if (promotion.isDiscountType()) {
            intent.setClass(EventsAndPromotionsActivity.this, TicketsActivity.class);
            intent.putExtra(TicketsActivity.TICKET_TYPE, promotion.getDicountedTicketEntityType());
            intent.putExtra(TicketsActivity.START_FROM_DEAL_SCREEN, true);
            intent.putExtra(NavigationManagerActivity.ACTIVITY_TO_START_ID, promotion.getDiscountedTicketEntityId());

        } else {
            intent.setClass(EventsAndPromotionsActivity.this, EventsAndPromotionsDetailActivity.class);
            FlurryAgent.logEvent(FlurryStrings.PromotionsDetailsPromotions);
            intent.putExtra(EventsAndPromotionsDetailActivity.CURRENT_TYPE, EventsAndPromotionsDetailActivity.TYPE_PROMOTION);
            intent.putExtra(EventsPromotionsBase.ID_COL, promotion.getId());
            intent.putExtra(EventsAndPromotionsDetailActivity.ISLANDER_CLAIMED_DEAL, new Gson().toJson(promotion));
        }

        startActivity(intent);
    }
}