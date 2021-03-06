package com.example.zark.baking;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.IdlingResource;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.zark.baking.models.Recipe;
import com.example.zark.baking.testing.SimpleIdlingResource;
import com.example.zark.baking.utilities.MyNetworkUtils;
import com.example.zark.baking.utilities.RecipeBus;
import com.example.zark.baking.widgets.WidgetProvider;
import com.google.gson.Gson;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

/**
 * Created by Andrew Osborne 2017
 * <p>
 * Default recipe thumbnail Created by Onlyyouqj - Freepik.com
 */

public class MainActivity extends AppCompatActivity
        implements RecipeCardsFragment.OnRecipeSelectionListener {

    public static Bus sRecipeBus;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_RECIPE_CARDS_FRAGMENT = "RecipeCardsFragment";
    private static final String KEY_SELECTED_RECIPE = "selectedRecipe";

    private Recipe mSelectedRecipe;
    private RecipeCardsFragment mRecipeCardsFragment;
    private TextView mEmptyState;

    @VisibleForTesting
    @NonNull
    public IdlingResource getIdlingResource() {
        RecipeCardsFragment recipeCardsFragment = new RecipeCardsFragment();
        IdlingResource idlingResource = recipeCardsFragment.getIdlingResource();
        return idlingResource;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEmptyState = findViewById(R.id.empty_view);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        // Event Bus for sending Recipe objects
        sRecipeBus = RecipeBus.getBus();

        if (!MyNetworkUtils.doesNetworkConnectionExist(this)) {
            showEmptyState();
            return;
        } else {
            hideEmptyState();
        }

        if (savedInstanceState == null) {
            mRecipeCardsFragment = new RecipeCardsFragment();
            displayRecipeCardFragment();
        } else {
            mRecipeCardsFragment = (RecipeCardsFragment) getSupportFragmentManager()
                    .findFragmentByTag(KEY_RECIPE_CARDS_FRAGMENT);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public void displayRecipeCardFragment() {

        getSupportFragmentManager().beginTransaction().replace(
                R.id.frag_container, mRecipeCardsFragment).commit();
    }

    public void showEmptyState() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().show();
        }
        mEmptyState.setVisibility(View.VISIBLE);
    }

    public void hideEmptyState() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        mEmptyState.setVisibility(View.GONE);
    }

    /**
     * From RecipeCardsFragment
     */
    @Override
    public void onRecipeSelection() {

        // First store this information in SharedPreferences for retrieval by the widget
        Gson gson = new Gson();
        String recipeString = gson.toJson(mSelectedRecipe);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_SELECTED_RECIPE, recipeString);
        editor.apply();

        // Notify the widget that the data has changed
        ComponentName widget = new ComponentName(getApplication(), WidgetProvider.class);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(widget);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list);
        WidgetProvider widgetProvider = new WidgetProvider();
        widgetProvider.updateWidgetTitles(getApplication(), ids);

        // Then open the recipe itself
        Intent launchDetailActivityIntent = new Intent(this, RecipeDetailActivity.class);
        launchDetailActivityIntent.putExtra(KEY_SELECTED_RECIPE, mSelectedRecipe);

        startActivity(launchDetailActivityIntent);
    }

    /**
     * Receives the Recipe object corresponding to the user-selected recipe CardView.
     */
    @Subscribe
    public void getRecipeObjectFromAdapter(Recipe selectedRecipe) {
        mSelectedRecipe = selectedRecipe;
    }

    @Override
    protected void onStart() {
        super.onStart();
        sRecipeBus.register(this);

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (sRecipeBus != null) {
            sRecipeBus.unregister(this);
        }
    }
}
