package net.osmand.plus.configmap.tracks;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_NO_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_NO_VISIBLE_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_RECENTLY_VISIBLE_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_SORT_TRACKS;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SelectedTracksHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final GpxSelectionHelper selectionHelper;

	private final Set<TrackItem> allTrackItems = new HashSet<>();
	private final Set<TrackItem> selectedTrackItems = new HashSet<>();
	private final Set<TrackItem> originalSelectedTrackItems = new HashSet<>();
	private final Set<TrackItem> recentlyVisibleTrackItem = new HashSet<>();
	private final Map<String, TrackTab> trackTabs = new LinkedHashMap<>();


	public SelectedTracksHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.selectionHelper = app.getSelectedGpxHelper();
	}

	@NonNull
	public Map<String, TrackTab> getTrackTabs() {
		return trackTabs;
	}

	@NonNull
	public Set<TrackItem> getSelectedTracks() {
		return new HashSet<>(selectedTrackItems);
	}

	@NonNull
	public Set<TrackItem> getRecentlyVisibleTracks() {
		return new HashSet<>(recentlyVisibleTrackItem);
	}

	public boolean hasItemsToApply() {
		return !Algorithms.objectEquals(selectedTrackItems, originalSelectedTrackItems);
	}

	public void ontrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {
		if (selected) {
			selectedTrackItems.addAll(trackItems);
		} else {
			selectedTrackItems.removeAll(trackItems);
		}
	}

	public void updateTrackItems(@NonNull List<TrackItem> trackItems) {
		allTrackItems.clear();
		allTrackItems.addAll(trackItems);

		Map<String, TrackTab> trackTabs = new LinkedHashMap<>();
		for (TrackItem item : trackItems) {
			addLocalIndexInfo(trackTabs, item);
		}
		updateTrackTabs(trackTabs, trackItems);
	}

	private void updateTrackTabs(@NonNull Map<String, TrackTab> folderTabs, @NonNull List<TrackItem> trackItems) {
		processVisibleTracks();
		processRecentlyVisibleTracks();

		trackTabs.clear();
		trackTabs.put(TrackTabType.ON_MAP.name(), getTracksOnMapTab());
		trackTabs.put(TrackTabType.ALL.name(), getAllTracksTab());
		trackTabs.putAll(folderTabs);

		loadTabsSortModes();
		sortTrackTabs();
	}

	@NonNull
	private TrackTab getTracksOnMapTab() {
		TrackTab trackTab = new TrackTab(app.getString(R.string.shared_string_on_map), TrackTabType.ON_MAP);
		trackTab.items.addAll(getOnMapTabItems());
		return trackTab;
	}

	@NonNull
	private TrackTab getAllTracksTab() {
		TrackTab trackTab = new TrackTab(app.getString(R.string.shared_string_all), TrackTabType.ALL);
		trackTab.items.addAll(getAllTabItems());
		return trackTab;
	}

	@NonNull
	private List<Object> getOnMapTabItems() {
		List<Object> items = new ArrayList<>();
		items.add(TYPE_SORT_TRACKS);
		items.addAll(getVisibleItems());
		items.addAll(getRecentlyVisibleItems());
		return items;
	}

	@NonNull
	private List<Object> getAllTabItems() {
		List<Object> items = new ArrayList<>();
		items.add(TYPE_SORT_TRACKS);
		if (Algorithms.isEmpty(allTrackItems)) {
			items.add(TYPE_NO_TRACKS);
		} else {
			items.addAll(allTrackItems);
		}
		return items;
	}

	@NonNull
	private List<Object> getRecentlyVisibleItems() {
		List<Object> items = new ArrayList<>();
		if (!Algorithms.isEmpty(recentlyVisibleTrackItem)) {
			items.add(TYPE_RECENTLY_VISIBLE_TRACKS);
			items.addAll(recentlyVisibleTrackItem);
		}
		return items;
	}

	@NonNull
	private List<Object> getVisibleItems() {
		List<Object> items = new ArrayList<>();
		if (!Algorithms.isEmpty(originalSelectedTrackItems)) {
			items.addAll(originalSelectedTrackItems);
		} else if (Algorithms.isEmpty(allTrackItems)) {
			items.add(TYPE_NO_TRACKS);
		} else {
			items.add(TYPE_NO_VISIBLE_TRACKS);
		}
		return items;
	}

	private void processRecentlyVisibleTracks() {
		LatLon latLon = app.getMapViewTrackingUtilities().getDefaultLocation();
		for (GPXFile gpxFile : selectionHelper.getSelectedGpxFilesBackUp().keySet()) {
			SelectedGpxFile selectedGpxFile = selectionHelper.getSelectedFileByPath(gpxFile.path);
			if (selectedGpxFile == null) {
				TrackItem trackItem = new TrackItem(new File(gpxFile.path));
				recentlyVisibleTrackItem.add(trackItem);
			}
		}
	}

	private void processVisibleTracks() {
		if (selectionHelper.isAnyGpxFileSelected()) {
			for (TrackItem info : allTrackItems) {
				SelectedGpxFile selectedGpxFile = selectionHelper.getSelectedFileByPath(info.getPath());
				if (selectedGpxFile != null) {
					selectedTrackItems.add(info);
					originalSelectedTrackItems.add(info);
				}
			}
		}
	}

	@NonNull
	public TrackTab addLocalIndexInfo(@NonNull Map<String, TrackTab> trackTabs, @NonNull TrackItem item) {
		File dir = item.getFile().getParentFile();
		String name = dir.getName();
		if (GPX_INDEX_DIR.equals(name + "/")) {
			name = app.getString(R.string.shared_string_tracks);
		}
		TrackTab trackTab = trackTabs.get(name);
		if (trackTab == null) {
			trackTab = new TrackTab(name, TrackTabType.FOLDER);
			trackTab.items.add(TYPE_SORT_TRACKS);
			trackTabs.put(name, trackTab);
		}
		trackTab.items.add(item);
		return trackTab;
	}

	public void addTrackItem(@NonNull TrackItem item) {
		allTrackItems.add(item);
		TrackTab folderTab = addLocalIndexInfo(trackTabs, item);

		TrackTab onMapTab = trackTabs.get(TrackTabType.ON_MAP.name());
		if (onMapTab != null) {
			onMapTab.items.clear();
			onMapTab.items.addAll(getOnMapTabItems());
		}
		TrackTab allTab = trackTabs.get(TrackTabType.ALL.name());
		if (allTab != null) {
			allTab.items.clear();
			allTab.items.addAll(getAllTabItems());
		}
		sortTrackTab(folderTab);
		sortTrackTab(onMapTab);
		sortTrackTab(allTab);
	}

	public void saveTracksVisibility() {
		selectionHelper.clearAllGpxFilesToShow(true);

		Map<String, Boolean> selectedFileNames = new HashMap<>();
		for (TrackItem trackItem : selectedTrackItems) {
			selectedFileNames.put(trackItem.getPath(), true);
		}
		selectionHelper.runSelection(selectedFileNames, null);
	}

	private void sortTrackTabs() {
		for (TrackTab trackTab : trackTabs.values()) {
			sortTrackTab(trackTab);
		}
	}

	public void sortTrackTab(@NonNull TrackTab trackTab) {
		LatLon latLon = app.getMapViewTrackingUtilities().getDefaultLocation();
		if (trackTab.type == TrackTabType.ON_MAP) {
			List<Object> visibleItems = getVisibleItems();
			List<Object> recentlyVisibleItems = getRecentlyVisibleItems();

			TracksComparator comparator = new TracksComparator(trackTab, latLon);
			Collections.sort(visibleItems, comparator);
			Collections.sort(recentlyVisibleItems, comparator);

			trackTab.items.clear();
			trackTab.items.add(TYPE_SORT_TRACKS);
			trackTab.items.addAll(visibleItems);
			trackTab.items.addAll(recentlyVisibleItems);
		} else {
			Collections.sort(trackTab.items, new TracksComparator(trackTab, latLon));
		}
	}

	public void loadTabsSortModes() {
		List<String> sortModes = settings.TRACKS_TABS_SORT_MODES.getStringsList();
		if (!Algorithms.isEmpty(sortModes)) {
			for (String sortMode : sortModes) {
				String[] tabSortMode = sortMode.split(",,");
				if (tabSortMode != null && tabSortMode.length == 2) {
					TrackTab trackTab = trackTabs.get(tabSortMode[0]);
					if (trackTab != null) {
						trackTab.setSortMode(TracksSortMode.getByValue(tabSortMode[1]));
					}
				}
			}
		}
	}

	public void saveTabsSortModes() {
		List<String> sortTypes = new ArrayList<>();
		for (TrackTab trackTab : trackTabs.values()) {
			String name = trackTab.getTypeName();
			String sortType = trackTab.getSortMode().name();
			sortTypes.add(name + ",," + sortType);
		}
		settings.TRACKS_TABS_SORT_MODES.setStringsList(sortTypes);
	}
}