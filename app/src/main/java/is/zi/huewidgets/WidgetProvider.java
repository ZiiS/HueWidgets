// Copyright © 2019 Barnaby Shearer <b@Zi.iS>

package is.zi.huewidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import is.zi.NonNull;


public class WidgetProvider extends AppWidgetProvider {


    public static void doUpdate(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        Intent intent = new Intent(context, PopupActivity.class);
        intent.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                appWidgetId
        );
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        views.setOnClickPendingIntent(R.id.widget, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, @NonNull int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            WidgetProvider.doUpdate(context, appWidgetManager, appWidgetId);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onDeleted(@NonNull Context context, @NonNull int[] appWidgetIds) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(
                context.getPackageName(),
                0
        ).edit();
        for (int appWidgetId : appWidgetIds) {
            prefs.remove("widget_" + appWidgetId);
        }
        prefs.apply();
    }

}
