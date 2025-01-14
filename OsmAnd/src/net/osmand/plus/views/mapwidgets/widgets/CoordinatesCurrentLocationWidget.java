package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.COORDINATES_CURRENT_LOCATION;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider.GPSInfo;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

public class CoordinatesCurrentLocationWidget extends CoordinatesBaseWidget {

	@Nullable
	@Override
	public OsmandPreference<Boolean> getWidgetVisibilityPref() {
		return settings.SHOW_CURRENT_LOCATION_COORDINATES_WIDGET;
	}

	public CoordinatesCurrentLocationWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, COORDINATES_CURRENT_LOCATION);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		boolean visible = mapActivity.getWidgetsVisibilityHelper().shouldShowTopCurrentLocationCoordinatesWidget();
		updateVisibility(visible);
		if (visible) {
			Location lastKnownLocation = locationProvider.getLastKnownLocation();
			if (lastKnownLocation == null) {
				showSearchingGpsMessage();
			} else {
				showFormattedCoordinates(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
			}
		}
	}

	private void showSearchingGpsMessage() {
		AndroidUiHelper.updateVisibility(firstIcon, false);
		AndroidUiHelper.updateVisibility(divider, false);
		AndroidUiHelper.updateVisibility(secondContainer, false);
		GPSInfo gpsInfo = locationProvider.getGPSInfo();
		String message = getString(R.string.searching_gps) + "… " + gpsInfo.usedSatellites + "/" + gpsInfo.foundSatellites;
		firstCoordinate.setText(message);
	}
}