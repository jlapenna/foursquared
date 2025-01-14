/**
 * Copyright 2009 Joe LaPenna
 */

package com.joelapenna.foursquared.app;

import com.joelapenna.foursquare.error.FoursquareError;
import com.joelapenna.foursquare.error.FoursquareException;
import com.joelapenna.foursquare.types.Checkin;
import com.joelapenna.foursquare.types.Group;
import com.joelapenna.foursquared.Foursquared;
import com.joelapenna.foursquared.FoursquaredSettings;
import com.joelapenna.foursquared.appwidget.FriendsAppWidgetProvider;
import com.joelapenna.foursquared.error.LocationException;
import com.joelapenna.foursquared.location.LocationUtils;
import com.joelapenna.foursquared.util.Comparators;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;

/**
 * @author Joe LaPenna (joe@joelapenna.com)
 */
public class FoursquaredService extends Service {
    private static final String TAG = "FoursquaredService";
    private static final boolean DEBUG = FoursquaredSettings.DEBUG;

    @Override
    public void onStart(Intent intent, int startId) {
        if (DEBUG) Log.d(TAG, "onStart: " + intent.toString() + ", " + String.valueOf(startId));

        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            updateWidgets();
        }
        stopSelfResult(startId);
    }

    private void updateWidgets() {
        if (DEBUG) Log.d(TAG, "updateWidgets");
        Group<Checkin> checkins = null;
        Foursquared foursquared = ((Foursquared) getApplication());
        if (foursquared.isReady()) {
            if (DEBUG) Log.d(TAG, "User settings are ready, starting normal widget update.");
            try {
                checkins = foursquared.getFoursquare().checkins(
                        LocationUtils.createFoursquareLocation(foursquared.getLastKnownLocation()));
            } catch (Exception e) {
                if (DEBUG) Log.d(TAG, "Exception: Skipping widget update.", e);
                return;
            }
            Collections.sort(checkins, Comparators.getCheckinRecencyComparator());

            // Request the user photos for the checkins... At the moment, this
            // is async. It is
            // likely this means that the first update of the widget will never
            // show user photos
            // as the photos will still be downloading when the getInputStream
            // call is made in
            // SpecialDealsAppWidgetProvider.
            for (int i = 0; i < checkins.size(); i++) {
                Uri photoUri = Uri.parse((checkins.get(i)).getUser().getPhoto());
                if (!foursquared.getRemoteResourceManager().exists(photoUri)) {
                    foursquared.getRemoteResourceManager().request(photoUri);
                }
            }
        }

        AppWidgetManager am = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = am.getAppWidgetIds(new ComponentName(this,
                FriendsAppWidgetProvider.class));

        for (int i = 0; i < appWidgetIds.length; i++) {
            FriendsAppWidgetProvider.updateAppWidget((Context) this, foursquared
                    .getRemoteResourceManager(), am, appWidgetIds[i], checkins);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

}
