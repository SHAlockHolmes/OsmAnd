package net.osmand.plus.dashboard;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.gpx.GPXFile;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchHistoryFragment;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.search.QuickSearchHelper;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.fragments.TrackSelectSegmentBottomSheet;
import net.osmand.plus.track.fragments.TrackSelectSegmentBottomSheet.OnSegmentSelectedListener;
import net.osmand.plus.track.helpers.GpxNavigationHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis on 24.11.2014.
 */
public class DashRecentsFragment extends DashLocationFragment implements OnSegmentSelectedListener {

	public static final String TAG = "DASH_RECENTS_FRAGMENT";
	public static final int TITLE_ID = R.string.shared_string_history;

	private static final String ROW_NUMBER_TAG = TAG + "_row_number";
	private static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashboardOnMap.DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return TITLE_ID;
				}
			};
	static final DashFragmentData FRAGMENT_DATA = new DashFragmentData(
			TAG, DashRecentsFragment.class, SHOULD_SHOW_FUNCTION, 80, ROW_NUMBER_TAG);

	@Override
	public View initView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);

		((TextView) view.findViewById(R.id.fav_text)).setText(TITLE_ID);
		(view.findViewById(R.id.show_all)).setOnClickListener(v -> {
			closeDashboard();
			if (getActivity() instanceof MapActivity) {
				MapActivity mapActivity = (MapActivity) getActivity();
				mapActivity.showQuickSearch(MapActivity.ShowQuickSearchMode.NEW, false);
			}
		});

		return view;
	}

	@Override
	public void onOpenDash() {
		setupRecents();
	}

	public void setupRecents() {
		View mainView = getView();
		if (mainView == null) {
			return;
		}
		OsmandApplication app = requireMyApplication();

		SearchHistoryHelper helper = SearchHistoryHelper.getInstance(app);
		List<HistoryEntry> historyEntries = helper.getHistoryEntries(true);
		boolean historyEnabled = app.getSettings().SEARCH_HISTORY.get();

		if (Algorithms.isEmpty(historyEntries) || !historyEnabled) {
			AndroidUiHelper.updateVisibility(mainView.findViewById(R.id.main_fav), false);
			return;
		} else {
			AndroidUiHelper.updateVisibility(mainView.findViewById(R.id.main_fav), true);
		}

		LinearLayout recents = mainView.findViewById(R.id.items);
		recents.removeAllViews();
		DashboardOnMap.handleNumberOfRows(historyEntries, app.getSettings(), ROW_NUMBER_TAG);
		LatLon loc = getDefaultLocation();

		for (HistoryEntry historyEntry : historyEntries) {
			LayoutInflater inflater = requireActivity().getLayoutInflater();
			View itemView = inflater.inflate(R.layout.search_history_list_item, null, false);
			SearchHistoryFragment.updateHistoryItem(historyEntry, itemView, loc, app);
			itemView.findViewById(R.id.divider).setVisibility(View.VISIBLE);
			itemView.findViewById(R.id.navigate_to).setVisibility(View.VISIBLE);

			Drawable navigationIcon = app.getUIUtilities().getThemedIcon(R.drawable.ic_action_gdirections_dark);
			((ImageView) itemView.findViewById(R.id.navigate_to)).setImageDrawable(navigationIcon);

			itemView.findViewById(R.id.navigate_to).setOnClickListener(v -> runNavigation(historyEntry));
			itemView.setOnClickListener(v -> showContextMenu(historyEntry));
			setupDistanceAndDirection(historyEntry, itemView);

			recents.addView(itemView);
		}
	}

	private void runNavigation(@NonNull HistoryEntry historyEntry) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		OsmandApplication app = mapActivity.getMyApplication();

		PointDescription pointDescription = historyEntry.getName();
		if (pointDescription.isGpxFile()) {
			File file = new File(app.getAppPath(GPX_INDEX_DIR), pointDescription.getName());
			GpxSelectionHelper.getGpxFile(mapActivity, file, true, gpxFile -> {
				MapActivity activity = getMapActivity();
				if (activity != null) {
					navigateGpxFile(gpxFile, activity);
				}
				return false;
			});
		} else {
			DirectionsDialogs.directionsToDialogAndLaunchMap(mapActivity, historyEntry.getLat(),
					historyEntry.getLon(), historyEntry.getName());
		}
	}

	private void navigateGpxFile(@NonNull GPXFile gpxFile, @NonNull MapActivity mapActivity) {
		if (TrackSelectSegmentBottomSheet.shouldShowForGpxFile(gpxFile)) {
			FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
			TrackSelectSegmentBottomSheet.showInstance(fragmentManager, gpxFile, this);
		} else {
			GpxNavigationHelper.startNavigationForGpx(gpxFile, mapActivity);
			closeDashboard();
		}
	}

	private void showContextMenu(@NonNull HistoryEntry historyEntry) {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}
		OsmandApplication app = requireMyApplication();

		PointDescription pointDescription = historyEntry.getName();
		if (pointDescription.isGpxFile()) {
			File tracksDir = app.getAppPath(GPX_INDEX_DIR);
			String relativeGpxPath = pointDescription.getName();
			File gpxFile = new File(tracksDir, relativeGpxPath);
			if (gpxFile.isFile()) {
				SearchHistoryHelper.getInstance(app).addNewItemToHistory(0, 0, pointDescription);
				TrackMenuFragment.openTrack(activity, gpxFile, null);
				closeDashboard();
			}
		} else {
			app.getSettings().setMapLocationToShow(historyEntry.getLat(), historyEntry.getLon(),
					15, historyEntry.getName(), true, historyEntry);
			MapActivity.launchMapActivityMoveToTop(getActivity());
		}
	}

	private void setupDistanceAndDirection(@NonNull HistoryEntry historyEntry, @NonNull View itemView) {
		MapActivity mapActivity = requireMapActivity();
		OsmandApplication app = mapActivity.getMyApplication();

		ImageView directionArrow = itemView.findViewById(R.id.direction);
		TextView distanceText = itemView.findViewById(R.id.distance);
		PointDescription pointDescription = historyEntry.getName();
		if (pointDescription.isGpxFile()) {
			String relativeGpxPath = pointDescription.getName();
			File file = new File(app.getAppPath(GPX_INDEX_DIR), relativeGpxPath);
			GpxSelectionHelper.getGpxFile(mapActivity, file, false, gpxFile -> {
				QuadRect gpxRect = gpxFile.getRect();
				LatLon latLon = new LatLon(gpxRect.centerY(), gpxRect.centerX());
				DashLocationView locationView = new DashLocationView(directionArrow, distanceText, latLon);
				distances.add(locationView);
				updateAllWidgets();
				return true;
			});
		} else {
			LatLon latLon = new LatLon(historyEntry.getLat(), historyEntry.getLon());
			DashLocationView locationView = new DashLocationView(directionArrow, distanceText, latLon);
			distances.add(locationView);
		}
	}

	@Override
	public void onSegmentSelect(@NonNull GPXFile gpxFile, int selectedSegment) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			GpxNavigationHelper.startNavigationForSegment(gpxFile, selectedSegment, mapActivity);
			closeDashboard();
		}
	}

	@Override
	public void onRouteSelected(@NonNull GPXFile gpxFile, int selectedRoute) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			GpxNavigationHelper.startNavigationForRoute(gpxFile, selectedRoute, mapActivity);
			closeDashboard();
		}
	}
}